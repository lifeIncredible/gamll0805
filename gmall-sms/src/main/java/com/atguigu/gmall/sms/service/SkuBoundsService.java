package com.atguigu.gmall.sms.service;

import com.atguigu.gmall.sms.vo.ItemSaleVO;
import com.atguigu.gmall.sms.vo.SaleVo;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;

import java.util.List;


/**
 * 商品sku积分设置
 *
 * @author suxiaohu
 * @email lxf@atguigu.com
 * @date 2020-01-05 21:14:44
 */
public interface SkuBoundsService extends IService<SkuBoundsEntity> {

    PageVo queryPage(QueryCondition params);

    void saveSales(SaleVo saleVo);

    List<ItemSaleVO> queryItemSaleVOBySkuId(Long skuId);
}

