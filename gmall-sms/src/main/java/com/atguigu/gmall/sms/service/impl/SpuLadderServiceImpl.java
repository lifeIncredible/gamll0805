package com.atguigu.gmall.sms.service.impl;

import com.atguigu.gmall.sms.dao.SpuLadderDao;
import com.atguigu.gmall.sms.entity.SpuLadderEntity;
import com.atguigu.gmall.sms.service.SpuLadderService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;


@Service("spuLadderService")
public class SpuLadderServiceImpl extends ServiceImpl<SpuLadderDao, SpuLadderEntity> implements SpuLadderService {

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<SpuLadderEntity> page = this.page(
                new Query<SpuLadderEntity>().getPage(params),
                new QueryWrapper<SpuLadderEntity>()
        );

        return new PageVo(page);
    }

}