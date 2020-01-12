package com.atguigu.gmall.index.service.impl;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IndexServiceImpl implements IndexService {
    @Autowired
    private GmallPmsClient pmsClient;

    @Override
    public List<CategoryEntity> queryLvl1Categories() {

        Resp<List<CategoryEntity>> categoriesResp = pmsClient.queryCategoriesByLevelOrPid(1, null);
        List<CategoryEntity> categoryEntities = categoriesResp.getData();
        return categoryEntities;
    }

    @Override
    public List<CategoryVO> queryCategoriesWithSub(Long pid) {
        Resp<List<CategoryVO>> listResp = this.pmsClient.queryCategoriesWithSup(pid);
        return  listResp.getData();
    }
}
