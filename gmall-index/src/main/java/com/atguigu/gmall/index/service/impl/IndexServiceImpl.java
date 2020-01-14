package com.atguigu.gmall.index.service.impl;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.index.config.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVO;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexServiceImpl implements IndexService {
    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    private static final String KEY_PREFIX = "index:cates:";

    @Override
    public List<CategoryEntity> queryLvl1Categories() {

        Resp<List<CategoryEntity>> categoriesResp = pmsClient.queryCategoriesByLevelOrPid(1, null);
        List<CategoryEntity> categoryEntities = categoriesResp.getData();
        return categoryEntities;
    }

    @GmallCache
    @Override
    public List<CategoryVO> queryCategoriesWithSub(Long pid) {

        //1.获取缓存中的数据
//        String cateJson = this.stringRedisTemplate.opsForValue().get(KEY_PREFIX + pid);

        //2.如果有直接返回
//        if (StringUtils.isNotBlank(cateJson)) {
//            return JSON.parseArray(cateJson, CategoryVO.class);
//        }

        //加分布式锁
        //加上锁之后把访问数据库的全部请求拦截了，pms中商品很多，不应该全部拦截，所以加上个pid，访问不同商品，就可以进入数据库
//        RLock lock = this.redissonClient.getLock("lock"+ pid);
//        lock.lock();

        //当有一个请求进入数据库查询之后会放入缓存，所以应该再判断缓存中有没有,返回之前应该注意释放锁
//        String cateJson2 = this.stringRedisTemplate.opsForValue().get(KEY_PREFIX + pid);
//        if (StringUtils.isNotBlank(cateJson2)) {
//            lock.unlock();
//            return JSON.parseArray(cateJson2, CategoryVO.class);
//        }


        //3.Redis中没有就远程调用pms查询数据库,查数据库前加分布式锁Redisson框架
        Resp<List<CategoryVO>> listResp = this.pmsClient.queryCategoriesWithSup(pid);
        List<CategoryVO> vos = listResp.getData();

        /*
           4.在查询完数据库时按理说不应该先判断数据库中的数据 有没有吗？
            如果有100W大量恶意的请求同时访问不存在的数据，会先查询缓存，缓存没有直接进入数据库进行最后校验有没有
            可是数据库处理不了这么大的并发请求，直接宕机,造成缓存穿透。
            所以解决办法就是即使数据库中没有的数据，数据为空也要放入缓存。
        */

        //5.查询数据库完成后放入缓存(加随机时间是为了防止大量的key值同时失效，大量请求同时访问数据库，造成缓存雪崩问题)
//        this.stringRedisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(vos), new Random().nextInt(5), TimeUnit.DAYS);
//
//        lock.unlock();

        return vos;
    }










    /**
     * 此方法用于测试redis会出现常见的问题：（用ab压力5000个请求，100个并发，每有一个请求，健num的值+1）
     * 1.不加本地锁
     *          假设现在num值是100，并发的100个请求同时对num+1,一个请求在修改变成101，
     *          另外一个请求修改时num还是100，就会把num再次变成101。最后造成数据丢失。最后num的值无法变成5000
     *
     * 2. 加上synchronized本地锁
     *           单个实例下5000个请求，num值变成5000,没有问题
     *
     * 3. 加synchronized本地锁的前提下，复制两个gmall-index服务进行测试
     *            测试结果发现num值为2835，无法达到5000，说明本地锁在集群环境下还是有问题
     *             有可能JVM都不一样了，不在同一个机器上了，synchronize和lock都是JVM提供的
     *              只能锁当前项目。
     *              所以要引入分布式锁。
     */
    @Override
    public void testLock() {

        String numString = this.stringRedisTemplate.opsForValue().get("num");
        if (numString == null) {
            return;
        }
        Integer num = new Integer(numString);

        this.stringRedisTemplate.opsForValue().set("num", String.valueOf(++num));

    }


    /**
     * 此方法用于测试在企业中自己写的分布式锁
     *
         外层加上一个Redis中setnx方法，变相成为锁
         1.ab 压力测试后发现num值为5000，没有出现数据的丢失
            还是存在问题:
                      ①   gmall_index 获取到锁还没有执行到业务逻辑中间就挂掉了,无法自己释放锁。
                          锁一直被gmall_index占用。其他两个服务无法获取到锁，造成了死锁。即使
                          重启服务还是会从刚才挂掉地方重启，也无法释放锁。
                    解决办法：为setnx命令加上过期时间，即使服务挂掉也会自动释放。
-------------------------------------------------------------------------------------------------------------------------
                    ②    加上时间后再次出现问题，假如锁5秒失效。但是业务逻辑要7秒才能执行完,
                          此时另一个请求就进来了，又执行一半，原先的请求就执行完了，把锁删除(释放)了。那
                          刚才进来的请求就跟没加锁一样，就会造成会有更多请求进来，永远都是上一个请求把下一个请求
                          的锁给释放掉。一次性衍生出两个问题：
                                                    (1)锁的过期时间不好确定
                                                    (2)原先的请求会把新进来请求的锁删除掉
                    解决办法： 要保证是自己的锁，可以在value上加一个uuid唯一标志。删除时先获取值看看是不是自己的锁
------------------------------------------------------------------------------------------------------------------------
                    ③   若刚判断是自己锁的时候，突然锁过期了，又有另外一个请求过来，不就又把另外请求的锁给删除了吗？

                    解决办法： 要保证删除锁和判断锁的原子性,要保证原子性就使用lua脚本
      */
    @Override
    public void testDsLock() {
        String uuid = UUID.randomUUID().toString();
        // 所有请求执行Redis中setnx命令，如果key存在不执行，返回值为true，说明获取到锁
        Boolean flag = this.stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid, 5, TimeUnit.SECONDS);
        // 如果返回值为true，执行业务逻辑
        if (flag) {

            String numString = this.stringRedisTemplate.opsForValue().get("num");
            if (numString == null) {
                return;
            }
            Integer num = new Integer(numString);
            this.stringRedisTemplate.opsForValue().set("num", String.valueOf(++num));

            //优化之LUA脚本保证删除的原子性
            String script ="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            this.stringRedisTemplate.execute(new DefaultRedisScript<>(script,Long.class), Arrays.asList("lock"),uuid);
            //执行业务逻辑之前先判断锁是不是当前线程的,防止误删锁
//            String lock = stringRedisTemplate.opsForValue().get("lock");
//            if (StringUtils.equals(lock, uuid)) {
//                this.stringRedisTemplate.delete("lock");
//            }
        } else {
            //如果没有获取到锁，重试
            try {
                TimeUnit.SECONDS.sleep(1);
                testDsLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }


    }


}
