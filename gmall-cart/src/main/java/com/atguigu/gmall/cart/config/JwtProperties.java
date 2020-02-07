package com.atguigu.gmall.cart.config;

import com.atguigu.core.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.security.PublicKey;

/**
 * @author shkstart
 * @create 2020-01-22 16:17
 */
@ConfigurationProperties(prefix = "jwt.token")
@Data
public class JwtProperties {

    private  String pubKeyPath;
    private  String cookieName;
    private String userKey;
    private PublicKey publicKey;
    private Integer expireTime;


    @PostConstruct
    public void init() {
        try {
            publicKey = RsaUtils.getPublicKey(pubKeyPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
