package com.atguigu.gmall.order.controller;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author shkstart
 * @create 2020-02-08 15:00
 */
@RestController
@RequestMapping("order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 生成订单并提交
     * @return
     */
    @GetMapping("confirm")
    public Resp<OrderConfirmVo> confirm(){
        /*只需要获取用户id就可以了，然后就可以从redis中获取
        选中状态为true的商品了*/
        OrderConfirmVo orderConfirmVo = this.orderService.confirm();
        return Resp.ok(orderConfirmVo);
    }
}
