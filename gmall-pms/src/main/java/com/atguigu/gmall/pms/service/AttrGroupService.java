package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.GroupVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;

import java.util.List;


/**
 * 属性分组
 *
 * @author suxiaohu
 * @email lxf@atguigu.com
 * @date 2020-01-02 17:13:27
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageVo queryPage(QueryCondition params);

    PageVo querGroupByCidPage(QueryCondition queryCondition, Long catId);

    GroupVo queryGroupVoByGid(Long gid);


    List<GroupVo> queryGroupVoByCid(long cid);
}

