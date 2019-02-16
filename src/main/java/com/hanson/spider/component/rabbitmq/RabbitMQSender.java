package com.hanson.spider.component.rabbitmq;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

/**
 * @author Hanson create on 2019年2月10日
 */

@Component
public class RabbitMQSender {
	@Autowired
	private AmqpTemplate template;

	public void send(String queueName, JSONObject msg) {
		template.convertAndSend(queueName, msg);
	}
}
