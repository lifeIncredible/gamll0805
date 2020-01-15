package com.atguigu.gmall.sms.service.impl;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.gmall.sms.dao.SkuBoundsDao;
import com.atguigu.gmall.sms.dao.SkuFullReductionDao;
import com.atguigu.gmall.sms.dao.SkuLadderDao;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;
import com.atguigu.gmall.sms.entity.SkuLadderEntity;
import com.atguigu.gmall.sms.service.SkuBoundsService;
import com.atguigu.gmall.sms.vo.ItemSaleVO;
import com.atguigu.gmall.sms.vo.SaleVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Service("skuBoundsService")
public class SkuBoundsServiceImpl extends ServiceImpl<SkuBoundsDao, SkuBoundsEntity> implements SkuBoundsService {
    @Autowired
    private SkuLadderDao ladderDao;

    @Autowired
    private SkuFullReductionDao reductionDao;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<SkuBoundsEntity> page = this.page(
                new Query<SkuBoundsEntity>().getPage(params),
                new QueryWrapper<SkuBoundsEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public void saveSales(SaleVo saleVo) {
        // skuBounds 积分
        SkuBoundsEntity skuBoundsEntity = new SkuBoundsEntity();
        BeanUtils.copyProperties(saleVo, skuBoundsEntity);
        List<String> works = saleVo.getWork();
        skuBoundsEntity.setWork(new Integer(works.get(0)) + new Integer(works.get(1)) * 2 + new Integer(works.get(2)) * 4 + new Integer(works.get(3)) * 8);
        this.save(skuBoundsEntity);

        // skuLadder打折
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        BeanUtils.copyProperties(saleVo, skuLadderEntity);
        skuLadderEntity.setAddOther(saleVo.getLadderAddOther());
        this.ladderDao.insert(skuLadderEntity);

        // FullReduction满减
        SkuFullReductionEntity reductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(saleVo, reductionEntity);
        reductionEntity.setAddOther(saleVo.getFullAddOther());
        this.reductionDao.insert(reductionEntity);
    }

    @Override
    public List<ItemSaleVO> queryItemSaleVOBySkuId(Long skuId) {

        List<ItemSaleVO> itemSaleVOS = new ArrayList<>();
        //根据skuId查询积分信息
        SkuBoundsEntity skuBoundsEntity = this.getOne(new QueryWrapper<SkuBoundsEntity>().eq("sku_id", skuId));
        if (skuBoundsEntity != null){
            ItemSaleVO itemSaleVO = new ItemSaleVO();
           itemSaleVOS.add(itemSaleVO);
           itemSaleVO.setDesc("积分");
           itemSaleVO.setType("赠送:"+skuBoundsEntity.getGrowBounds()+"成长积分,"+skuBoundsEntity.getBuyBounds()+"购物积分");
        }

        //根据skuid查询打折信息
        SkuLadderEntity skuLadderEntity = this.ladderDao.selectOne(new QueryWrapper<SkuLadderEntity>().eq("sku_id", skuId));
        if (skuBoundsEntity != null) {
            ItemSaleVO itemSaleVO = new ItemSaleVO();
            itemSaleVOS.add(itemSaleVO);
            itemSaleVO.setType("打折");
            itemSaleVO.setDesc("满"+skuLadderEntity.getFullCount()+"件，打"+skuLadderEntity.getDiscount().divide(new BigDecimal(10))+"折");
        }

        //根据skuid查询满减信息
        SkuFullReductionEntity skuFullReductionEntity = this.reductionDao.selectOne(new QueryWrapper<SkuFullReductionEntity>().eq("sku_id", skuId));
        if (skuFullReductionEntity != null) {
            ItemSaleVO itemSaleVO = new ItemSaleVO();
            itemSaleVOS.add(itemSaleVO);
            itemSaleVO.setDesc("满"+skuFullReductionEntity.getFullPrice()+"减"+skuFullReductionEntity.getReducePrice());
        }
        return itemSaleVOS;
    }

}