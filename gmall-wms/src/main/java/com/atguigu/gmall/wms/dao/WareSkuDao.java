package com.atguigu.gmall.wms.dao;

import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 * 
 * @author suxiaohu
 * @email lxf@atguigu.com
 * @date 2020-01-02 20:55:30
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

     List<WareSkuEntity> check(@Param("skuId") Long skuId, @Param("count") Integer count);

     int Lock(@Param("wareskuId") Long wareSkuId, @Param("count") Integer count);

     int unLock(@Param("wareskuId") Long wareSkuId, @Param("count") Integer count);
}
