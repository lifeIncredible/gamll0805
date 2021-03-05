package com.atguigu.gmall.oms.listener;

import com.atguigu.gmall.oms.dao.OrderDao;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author shkstart
 * @create 2020-03-23 2:03
 */
@Component
public class OrderCloseListener {

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private AmqpTemplate amqpTemplate;

    //用户超时未支付定时关单
    @RabbitListener(queues = {"ORDER-DEAD-QUEUE"})
    public void closeOrder(String orderToken) {

        //1.关闭订单
        int i = this.orderDao.closeOrder(orderToken);
        if ( i ==1){
            //2. 立马解锁库存
            this.amqpTemplate.convertAndSend("STOCK-UNLOCK-QUEUE","stock.unlock",orderToken);
        }
    }

}
