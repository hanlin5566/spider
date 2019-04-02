package com.hanson.spider.service;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.io.IOUtils;
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
import com.hanson.spider.component.http.HttpClient;
import com.hanson.spider.component.parser.SYFCParser;
import com.hanson.spider.misc.SpiderResponseCode;
import com.hanson.spider.thread.SpiderConsumer;
import com.hanson.spider.thread.SpiderConsumer.Spider;
import com.hanson.spider.thread.SpiderThread;
import com.hanson.base.exception.ServiceException;

/**
 * @author Hanson create on 2018年3月11日
 * http://www.syfc.com.cn/work/ysxk/query_xukezheng.jsp
 * 预售许可证列表采集
 */
@Service
public class SYFCSalesNumListSpiderService {
	Logger logger = LoggerFactory.getLogger(this.getClass());
	@Value("${syfc.salesNo.url:http://www.syfc.com.cn/work/ysxk/query_xukezheng.jsp}")
	private String url;

	@Autowired
	private HttpClient httpclient;

	@Autowired
	private FileUtils fileUtils;

	@Autowired
	private SYFCParser parser;

	@Autowired
	private MongoTemplate mongoTemplate;
	
	String moudleName = "";


	// 生成模板规则
	private final String FORMAT_ISO = "yyyy-MM-dd_HH-mm-ss";
	private final String FORMAT_DATE = "yyyy-MM-dd";
	//mongo 插入日期格式
	private final String MONGO_ISO = "yyyy-MM-dd HH:mm:ss";
	//mongo collection name
	//记录采集内容
	private final String taskCollectionName = "syfc_sales_num_task";
	//记录每一页的内容TODO:为了统一-将syfc_sales_num修改为syfc_sales_num_list
	private final String eachSalesCollectionName = "syfc_sales_num_list";

	
	private final SimpleDateFormat sdf_date = new SimpleDateFormat(this.FORMAT_DATE);
	private final SimpleDateFormat sdf_iso = new SimpleDateFormat(this.FORMAT_ISO);
	private final SimpleDateFormat mongo_iso = new SimpleDateFormat(this.MONGO_ISO);
	
