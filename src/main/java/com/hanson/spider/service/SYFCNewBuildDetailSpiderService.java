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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hanson.spider.component.FileUtils;
import com.hanson.spider.component.parser.SYFCParser;
import com.hanson.spider.component.rabbitmq.RabbitMQSender;
import com.hanson.spider.misc.SpiderResponseCode;
import com.hanson.spider.thread.SpiderConsumerPushMQ;
import com.hzcf.base.exception.ServiceException;

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
	private final String recordCollectionName = "syfc_new_build_detail";
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
		List<JSONObject> list = mongoTemplate.find(query, JSONObject.class, recordCollectionName);
//		List<JSONObject> list = mongoTemplate.find(query, JSONObject.class, recordCollectionName+"_"+sdf_date.format(new Date()));
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
//		String collectionName = recordCollectionName+"_"+sdf_date.format(new Date());
		String collectionName = recordCollectionName;
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
			
		} catch (Exception e) {
			logger.error("持久化mongo发生错误 NO:{},请求发生错误",no,e);
		}
	}
}
