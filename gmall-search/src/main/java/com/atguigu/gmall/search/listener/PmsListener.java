package com.atguigu.gmall.search.listener;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValue;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PmsListener {
    @Autowired
    private GmallPmsClient gmallPmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GoodsRepository goodsRepository;


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "GMALL-SEARCH-QUEUE", durable = "true"),
            exchange = @Exchange(value = "GMALL-PMS-EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"item.insert"}
    )
    )
    public void listener(Long spuId) {

        Resp<List<SkuInfoEntity>> skuResp = this.gmallPmsClient.querySkuBySpuId(spuId);
                List<SkuInfoEntity> skuInfoEntities = skuResp.getData();
                if (!CollectionUtils.isEmpty(skuInfoEntities)) {
                    List<Goods> goodsList = skuInfoEntities.stream().map(skuInfoEntity -> {
                        Goods goods = new Goods();

                //查询库存信息
                Resp<List<WareSkuEntity>> wareSkuResp = this.wmsClient.queryWareSkuBySkuId(skuInfoEntity.getSkuId());
                List<WareSkuEntity> wareSkuEntities = wareSkuResp.getData();
                if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                    goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
                }
                goods.setSkuId(skuInfoEntity.getSkuId());
                goods.setSale(10L);
                goods.setPrice(skuInfoEntity.getPrice().doubleValue());

                Resp<SpuInfoEntity> spuInfoEntityResp = this.gmallPmsClient.querySpuById(spuId);
                SpuInfoEntity spuInfoEntity = spuInfoEntityResp.getData();
                goods.setCreateTime(spuInfoEntity.getCreateTime());

                //通过BrandId 查询分类信息为goods.setCategoryName(null);赋值
                Resp<CategoryEntity> categoryEntityResp = this.gmallPmsClient.queryGategoryById(skuInfoEntity.getBrandId());
                CategoryEntity categoryEntity = categoryEntityResp.getData();
                if (categoryEntity != null) {
                    goods.setCategoryId(skuInfoEntity.getCatalogId());
                    goods.setCategoryName(categoryEntity.getName());
                }

                //通过品牌Id查询品牌信息，为goods.setBrandName(null);赋值
                Resp<BrandEntity> brandEntityResp = this.gmallPmsClient.queryBrandById(skuInfoEntity.getBrandId());
                BrandEntity brandEntity = brandEntityResp.getData();
                if (brandEntity != null) {
                    goods.setBrandId(skuInfoEntity.getBrandId());
                    goods.setBrandName(brandEntity.getName());
                }

                //通过spu的id 查询出销售属性
                Resp<List<ProductAttrValueEntity>> attrValueResp = this.gmallPmsClient.querySearchAttrValue(spuInfoEntity.getId());
                List<ProductAttrValueEntity> attrValueEntities = attrValueResp.getData();
                List<SearchAttrValue> searchAttrValues = attrValueEntities.stream().map(attrValueEntity -> {
                    SearchAttrValue searchAttrValue = new SearchAttrValue();
                    searchAttrValue.setAttrId(attrValueEntity.getId());
                    searchAttrValue.setAttrName(attrValueEntity.getAttrName());
                    searchAttrValue.setAttrValue(attrValueEntity.getAttrValue());
                    return searchAttrValue;
                }).collect(Collectors.toList());


                goods.setAttrs(searchAttrValues);
                goods.setDefaultImage(skuInfoEntity.getSkuDefaultImg());
                goods.setSkuTitle(skuInfoEntity.getSkuTitle());
                goods.setSkuSubTitle(skuInfoEntity.getSkuSubtitle());

                return goods;
            }).collect(Collectors.toList());
            this.goodsRepository.saveAll(goodsList);
        }
    }
}
