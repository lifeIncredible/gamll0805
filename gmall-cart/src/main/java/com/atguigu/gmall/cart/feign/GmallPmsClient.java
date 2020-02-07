package com.atguigu.gmall.cart.feign;

import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author shkstart
 * @create 2020-01-14 18:16
 */
@FeignClient("pms-server")
public interface GmallPmsClient extends GmallPmsApi {
}
