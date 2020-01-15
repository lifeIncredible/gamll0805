package com.atguigu.gmall.item.service;

import com.atguigu.gmall.item.vo.ItemVO;

/**
 * @author shkstart
 * @create 2020-01-14 18:11
 */
public interface ItemService {
    ItemVO queryItemVO(Long skuId);
}
