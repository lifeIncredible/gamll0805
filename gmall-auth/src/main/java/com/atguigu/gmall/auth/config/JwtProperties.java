package com.atguigu.gmall.auth.config;

import com.atguigu.core.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * @author shkstart
 * @create 2020-01-22 16:17
 */
@ConfigurationProperties(prefix = "jwt.token")
@Data
public class JwtProperties {

    private  String pubKeyPath;
    private  String priKeyPath;
    private  String secret;
    private  String cookieName;
    private  Integer expireTime;

    private PublicKey publicKey;
    private PrivateKey privateKey;

    /**
     * @PostConstruct该注解被用来修饰一个非静态的void（）方法。
     * @PostConstruct修饰的方法会在服务器加载Servlet的时候运行，并且只会被服务器执行一次。
     * @PostConstruct在构造函数之后执行，init（）方法之前执行。
     */
    @PostConstruct
    public void init() {
        try {
            File pubFile = new File(pubKeyPath);
            File priFile = new File(priKeyPath);
            if (!pubFile.exists()||!priFile.exists()) {
                RsaUtils.generateKey(pubKeyPath,priKeyPath,secret);
            }
            publicKey = RsaUtils.getPublicKey(pubKeyPath);
            privateKey =RsaUtils.getPrivateKey(priKeyPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
