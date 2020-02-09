package com.atguigu.gmall.order.vo;

import com.atguigu.ums.entity.MemberReceiveAddressEntity;
import lombok.Data;

import java.util.List;

/**
 * 订单页面数据
 * @author shkstart
 * @create 2020-02-07 22:27
 */
@Data
public class OrderConfirmVo {

    private List<MemberReceiveAddressEntity> address;

    //订单商品详细信息
    private List<OrderItemVo> orderItems;

    //积分信息
    private Integer bounds;

    //该字段是为了防止订单恶意重复提交，导致锁定库存其他用户无法购买
    private String orderToken;


}
