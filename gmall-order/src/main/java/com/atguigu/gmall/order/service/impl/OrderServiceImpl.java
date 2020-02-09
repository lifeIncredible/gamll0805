package com.atguigu.gmall.order.service.impl;

import com.atguigu.core.bean.Resp;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.order.vo.OrderItemVo;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVO;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.ums.entity.MemberEntity;
import com.atguigu.ums.entity.MemberReceiveAddressEntity;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
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
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    private static final String TOKEN_PREFIX = "order:token:";

    @Override
    public OrderConfirmVo confirm() {
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();

        //获取用户的登录信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();

        //获取用户地址信息(远程接口) ums方法通过用户id查询用户的收货地址
        CompletableFuture<Void> AddresssFuture = CompletableFuture.runAsync(() -> {
            Resp<List<MemberReceiveAddressEntity>> listResp = this.umsClient.queryAddressesByUserId(userInfo.getUserId());
            List<MemberReceiveAddressEntity> addressEntities = listResp.getData();
            orderConfirmVo.setAddress(addressEntities);
        }, threadPoolExecutor);



        /*
           获取订单详情列表(远程接口)
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


                //查询商品的库存相关信息
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
}
