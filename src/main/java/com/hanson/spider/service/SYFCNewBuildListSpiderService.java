package com.hanson.spider.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

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
 * 新建楼盘列表采集
 * 按页采集，并且将每页存入了一个document，每页的list为sales_build_list字段
 */
@Service
public class SYFCNewBuildListSpiderService {
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
	private final String recordCollectionName = "syfc_new_build_list";

	
	private final SimpleDateFormat sdf_date = new SimpleDateFormat(this.FORMAT_DATE);
	private final SimpleDateFormat sdf_iso = new SimpleDateFormat(this.FORMAT_ISO);
	private final SimpleDateFormat mongo_iso = new SimpleDateFormat(this.MONGO_ISO);
	AtomicInteger count;
	
	/**
	 * 队列方式全新采集
	 * @param taskId
	 */
	public void collectNewBuildList() {
		BlockingQueue<SpiderConsumerPushMQ.Spider> consumerQueue = new LinkedBlockingQueue<SpiderConsumerPushMQ.Spider>();
		try {
			JSONObject readLastTask = readLastTask();
			if(readLastTask == null) {
				count = new AtomicInteger(0);
			}else {
				count =  new AtomicInteger(readLastTask.getInteger("no"));
			}
			//work/xjlp/new_building.jsp?page=2
			int no = count.incrementAndGet();
			for(int i = no; i <= 123; i++) {
				SpiderConsumerPushMQ.Spider spider = new SpiderConsumerPushMQ.Spider(no,no+"","syfc_new_build" + no, url+ "/work/xjlp/new_building.jsp?page="+no,null);
				consumerQueue.put(spider);
				no = count.incrementAndGet();
			}
		} catch (InterruptedException e) {
			throw new ServiceException(SpiderResponseCode.SPIDER_GET_PRODUCER_ERROR,String.format(SpiderResponseCode.SPIDER_GET_PRODUCER_ERROR.detailMsg(), e.getMessage()), e);
		}
		String queueName = "syfcNewBuildList";
		SpiderConsumerPushMQ consumer = new SpiderConsumerPushMQ(consumerQueue,sender,queueName);
		
		new Thread(consumer).start();
	}
	
	private JSONObject readLastTask() {
		Query query = new Query();
		//采集最后一页，继续采集.
		query.addCriteria(Criteria.where("collect_state").is(1));
		query.with(new Sort(new Order(Direction.DESC,"no")));
		JSONObject lastRecord = mongoTemplate.findOne(query, JSONObject.class, recordCollectionName);
		return lastRecord;
	}
	
	public void saveResult(JSONObject ret) {
		//文件目录名称
		String date_str = sdf_date.format(new Date());
		String folderName = "syfc_build_list" + date_str;
		int no;
		String body = ret.getString("body");
		Boolean success = ret.getBoolean("success");
		no = ret.getInteger("no");
		if(!success) {
			logger.error("请求返回体发生错误 NO:{},请求发生请求页面失败错误",no);
		}
		// 保存文件
		fileUtils.saveFile(folderName, "syfc_build_list_"+no+"_"+sdf_iso.format(new Date()), body);
		// 解析页面
		JSONArray parseBuildList = parser.parseBuildList(body);
		// 持久化解析结果到mongo
		try {
			//insert
			JSONObject insert = new JSONObject();
			insert.put("collect_time", mongo_iso.format(new Date()));
			insert.put("sales_build_list", parseBuildList);
			insert.put("no", no);
			insert.put("collect_state", 1);
			mongoTemplate.insert(insert,recordCollectionName);
		} catch (Exception e) {
			logger.error("持久化mongo发生错误 NO:{},请求发生错误",no,e);
		}
	}
}
