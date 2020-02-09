package com.atguigu.gmall.order.feign;

import com.atguigu.ums.api.GmallUmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author shkstart
 * @create 2020-01-14 18:16
 */
@FeignClient("ums-server")
public interface GmallUmsClient extends GmallUmsApi {
}
