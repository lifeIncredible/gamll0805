package com.atguigu.gmall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVO;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author shkstart
 * @create 2020-01-26 22:22
 */
@Service
public class CartServiceImpl implements CartService {

    private static final String KEY_PREFIX = "cart:item:";
    private static final String PRICE_PREFIX = "cart:price:";
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GmallSmsClient smsClient;


    /**
     * 添加商品到购物车
     * @param cart
     */
    @Override
    public void addCart(Cart cart) {
        //获取用户登录信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String key = KEY_PREFIX;
        if (userInfo.getUserId() != null) {
            key += userInfo.getUserId();
        } else {
            key += userInfo.getUserKey();
        }

        // 1.查询购物车信息
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(key);

        //2.判断购物车中是否有该商品
        String skuId = cart.getSkuId().toString();
        Integer count = cart.getCount();
        if (hashOps.hasKey(skuId)) {
            //有 更新数量
            String cartJson = hashOps.get(skuId).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(cart.getCount() + count);

        } else {
            //无 新增一条记录
            cart.setCheck(true);//添加进去默认就是选中状态

            //查询sku相关信息
            Resp<SkuInfoEntity> skuInfoEntityResp = this.pmsClient.querySkuById(cart.getSkuId());
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
            if (skuInfoEntity == null) {
                return;
            }
            cart.setPrice(skuInfoEntity.getPrice());
            cart.setImage(skuInfoEntity.getSkuDefaultImg());
            cart.setSkuTitle(skuInfoEntity.getSkuTitle());

            //查询库存信息
            Resp<List<WareSkuEntity>> wareSkuResp = this.wmsClient.queryWareSkuBySkuId(cart.getSkuId());
            List<WareSkuEntity> wmsEntites = wareSkuResp.getData();
            if (CollectionUtils.isEmpty(wmsEntites)) {
                cart.setStore(wmsEntites.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
            }

            //查询sku销售属性
            Resp<List<SkuSaleAttrValueEntity>> saleAttrResp = this.pmsClient.querySaleAttrValueBySkuId(cart.getSkuId());
            List<SkuSaleAttrValueEntity> saleAttrValueEntities = saleAttrResp.getData();
           cart.setSaleAttrs(saleAttrValueEntities);


            //查询营销信息
            Resp<List<ItemSaleVO>> saleResp = this.smsClient.queryItemSaleVOBySkuId(cart.getSkuId());
            List<ItemSaleVO> itemSaleVOS = saleResp.getData();
            cart.setSales(itemSaleVOS);

            //当前价格，因为刚加入购物车时跟刚加入的购物车价格应该一样
            this.redisTemplate.opsForValue().set(PRICE_PREFIX + skuId,skuInfoEntity.getPrice().toString());

        }
        hashOps.put(skuId, JSON.toJSONString(cart));

    }


    /**
     * 查询购物车
     * @return
     */
    @Override
    public List<Cart> queryCarts() {

        //获取用户登录信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();
        Long userId = userInfo.getUserId();

        // 1.先查询未登录的购物车
        userKey  = KEY_PREFIX + userKey;
        BoundHashOperations<String, Object, Object> userKeyHashOps = this.redisTemplate.boundHashOps(userKey);
        List<Object> values = userKeyHashOps.values();
        List<Cart> userKeyCarts = null;
        if (!CollectionUtils.isEmpty(values)) {
            userKeyCarts = values.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);

                //查询当前价格,并重新赋值
                String currentPrice = this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId());
                cart.setCurrentPrice(new BigDecimal(currentPrice));
                return cart;
            }).collect(Collectors.toList());
        }

        //2. 判断是否登录，未登录直接返回
        if (userId == null) {
            return userKeyCarts;
        }

        //3. 登录了合并未登录的购物车到登录状态的购物车
        String userIdKey = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> userIdHashOps = this.redisTemplate.boundHashOps(userIdKey);

        //如果未登录状态购物车集合不为空，就遍历合并
        if (!CollectionUtils.isEmpty(userKeyCarts)) {
          userKeyCarts.forEach(cart -> {
              //如果登录状态下有该记录就更新数量
              if (userIdHashOps.hasKey(cart.getSkuId().toString())){
                  String cartJson = userIdHashOps.get(cart.getSkuId().toString()).toString();
                  Integer count = cart.getCount();
                  cart = JSON.parseObject(cartJson, Cart.class);
                  cart.setCount(cart.getCount()+count);
              }
              //如果登录状态下没有该记录就新增
              userIdHashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
          });
            //4. 删除未登录状态的购物车
           this.redisTemplate.delete(userKey);
        }

       // 5. 查询展示
        List<Object> userIdCartJsons = userIdHashOps.values();
        if (!CollectionUtils.isEmpty(userIdCartJsons)) {
          return userIdCartJsons.stream().map(cartJson -> {
              Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
              String currentPrice = this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId());
              cart.setCurrentPrice(new BigDecimal(currentPrice));
              return cart;
          }).collect(Collectors.toList());
        }
        return null;
    }


    /**
     * 修改购物车商品数量
     * @param cart
     */
    @Override
    public void updateNum(Cart cart) {
        //获取登录状态信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        //组装外层的Key
        String key = KEY_PREFIX;
        if (userInfo.getUserId() != null){
            key+= userInfo.getUserId();
        }else{
            key+= userInfo.getUserKey();
        }
        //获取内层的map(购物车数据的操作对象)
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        //判断购物车中有没有这个数据
        if (hashOps.hasKey(cart.getSkuId().toString())){
            String catJson = hashOps.get(cart.getSkuId().toString()).toString();
            Integer count = cart.getCount();
             cart = JSON.parseObject(catJson, Cart.class);
             cart.setCount(count);
             hashOps.put(cart.getSkuId().toString(), JSON.toJSONString(cart));
        }
    }


    /**
     * 检查是否选中
     * @param cart
     */
    @Override
    public void check(Cart cart) {
        //获取登录状态信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        //组装外层的Key
        String key = KEY_PREFIX;
        if (userInfo.getUserId() != null){
            key+= userInfo.getUserId();
        }else{
            key+= userInfo.getUserKey();
        }

        //获取内层的map(购物车数据的操作对象)
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);

        //判断购物车中有没有这个数据
        if (hashOps.hasKey(cart.getSkuId().toString())){
            String catJson = hashOps.get(cart.getSkuId().toString()).toString();
            Boolean check = cart.getCheck();
            cart = JSON.parseObject(catJson,Cart.class);
            cart.setCheck(check);
            hashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
        }
    }


    /**
     * 删除购物车商品
     * @param skuId
     */
    @Override
    public void delete(Long skuId) {

        //获取登录状态信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();

        //组装外层的Key
        String key = KEY_PREFIX;
        if (userInfo.getUserId() != null){
            key+= userInfo.getUserId();
        }else{
            key+= userInfo.getUserKey();
        }

        //获取内层的map(购物车数据的操作对象)
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);

        //判断购物车中有没有这个数据
        if (hashOps.hasKey(skuId.toString())){
            hashOps.delete(skuId.toString()); //以内层的map进行删除
        //this.redisTemplate.delete(skuId.toString());//错误，以外层的map进行删除，可以删除以用户的id删除这个用户的所有购物车
        }

    }


    /**
     * 被order服务远程调用查询购物车中选中的商品
     * @param userId
     * @return
     */
    @Override
    public List<Cart> queryCheckedCarts(Long userId) {
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);

        //获取所有的购物车记录
        List<Object> values = hashOps.values();
        if (!CollectionUtils.isEmpty(values)){
            return  values.stream().map(cartJson -> JSON.parseObject(cartJson.toString(),Cart.class)).filter(cart -> cart.getCheck()).collect(Collectors.toList());
        }
        return null;
    }





}
