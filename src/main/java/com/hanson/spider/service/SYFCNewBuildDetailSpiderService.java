package com.hanson.spider.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hanson.spider.component.FileUtils;
import com.hanson.spider.component.parser.SYFCParser;
import com.hanson.spider.component.rabbitmq.RabbitMQSender;
import com.hanson.spider.misc.SpiderResponseCode;
import com.hanson.spider.thread.SpiderConsumerPushMQ;
import com.hanson.base.exception.ServiceException;

/**
 * @author Hanson create on 2018年3月11日
 * http://www.syfc.com.cn/work/xjlp/new_building.jsp
 * 新建楼盘栋列表采集
 */
@Service
public class SYFCNewBuildDetailSpiderService {
	Logger logger = LoggerFactory.getLogger(this.getClass());
	@Value("${syfc.home.url:http://www.syfc.com.cn/}")
	private String url;

	@Autowired
	private FileUtils fileUtils;

	@Autowired
	private SYFCParser parser;

	@Autowired
	private MongoTemplate mongoTemplate;
	
	@Autowired
    private RabbitMQSender sender;


	// 生成模板规则
	private final String FORMAT_ISO = "yyyy-MM-dd_HH-mm-ss";
	private final String FORMAT_DATE = "yyyy-MM-dd";
	//mongo 插入日期格式
	private final String MONGO_ISO = "yyyy-MM-dd HH:mm:ss";
	//mongo collection name
	private final String recordCollectionName = "syfc_new_build_detail";//基准最新的新建房屋列表
	private final String listCollectionName = "syfc_new_build_list";

	
	private final SimpleDateFormat sdf_date = new SimpleDateFormat(this.FORMAT_DATE);
	private final SimpleDateFormat sdf_iso = new SimpleDateFormat(this.FORMAT_ISO);
	private final SimpleDateFormat mongo_iso = new SimpleDateFormat(this.MONGO_ISO);
	
	/**
	 * 队列方式全新采集
	 * @param taskId
	 */
	public void collectNewBuildDetail() {
		BlockingQueue<SpiderConsumerPushMQ.Spider> consumerQueue = new LinkedBlockingQueue<SpiderConsumerPushMQ.Spider>();
		List<JSONObject> unfinishedList = this.readUnFinishedTask();
		for (int i = 0; i < unfinishedList.size(); i++) {
			JSONObject jsonObject = unfinishedList.get(i);
//			String deltailUri = jsonObject.getString("deltail_uri");
			String third_record_id = jsonObject.getString("third_record_id");
			String deltailUri = "/work/xjlp/build_list.jsp?xmmcid="+third_record_id;
			try {
				//http://www.syfc.com.cn/work/xjlp/build_list.jsp?xmmcid=64583&xmmc=居住、商业（一期）
				SpiderConsumerPushMQ.Spider spider = new SpiderConsumerPushMQ.Spider(i,third_record_id, "syfc_sales_build_detail" + third_record_id, url+ deltailUri,null);
				consumerQueue.put(spider);
			} catch (InterruptedException e) {
				throw new ServiceException(SpiderResponseCode.SPIDER_GET_PRODUCER_ERROR,String.format(SpiderResponseCode.SPIDER_GET_PRODUCER_ERROR.detailMsg(), e.getMessage()), e);
			}
		}
		String queueName = "syfcSalesBuildDetail";
		SpiderConsumerPushMQ consumer = new SpiderConsumerPushMQ(consumerQueue,sender,queueName);
		new Thread(consumer).start();
	}
	
	private List<JSONObject> readUnFinishedTask() {
		Query query = new Query();
		//不等于1的记录，未采集详情信息的记录
		query.addCriteria(Criteria.where("collect_state").ne(1));
//		List<JSONObject> list = mongoTemplate.find(query, JSONObject.class, recordCollectionName);
		List<JSONObject> list = mongoTemplate.find(query, JSONObject.class, recordCollectionName+"_"+sdf_date.format(new Date()));
		return list;
	}
	
	public void transformTask() {
		//采集list生成采集任务
		List<JSONObject> list = mongoTemplate.findAll(JSONObject.class, listCollectionName);
		for (JSONObject jsonObject : list) {
			JSONArray salesBuildList = jsonObject.getJSONArray("sales_build_list");
			for (Object object : salesBuildList) {
				JSONObject salesBuild = (JSONObject)object;
				salesBuild.put("collect_state", 0);
//				mongoTemplate.insert(salesBuild,recordCollectionName+"_"+sdf_date.format(new Date()));
				mongoTemplate.insert(salesBuild,recordCollectionName);
			}
		}
		//TODO:改字段名
//		List<JSONObject> list = mongoTemplate.findAll(JSONObject.class, listCollectionName);
//		for (JSONObject jsonObject : list) {
//			JSONArray salesBuildList = jsonObject.getJSONArray("sales_build_list");
//			for (Object object : salesBuildList) {
//				JSONObject salesBuild = (JSONObject)object;
//				String value = salesBuild.getString("third_pard_id");
//				salesBuild.remove("third_pard_id");
//				salesBuild.put("third_record_id",value);
////				salesBuild.put("collect_state", 0);
////				mongoTemplate.insert(salesBuild,recordCollectionName+"_"+sdf_date.format(new Date()));
//			}
//			jsonObject.put("sales_build_list", salesBuildList);
//			mongoTemplate.insert(jsonObject,listCollectionName+"_reName");
//		}
		
	}
	
