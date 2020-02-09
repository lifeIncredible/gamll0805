package com.atguigu.gmall.order.vo;

import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author shkstart
 * @create 2020-02-07 22:30
 */
@Data
public class OrderItemVo {

    private Long skuId;
    private String skuTitle;
    private String image;
    private List<SkuSaleAttrValueEntity> saleAttrs; //销售属性
    private BigDecimal price;//价格必须实时从数据库中查询获取
    private Integer count;
    private Boolean store = false; //库存
    private List<ItemSaleVO> sales; //促销信息

    private BigDecimal weight; //重量
}
