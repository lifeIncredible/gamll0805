package com.atguigu.gmall.wms.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


/**
 * @author 苏晓虎
 * @create 2020-03-23 0:45
 * 此配置类用于创建延迟队列和死信队列，因为 @RabbitListener注解无法创建
 * 解决锁定库存后，创建订单前，订单服务宕机导致库存一直被锁问题
 */
@Configuration
public class RabbitmqConfig {

    /**
     * 延时队列
     * 延时时间：1min
     * 死信路由: order-exchange
     * 死信routingKey: wms.dead
     * @return
     */
    @Bean("ttl-queue")
    public Queue ttlqueue(){
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", "ORDER-EXCHANGE");
        arguments.put("x-dead-letter-routing-key", "wms.dead");
        arguments.put("x-message-ttl", 90000);//单位是毫秒
       return new Queue("WMS-TTL-QUEUE",true,false,false,arguments);
    }




    /**
     * 延时队列绑定要order-exchange路由
     * @return
     */
    @Bean("ttl-binding")
    public Binding ttlBinding(){
        return new Binding("WMS-TTL-QUEUE", Binding.DestinationType.QUEUE ,"ORDER-EXCHANGE","wms.ttl",null);
    }


    /**
     * 死信队列
     * @return
     */
//    @Bean("dead-queue")
//    public Queue deadQueue(){
//
//        return new Queue("WMS-DEAD-QUEUE",true,false,false,null);
//    }




    /**
     * 死信队列绑定要order-exchange路由
     * @return
     */
//    @Bean("dead-binding")
//    public Binding deadBinding(){
//        return new Binding("WMS-DEAD-QUEUE", Binding.DestinationType.QUEUE ,"ORDER-EXCHANGE","wms.dead",null);
//    }

}