	public void saveResult(JSONObject ret) {
		//文件目录名称
		String date_str = sdf_date.format(new Date());
		String folderName = "syfc_build_detail_" + date_str;
		String collectionName = recordCollectionName+"_"+sdf_date.format(new Date());
//		String collectionName = recordCollectionName;
		int no;
		String body = ret.getString("body");
		Boolean success = ret.getBoolean("success");
		no = ret.getInteger("no");
		String third_record_id = ret.getString("id");
		if(!success) {
			logger.error("请求返回体发生错误 NO:{},请求发生请求页面失败错误",no);
		}
		// 保存文件
		fileUtils.saveFile(folderName, "syfc_build_detail_"+third_record_id+"_"+sdf_iso.format(new Date()), body);
		// 解析页面
		JSONArray parseBuildDetail = parser.parseNewBuildDetail(body);
		// 持久化解析结果到mongo
		try {
			Query detailQuery = new Query();
			detailQuery.addCriteria(Criteria.where("third_record_id").is(third_record_id));
			
			JSONObject salesNumRecord = mongoTemplate.findOne(detailQuery, JSONObject.class, collectionName);
			if(salesNumRecord != null ) {
				//update
				Update update = Update.
				update("build_detail_list", parseBuildDetail)
				.set("update_time", mongo_iso.format(new Date()))
				.set("collect_state",1)//设置状态为1
				;
				mongoTemplate.updateFirst(detailQuery, update, collectionName);
			}else {
				//insert
				logger.error("数据不一致，新增预售许可证详情");
				//insert
				JSONObject insert = new JSONObject();
				insert.put("collect_time", mongo_iso.format(new Date()));
				insert.put("sales_build_list", parseBuildDetail);
				insert.put("third_record_id", third_record_id);
				insert.put("no", no);
				insert.put("collect_state", 1);
				mongoTemplate.insert(insert,collectionName);
			}
			//更新全量
			//update
			Update update = Update.
			update("build_detail_list", parseBuildDetail)
			.set("update_time", mongo_iso.format(new Date()))
			.set("collect_state",1)//设置状态为1
			;
			mongoTemplate.updateFirst(detailQuery, update, recordCollectionName);
		} catch (Exception e) {
			logger.error("持久化mongo发生错误 NO:{},请求发生错误",no,e);
		}
	}
	
	public void incrementNewBuildDetail() {
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
		//查找采集到的最后一个预售许可证，按照审批时间倒序
		Query query = new Query();  
		query.with(new Sort(new Order(Direction.DESC,"start_sales_date")));
		JSONObject lastBuildDetail = mongoTemplate.findOne(query, JSONObject.class, recordCollectionName);
		String date = lastBuildDetail.getString("start_sales_date");
		for (Object object : parseBuildList) {
			JSONObject build = (JSONObject)object;
			/**
			 * json.put("third_record_id", third_record_id);
				json.put("deltail_uri", deltail_uri);
				json.put("program_describe", program_describe);
				json.put("district", district);
				json.put("build_count", build_count);
				json.put("company", company);
				json.put("start_sales_date", start_sales_date);
				json.put("subordinate_district", subordinate_district);
			 */
			String currentDate = build.getString("start_sales_date");
			String third_record_id = build.getString("third_record_id");
			try {
				long dateLong = sdf_date.parse(date).getTime();
				long currentDateLong = sdf_date.parse(currentDate).getTime();
				//只有大于才添加，如果恰巧错过更新时间，则只能T+1再添加
				if(currentDateLong > dateLong) {
					Query detailQuery = new Query();
					detailQuery.addCriteria(Criteria.where("third_record_id").is(third_record_id));
					JSONObject buildRecord = mongoTemplate.findOne(detailQuery, JSONObject.class, recordCollectionName);
					//没有则新增
					if(buildRecord == null) {
						build.put("collect_time", mongo_iso.format(new Date()));
						build.put("collect_state", 0);
						mongoTemplate.insert(build,recordCollectionName);
					}else {
						//数据被更新过，则保留原有副本，并增加ver
						int ver = 0;
						if(buildRecord.containsKey("ver")) {
							ver = buildRecord.getInteger("ver");
						}
						Update update = Update.
							update("build_detail_old_"+ver, buildRecord)
							.set("ver", ++ver)
							.set("deltail_uri", build.get("deltail_uri"))
							.set("program_describe", build.get("program_describe"))
							.set("district", build.get("district"))
							.set("build_count", build.get("build_count"))
							.set("company", build.get("company"))
							.set("start_sales_date", build.get("start_sales_date"))
							.set("subordinate_district", build.get("subordinate_district"))
							.set("update_time", mongo_iso.format(new Date()))
							.set("collect_state",0)//设置状态为1
							;
						mongoTemplate.updateFirst(detailQuery, update, recordCollectionName);
					}
				}
			} catch (Exception e) {
				logger.error("新增预售许可证错误，解析异常{}",third_record_id);
			}
		}
	}
	
	public void initTodayNewBuildDetail() {
		//生成全量当天采集任务
		//TODO:此处可以添加逻辑是否继续每日采集
		List<JSONObject> lastBuildDetailList = mongoTemplate.findAll(JSONObject.class, recordCollectionName);
		for (Object object : lastBuildDetailList) {
			JSONObject salesBuild = (JSONObject)object;
			salesBuild.put("collect_state", 0);
			Query query = new Query();
			query.addCriteria(Criteria.where("third_record_id").is(salesBuild.get("third_record_id")));
			JSONObject result = mongoTemplate.findOne(query, JSONObject.class, recordCollectionName+"_"+sdf_date.format(new Date()));
			if(result == null) {
				//新增记录
				mongoTemplate.insert(salesBuild,recordCollectionName+"_"+sdf_date.format(new Date()));
			}
		}
	}
}
