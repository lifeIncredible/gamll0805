package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.cart.pojo.Cart;

import java.util.List;

/**
 * @author shkstart
 * @create 2020-01-26 22:21
 */
public interface CartService {

    void addCart(Cart cart);

    List<Cart> queryCarts();

    void updateNum(Cart cart);

    void check(Cart cart);

    void delete(Long skuId);

    List<Cart> queryCheckedCarts(Long userId);


}
