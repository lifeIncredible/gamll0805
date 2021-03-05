package com.atguigu.gmall.cart.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * @author shkstart
 * @create 2020-02-07 21:11
 */
@Component
public class CartListener {

    @Autowired
    private GmallPmsClient pmsClient;

    private static final String PRICE_PREFIX = "cart:price:";

    private static final String KEY_PREFIX = "cart:item:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "CART-UPDATE-QUEUE",durable = "true"),
            exchange = @Exchange(value = "GMALL-PMS-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"item.update"}
    ))
    public void listener(Long spuId){

        Resp<List<SkuInfoEntity>> listResp = this.pmsClient.querySkuBySpuId(spuId);
        List<SkuInfoEntity> skuInfoEntites = listResp.getData();
        if (CollectionUtils.isEmpty(skuInfoEntites)) {
            skuInfoEntites.forEach(skuInfoEntity -> {
                this.redisTemplate.opsForValue().set(PRICE_PREFIX + skuInfoEntity.getSkuId(),skuInfoEntity.getPrice().toString());
            });
        }
    }









    /**
     * 删除购物车中的响应记录
     * @param map
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "CART-DELETE-QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER-EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"cart.delete"}
    ))
    public void deleteListener(Map<String, Object> map){
        try {
            Long userId = (Long)map.get("userId");
            Object skuIds = map.get("skuIds");
            if (skuIds == null) {
                return;
            }
            BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);

            List<String> ids = JSON.parseArray(skuIds.toString(), String.class);

            hashOps.delete(ids.toArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }








}
