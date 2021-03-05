package com.atguigu.gmall.item.service.impl;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.item.vo.ItemVO;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVO;
import com.atguigu.gmall.sms.vo.ItemSaleVO;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author shkstart
 * @create 2020-01-14 18:11
 */
@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;


    /**
     * 此方法用于谷粒商城展示商品详情页。
     * 因为调用了商品管理微服务，库存管理微服务，营销管理微服务3个微服务
     * 来查询信息构成商品详情页数据，服务之间调用时间过长，所以加入了线程池和使用了异步编排技术
     * @param skuId
     * @return
     */
    @Override
    public ItemVO queryItemVO(Long skuId) {

        ItemVO itemVO = new ItemVO();
        itemVO.setSkuId(skuId);

        //根据sku的id查询sku
        CompletableFuture<SkuInfoEntity> skuCompletableFuture = CompletableFuture.supplyAsync(() -> {
            Resp<SkuInfoEntity> skuInfoEntityResp = pmsClient.querySkuById(skuId);
        SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
        if (skuInfoEntity == null) {
            return null;
        }
        itemVO.setWeight(skuInfoEntity.getWeight());
        itemVO.setSkuTitle(skuInfoEntity.getSkuTitle());
        itemVO.setSkuSubTitle(skuInfoEntity.getSkuSubtitle());
        itemVO.setPrice(skuInfoEntity.getPrice());
        return skuInfoEntity;
    }, threadPoolExecutor);


        //根据sku中的categoryId查询分类
        CompletableFuture<Void> categroyCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<CategoryEntity> categoryEntityResp = this.pmsClient.queryGategoryById(skuInfoEntity.getCatalogId());
            CategoryEntity categoryEntity = categoryEntityResp.getData();
            if (categoryEntity != null) {
                itemVO.setCategoryId(categoryEntity.getCatId());
                itemVO.setCategoryName(categoryEntity.getName());
            }
        }, threadPoolExecutor);


        //根据sku中的brandId查询品牌
        CompletableFuture<Void> brandCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<BrandEntity> brandEntityResp = this.pmsClient.queryBrandById(skuInfoEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResp.getData();
            if (brandEntity != null) {
                itemVO.setBrandId(brandEntity.getBrandId());
                itemVO.setBrandName(brandEntity.getName());
            }
        }, threadPoolExecutor);


        //根据sku中的spuId查询spu
        CompletableFuture<Void> spuCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<SpuInfoEntity> spuInfoEntityResp = this.pmsClient.querySpuById(skuInfoEntity.getSpuId());
            SpuInfoEntity spuInfoEntity = spuInfoEntityResp.getData();
            if (spuInfoEntity != null) {
                itemVO.setSpuId(spuInfoEntity.getId());
                itemVO.setSpuName(spuInfoEntity.getSpuName());
            }
        }, threadPoolExecutor);


        //根据sku的Id查询图片
        CompletableFuture<Void> imagesCompletableFuture = CompletableFuture.runAsync(() -> {
            Resp<List<SkuImagesEntity>> imagesResp = this.pmsClient.queryImagesBySkuId(skuId);
            List<SkuImagesEntity> skuImagesEntities = imagesResp.getData();
            itemVO.setImages(skuImagesEntities);
        }, threadPoolExecutor);


        //根据sku的id查询库存是否有货
        CompletableFuture<Void> stockCompletableFuture = CompletableFuture.runAsync(() -> {
            Resp<List<WareSkuEntity>> wareResp = this.wmsClient.queryWareSkuBySkuId(skuId);
            List<WareSkuEntity> wareSkuEntities = wareResp.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                itemVO.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
            }
        }, threadPoolExecutor);


        //根据skuId来查询营销信息(积分，打折，满减)
        CompletableFuture<Void> saleCompletableFuture = CompletableFuture.runAsync(() -> {
            Resp<List<ItemSaleVO>> itemSalesResp = this.smsClient.queryItemSaleVOBySkuId(skuId);
            List<ItemSaleVO> itemSaleVOS = itemSalesResp.getData();
            itemVO.setSales(itemSaleVOS);
        }, threadPoolExecutor);


        //根据sku中的spuId查询描述信息
        CompletableFuture<Void> descCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<SpuInfoDescEntity> spuInfoDescEntityResp = this.pmsClient.queryDescBySpuId(skuInfoEntity.getSpuId());
            SpuInfoDescEntity spuInfoDescEntity = spuInfoDescEntityResp.getData();
            if (spuInfoDescEntity != null && StringUtils.isNotBlank(spuInfoDescEntity.getDecript())) {
                String decript = spuInfoDescEntity.getDecript();
                itemVO.setDesc(Arrays.asList(StringUtils.split(spuInfoDescEntity.getDecript(), ",")));
            }
        }, threadPoolExecutor);


        //1.根据sku中的categoryId查询组
        //2.遍历组到中间表查询每个组的规格参数id
        //3.根据spuId和attrId查询规格参数名及值
        CompletableFuture<Void> groupCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<List<ItemGroupVO>> groupResp = this.pmsClient.queryItemGroupVOsByCidAndSpuId(skuInfoEntity.getCatalogId(), skuInfoEntity.getSpuId());
            List<ItemGroupVO> itemGroupVOS = groupResp.getData();
            itemVO.setGroupVOS(itemGroupVOS);
        }, threadPoolExecutor);


        //1.根据sku中的spuId查询skus
        //2.根据skus获取skuids
        //3.根据skuIds查询销售属性
        CompletableFuture<Void> attrCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {

            Resp<List<SkuSaleAttrValueEntity>> attrValuesResp = this.pmsClient.querySaleAttrValueBySpuId(skuInfoEntity.getSpuId());
            List<SkuSaleAttrValueEntity> saleAttrValueEntities = attrValuesResp.getData();
            itemVO.setSaleAttrValues(saleAttrValueEntities);
        }, threadPoolExecutor);


        //统一执行
        CompletableFuture.allOf(categroyCompletableFuture, brandCompletableFuture, spuCompletableFuture, imagesCompletableFuture, stockCompletableFuture
                , saleCompletableFuture, descCompletableFuture, groupCompletableFuture, attrCompletableFuture).join();

        return itemVO;
    }
}
