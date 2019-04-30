package com.hanson.spider.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hanson.spider.component.parser.SYFCParser;	

/**
 * @author Hanson
 * create on 2019年4月3日
 */
@Service
public class CommunitySpiderService {
	
	@Autowired
	private SYFCParser parser;

	@Autowired
	private MongoTemplate mongoTemplate;
	
	private final String recordCollectionName = "syfc_community";//基准最新的新建房屋列表
	/**
	 * 每一小时执行一次
	 */
	@Scheduled(cron = "0 0 */1 * * ?")
	public void increment() {
		int connectTimeout = 1000*30;//五分钟
		int readTimeout = 1000*30;//五分钟
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();  
		requestFactory.setConnectTimeout(connectTimeout);// 设置超时  
		requestFactory.setReadTimeout(readTimeout);  
		//设置代理
		//利用复杂构造器可以实现超时设置，内部实际实现为 HttpClient  
		RestTemplate restTemplate = new RestTemplate(requestFactory);  
		String url = "http://www.syfc.com.cn/work/xjlp/new_building.jsp";
		String body = restTemplate.getForObject(url,String.class);
		// 解析页面
		JSONArray parseBuildList = parser.parseBuildList(body);
		for (Object object : parseBuildList) {
			JSONObject community = (JSONObject)object;
			Query query = new Query();
			query.addCriteria(Criteria.where("community_id").is(community.getInteger("third_record_id")));
			Update update = Update.update("program_describe", community.get("program_describe"))
			.set("district", community.get("district"))
			.set("approve_date", community.get("start_sales_date"))
			.set("subordinate_district", community.get("subordinate_district"))
			.set("company", community.get("company"))
			.set("build_count", community.getInteger("build_count"))
			.set("community_id", community.getInteger("third_record_id"))
			.set("update_time", community.get("update_time"));
			mongoTemplate.upsert(query , update, JSONObject.class,recordCollectionName);
		}
	}
}

