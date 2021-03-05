package com.atguigu.gmall.wms.vo;

import lombok.Data;

/**
 * 验库存并锁定库存
 * @author shkstart
 * @create 2020-03-06 17:11
 */
@Data
public class SkuLockVo {
    private String orderToken;
    private Long skuId;
    private Integer count;
    private Boolean lock = false; //锁定状态，true 验库存并锁定库存成功 , false 失败
    private Long wareSkuId; //如果锁定成功的情形下，需要记录锁定的仓库id
}
