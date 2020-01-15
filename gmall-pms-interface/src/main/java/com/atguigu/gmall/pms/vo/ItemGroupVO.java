package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.ProductAttrValueEntity;
import lombok.Data;

import java.util.List;

/**
 * @author shkstart
 * @create 2020-01-14 13:56
 */
@Data
public class ItemGroupVO {

    private  Long id;
    private  String name;
    private List<ProductAttrValueEntity> baseAttrValues;

}

