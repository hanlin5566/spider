package com.hanson.spider.service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class BuildingSpiderService {
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private SYFCParser parser;

	@Autowired
	private MongoTemplate mongoTemplate;
	
	private final String communityCollectionName = "syfc_community";//基准最新的新建房屋列表
	
	//半小时一次
	@Scheduled(cron = "0 */30 0 * * ?")
	public void increment() {
		Query detailQuery = new Query();
		//未采集Building的community
		detailQuery.addCriteria(Criteria.where("gen_mongo_build").ne(1));
		List<JSONObject> buildRecord = mongoTemplate.find(detailQuery, JSONObject.class, communityCollectionName);
		for (JSONObject jsonObject : buildRecord) {
			Integer community_id = jsonObject.getInteger("community_id");
			String url = "http://www.syfc.com.cn/work/xjlp/build_list.jsp?xmmcid="+community_id;
			try {
				int connectTimeout = 1000*30;//五分钟
				int readTimeout = 1000*30;//五分钟
				SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();  
				requestFactory.setConnectTimeout(connectTimeout);// 设置超时  
				requestFactory.setReadTimeout(readTimeout);  
				//设置代理
				//利用复杂构造器可以实现超时设置，内部实际实现为 HttpClient  
				//http://www.syfc.com.cn/work/xjlp/build_list.jsp?xmmcid=64382
				RestTemplate restTemplate = new RestTemplate(requestFactory);  
				String body = restTemplate.getForObject(url,String.class);
				// 解析页面
				JSONArray parseBuildDetail = parser.parseNewBuildDetail(body);
				for (Object building : parseBuildDetail) {
					JSONObject buildingJSON = (JSONObject)building;
					Query query = new Query();
					String org_third_record_id = null;
					try {
						buildingJSON.getInteger("third_record_id");
					} catch (Exception e) {
						//只保留数字
						String building_id = buildingJSON.getString("third_record_id");
						org_third_record_id = building_id;
						String regEx="[^0-9]";  
						Pattern p = Pattern.compile(regEx);  
						Matcher m = p.matcher(building_id);  
						buildingJSON.put("third_record_id",m.replaceAll(""));
					}
					query.addCriteria(Criteria.where("building_id").is(buildingJSON.getInteger("third_record_id")));
					Update update = Update.update("build_location", buildingJSON.get("build_location"))
							.set("sales_available_count", buildingJSON.get("sales_available_count"))
							.set("sales_unvailable_count", buildingJSON.get("sales_unvailable_count"))
							.set("saled_count", buildingJSON.get("saled_count"))
							.set("can_sales_count", buildingJSON.get("can_sales_count"))
							.set("community_id", community_id)
							.set("building_id", buildingJSON.getInteger("third_record_id"));
					if(org_third_record_id != null) {
						update.set("org_third_record_id", org_third_record_id);
					}
					mongoTemplate.upsert(query , update, JSONObject.class,"syfc_building");
				}
				//更新楼盘的建筑采集信息
				this.updateCommunityBuildCount(community_id, parseBuildDetail.size());
				logger.warn("建筑楼盘ID{}采集成功,建筑栋数{}",parseBuildDetail.size());
			} catch(Exception ex) {
				logger.warn("建筑采集失败楼盘ID{},建筑ID{}",community_id,ex);
			}
		}
		
		
	}
	
	
	private void updateCommunityBuildCount(int communityId,int buildCount) {
		Query query = new Query();
		query.addCriteria(Criteria.where("community_id").is(communityId));
		Update update = Update.update("build_count", buildCount)
				.set("gen_mongo_build", 1);
		mongoTemplate.upsert(query , update, JSONObject.class,communityCollectionName);
	}
}

