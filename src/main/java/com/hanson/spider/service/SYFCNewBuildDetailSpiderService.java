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
import com.hanson.spider.misc.SpiderResponseCode;
import com.hanson.spider.thread.SpiderConsumer;
import com.hanson.spider.thread.SpiderConsumer.Spider;
import com.hzcf.base.exception.ServiceException;

/**
 * @author Hanson create on 2018年3月11日
 * http://www.syfc.com.cn/work/xjlp/new_building.jsp
 * 新建楼盘列表采集
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


	// 生成模板规则
	private final String FORMAT_ISO = "yyyy-MM-dd_HH-mm-ss";
	private final String FORMAT_DATE = "yyyy-MM-dd";
	//mongo 插入日期格式
	private final String MONGO_ISO = "yyyy-MM-dd HH:mm:ss";
	//mongo collection name
	private final String recordCollectionName = "syfc_new_build_detail";

	
	private final SimpleDateFormat sdf_date = new SimpleDateFormat(this.FORMAT_DATE);
	private final SimpleDateFormat sdf_iso = new SimpleDateFormat(this.FORMAT_ISO);
	private final SimpleDateFormat mongo_iso = new SimpleDateFormat(this.MONGO_ISO);
	AtomicInteger count;
	
	/**
	 * 队列方式全新采集
	 * @param taskId
	 */
	public void collectNewBuildList() {
		BlockingQueue<SpiderConsumer.Spider> consumerQueue = new LinkedBlockingQueue<SpiderConsumer.Spider>();
		BlockingQueue<JSONObject> producerQueue = new LinkedBlockingQueue<JSONObject>();
		try {
			JSONObject readLastTask = readLastTask();
			if(readLastTask == null) {
				count = new AtomicInteger(0);
			}else {
				count =  new AtomicInteger(readLastTask.getInteger("no"));
			}
			//work/xjlp/new_building.jsp?page=2
			int no = count.incrementAndGet();
			//houseId 
			int houseId = 488423;
			//http://www.syfc.com.cn/work/xjlp/door_list.jsp?houseid=488423
			Spider spider = new SpiderConsumer.Spider(no, "syfc_new_build_detail" + no, url+ "/work/xjlp/door_list.jsp?houseid="+houseId,null);
			consumerQueue.put(spider);
		} catch (InterruptedException e) {
			throw new ServiceException(SpiderResponseCode.SPIDER_GET_PRODUCER_ERROR,String.format(SpiderResponseCode.SPIDER_GET_PRODUCER_ERROR.detailMsg(), e.getMessage()), e);
		}
		SpiderConsumer consumer = new SpiderConsumer(consumerQueue, producerQueue);
		
		new Thread(consumer).start();
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				while(true) {
					try {
						JSONObject ret = producerQueue.take();
						//成功是1 ，失败是-1.
						//save
						saveResult(ret);
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						throw new ServiceException(SpiderResponseCode.Spider_SALES_PAGE_ERROR,String.format(SpiderResponseCode.Spider_SALES_PAGE_ERROR.detailMsg(), e.getMessage()), e);
					}
				}
			}
		}).start();
	}
	
	private JSONObject readLastTask() {
		Query query = new Query();
		//不等于1的记录，未采集详情信息的记录
		query.addCriteria(Criteria.where("collect_state").is(1));
		query.with(new Sort(new Order(Direction.DESC,"no")));
		JSONObject lastRecord = mongoTemplate.findOne(query, JSONObject.class, recordCollectionName);
		return lastRecord;
	}
	
	private void saveResult(JSONObject ret) {
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