	public void getSlaesNo(JSONObject param) {
		String content = httpclient.get(url, param);
		
		//获取总页数
		int pageCount = parser.getPageCount(content);
		// 记录获取状态 -1 失败，1成功。
		int[] status = new int[pageCount];
		//UUID
		String uuid = UUID.randomUUID().toString().replaceAll("-", "");
		// 记录获取到的页数至mongo
		JSONObject insertJson = new JSONObject();
		insertJson.put("uuid", uuid);
		insertJson.put("page_count", pageCount);
		insertJson.put("status", status);
		insertJson.put("collect_time", mongo_iso.format(new Date()));
		mongoTemplate.insert(insertJson, taskCollectionName);
		
		collectSalesNum(status, uuid);
	}
	/**
	 * 多线程采集，根据status的状态判断是否采集（1不采集，其他采集）
	 * @param status
	 * @param uuid
	 */
	private void collectSalesNum(int[] status, String uuid) {
		//日期字符串，用于区分天标识
		int pageCount = status.length;
		// 根据页数初始化线程池个数
		ExecutorService threadPool = Executors.newFixedThreadPool(pageCount);
		CompletionService<JSONObject> cs = new ExecutorCompletionService<JSONObject>(threadPool);
		int except = 0;
		for (int i = 0; i < pageCount; i++) {
			//http://www.syfc.com.cn/work/ysxk/query_xukezheng.jsp?cur_page=2
			if(status[i] == 1) {
				except++;
				continue;
			}
			int page = i+1;
			SpiderThread task = new SpiderThread(i, "syfc_sales" + i, url+"?cur_page="+page, null);
			logger.debug("submit uri{} pageNo{}",url,page);
			cs.submit(task);
		}
		
		// 获取所有返回值
		int no = 0;
		for (int i = 0; i < pageCount - except; i++) {
			try {
				JSONObject ret = cs.take().get();
				logger.debug("take result {} pageNo{}",ret.toJSONString());
				Boolean success = ret.getBoolean("success");
				//成功是1 ，失败是-1.
				int state = success ? 1 : -1;
				status[no] = state;
				saveResult(status, uuid, ret);
			} catch (Exception e) {
				status[no] = -1;
				throw new ServiceException(SpiderResponseCode.Spider_SALES_PAGE_ERROR,String.format(SpiderResponseCode.Spider_SALES_PAGE_ERROR.detailMsg(), e.getMessage()), e);
			}
			
		}
	}
	
	
	/**
	 * 立即开始方式继续采集
	 * @param taskId
	 */
	public void continueGetSlaesNo(String taskId) {
		int[] status = this.readUnFinishedTask(taskId);
		//使用队列方式采集
		collectSalesNum(status, taskId);
	}
	
	
	/**
	 * 队列方式继续采集
	 * @param taskId
	 */
	public void continueGetSlaesNoByBlocked(String taskId) {
		int[] status = this.readUnFinishedTask(taskId);
		int pageCount = status.length;
		BlockingQueue<SpiderConsumer.Spider> consumerQueue = new LinkedBlockingQueue<SpiderConsumer.Spider>();
		BlockingQueue<JSONObject> producerQueue = new LinkedBlockingQueue<JSONObject>();
		for (int i = 0; i < pageCount; i++) {
			//http://www.syfc.com.cn/work/ysxk/query_xukezheng.jsp?cur_page=2
			if(status[i] == 1) {
				continue;
			}
			int page = i+1;
			//i, "syfc_sales" + i, url+"?cur_page="+page
			try {
				Spider spider = new SpiderConsumer.Spider(i, "syfc_sales" + i, url+"?cur_page="+page,null);
				consumerQueue.put(spider);
			} catch (InterruptedException e) {
				throw new ServiceException(SpiderResponseCode.SPIDER_GET_PRODUCER_ERROR,String.format(SpiderResponseCode.SPIDER_GET_PRODUCER_ERROR.detailMsg(), e.getMessage()), e);
			}
		}
		SpiderConsumer consumer = new SpiderConsumer(consumerQueue, producerQueue);
		
		new Thread(consumer).start();
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				while(true) {
					try {
						JSONObject ret = producerQueue.take();
						Boolean success = ret.getBoolean("success");
						//成功是1 ，失败是-1.
						int state = success ? 1 : -1;
						int no = ret.getInteger("no");
						status[no] = state;
						//save
						saveResult(status, taskId, ret);
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						throw new ServiceException(SpiderResponseCode.Spider_SALES_PAGE_ERROR,String.format(SpiderResponseCode.Spider_SALES_PAGE_ERROR.detailMsg(), e.getMessage()), e);
					}
				}
			}
		}).start();
	}

	private int[] readUnFinishedTask(String taskId) {
		//按照ID读取未完成的任务
		Query query = new Query();
		query.addCriteria(Criteria.where("taskId").is(taskId));
		List<JSONObject> salesNumList = mongoTemplate.find(query, JSONObject.class, eachSalesCollectionName);
		//查找task
		Query salesNumTaskQuery = new Query();
		salesNumTaskQuery.addCriteria(Criteria.where("uuid").is(taskId));
		JSONObject salesNumTask = mongoTemplate.findOne(salesNumTaskQuery, JSONObject.class, taskCollectionName);
		Integer pageCount = salesNumTask.getInteger("page_count");
		// 记录获取状态 -1 失败，1成功。
		int[] status = new int[pageCount];
		for (JSONObject slaesNum : salesNumList) {
			//成功获取到的记录
			Integer no = slaesNum.getInteger("no");
			status[no] = 1;
		}
		for (int i = 0 ; i < status.length ; i++) {
			if(status[i]<1) {
				logger.error("为采集到记录NO{},statue{}",i,status[i]);
			}
		}
		//更新task
		Update update = Update.update("status", status);
		mongoTemplate.updateFirst(salesNumTaskQuery, update, taskCollectionName);
		return status;
	}
	
	private void saveResult(int[] status, String uuid,JSONObject ret) {
		//文件目录名称
		String date_str = sdf_date.format(new Date());
		String folderName = "syfc_sales_num_" + date_str;
		int no;
		String body = ret.getString("body");
		no = ret.getInteger("no");
		// 保存文件
		fileUtils.saveFile(folderName, "syfc_"+no+"_"+sdf_iso.format(new Date()), body);
		// 解析页面
		JSONArray parseSalesNoArray = parser.parseSalesNoList(body);
		// 持久化解析结果到mongo
		try {
			JSONObject salesJsonObject = new JSONObject();
			salesJsonObject.put("sales_no_list", parseSalesNoArray);
			salesJsonObject.put("taskId", uuid);
			salesJsonObject.put("no", no);
			salesJsonObject.put("collect_time", mongo_iso.format(new Date()));
			mongoTemplate.insert(salesJsonObject, eachSalesCollectionName);
		} catch (Exception e) {
			status[no] = -1;
			logger.error("持久化mongo发生错误 NO:{},请求发生错误",no,e);
		}
		//更新一次进度
		Query query = new Query();
		query.addCriteria(Criteria.where("uuid").is(uuid));
		Update update = Update.update("status", status);
		mongoTemplate.updateFirst(query, update, taskCollectionName);
	}
	
	public void recoverSalesNumList() {
		int i = 0;
		try {
			File folder = new File("D:\\body\\syfc\\syfc_sales_num_508665aa75b64389bd27b187f7d18b15");
			//UUID
			if(folder.isDirectory()) {
				File[] listFiles = folder.listFiles();
				String uuid = UUID.randomUUID().toString().replaceAll("-", "");
				int[] status = new int[listFiles.length];
				for (; i < listFiles.length; i++) {
					FileInputStream fis = new FileInputStream(listFiles[i]);
					String content = IOUtils.toString(fis);
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("body", content);
					jsonObject.put("no", i);
					jsonObject.put("name", "syfc_sales" + i);
					status[i] = 1;
					this.saveResult(status, uuid, jsonObject);
				}
				
			}
		} catch (Exception e) {
			logger.error("恢复数据发生错误 NO:{},请求发生错误",i,e);
		}
	}
}
