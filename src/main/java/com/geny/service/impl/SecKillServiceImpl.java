package com.geny.service.impl;

import com.geny.exception.SecKillException;
import com.geny.lock.RedisLock;
import com.geny.service.SecKillService;
import com.geny.utils.KeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by shmily on 2018/7/2.
 */
@Service
public class SecKillServiceImpl implements SecKillService {

    private static final int TIMEOUT = 10 * 1000; //超时时间 10s

    @Autowired
    private RedisLock redisLock;

    // 模拟产品表
    static Map<String,Integer> products;

    // 模拟库存表
    static Map<String,Integer> stock;

    // 模拟订单表
    static Map<String,String> orders;

    static {
        products = new HashMap<String, Integer>();
        stock = new HashMap<String, Integer>();
        orders = new HashMap<String, String>();
        products.put("123456", 10000);
        stock.put("123456", 10000);
    }

    private String queryMap(String productId) {
        return "国庆活动，iPhone X 特价，限量份"
                + products.get(productId)
                + " 还剩：" + stock.get(productId) + " 份"
                + " 该商品成功下单用户数目："
                + orders.size() + " 人";
    }


    public String querySecKillProductInfo(String productInfo) {
        return this.queryMap(productInfo);
    }

    public void orderProductMockDiffUser(String productId) {
        //加锁
        long time = System.currentTimeMillis() + TIMEOUT;
        if (!redisLock.lock(productId, String.valueOf(time))){
            throw new SecKillException(101, "服务器被挤爆了，换个姿势试一试！");
        }

        //1.查询商品库存，为 0 则活动结束
        int stockNum = stock.get(productId);
        if (stockNum == 0) {
            throw new SecKillException(100, "活动结束");
        } else {
            //2.下单（模拟不同用户 id 不同）
            orders.put(KeyUtil.genUniqueKey(), productId);
            //3.减库存
            stockNum = stockNum - 1;
            try {
                // 模拟耗时请求
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            stock.put(productId, stockNum);
        }

        //解锁
        redisLock.unlock(productId, String.valueOf(time));
    }
}
