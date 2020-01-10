package com.atguigu.gmall.search;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValue;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.models.auth.In;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
class GmallSearchApplicationTests {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private GmallPmsClient gmallPmsClient;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private GmallWmsClient wmsClient;

    @Test
    void contextLoads() {
        this.restTemplate.createIndex(Goods.class);
        this.restTemplate.putMapping(Goods.class);
    }

    @Test
    void  importDataTest(){
        Long pageNum = 1L;
        Long pageSize = 100L;

        do {
            QueryCondition queryCondition = new QueryCondition();
            queryCondition.setPage(pageNum);
            queryCondition.setLimit(pageSize);

            //分页查询spu
            Resp<List<SpuInfoEntity>> listResp = this.gmallPmsClient.querySpuByPage(queryCondition);

            //判断spu是否为空
            List<SpuInfoEntity> spuInfoEntities = listResp.getData();
            if (CollectionUtils.isEmpty(spuInfoEntities)){
                return;
            }

            //遍历SPU，获取SKU导入ElasticSearch
            spuInfoEntities.forEach(spuInfoEntity -> {
                 Resp<List<SkuInfoEntity>> skuResp = this.gmallPmsClient.querySkuBySpuId(spuInfoEntity.getId());
                 List<SkuInfoEntity> skuInfoEntities = skuResp.getData();
                 if (!CollectionUtils.isEmpty(skuInfoEntities)){
                     List<Goods> goodsList = skuInfoEntities.stream().map(skuInfoEntity -> {
                         Goods goods = new Goods();

                         //查询库存信息
                         Resp<List<WareSkuEntity>> wareSkuResp = this.wmsClient.queryWareSkuBySkuId(skuInfoEntity.getSkuId());
                         List<WareSkuEntity> wareSkuEntities = wareSkuResp.getData();
                         if (!CollectionUtils.isEmpty(wareSkuEntities)){
                             goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock()>0));
                         }
                         goods.setSkuId(skuInfoEntity.getSkuId());
                         goods.setSale(10L);
                         goods.setPrice(skuInfoEntity.getPrice().doubleValue());
                         goods.setCreateTime(spuInfoEntity.getCreateTime());

                         //通过BrandId 查询分类信息为goods.setCategoryName(null);赋值
                         Resp<CategoryEntity> categoryEntityResp = this.gmallPmsClient.queryGategoryById(skuInfoEntity.getBrandId());
                         CategoryEntity categoryEntity = categoryEntityResp.getData();
                         if (categoryEntity!=null){
                             goods.setCategoryId(skuInfoEntity.getCatalogId());
                             goods.setCategoryName(categoryEntity.getName());
                         }

                         //通过品牌Id查询品牌信息，为goods.setBrandName(null);赋值
                         Resp<BrandEntity> brandEntityResp = this.gmallPmsClient.queryBrandById(skuInfoEntity.getBrandId());
                         BrandEntity brandEntity = brandEntityResp.getData();
                         if (brandEntity != null){
                             goods.setBrandId(skuInfoEntity.getBrandId());
                             goods.setBrandName(brandEntity.getName());
                         }


                         //通过spu的id 查询出销售属性
                         Resp<List<ProductAttrValueEntity>> attrValueResp = this.gmallPmsClient.querySearchAttrValue(spuInfoEntity.getId());
                         List<ProductAttrValueEntity> attrValueEntities = attrValueResp.getData();
                         List<SearchAttrValue> searchAttrValues = attrValueEntities.stream().map(attrValueEntity -> {
                             SearchAttrValue searchAttrValue = new SearchAttrValue();
                             searchAttrValue.setAttId(attrValueEntity.getId());
                             searchAttrValue.setAttrName(attrValueEntity.getAttrName());
                             searchAttrValue.setAttValue(attrValueEntity.getAttrValue());
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
            });


            pageSize = (long)spuInfoEntities.size();
            pageNum++;
        }while (pageSize==100);





    }

}
