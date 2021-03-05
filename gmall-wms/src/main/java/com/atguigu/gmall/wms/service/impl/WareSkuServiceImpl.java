package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.gmall.wms.dao.WareSkuDao;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.service.WareSkuService;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private WareSkuDao wareSkuDao;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AmqpTemplate amqpTemplate;

    private static final String KEY_PREFIX ="wms:stock:";


    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageVo(page);
    }



    /**
     * 点击去结算验库存并锁库存,库存不满足订单需求就解锁库存,防止订单创建成功前服务器宕机并且库存已锁设置了定时任务关单
     * @param skuLockVos 商品清单的商品
     * @return
     */
    @Override
    public List<SkuLockVo> checkAndLock(List<SkuLockVo> skuLockVos) {

        // 1.先判断传递的数据是否为空
        if (CollectionUtils.isEmpty(skuLockVos)){
            return null;
        }
        // 2.遍历清单集合，验库存并锁库存
        skuLockVos.forEach(skuLockVo -> {
            this.checkLock(skuLockVo);
        });

        // 3. 锁定结果集中是否包含锁定失败的商品(如果任何一个商品锁定失败，已经锁定成功的商品应该回滚)
        if(skuLockVos.stream().anyMatch(skuLockVo -> skuLockVo.getLock() == false)){
            //获取已经锁定成功的商品，解锁库存
            skuLockVos.stream().filter(skuLockVo ->
                skuLockVo.getLock()).forEach(skuLockVo -> {
                    //解锁库存
            this.wareSkuDao.unLock(skuLockVo.getWareSkuId(),skuLockVo.getCount());
            });

            return  skuLockVos; //锁定失败的时候就直接返回一个skuLockVos,里面有锁定成功的也有锁定失败的
        }

        //把库存的锁定信息保存到Redis中去，方便获取锁定库存的信息
        String orderToken = skuLockVos.get(0).getOrderToken();
        this.redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, JSON.toJSONString(skuLockVos));


        /*   面试题：创建订单之前服务器宕机解决方案:
                无论有没有创建成功订单都要定时任务,释放库存
                创建订单之前服务器宕机而设置的定时关单时间(35min) > 正常未支付而关单的时间(30min)
                rabbitmq的消息ttl和死信Exchange结合[延时队列 + 死信队列]（推荐）

                ps: 订单创建成功之后立马发送消息给延时队列【延时队列: 不能有消费者，设置消息的生存时间是30min】
                     30分钟之后变成死信，通过【死信交换机】转发给【死信队列】。
                     消费者监听【死信队列】，消费者拿到消息之后，关单并解锁库存
        */

        this.amqpTemplate.convertAndSend("ORDER-EXCHANGE","wms.ttl",orderToken);


        //锁定成功的时候不需要返回了
        return null;

    }



    /**
     * 由于步骤繁琐，所以提取成一个方法,被上面的checkAndLock（）方法调用
     * 验库存(查)，锁库存(修改)实现过程
     * @param skuLockVo
     */
    private void checkLock(SkuLockVo skuLockVo){
        RLock fairLock = this.redissonClient.getFairLock("lock" + skuLockVo.getSkuId());
        fairLock.lock();

            //验库存
             //首先返回哪个仓库的库存满足订单列表的需求
        List<WareSkuEntity> wareSkuEntityList = this.wareSkuDao.check(skuLockVo.getSkuId(), skuLockVo.getCount());

        if (!CollectionUtils.isEmpty(wareSkuEntityList)){
            //锁库存,大数据分析就近的仓库锁库存，这里我们就取第一个仓库
            WareSkuEntity wareSkuEntity = wareSkuEntityList.get(0);
            int lock = this.wareSkuDao.Lock(wareSkuEntity.getId(), skuLockVo.getCount());
            if (lock != 0){
                skuLockVo.setLock(true);
                skuLockVo.setWareSkuId(wareSkuEntity.getId());
            }

        }

        fairLock.unlock();
    }



    /**
     *  演示spring的schedule定时任务,利用定时线程池关单
     *  缺点：
     *      (1)一旦服务器挂掉，线程池中的消息会丢失
     *      (2)消耗系统内存，增加了数据库的压力，存在较大的时间误差
     *          ps:每隔3分钟去遍历(耗内存)一次订单表，看看哪些订单超时，可有的订单正好3分01秒过期，就要再等2分59秒才能关闭.
     */
    public static void main(String[] args) {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3);
        scheduledExecutorService.scheduleAtFixedRate(()->{
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            System.out.println("这是一个定时任务:"+simpleDateFormat.format(new Date()));
        },1,5, TimeUnit.SECONDS);
    }

}