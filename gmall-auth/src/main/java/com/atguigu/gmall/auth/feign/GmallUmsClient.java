package com.atguigu.gmall.auth.feign;

import com.atguigu.ums.api.GmallUmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author shkstart
 * @create 2020-01-22 18:17
 */
@FeignClient("ums-server")
public interface GmallUmsClient extends GmallUmsApi {
}
