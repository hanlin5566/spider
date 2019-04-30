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
import com.hanson.spider.thread.SalesPriceManySpiderConsumerPushMQ;
import com.hanson.base.exception.ServiceException;

/**
 * @author Hanson
 * create on 2019年2月10日
 * 根据预售许可证采集销售价格列表
 * http://218.25.83.4:7003/newbargain/download/findys/ys_info.jsp
 */
@Service
public class SYFCSalesPriceManyListSpiderService {
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private FileUtils fileUtils;

	@Autowired
	private SYFCParser parser;

	@Autowired
	private MongoTemplate mongoTemplate;
	
	@Autowired
	RabbitMQSender sender;
	
	// 生成模板规则
	private final String FORMAT_ISO = "yyyy-MM-dd_HH-mm-ss";
	private final String FORMAT_DATE = "yyyy-MM-dd";
	//mongo 插入日期格式
	private final String MONGO_ISO = "yyyy-MM-dd HH:mm:ss";
	//mongo collection name
	private final String recordCollectionName = "syfc_sales_price_many_list";
	private final String priceListCollectionName = "syfc_sales_price_list";
	
	
	private final SimpleDateFormat sdf_date = new SimpleDateFormat(this.FORMAT_DATE);
	private final SimpleDateFormat sdf_iso = new SimpleDateFormat(this.FORMAT_ISO);
	private final SimpleDateFormat mongo_iso = new SimpleDateFormat(this.MONGO_ISO);
	//存放验证码的路径
	@Value("${syfc.verifyCode.folder:D:\\body\\syfc\\verifyCode}")
	private String verifyCodeFolder;
	
	//爬取的地址
	@Value("${syfc.salesprice.uri:218.25.83.4:7003}")
	private String url;
	
	@Value("${syfc.tesseract.path:/opt/}")
	private String datapath;
	
	//http://218.25.83.4:7003/newbargain/download/findys/ys_info.jsp?kfs=&xmxq=&ldz=&ysid=&yzmcode=ssss&flagcx=1
	/**
	 * 改为单线程采集 
	 **/
	public void collectPriceList(){
		BlockingQueue<SalesPriceManySpiderConsumerPushMQ.Spider> consumerQueue = new LinkedBlockingQueue<SalesPriceManySpiderConsumerPushMQ.Spider>();
		try {
			List<JSONObject> readLastTask = readUnFinishedTask();
			for (JSONObject jsonObject : readLastTask) {
				Integer pageNum = jsonObject.getInteger("page_num");
				SalesPriceManySpiderConsumerPushMQ.Spider spider = new SalesPriceManySpiderConsumerPushMQ.Spider(pageNum,"syfc_sales_price" + pageNum, url, null);
				consumerQueue.put(spider);
			}
		} catch (InterruptedException e) {
			throw new ServiceException(SpiderResponseCode.SPIDER_GET_PRODUCER_ERROR,String.format(SpiderResponseCode.SPIDER_GET_PRODUCER_ERROR.detailMsg(), e.getMessage()), e);
		}
		String queueName = "syfcSalesPriceManyList";
		SalesPriceManySpiderConsumerPushMQ consumer = new SalesPriceManySpiderConsumerPushMQ(consumerQueue,sender,queueName,verifyCodeFolder,datapath);
		
		new Thread(consumer).start();
	}
	
	public List<JSONObject> readUnFinishedTask() {
		Query query = new Query();
		//不等于1的记录，未采集详情信息的记录
		query.addCriteria(Criteria.where("collect_state").ne(1));
		List<JSONObject> salespriceList = mongoTemplate.find(query, JSONObject.class, recordCollectionName);
		return salespriceList;
	}
	
