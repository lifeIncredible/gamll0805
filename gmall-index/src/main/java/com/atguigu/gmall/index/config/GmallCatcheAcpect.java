package com.atguigu.gmall.index.config;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
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
 * 此切面类用于加分布式锁
 * @author shkstart
 * @create 2020-01-13 21:19
 */

@Aspect  //切面注解
@Component
public class GmallCatcheAcpect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 环绕通知是所有通知类型中功能最为强大的，能够全面地控制连接点，甚至可以控制是否执行连接点。
     * 环绕通知必须满足四个条件：
     *          1.返回值必须是Object
     *          2.参数类型必须是ProceedingJoinPoint,它是 JoinPoint的子接口，允许控制何时执行，是否执行连接点。
     *          3.方法必须抛出Throwable异常
     *          4.调用ProceedingJoinPoint的proceed()方法来执行被代理的方法
     *
     *       环绕通知的方法需要返回目标方法执行之后的结果，即调用 joinPoint.proceed();的返回值，否则会出现空指针异常。
     */

   // @Around("execution(* com.atguigu.gmall.index.service.*.*(..))")不能这么写，因为并不是所有的service方法都要切面类加分布式锁
    @Around("@annotation(com.atguigu.gmall.index.config.GmallCache)")
    public Object around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable{

        //通过proceedingJoinPoint的方法获取目标类的方法引用
        MethodSignature signature = (MethodSignature)proceedingJoinPoint.getSignature();
        //获取目标方法
        Method method = signature.getMethod();
        //获取目标方法上的注解(需指定注解类型，因为方法上可能会有多个注解),之后就可以通过该注解对象获取注解内的属性值(注解对象.属性值--->gmallCache.timeout)
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
        //获取目标方法的返回值
        Class returnType = signature.getReturnType();
        //因为要把proceedingJoinPoint.getArgs()当做唯一标志与锁的名字拼接,但是返回值是一个Object[],toString方法没有可读性是一个地址，所以转成List集合
        List<Object> args = Arrays.asList(proceedingJoinPoint.getArgs());

        //1. 获取缓存中的数据
            //先获取目标方法上@GmallCache的属性前缀（自定义缓存key值）
        String prefix = gmallCache.value();
        String key = prefix + args ;
        //抽取成getCache方法，判断缓存中是否存在
        Object cache = this.getCache(key, returnType);
        if (cache != null) {
            return  cache;
        }

        //3.为空，加分布式锁
        String lockName = gmallCache.name();
        RLock fairLock = this.redissonClient.getFairLock(lockName + args);
        fairLock.lock();

        //........查询数据库放入缓存在indexServiceImpl的queryCategoriesWithSub（Long pid）中做的

        //4.判断缓存是否为空
        Object cache1 = this.getCache(key, returnType);
        if (cache1 != null) {
            fairLock.unlock();
            return cache1;
        }

        /*
          通过proceedingJoinPoint的proceed方法执行目标方法,参数需要args
          在方法之前就可以做前置通知
          方法之后就可以做后置通知
         */
        Object result = proceedingJoinPoint.proceed(proceedingJoinPoint.getArgs());

        //5.把数据放入缓存，并设置缓存随机过期时间,在@GmallCache中设置了默认为30分钟，这里在30分钟基础上对每个数据加上随机值,防止缓存的雪崩
        this.redisTemplate.opsForValue().set(key,JSON.toJSONString(result),gmallCache.timeout()+ new Random().nextInt(gmallCache.bound()), TimeUnit.MINUTES);

        //释放分布式锁
        fairLock.unlock();

        return  result;
    }


    /**
     * 获取缓存中数据
     * @param key
     * @param returnType
     * @return
     */
    private  Object getCache(String key,Class returnType){
        String jsonString = this.redisTemplate.opsForValue().get(key);

        //2.判断数据是否为空，不为空就转成对象返回数据
        if (StringUtils.isNotBlank(jsonString)) {
            return  JSON.parseObject(jsonString,returnType);
        }

        //如果没有就返回空
        return  null;
    }

}
