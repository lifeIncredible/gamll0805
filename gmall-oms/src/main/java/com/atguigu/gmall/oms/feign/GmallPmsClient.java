package com.atguigu.gmall.oms.feign;

import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author shkstart
 * @create 2020-03-21 22:59
 */
@FeignClient("pms-server")
public interface GmallPmsClient extends GmallPmsApi {
}
