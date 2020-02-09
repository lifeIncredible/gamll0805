package com.atguigu.gmall.order.feign;

import com.atguigu.gmall.cart.api.GmallCartApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author shkstart
 * @create 2020-01-14 18:16
 */
@FeignClient("cart-server")
public interface GmallCartClient extends GmallCartApi {
}