	public void saveResult(JSONObject ret) {
		//文件目录名称
		String date_str = sdf_date.format(new Date());
		String folderName = "syfc_sales_price_many_list" + date_str;
		String body = ret.getString("body");
		Integer pageNum = ret.getInteger("pageNum");
		Boolean success = ret.getBoolean("success");
		if(!success) {
			logger.error("请求返回体发生错误 pageNum:{},请求发生请求页面失败错误",pageNum);
		}
		// 保存文件
		fileUtils.saveFile(folderName, "syfc_sales_price_many_list"+pageNum+"_"+sdf_iso.format(new Date()), body);
		
		// 解析页面
		JSONArray parseList = parser.parseSalesPriceList(body);
		// 持久化解析结果到mongo
		if(parseList == null || parseList.size() == 0) {
			logger.error("未解析到售价列表,不进行更新。pageNum:{}",pageNum);
			return;
		}
		logger.info("syfc_sales_price_many_list 采集成功，page_num:{},正在入库。",pageNum);
		try {
			Query query = new Query();
			query.addCriteria(Criteria.where("page_num").is(pageNum));
			JSONObject salesPriceManyList = mongoTemplate.findOne(query, JSONObject.class, recordCollectionName);
			if(salesPriceManyList != null ) {
				//update
				Update update = Update.
				update("sales_price_list", parseList)
				.set("update_time", mongo_iso.format(new Date()))
				.set("collect_state",1)//设置状态为1
				;
				mongoTemplate.updateFirst(query, update, recordCollectionName);
			}else {
				logger.error("数据不一致，新增预售许可证价格列表");
				//insert
				JSONObject insert = new JSONObject();
				insert.put("collect_time", mongo_iso.format(new Date()));
				insert.put("sales_price_list", parseList);
				insert.put("page_num", pageNum);
				insert.put("collect_state", 1);
				mongoTemplate.insert(insert,recordCollectionName);
			}
			
			//更新salesprice_list
			for (Object object : parseList) {
				JSONObject json = (JSONObject)object;
				int third_record_id = json.getInteger("sales_price_third_record_id");
				String program_localtion_detail = json.getString("sales_price_program_localtion_detail");
				String approve_date = json.getString("sales_price_approve_date");
				String company = json.getString("sales_price_company");
				String sales_no = json.getString("sales_no");
				String program_describe = json.getString("sales_price_program_describe");
				Query updateQuery = new Query();
				updateQuery.addCriteria(Criteria.where("third_record_id").is(third_record_id));
				JSONObject salesPriceRecord = mongoTemplate.findOne(updateQuery, JSONObject.class, priceListCollectionName);
				if(salesPriceRecord != null ) {
					//update 暂时不更新
//					Update update = Update.
//					update("sales_price_list", parseList)
//					update.("program_localtion_detail", program_localtion_detail).
//					set("approve_date", approve_date);
//					insert.put("company", company);
//					insert.put("sales_no", sales_no);
//					insert.put("program_describe", program_describe);
//					.set("update_time", mongo_iso.format(new Date()))
//					;
//					mongoTemplate.updateFirst(updateQuery, update, recordCollectionName);
				}else {
					//insert
					JSONObject insert = new JSONObject();
					insert.put("collect_time", mongo_iso.format(new Date()));
					insert.put("collect_state", 0);
					insert.put("sync_state", 0);//是否同步到sales_price_detail
					insert.put("third_record_id", third_record_id);
					insert.put("program_localtion_detail", program_localtion_detail);
					insert.put("approve_date", approve_date);
					insert.put("company", company);
					insert.put("sales_no", sales_no);
					insert.put("program_describe", program_describe);
					mongoTemplate.insert(insert,priceListCollectionName);
				}
			}
		} catch (Exception e) {
			logger.error("持久化mongo发生错误 pageNum:{},请求发生错误",pageNum,e);
			throw new ServiceException(SpiderResponseCode.Spider_SALES_PAGE_ERROR);
		}
	}
	
	public void initSalesPriceManyList() {
		for (int i = 1; i <= 1694; i++) {
			Query query = new Query();
			query.addCriteria(Criteria.where("page_num").is(i));
			Update update = Update.
			update("page_num",i);
			mongoTemplate.upsert(query, update, recordCollectionName);
		}
	}
}

