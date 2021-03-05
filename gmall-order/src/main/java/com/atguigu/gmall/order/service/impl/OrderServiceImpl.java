package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.core.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVO;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.atguigu.ums.entity.MemberEntity;
import com.atguigu.ums.entity.MemberReceiveAddressEntity;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author shkstart
 * @create 2020-02-08 15:04
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallUmsClient umsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GmallCartClient cartClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallOmsClient omsClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private AmqpTemplate amqpTemplate;

    private static final String TOKEN_PREFIX = "order:token:";

    /**
     * 生成订单
     * @return
     */
    @Override
    public OrderConfirmVo confirm() {
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();

        //1.获取用户的登录信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();

        //获取用户地址信息(远程接口) gmall-ums用户管理微服务远程调用 通过用户id查询用户的收货地址
        CompletableFuture<Void> AddresssFuture = CompletableFuture.runAsync(() -> {
                        Resp<List<MemberReceiveAddressEntity>> listResp = this.umsClient.queryAddressesByUserId(userInfo.getUserId());
                        List<MemberReceiveAddressEntity> addressEntities = listResp.getData();
                        if (!CollectionUtils.isEmpty(addressEntities)){
                            orderConfirmVo.setAddress(addressEntities);
                        }
        }, threadPoolExecutor);



        /*
           2.获取订单详情列表(远程接口)
          要在订单确认页展示哪几条商品呢
          购物车中商品非常多，应该展示选中的商品
          那么就需要feign来调用cart服务接口
         */
        CompletableFuture<Void> itemFuture = CompletableFuture.supplyAsync(() -> {
            return cartClient.queryCheckedCarts(userInfo.getUserId());
        }).thenAcceptAsync(carts -> {
            List<OrderItemVo> orderItems = carts.stream().map(cart -> {
                Long skuId = cart.getSkuId();
                Integer count = cart.getCount();

                OrderItemVo orderItemVo = new OrderItemVo();
                orderItemVo.setCount(count);
                orderItemVo.setSkuId(skuId);

                //查询SKU相关信息
                CompletableFuture<Void> skuFuture = CompletableFuture.runAsync(() -> {
                    Resp<SkuInfoEntity> skuInfoEntityResp = this.pmsClient.querySkuById(skuId);
                    SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
                    if (skuInfoEntity != null) {
                        orderItemVo.setPrice(skuInfoEntity.getPrice());
                        orderItemVo.setImage(skuInfoEntity.getSkuDefaultImg());
                        orderItemVo.setSkuTitle(skuInfoEntity.getSkuTitle());
                        orderItemVo.setWeight(skuInfoEntity.getWeight());
                    }
                }, threadPoolExecutor);


                //通过商品id查询商品的库存相关信息
                CompletableFuture<Void> storeFuture = CompletableFuture.runAsync(() -> {
                    Resp<List<WareSkuEntity>> wareSkuResp = this.wmsClient.queryWareSkuBySkuId(skuId);
                    List<WareSkuEntity> wareSkuEntities = wareSkuResp.getData();
                    if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                        boolean store = wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0);
                        orderItemVo.setStore(store);
                    }
                }, threadPoolExecutor);

                //查询销售相关信息
                CompletableFuture<Void> saleAttrValueFuture = CompletableFuture.runAsync(() -> {
                    Resp<List<SkuSaleAttrValueEntity>> saleAttrResp = this.pmsClient.querySaleAttrValueBySkuId(skuId);
                    List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = saleAttrResp.getData();
                    if (!CollectionUtils.isEmpty(skuSaleAttrValueEntities)) {
                        orderItemVo.setSaleAttrs(skuSaleAttrValueEntities);
                    }
                }, threadPoolExecutor);

                //查询营销信息
                CompletableFuture<Void> salesFuture = CompletableFuture.runAsync(() -> {
                    Resp<List<ItemSaleVO>> saleResp = this.smsClient.queryItemSaleVOBySkuId(skuId);
                    List<ItemSaleVO> itemSaleVOS = saleResp.getData();
                    if (!CollectionUtils.isEmpty(itemSaleVOS)) {
                        orderItemVo.setSales(itemSaleVOS);
                    }
                }, threadPoolExecutor);

                CompletableFuture.allOf(skuFuture, storeFuture, saleAttrValueFuture, salesFuture).join();

                return orderItemVo;
            }).collect(Collectors.toList());
            orderConfirmVo.setOrderItems(orderItems);
        }, threadPoolExecutor);



        //获取用户积分信息(远程接口 ums)
        CompletableFuture<Void> boundsFuture = CompletableFuture.runAsync(() -> {
            Resp<MemberEntity> memberEntityResp = this.umsClient.queryMemberById(userInfo.getUserId());
            MemberEntity memberEntity = memberEntityResp.getData();
            if (memberEntity != null) {
                orderConfirmVo.setBounds(memberEntity.getIntegration());
            }
        }, threadPoolExecutor);

        //防止重复提交唯一标志
        //uuid、 缺点:可读性很差
        // redis incr命令返回一个自增长的数字  缺点： id长度不一致
        // 分布式id生成器(mybatis-plus已经提供)  64位
        CompletableFuture<Void> tokenFuture = CompletableFuture.runAsync(() -> {
            String orderToken = IdWorker.getTimeId();
            orderConfirmVo.setOrderToken(orderToken); //浏览器一份
            //保存redis一份
            this.redisTemplate.opsForValue().set(TOKEN_PREFIX + orderToken, orderToken, 3, TimeUnit.HOURS);
        }, threadPoolExecutor);

        CompletableFuture.allOf(AddresssFuture,itemFuture,boundsFuture,tokenFuture).join();
        return orderConfirmVo;
    }




    /**
     * 确认下单
     */
    @Transactional
    @Override
    public OrderEntity submit(OrderSubmitVo orderSubmitVo) {
//        1. 校验是否重复提交(是：提示   否：跳转到支付页面,创建订单)  使用lua脚本保证提交订单和删除redis中订单编号同时进行
        // 判断redis中有没有，有-说明第一次提交，放行并删除redis中的orderToken
        String orderToken = orderSubmitVo.getOrderToken();
//        String token = this.redisTemplate.opsForValue().get(TOKEN_PREFIX + orderSubmitVo.getOrderToken());
////        if (StringUtils.isEmpty(token)){
////            return;
////        }

        String script ="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(TOKEN_PREFIX + orderSubmitVo.getOrderToken()), orderToken);
        if (flag == 0){
            throw  new OrderException("请不要重复提交订单");
        }

//        2. 验价(总价格是否发生了变化)

        //获取页面提交的总价格
         BigDecimal totalPrice = orderSubmitVo.getTotalPrice();

        //获取数据库的实时价格
        List<OrderItemVo> items = orderSubmitVo.getItems();
        if (CollectionUtils.isEmpty(items)){
            throw  new OrderException("请勾选要购买的商品!");
        }
        BigDecimal currentPrice = items.stream().map(orderItemVo -> {
            Long skuId = orderItemVo.getSkuId();
            Resp<SkuInfoEntity> skuInfoEntityResp = this.pmsClient.querySkuById(skuId);
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
            if (skuInfoEntity != null) {
                return skuInfoEntity.getPrice().multiply(new BigDecimal(orderItemVo.getCount()));//获取了每个sku的实时价格 * 数量
            }
            return new BigDecimal(0);
        }).reduce((a, b) -> a.add(b)).get();

        //比较价格是否一致
        if (totalPrice.compareTo(currentPrice) != 0 ){
            throw  new OrderException("页面已过期，请刷新后再试!");
        }

//        3.验证库存并锁定库存 调用wms里新写的方法
        List<SkuLockVo> skuLockVos = items.stream().map(orderItemVo -> {

        SkuLockVo skuLockVo = new SkuLockVo();
        skuLockVo.setSkuId(orderItemVo.getSkuId());
        skuLockVo.setCount(orderItemVo.getCount());
        skuLockVo.setOrderToken(orderSubmitVo.getOrderToken());
        return skuLockVo;
    }).collect(Collectors.toList());
        Resp<List<SkuLockVo>> skuLockResp = this.wmsClient.checkAndLock(skuLockVos);
        // 创建订单之前发生异常应该在哪释放库存：(1) 这里释放库存，可能会没有机会执行（原因：远程wms锁库存虽然成功，但是feign远程调用时响应时出现了网络传输异常,order根本没收到）
        //应该在wms里面释放库存
        List<SkuLockVo> lockVos = skuLockResp.getData();
        if (!CollectionUtils.isEmpty(lockVos)) {
            throw new OrderException(JSON.toJSONString(lockVos));
        }

        // 4.创建订单(订单状态：未付款的状态)
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        OrderEntity  orderEntity = null;
        try {
            Resp<OrderEntity> orderEntityResp = this.omsClient.saveOrder(orderSubmitVo, userInfo.getUserId());
             orderEntity = orderEntityResp.getData();

            //用户超时30分钟未支付定时关单问题解析：如果在这里定时关单，可能订单创建成功，而没有正常响应

        } catch (Exception e) {
            e.printStackTrace();
            // 订单创建异常应该立马释放库存： feign（阻塞）  消息队列（异步）
            this.amqpTemplate.convertAndSend("ORDER-EXCHANGE", "stock.unlock",orderSubmitVo.getOrderToken());
            throw  new RuntimeException("订单保存失败，服务错误！");
        }

        //5.删除购物车中的响应记录
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", userInfo.getUserId());
            List<Long> skuIds = items.stream().map(orderItemVO -> orderItemVO.getSkuId()).collect(Collectors.toList());
            map.put("skuIds", JSON.toJSONString(skuIds));
            this.amqpTemplate.convertAndSend("ORDER-EXCHANGE", "cart.delete", map);
        } catch (AmqpException e) {
            e.printStackTrace();
        }

        return orderEntity;
    }



}
