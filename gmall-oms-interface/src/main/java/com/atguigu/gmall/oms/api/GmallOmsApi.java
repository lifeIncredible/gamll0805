package com.atguigu.gmall.oms.api;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author shkstart
 * @create 2020-03-21 23:18
 */
public interface GmallOmsApi {

    /**
     * 保存订单
     * @param submitVo 订单确认页数据
     * @param userId 用户id
     * @return
     */
    @PostMapping("oms/order/{userId}")
    public Resp<OrderEntity> saveOrder(@RequestBody OrderSubmitVo submitVo, @PathVariable("userId") Long userId);
}
