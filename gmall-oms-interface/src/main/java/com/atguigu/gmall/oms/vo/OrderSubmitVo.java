package com.atguigu.gmall.oms.vo;

import com.atguigu.ums.entity.MemberReceiveAddressEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 *订单结算页数据
 * @author shkstart
 * @create 2020-02-10 20:11
 */
@Data
public class OrderSubmitVo {

    private String orderToken; //防止重复提交

    private MemberReceiveAddressEntity address;

    private Integer payType; //支付方式

    private String deliveryCompany; //物流公司(配送方式)

    private List<OrderItemVo> items; //送货清单

    private Integer bounds; //消费积分

    private BigDecimal totalPrice; //总价,验价

}
