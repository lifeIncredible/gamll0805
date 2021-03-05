package com.atguigu.gmall.oms.service;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.baomidou.mybatisplus.extension.service.IService;


/**
 * 订单
 *
 * @author suxiaohu
 * @email 1054810745@qq.com
 * @date 2020-03-11 21:47:51
 */
public interface OrderService extends IService<OrderEntity> {

    PageVo queryPage(QueryCondition params);

    //新增订单
    OrderEntity saveOrder(OrderSubmitVo submitVo, Long userId);

}

