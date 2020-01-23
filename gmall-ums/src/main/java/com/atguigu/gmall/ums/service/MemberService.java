package com.atguigu.gmall.ums.service;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.baomidou.mybatisplus.extension.service.IService;


/**
 * 会员
 *
 * @author suxiaohu
 * @email lxf@atguigu.com
 * @date 2020-01-15 18:46:03
 */
public interface MemberService extends IService<MemberEntity> {

    PageVo queryPage(QueryCondition params);

    Boolean checkData(String data, Integer type);

    void register(MemberEntity memberEntity, String code);

    MemberEntity querUser(String username, String password);
}

