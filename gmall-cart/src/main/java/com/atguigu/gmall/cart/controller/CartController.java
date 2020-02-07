package com.atguigu.gmall.cart.controller;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author shkstart
 * @create 2020-01-26 20:29
 */
@RestController
@RequestMapping("cart")
public class CartController {

    @Autowired
    private CartService catService;


    /**
     * 此方法用于测试是否能拿到登录状态下的userId和未登录状态下的userkey
     *
     * @return
     */
    @GetMapping("test")
    public String test() {
        System.out.println(LoginInterceptor.getUserInfo());
        return "xxx";
    }


    /**
     * 添加商品到购物车
     *
     * @param cart
     * @return
     */
    @PostMapping
    public Resp<Object> addCart(@RequestBody Cart cart) {
        this.catService.addCart(cart);
        return Resp.ok(null);
    }


    /**
     * 查询购物车
     * @return
     */
    @GetMapping
    public Resp<List<Cart>> queryCarts(){
        List<Cart> carts =  this.catService.queryCarts();
        return Resp.ok(carts);
    }

}
