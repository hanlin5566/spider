package com.hanson.spider.component.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.hanson.spider.service.SYFCNewBuildHouseSpiderService;
import com.rabbitmq.client.Channel;

@Component
@RabbitListener(queues = "syfcSalesBuildHouse")
public class SYFCNewBuildHouseMQReceiver {
	Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	SYFCNewBuildHouseSpiderService service;
	
    @RabbitHandler
    public void process(JSONObject msg,Channel channel, Message message) {
    	try {
			//成功是1 ，失败是-1.
			//save
			service.saveResult(msg);
			Thread.sleep(1000);
    		//告诉服务器收到这条消息 已经被我消费了 可以在队列删掉 这样以后就不会再发了 否则消息服务器以为这条消息没处理掉 后续还会在发
    		channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            //丢弃这条消息
            //channel.basicNack(message.getMessageProperties().getDeliveryTag(), false,false);
            logger.error("SYFCNewBuildHouseMQReceiver receiver fail",e);
        }
    }
 
}