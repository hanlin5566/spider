package com.hanson.spider.component.rabbitmq;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.alibaba.fastjson.JSONObject;
import com.hanson.spider.SpiderBootStarp;

/**
 * @author Hanson
 * create on 2019年2月10日
 */
@RunWith(SpringJUnit4ClassRunner.class)  
@SpringBootTest(classes = SpiderBootStarp.class)
public class SYFCSalesNumDetailMQSenderTest {
	@Autowired
    private RabbitMQSender sender;
	
	@Test
	public void test() {
		JSONObject ret = new JSONObject();
		ret.put("body", "content");
		ret.put("no", 1);
		ret.put("name", "syfcSalesNumDetail1");
		ret.put("success", true);
		sender.send("syfcSalesNumDetail",ret);
//		sender.send("syfcSalesPriceList","hello syfcSalesPriceList");
	}
}

