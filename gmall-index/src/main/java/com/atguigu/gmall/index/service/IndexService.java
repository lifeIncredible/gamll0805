package com.atguigu.gmall.index.service;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVO;

import java.util.List;

public interface IndexService {
    List<CategoryEntity> queryLvl1Categories();


    List<CategoryVO> queryCategoriesWithSub(Long pid);

    void testLock();

    void testDsLock();
}
