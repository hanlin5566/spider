//package com.hanson.spider.service;
//
//import java.util.List;
//
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.core.query.Query;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//
//import com.alibaba.fastjson.JSONObject;
//import com.hanson.spider.SpiderBootStarp;
//
///**
// * @author Hanson
// * create on 2019年4月3日
// */
//@RunWith(SpringJUnit4ClassRunner.class)  
//@SpringBootTest(classes = SpiderBootStarp.class)
//public class CommunitySpiderServiceTest {
//	@Autowired
//	private MongoTemplate mongoTemplate;
//	
//	private final String COMMUNITY_COLLECTION = "syfc_community";
//	private final String OLD_COMMUNITY_COLLECTION = "syfc_new_build_detail";
//
//	@Test
//	public void incrementCommunity() {
//		Query query = new Query();
//		List<JSONObject> find = mongoTemplate.find(query, JSONObject.class,OLD_COMMUNITY_COLLECTION);
//		for (JSONObject jsonObject : find) {
//			
//		}
//	}
//
//}
//
