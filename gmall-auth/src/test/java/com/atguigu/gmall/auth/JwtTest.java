package com.atguigu.gmall.auth;

import com.atguigu.core.utils.JwtUtils;
import com.atguigu.core.utils.RsaUtils;
import org.junit.Before;
import org.junit.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

/**
 * @author shkstart
 * @create 2020-01-22 13:19
 */
public class JwtTest {
    private static final String pubKeyPath = "E:\\Idea\\rsa\\rsa.pub";

    private static final String priKeyPath = "E:\\Idea\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "234");
    }

    //before方法的特点：会在每次test方法执行之前执行
    @Before
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 1);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJuYW1lIjoiZmVuZ2ppZSIsImlkIjozLCJleHAiOjE1Nzk2OTc1MzJ9.j8q2hBtDon4kDEEBnN-Qg-8SkuSwgEycbRN5bJ1L2DMI7gx3SDDERoi670-vPi_VCkJRInJJzsl2TbFEU_vCIWsoDZn3cLFP_u8L-_jyPRTrEe2Vc8nNASeohPJ6d9-0lD4SUFexG2ujUEOMIr9_Q2rzV9FUDtQspprOFUVzlf4LjQDwPBK-Co1PijOUOskyTQESCks4BPIeLAct-uUJTwSi6GIFyEtmZ52kfgI394PNzwn8sL-6Opgoe_qclGM5qw5iY7g3lUhScxo7WEEnOTxAnEnKLEJcj_EqWj5cPVm5C29gp1uMw2Xe_HWho-i2M4rX4TnIyc3r1FcJW5eoUg";
        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}
