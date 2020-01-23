package com.atguigu.gmallgate.way.filter;

import com.atguigu.core.utils.JwtUtils;
import com.atguigu.gmallgate.way.config.JwtProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author shkstart
 * @create 2020-01-23 15:41
 */
@Component
@EnableConfigurationProperties(JwtProperties.class)
public class AuthGetwayFilter implements GatewayFilter {

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // 1.获取JWT类型的token信息
        //因为token放在cookie中,所以必须先获取cookie，因为每次请求都会携带cookie所以必须先获取request
        MultiValueMap<String, HttpCookie> cookies = request.getCookies();
        if (CollectionUtils.isEmpty(cookies) || !cookies.containsKey(this.jwtProperties.getCookieName())) {
            //如果cookie为空或者cookie中不包含token信息 401 未认证
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            //结束
            return response.setComplete();
        }
        //获取token
        HttpCookie cookie = cookies.getFirst(this.jwtProperties.getCookieName());
        if (cookie == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        String token = cookie.getValue();
        // 2.如果token为空
        if (StringUtils.isEmpty(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        // 3.解析token信息
        try {
            //正常解析，放行
            JwtUtils.getInfoFromToken(token, this.jwtProperties.getPublicKey());
        } catch (Exception e) {
            e.printStackTrace();
            //如果解析异常直接拦截
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }


        return chain.filter(exchange);
    }
}
