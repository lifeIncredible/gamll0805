package com.atguigu.gmall.ums.service.impl;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.gmall.ums.dao.MemberDao;
import com.atguigu.gmall.ums.service.MemberService;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.ums.exception.UmsException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    private StringRedisTemplate redisTemplate;


    /**
     * 此方法用于搭建ums服务后向ums_member表插入一条数据,验证服务是否正常运行
     * @param data
     * @param type
     * @return
     */
    @Override
    public Boolean checkData(String data, Integer type) {
        QueryWrapper<MemberEntity> wrapper = new QueryWrapper<>();
        switch (type) {
            case 1: wrapper.eq("username",data);break;
            case 2: wrapper.eq("mobile",data);break;
            case 3: wrapper.eq("email",data);break;
            default:
                return null;
        }

        return  this.count(wrapper) == 0;
    }



    /**
     * 用户注册功能
     * @param memberEntity
     * @param code
     */
    @Override
    public void register(MemberEntity memberEntity, String code) {

        // 1。校验验证码是否正确
        String redisCode = redisTemplate.opsForValue().get(memberEntity.getMobile());
        if (!StringUtils.equals(redisCode,code)){
            throw  new UmsException("用户验证码不正确!");
        }

        //2. 生成盐
        String salt = UUID.randomUUID().toString().substring(0,6);
        memberEntity.setSalt(salt);


        //3.加盐加密
        memberEntity.setPassword(DigestUtils.md5Hex( memberEntity.getPassword()+salt));


        // 4.保存用户信息
        memberEntity.setLevelId(1L); //默认级别
        memberEntity.setSourceType(1); //用户来源
        memberEntity.setIntegration(1000); //积分
        memberEntity.setGrowth(1000);   //成长值
        memberEntity.setStatus(1);  //账号启用状态
        memberEntity.setCreateTime(new Date());
        this.save(memberEntity);

        //5.删除验证码
        this.redisTemplate.delete(memberEntity.getMobile());
    }

    /**
     * 查询用户功能
     * @param username
     * @param password
     * @return
     */
    @Override
    public MemberEntity querUser(String username, String password) {

        //1.先根据用户名查询用户信息
        MemberEntity memberEntity = this.getOne(new QueryWrapper<MemberEntity>().eq("username", username));

        //2. 判断用户名是否存在
        if (memberEntity == null) {
            return memberEntity;
        }

        //3. 如果存在,获取盐对用户输入的密码进行加盐加密
        password = DigestUtils.md5Hex(password + memberEntity.getSalt());

        //4. 用户输入的密码加密之后和数据库中的密码进行比较
        if (!StringUtils.equals(password,memberEntity.getPassword())) {
            return null;
        }

        return memberEntity;
    }


    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageVo(page);
    }
}