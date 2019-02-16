package com.hanson.spider.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
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
import com.hzcf.base.exception.ServiceException;

/**
 * @author Hanson create on 2018年3月11日
 * 从预售许可证列表，采集详情
 * http://www.syfc.com.cn/work/ysxk/ysxkzinfo.jsp?id=23265185
 */
@Service
public class SYFCSalesNumDetailSpiderService {
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
	private final String salesNumberRecordCollectionName = "syfc_sales_num_detail";

	
	private final SimpleDateFormat sdf_date = new SimpleDateFormat(this.FORMAT_DATE);
	private final SimpleDateFormat sdf_iso = new SimpleDateFormat(this.FORMAT_ISO);
	private final SimpleDateFormat mongo_iso = new SimpleDateFormat(this.MONGO_ISO);
	
	/**
	 * 队列方式继续采集
	 * @param taskId
	 */
	public void collectSalesNumDetail() {
		BlockingQueue<SpiderConsumerPushMQ.Spider> consumerQueue = new LinkedBlockingQueue<SpiderConsumerPushMQ.Spider>();
		List<JSONObject> unfinishedList = this.readUnFinishedTask();
		for (int i = 0; i < unfinishedList.size(); i++) {
			JSONObject jsonObject = unfinishedList.get(i);
			String deltailUri = jsonObject.getString("deltail_uri");
			int no = jsonObject.getInteger("no");
			try {
				//http://www.syfc.com.cn/work/ysxk/ysxkzinfo.jsp?id=23265185
				SpiderConsumerPushMQ.Spider spider = new SpiderConsumerPushMQ.Spider(no, "syfc_sales_num_detail" + no, url+ deltailUri,null);
				consumerQueue.put(spider);
			} catch (InterruptedException e) {
				throw new ServiceException(SpiderResponseCode.SPIDER_GET_PRODUCER_ERROR,String.format(SpiderResponseCode.SPIDER_GET_PRODUCER_ERROR.detailMsg(), e.getMessage()), e);
			}
		}
		String queueName = "syfcSalesNumDetail";
		SpiderConsumerPushMQ consumer = new SpiderConsumerPushMQ(consumerQueue,sender,queueName);
		
		new Thread(consumer).start();
		
	}
	
	/**
	 * 根据 预售许可证号 比较，增量添加 预售许可证列表
	 * collection格式为collection_new
	 */
	AtomicInteger noCount = new AtomicInteger(0);
	public void incrementCollectSalesNo() {
		int connectTimeout = 1000*30;//五分钟
		int readTimeout = 1000*30;//五分钟
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();  
		requestFactory.setConnectTimeout(connectTimeout);// 设置超时  
		requestFactory.setReadTimeout(readTimeout);  
		//设置代理
//		SocketAddress address = new InetSocketAddress("115.151.1.96", 808);
//		Proxy proxy = new Proxy(Proxy.Type.HTTP, address);
//		requestFactory.setProxy(proxy);
		//利用复杂构造器可以实现超时设置，内部实际实现为 HttpClient  
		RestTemplate restTemplate = new RestTemplate(requestFactory);  
		String url = "http://www.syfc.com.cn/work/ysxk/query_xukezheng.jsp";
		String body = restTemplate.getForObject(url,String.class);
		JSONArray parseSalesNoList = parser.parseSalesNoList(body);
		//查找采集到的最后一个预售许可证，按照审批时间倒序
		Query query = new Query();  
		query.with(new Sort(new Order(Direction.DESC,"date")));
		JSONObject lastSalesDetail = mongoTemplate.findOne(query, JSONObject.class, salesNumberRecordCollectionName);
		int no = lastSalesDetail.getInteger("sales_no");
		String date = lastSalesDetail.getString("date");
		for (Object object : parseSalesNoList) {
			JSONObject salesNO = (JSONObject)object;
			//重置reNo
			int reNo = noCount.decrementAndGet();
			String currentDate = salesNO.getString("date");
			int currentNo = salesNO.getIntValue("sales_no");
			try {
				long dateLong = sdf_date.parse(date).getTime();
				long currentDateLong = sdf_date.parse(currentDate).getTime();
				if(currentNo > no && currentDateLong > dateLong) {
					//大于最后一次采集的许可证号贼新增
					salesNO.put("no", reNo);
					salesNO.put("collect_time", mongo_iso.format(new Date()));
					mongoTemplate.insert(salesNO,salesNumberRecordCollectionName+"_new");
				}
			} catch (Exception e) {
				logger.error("新增预售许可证错误，解析异常{}",no);
			}
		}
	}

