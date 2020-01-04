package com.atguigu.gmallgate.way.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * CORS（跨域资源共享）解决浏览器的同源策略跨域问题
 *        跨域问题 是针对ajax的一种限制。
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(){

        //CORS配置对象
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("http://localhost:1000"); //允许跨域请求的路径
        config.addAllowedHeader("*");//允许请求头携带的头信息
        config.addAllowedMethod("*");//允许那些请求方法跨域访问
        config.setAllowCredentials(true);//是否允许携带cookie
        UrlBasedCorsConfigurationSource configurationSource = new UrlBasedCorsConfigurationSource();


        configurationSource.registerCorsConfiguration("/**",config);
        return  new CorsWebFilter(configurationSource);
    }
}
