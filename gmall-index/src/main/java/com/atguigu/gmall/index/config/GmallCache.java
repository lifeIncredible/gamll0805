package com.atguigu.gmall.index.config;

import java.lang.annotation.*;

/**
 * 自定义注解并在切面类中使用配置，若在其他方法位置加上该注解，切面类可通过此注解找到其方法
 * @author shkstart
 * @create 2020-01-13 21:12
 */
@Target({ ElementType.METHOD}) //注解作用的范围，方法上
@Retention(RetentionPolicy.RUNTIME) //是运行时生效
@Documented         //是否保存到文档
public @interface GmallCache {

   //自定义缓存的key值
    String value() default "";

    //自定义缓存的有效时间
    //单位是分钟
    int timeout() default 30;

    //防止雪崩，而设置随机值范围
    int bound() default 5;

    //自定义分布式锁的名称
    String name() default "lock";

}
