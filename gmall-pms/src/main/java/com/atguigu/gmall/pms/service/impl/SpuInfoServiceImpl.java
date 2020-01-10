package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.dao.SkuInfoDao;
import com.atguigu.gmall.pms.dao.SpuInfoDescDao;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.service.*;
import com.atguigu.gmall.pms.vo.BaseAttrValueVo;
import com.atguigu.gmall.pms.vo.SkuInfoVo;
import com.atguigu.gmall.pms.vo.SpuInfoVo;
import com.atguigu.gmall.sms.vo.SaleVo;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.pms.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {
    @Autowired
    private SpuInfoDescDao descDao;

    @Autowired
    private ProductAttrValueService attrValueService;


    @Autowired
    private SkuInfoDao skuInfoDao;

    @Autowired
    private SkuImagesService imagesService;

    @Autowired
    private SkuSaleAttrValueService saleAttrValueService;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private SpuInfoDescService spuInfoDescService;



    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public PageVo querySpuInfo(QueryCondition condition, Long catId) {

        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();

        //如果分类id 不为0，要根据分类id查，否则查全部
        if (catId !=0 ){
            wrapper.eq("catalog_id",catId);
        }

        //关键字查询
        String key = condition.getKey();
        if (StringUtils.isNotBlank(key)){
           wrapper.and(t->t.eq("id",key).or().like("spu_name",key));
        }


        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(condition),
                wrapper
        );

        return new PageVo(page);
    }

    @GlobalTransactional
    @Override
    public void bigSave(SpuInfoVo spuInfoVO) {
        //1.保存spu相关信息
        //1.1 spuInfo
        Long spuId = saveSpuInfo(spuInfoVO);

        // 1.2. spuInfoDesc spu描述信息
        spuInfoDescService.saveSpuInfoDesc(spuInfoVO, spuId);

        // 1.3. 基础属性相关信息
        saveBaseAttrsValue(spuInfoVO, spuId);


        // 2. sku相关信息
        saveSkuAndSales(spuInfoVO, spuId);


    }

    private void saveSkuAndSales(SpuInfoVo spuInfoVO, Long spuId) {
        List<SkuInfoVo> skus = spuInfoVO.getSkus();
        if (CollectionUtils.isEmpty(skus)) {
            return ;
        }

        // 2.1. skuInfo
        skus.forEach(sku ->{
            SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
            BeanUtils.copyProperties(sku,skuInfoEntity);
            skuInfoEntity.setSpuId(spuId);
            List<String> images = sku.getImages();
            if (!CollectionUtils.isEmpty(images)){
                skuInfoEntity.setSkuDefaultImg(skuInfoEntity.getSkuDefaultImg() == null ? images.get(0):skuInfoEntity.getSkuDefaultImg());
            }


            skuInfoEntity.setSkuCode(UUID.randomUUID().toString());
            skuInfoEntity.setCatalogId(spuInfoVO.getCatalogId());
            skuInfoEntity.setBrandId(spuInfoVO.getBrandId());
            this.skuInfoDao.insert(skuInfoEntity);
            Long skuId = skuInfoEntity.getSkuId();

            // 2.2. skuInfoImages
            if (!CollectionUtils.isEmpty(images)){
                List<SkuImagesEntity> skuImagesEntities = images.stream().map(image -> {
                    SkuImagesEntity imagesEntity = new SkuImagesEntity();
                    imagesEntity.setSkuId(skuId);
                    imagesEntity.setImgUrl(image);
                    imagesEntity.setImgSort(0);
                    imagesEntity.setDefaultImg(StringUtils.equals(image, skuInfoEntity.getSkuDefaultImg()) ? 1 : 0);
                    return imagesEntity;
                }).collect(Collectors.toList());
                imagesService.saveBatch(skuImagesEntities);
            }

            // 2.3. skuSaleAttrValue
            List<SkuSaleAttrValueEntity> saleAttrs = sku.getSaleAttrs();
            if (!CollectionUtils.isEmpty(saleAttrs)) {
                saleAttrs.forEach(skuSaleAttrValueEntity -> {
                    skuSaleAttrValueEntity.setSkuId(skuId);
                    skuSaleAttrValueEntity.setAttrSort(0);
                });
                saleAttrValueService.saveBatch(saleAttrs);
            }


            // 3. 营销相关信息
            SaleVo saleVo = new SaleVo();
            BeanUtils.copyProperties(sku, saleVo);
            saleVo.setSkuId(skuId);
            this.smsClient.saveSales(saleVo);
        });
    }

    private void saveBaseAttrsValue(SpuInfoVo spuInfoVO, Long spuId) {
        List<BaseAttrValueVo> baseAttrs = spuInfoVO.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)) {
            List<ProductAttrValueEntity> attrValues = baseAttrs.stream().map(baseAttrValueVo -> {
                ProductAttrValueEntity attrValueEntity = new ProductAttrValueEntity();
                BeanUtils.copyProperties(baseAttrValueVo, attrValueEntity);
                attrValueEntity.setSpuId(spuId);
                attrValueEntity.setAttrSort(0);
                attrValueEntity.setQuickShow(0);
                return attrValueEntity;
            }).collect(Collectors.toList());
            attrValueService.saveBatch(attrValues);
        }
    }



    private Long saveSpuInfo(SpuInfoVo spuInfoVO) {
        spuInfoVO.setCreateTime(new Date());
        spuInfoVO.setUodateTime(spuInfoVO.getCreateTime());
        this.save(spuInfoVO);
        return spuInfoVO.getId();
    }

}