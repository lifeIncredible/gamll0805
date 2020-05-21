package com.atguigu.gmall.cart.pojo;

import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author shkstart
 * @create 2020-01-23 20:21
 */
@Data
public class Cart {

    private Long skuId;
    private String skuTitle;
    private String image;
    private List<SkuSaleAttrValueEntity> saleAttrs; //销售属性
    private BigDecimal price; //加入购物车时的价格
    private BigDecimal currentPrice; //当前价格
    private Integer count;
    private Boolean store = false; //库存
    private Boolean check; //选中状态
    private List<ItemSaleVO> sales; //促销信息

}