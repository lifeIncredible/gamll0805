package com.atguigu.gmall.order.controller;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
     * 生成订单
     * @return
     */
    @GetMapping("confirm")
    public Resp<OrderConfirmVo> confirm(){
        /*只需要获取用户id就可以了，然后就可以从redis中获取
        选中状态为true的商品了*/
        OrderConfirmVo orderConfirmVo = this.orderService.confirm();
        return Resp.ok(orderConfirmVo);
    }

    /**
     * 提交订单
     * 结束之后，直接弹出一个支付页面（调用支付宝的支付接口）
     * @param orderSubmitVo
     * @return
     */
    @PostMapping("submit")
    public Resp<Object> submit(@RequestBody OrderSubmitVo orderSubmitVo){

        this.orderService.submit(orderSubmitVo);
        return  Resp.ok(null);
    }




    /**
     * 支付宝支付成功后的异步回调接口
     * 想让别人回调你的接口：
     *  1.自己的独立ip（电信公司：企业专线）
     *  2.买域名
     *  这里解决方案：内网穿透
     * @return
     */
    @PostMapping("pay/success")
    public Resp<Object> paySuccess(){

        return Resp.ok(null);
    }

}
