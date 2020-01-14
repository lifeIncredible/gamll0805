package com.atguigu.gmall.index.controller;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("index")
public class IndexController {

    @Autowired
    private IndexService indexService;


    @GetMapping("cates")
    public Resp<List<CategoryEntity>> queryLvl1Categories(){
     List<CategoryEntity> categoryEntities =  indexService.queryLvl1Categories();

     return  Resp.ok(categoryEntities);
    }

    @GetMapping("cates/{pid}")
    public Resp<List<CategoryVO>> queryCategoriesWithSub(@PathVariable("pid")Long pid){
      List<CategoryVO>  categoryVOs=  indexService.queryCategoriesWithSub(pid);
     return Resp.ok(categoryVOs);
    }

    /**
     * 测试本地锁
     * @return
     */
    @GetMapping("test/lock")
    public  Resp<Object> testLock(){
        this.indexService.testLock();
        return Resp.ok(null);
    }

    /**
     * 测试分布式锁
     * @return
     */
    @GetMapping("test/Dslock")
    public  Resp<Object> Dslock(){
        this.indexService.testDsLock();
        return Resp.ok(null);
    }
}
