package com.hanson.spider.component.rabbitmq;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Hanson
 * create on 2019年2月10日
 */
@Configuration
public class RabbitMQConfiguration {
	//定义Direct 队列 
	@Bean(name = "syfcSalesNumDetail")
    public Queue syfcSalesNumDetailQueue() {
         return new Queue("syfcSalesNumDetail");
    }
	
	@Bean(name = "syfcSalesPriceList")
	public Queue syfcSalesPriceListQueue() {
		return new Queue("syfcSalesPriceList");
	}
	
	@Bean(name = "syfcNewBuildList")
	public Queue syfcNewBuildListQueue() {
		return new Queue("syfcNewBuildList");
	}
}

