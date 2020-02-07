package com.atguigu.gmall.cart.feign;

import com.atguigu.gmall.sms.api.GmallSmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author shkstart
 * @create 2020-01-14 18:17
 */
@FeignClient("sms-server")
public interface GmallSmsClient extends GmallSmsApi {
}
