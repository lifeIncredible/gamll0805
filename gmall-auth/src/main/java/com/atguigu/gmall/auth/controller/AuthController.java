package com.atguigu.gmall.auth.controller;

import com.atguigu.core.bean.Resp;
import com.atguigu.core.utils.CookieUtils;
import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.service.AuthService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author shkstart
 * @create 2020-01-22 18:11
 */
@RestController
@RequestMapping("auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtProperties jwtProperties;

    /**
     *
     * 登录授权
     * @param userName 用户名
     * @param password 密码
     * @param request  请求
     * @param response  响应
     * @return
     */
    @PostMapping("accredit")
    public Resp<Object> accredit(
            @RequestParam("username")String  userName,
            @RequestParam("password")String password,
            HttpServletRequest request,
            HttpServletResponse response
    ){
         String token = this.authService.accredit(userName,password);

        //4. 放入cookie中
        if (StringUtils.isNotBlank(token)) {
            CookieUtils.setCookie(request,response,jwtProperties.getCookieName(),token,jwtProperties.getExpireTime() * 60);
        }

        return Resp.ok(null);
    }
}
