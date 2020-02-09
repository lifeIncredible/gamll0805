package com.atguigu.gmall.order.config;

import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 此配置类是注册到springMVC中
 * 在SpringMVC中自定义拦截器需要编写xml配置文件
          <mvc:interceptors>
              <!-- 声明自定义拦截器 -->
              <bean id="firstHandlerInterceptor"
                    class="com.atguigu.springmvc.interceptors.FirstHandlerInterceptor">
              </bean>
          </mvc:interceptors>
 * 但是在springboot工程中没有xml文件，若要自定义拦截器、视图解析器等等需要实现WebMvcConfigurer接口
 * @author shkstart
 * @create 2020-01-26 20:49
 */
@Configuration
public class GmallWebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(loginInterceptor).addPathPatterns("/**");
    }
}
