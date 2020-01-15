package com.atguigu.gmall.pms.service;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.baomidou.mybatisplus.extension.service.IService;


/**
 * sku图片
 *
 * @author suxiaohu
 * @email lxf@atguigu.com
 * @date 2020-01-02 17:13:27
 */
public interface SkuImagesService extends IService<SkuImagesEntity> {

    PageVo queryPage(QueryCondition params);

}

