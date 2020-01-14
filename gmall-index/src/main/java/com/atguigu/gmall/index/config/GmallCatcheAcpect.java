package com.atguigu.gmall.index.config;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author shkstart
 * @create 2020-01-13 21:19
 */

@Aspect
@Component
public class GmallCatcheAcpect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 环绕通知必须满足四个条件：
     *          1.返回值必须是Object
     *          2.形参必须是ProceedingJoinPoint
     *          3.方法必须抛出Throwable异常
     *          4.调用ProceedingJoinPoint的proceed()方法来执行被代理的方法
     *
     *       环绕通知的方法需要返回目标方法执行之后的结果，即调用 joinPoint.proceed();的返回值，否则会出现空指针异常。
     */

    public Object around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable{

        MethodSignature signature =(MethodSignature)proceedingJoinPoint.getSignature();
        Method method = signature.getMethod();
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
        Class returnType = signature.getReturnType();
        List<Object> args = Arrays.asList(proceedingJoinPoint.getArgs());
        //1. 获取缓存中的数据
        String prefix = gmallCache.value();
        String key = prefix + args ;
        Object cache = this.getCache(key, returnType);
        if (cache != null) {
            return  cache;
        }

        //3.为空，加分布式锁
        String lockName = gmallCache.name();
        RLock fairLock = this.redissonClient.getFairLock(lockName + args);
        fairLock.lock();

        //4.判断缓存是否为空
        Object cache1 = this.getCache(key, returnType);
        if (cache1 != null) {
            fairLock.unlock();
            return cache1;
        }

        Object result = proceedingJoinPoint.proceed(proceedingJoinPoint.getArgs());

        //5.把数据放入缓存
        this.redisTemplate.opsForValue().set(key,JSON.toJSONString(result),gmallCache.timeout()+ new Random().nextInt(gmallCache.bound()), TimeUnit.MINUTES);

        //释放分布式锁
        fairLock.unlock();

        return  result;
    }



    private  Object getCache(String key,Class returnType){
        String jsonString = this.redisTemplate.opsForValue().get(key);

        //2.判断数据是否为空
        if (StringUtils.isNotBlank(jsonString)) {
            return  JSON.parseObject(jsonString,returnType);
        }
        return  null;
    }

}
