package com.atguigu.gmall.oms.dao;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author suxiaohu
 * @email 1054810745@qq.com
 * @date 2020-03-11 21:47:51
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {

    public int closeOrder(String orderToken);
}
