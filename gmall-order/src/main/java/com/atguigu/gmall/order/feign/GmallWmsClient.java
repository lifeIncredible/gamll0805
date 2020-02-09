package com.atguigu.gmall.order.feign;

import com.atguigu.gmall.wms.api.GmallWmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author shkstart
 * @create 2020-01-14 18:18
 */
@FeignClient("wms-server")
public interface GmallWmsClient extends GmallWmsApi {
}