	private List<JSONObject> readUnFinishedTask() {
		Query query = new Query();
		//不等于1的记录，未采集详情信息的记录
		query.addCriteria(Criteria.where("collect_state").ne(1));
		List<JSONObject> salesNumList = mongoTemplate.find(query, JSONObject.class, salesNumberRecordCollectionName);
		return salesNumList;
	}
	
	public void saveResult(JSONObject ret) {
		//文件目录名称
		String date_str = sdf_date.format(new Date());
		String folderName = "syfc_sales_num_detail" + date_str;
		int no;
		String body = ret.getString("body");
		Boolean success = ret.getBoolean("success");
		no = ret.getInteger("no");
		if(!success) {
			logger.error("持久化mongo发生错误 NO:{},请求发生请求页面失败错误",no);
		}
		logger.info("syfc_sales_num_detail 采集成功，NO:{},正在入库。",no);
		// 保存文件
		fileUtils.saveFile(folderName, "syfc_salesNoDetail_"+no+"_"+sdf_iso.format(new Date()), body);
		// 解析页面
		JSONObject parseSalesNoDetail = parser.parseSalesNoDetail(body);
		// 持久化解析结果到mongo
		try {
			Query salesNumDetailQuery = new Query();
			salesNumDetailQuery.addCriteria(Criteria.where("third_record_id").is(parseSalesNoDetail.get("third_record_id")));
			JSONObject salesNumRecord = mongoTemplate.findOne(salesNumDetailQuery, JSONObject.class, salesNumberRecordCollectionName);
			if(salesNumRecord != null ) {
				//update
				Update update = Update.
				update("build_count", parseSalesNoDetail.get("build_count"))
				.set("total_build_area", parseSalesNoDetail.get("build_count"))
				.set("sales_area", parseSalesNoDetail.get("sales_area"))
				.set("dwelling_area", parseSalesNoDetail.get("dwelling_area"))
				.set("shop_area", parseSalesNoDetail.get("shop_area"))
				.set("public_area", parseSalesNoDetail.get("public_area"))
				.set("other_area", parseSalesNoDetail.get("other_area"))
				.set("approve_date", parseSalesNoDetail.get("approve_date"))
				.set("dwelling_build_no", parseSalesNoDetail.get("dwelling_build_no"))
				.set("shop_build_no", parseSalesNoDetail.get("shop_build_no"))
				.set("public_build_no", parseSalesNoDetail.get("public_build_no"))
				.set("other_build_no", parseSalesNoDetail.get("other_build_no"))
				.set("dwelling_build_count", parseSalesNoDetail.get("dwelling_build_count"))
				.set("shop_build_count", parseSalesNoDetail.get("shop_build_count"))
				.set("public_build_count", parseSalesNoDetail.get("public_build_count"))
				.set("other_build_count", parseSalesNoDetail.get("other_build_count"))
				.set("remark", parseSalesNoDetail.get("remark"))
				.set("update_time", mongo_iso.format(new Date()))
				.set("collect_state",1)//设置状态为1
				;
				mongoTemplate.updateFirst(salesNumDetailQuery, update, salesNumberRecordCollectionName);
			}else {
				//insert
				logger.error("数据不一致，新增预售许可证详情");
				parseSalesNoDetail.put("collect_time", mongo_iso.format(new Date()));
				mongoTemplate.insert(parseSalesNoDetail,salesNumberRecordCollectionName);
			}
		} catch (Exception e) {
			logger.error("持久化mongo发生错误 NO:{},请求发生错误",no,e);
			throw e;
		}
	}
}