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
	
	@Bean(name = "syfcNewBuildList")
	public Queue syfcNewBuildListQueue() {
		return new Queue("syfcNewBuildList");
	}
	
	@Bean(name = "syfcSalesPriceManyList")
	public Queue syfcNewBuildManyListQueue() {
		return new Queue("syfcSalesPriceManyList");
	}
	
	@Bean(name = "syfcSalesPriceList")
	public Queue syfcSalesPriceListQueue() {
		return new Queue("syfcSalesPriceList");
	}
	
	@Bean(name = "syfcSalesBuildDetail")
	public Queue syfcSalesBuildDetailQueue() {
		return new Queue("syfcSalesBuildDetail");
	}
	@Bean(name = "syfcSalesBuildHouse")
	public Queue syfcSalesBuildHouseQueue() {
		return new Queue("syfcSalesBuildHouse");
	}
	@Bean(name = "syfcSalesPriceDetail")
	public Queue syfcSalesPriceDetailQueue() {
		return new Queue("syfcSalesPriceDetail");
	}
}

