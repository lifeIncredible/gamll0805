package com.atguigu.gmallgate.way.config;

import com.atguigu.gmallgate.way.filter.AuthGetwayFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

/**
 * @author shkstart
 * @create 2020-01-23 15:58
 */
@Component
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory {

    @Autowired
    private AuthGetwayFilter authGetwayFilter;

    @Override
    public GatewayFilter apply(Object config) {
        return authGetwayFilter;
    }
}
