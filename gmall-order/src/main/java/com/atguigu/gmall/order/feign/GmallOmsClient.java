package com.atguigu.gmall.order.feign;

import com.atguigu.gmall.oms.api.GmallOmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author shkstart
 * @create 2020-03-21 23:27
 */
@FeignClient("oms-server")
public interface GmallOmsClient extends GmallOmsApi {
}
