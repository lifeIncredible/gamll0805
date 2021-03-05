package com.atguigu.gmall.order.interceptor;

import com.atguigu.core.bean.UserInfo;
import com.atguigu.core.utils.CookieUtils;
import com.atguigu.core.utils.JwtUtils;
import com.atguigu.gmall.order.config.JwtProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author shkstart
 * @create 2020-01-24 16:47
 * 自定义拦截器用于获取登录下的userId以及未登录的userKey
 */
@Component
@EnableConfigurationProperties(JwtProperties.class)
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    /*
    jdk 1.2提供的ThreadLocal
        1.ThreadLocal是一种变量类型，我们称之为“线程局部变量”
        2.每个线程访问这种变量的时候都会创建该变量的副本，这个变量副本为线程私有
        3.ThreadLocal类型的变量一般用private static加以修饰
    */
    private static ThreadLocal<UserInfo> THREAD_LOCAL  = new ThreadLocal<>();


    public LoginInterceptor() {
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        UserInfo userInfo = new UserInfo();
        //若登录必定有token信息
        String token = CookieUtils.getCookieValue(request, jwtProperties.getCookieName());


       /*if (StringUtils.isEmpty(token)) {
            return  false;
        }*/

        //如果token不为空,就通过core工程的JwtUtils工具类的getInfoFromToken方法解析token信息
        try {
            Map<String, Object> infoFromToken = JwtUtils.getInfoFromToken(token, this.jwtProperties.getPublicKey());
            Long id = Long.valueOf(infoFromToken.get("id").toString());
            userInfo.setUserId(id);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*把userInfo传递给后续的业务
         * 演示了3种方案:
         *   1.把userKey 和 userID放在request域中，通过getAttrbuite()和setAttrbuite()方法传递
         *       缺点：controller层中每个方法都得加上HttpServletRequest参数
         *   2.定义一个静态变量,然后通过类名.set(userInfo) 传递
         *       缺点： 在高并发多线程环境下,会不安全有可能把不同用户的userKey和userId传递过去
         *   3.使用ThreadLocal多线程局部变量传值
         * */
        //把userInfo传递给后续的业务
        THREAD_LOCAL.set(userInfo);
        return true;
    }


    /**
     * 因为ThreadLocal变量是私有的,无法传递给后续的业务(controller/service),所以专门抽取了一个静态方法用于传递
     */
    public static UserInfo getUserInfo() {
        return THREAD_LOCAL.get();
    }



    /**
     * afterCompletion这个方法在 DispatcherServlet 完全处理完请求后被调用，可以在该方法中进行一些资源清理的操作
     *
     * 为什么在afteCompletion方法中remove掉ThreadLocal声明的局部变量?
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //1.防止内存泄漏
        //2.由于我们使用的线程池： 请求结束不代表线程结束
        THREAD_LOCAL.remove();
    }


}
