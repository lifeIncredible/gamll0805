package com.atguigu.gmall.oms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.oms.dao.OrderDao;
import com.atguigu.gmall.oms.dao.OrderItemDao;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.atguigu.gmall.oms.feign.GmallPmsClient;
import com.atguigu.gmall.oms.service.OrderService;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SpuInfoEntity;
import com.atguigu.ums.entity.MemberReceiveAddressEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {


    @Autowired
    private OrderItemDao orderItemDao;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private AmqpTemplate amqpTemplate;


    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageVo(page);
    }


    /**
     * 新增订单
     *
     * @param submitVo 订单结算页数据
     * @param userId   用户id
     * @return
     */
    @Override
    public OrderEntity saveOrder(OrderSubmitVo submitVo, Long userId) {

        //新增主表(订单表 oms-order)
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(submitVo.getOrderToken()); //订单编号
        orderEntity.setTotalAmount(submitVo.getTotalPrice()); //总金额
        orderEntity.setPayType(submitVo.getPayType());//支付方式
        orderEntity.setSourceType(0);//订单来源[0->PC订单；1->app订单]
        orderEntity.setDeliveryCompany(submitVo.getDeliveryCompany());//'物流公司(配送方式)',
        orderEntity.setCreateTime(new Date()); //创建时间
        orderEntity.setModifyTime(null);//修改时间 //TODO
        orderEntity.setStatus(0);//'订单状态【0->待付款；1->待发货；2->已发货；3->已完成；4->已关闭；5->无效订单】',
        orderEntity.setDeleteStatus(0); //'删除状态【0->未删除；1->已删除】',
        //orderEntity.setGrowth(); //通过购买商品的积分优惠信息来设置
        orderEntity.setMemberId(userId); //用户id
        MemberReceiveAddressEntity address = submitVo.getAddress();
        orderEntity.setReceiverCity(address.getCity()); //城市
        orderEntity.setReceiverDetailAddress(address.getDetailAddress());//详细地址
        orderEntity.setReceiverName(address.getName()); //收货人姓名
        orderEntity.setReceiverPhone(address.getPhone());//收货人电话
        orderEntity.setReceiverPostCode(address.getPostCode());//邮编
        orderEntity.setReceiverProvince(address.getProvince());//省份/直辖市
        orderEntity.setReceiverRegion(address.getRegion());//区
        //orderEntity.setMemberUsername(); //根据id查询用户名
        // orderEntity.setAutoConfirmDay();//自动确认时间

        boolean flag = this.save(orderEntity);

        //判断主表是否为空，若不为空再新增子表
        if (flag) {
            //新增子表（订单详情表 oms-order-item）
            List<OrderItemVo> items = submitVo.getItems();

            if (!CollectionUtils.isEmpty(items)) {
                items.forEach(orderItemVo -> {
                    OrderItemEntity itemEntity = new OrderItemEntity();
                    itemEntity.setOrderSn(submitVo.getOrderToken());
                    itemEntity.setOrderId(orderEntity.getId());
                    //先查询sku信息
                    Resp<SkuInfoEntity> skuInfoEntityResp = this.pmsClient.querySkuById(orderItemVo.getSkuId());
                    SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
                    if (skuInfoEntity != null) {
                        itemEntity.setSkuId(orderItemVo.getSkuId());
                        itemEntity.setSkuQuantity(orderItemVo.getCount());
                        itemEntity.setSkuPic(orderItemVo.getImage());
                        itemEntity.setSkuName(orderItemVo.getSkuTitle());
                        itemEntity.setSkuAttrsVals(JSON.toJSONString(orderItemVo.getSaleAttrs()));
                        itemEntity.setSkuPrice(skuInfoEntity.getPrice());

                        //从sku中获取spu的id，根据spu的id查询spu,设置spu的信息
                        Long spuId = skuInfoEntity.getSpuId();
                        Resp<SpuInfoEntity> spuInfoEntityResp = this.pmsClient.querySpuById(spuId);
                        SpuInfoEntity spuInfoEntity = spuInfoEntityResp.getData();
                        if (spuInfoEntity != null) {
                            itemEntity.setSpuId(spuId);
                            itemEntity.setSpuName(spuInfoEntity.getSpuName());
                            itemEntity.setSpuBrand(spuInfoEntity.getBrandId().toString());
                            itemEntity.setCategoryId(spuInfoEntity.getCatalogId());
                        }
                        //查询优惠信息，设置优惠信息
                    }

                    orderItemDao.insert(itemEntity);
                });
            }
        }

        //订单创建完成之后，用户超时未支付正常定时关单
        this.amqpTemplate.convertAndSend("ORDER-EXCHANGE", "order.ttl", submitVo.getOrderToken());
        return orderEntity;
    }

}