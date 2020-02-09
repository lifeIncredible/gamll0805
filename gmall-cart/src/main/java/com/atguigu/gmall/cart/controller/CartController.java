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
     *
     * @return
     */
    @GetMapping
    public Resp<List<Cart>> queryCarts() {
        List<Cart> carts = this.catService.queryCarts();
        return Resp.ok(carts);
    }

    /**
     * 更新购物车商品数量
     *
     * @param cart
     * @return
     */
    @PostMapping("update")
    public Resp<Object> updateNum(@RequestBody Cart cart) {
        this.catService.updateNum(cart);
        return Resp.ok(null);
    }


    /**
     * 检查商品是否选中状态
     *
     * @param cart
     * @return
     */
    @PostMapping("check")
    public Resp<Object> check(@RequestBody Cart cart) {

        this.catService.check(cart);
        return Resp.ok(null);
    }


    /**
     * 删除购物车商品信息
     *
     * @param skuId
     * @return
     */
    @PostMapping("delete")
    public Resp<Object> delete(@RequestParam("skuId") Long skuId) {
        this.catService.delete(skuId);
        return Resp.ok(null);
    }


    /**
     * 被Order服务调用查询购物车选中的商品信息
     * @param userId
     * @return
     */
    @GetMapping("{userId}")
    public List<Cart> queryCheckedCarts(@PathVariable("userId")Long userId){
        // LoginInterceptor.getUserInfo()不可取,因为原先是浏览器的cookie中携带了token并获取userInfo，但是现在是被order服务调用的根本没有cookie
        return  this.catService.queryCheckedCarts(userId);
    }

}
