package com.atguigu.gmall.order.service;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;

/**
 * @author shkstart
 * @create 2020-02-08 15:03
 */
public interface OrderService {

    OrderConfirmVo confirm();

    OrderEntity submit(OrderSubmitVo orderSubmitVo);
}
