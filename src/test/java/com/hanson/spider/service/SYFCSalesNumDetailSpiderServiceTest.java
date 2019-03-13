package com.hanson.spider.service;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.alibaba.fastjson.JSONObject;
import com.hanson.spider.SpiderBootStarp;

/**
 * @author Hanson
 * create on 2019年3月12日
 */
@RunWith(SpringJUnit4ClassRunner.class)  
@SpringBootTest(classes = SpiderBootStarp.class)
public class SYFCSalesNumDetailSpiderServiceTest {
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private MongoTemplate mongoTemplate;

	@Test
	@Ignore
	public void check() {
		List<JSONObject> findAll = mongoTemplate.findAll(JSONObject.class, "syfc_sales_num_detail");
		for (JSONObject jsonObject : findAll) {
			String sales_no = jsonObject.getString("sales_no");
			boolean hasPrice = false;
			try {
				int sales_no_int = Integer.parseInt(sales_no);
				Query query = Query.query(Criteria.where("sales_no").is(sales_no_int));
				JSONObject find = mongoTemplate.findOne(query, JSONObject.class, "syfc_sales_num_price_list");
				if(find != null) {
					hasPrice = true;
				}else {
					logger.error("data is not consistent sales_no:{}",sales_no);
				}
			} catch (Exception e) {
				logger.error("sales no is not numberic sales_no:{}",sales_no,e);
				continue;
			}
			if(hasPrice) {
				Query updateQuery = Query.query(Criteria.where("sales_no").is(sales_no));
				Update update = Update.update("sales_price_collect_state", 1);
				mongoTemplate.updateFirst(updateQuery, update, JSONObject.class,"syfc_sales_num_detail");
			}
		}
	}

}

