package com.atguigu.gmall.wms.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.dao.WareSkuDao;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author 苏晓虎
 * @create 2020-03-22 23:13
 * 此消息队列用于订单创建时异常应该立马释放库存
 */

@Component
public class StockListener {

    private static final String KEY_PREFIX ="wms:stock:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private WareSkuDao wareSkuDao;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "STOCK-UNLOCK-QUEUE"),
            exchange = @Exchange(value = "ORDER-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"stock.unlock","wms.dead"}

    ))
    public void unlock(String orderToken){
        String json = this.redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
        if (StringUtils.isEmpty(json)){
            return;
        }
        //反序列化锁定库存信息
        List<SkuLockVo> skuLockVos = JSON.parseArray(json, SkuLockVo.class);
        skuLockVos.forEach(skuLockVo -> {
        this.wareSkuDao.unLock(skuLockVo.getWareSkuId(),skuLockVo.getCount());
        //删除redis中的订单号,防止90秒后重复关单
        this.redisTemplate.delete(KEY_PREFIX + orderToken);
        });

    }


    /*@RabbitListener(queues = {"WMS-DEAD-QUEUE"})
    public void testListener(String msg){
        System.out.println("消费者拿到死信消息:"+msg);
    }*/

}
