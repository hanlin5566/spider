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
import com.hanson.spider.thread.SalesPriceDetailConsumerPushMQ;
import com.hzcf.base.exception.ServiceException;

/**
 * @author Hanson
 * create on 2019年2月10日
 * 根据预售许可证采集销售价格列表
 * http://218.25.83.4:7003/newbargain/download/findys/ys_info.jsp
 */
@Service
public class SYFCSalesPriceDetailSpiderService {
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
	private final String recordCollectionName = "syfc_sales_price_detail";
	private final String priceListCollectionName = "syfc_sales_price_many_list";
	
	
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
	
	//http://218.25.83.4:7003/newbargain/download/findys/showPrice.jsp?buildingID=444323
	public void collect(){
		BlockingQueue<SalesPriceDetailConsumerPushMQ.Spider> consumerQueue = new LinkedBlockingQueue<SalesPriceDetailConsumerPushMQ.Spider>();
		try {
			List<JSONObject> readLastTask = readUnFinishedTask();
			for (JSONObject jsonObject : readLastTask) {
				Integer third_record_id = jsonObject.getInteger("third_record_id");
				SalesPriceDetailConsumerPushMQ.Spider spider = new SalesPriceDetailConsumerPushMQ.Spider(third_record_id,"syfc_sales_price_detail" + third_record_id, url, null);
				consumerQueue.put(spider);
			}
		} catch (InterruptedException e) {
			throw new ServiceException(SpiderResponseCode.SPIDER_GET_PRODUCER_ERROR,String.format(SpiderResponseCode.SPIDER_GET_PRODUCER_ERROR.detailMsg(), e.getMessage()), e);
		}
		String queueName = "syfcSalesPriceDetail";
		SalesPriceDetailConsumerPushMQ consumer = new SalesPriceDetailConsumerPushMQ(consumerQueue,sender,queueName,verifyCodeFolder,datapath);
		
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
		String folderName = "syfc_sales_price_detail" + date_str;
		String body = ret.getString("body");
		Integer id = ret.getInteger("id");
		Boolean success = ret.getBoolean("success");
		if(!success) {
			logger.error("请求返回体发生错误 pageNum:{},请求发生请求页面失败错误",id);
		}
		// 保存文件
		fileUtils.saveFile(folderName, "syfc_sales_price_detail"+id+"_"+sdf_iso.format(new Date()), body);
		
		// 解析页面
		JSONArray parseList = parser.parsePriceDetail(body);
		// 持久化解析结果到mongo
		if(parseList == null || parseList.size() == 0) {
			logger.error("未解析到售价列表,不进行更新。id:{}",id);
			return;
		}
		logger.info("syfc_sales_price_detail 采集成功，id:{},正在入库。",id);
		try {
			Query salesNumDetailQuery = new Query();
			salesNumDetailQuery.addCriteria(Criteria.where("third_record_id").is(id));
			JSONObject salesNumRecord = mongoTemplate.findOne(salesNumDetailQuery, JSONObject.class, recordCollectionName);
			if(salesNumRecord != null ) {
				//update
				Update update = Update.
				update("sales_price_detail_list", parseList)
				.set("update_time", mongo_iso.format(new Date()))
				.set("collect_state",1)//设置状态为1
				;
				mongoTemplate.updateFirst(salesNumDetailQuery, update, recordCollectionName);
			}else {
				logger.error("数据不一致，新增预售许可证价格列表");
				//insert
				JSONObject insert = new JSONObject();
				insert.put("collect_time", mongo_iso.format(new Date()));
				insert.put("sales_price_detail_list", parseList);
				insert.put("third_record_id", id);
				insert.put("collect_state", 1);
				mongoTemplate.insert(insert,recordCollectionName);
			}
		} catch (Exception e) {
			logger.error("持久化mongo发生错误 pageNum:{},请求发生错误",id,e);
			throw new ServiceException(SpiderResponseCode.Spider_SALES_PAGE_ERROR);
		}
	}
	
	public void initSalesPriceDetail() {
		Query query = new Query();
		//采集成功的售价ID列表
		query.addCriteria(Criteria.where("collect_state").is(1));
		List<JSONObject> list = mongoTemplate.find(query,JSONObject.class, priceListCollectionName);
		for (JSONObject json : list) {
			JSONArray salesPriceList = json.getJSONArray("sales_price_list");
			for (Object salesPrice : salesPriceList) {
				//初始化任务
				/**
				 *  "sales_price_sub_no" : "1",
		            "sales_price_program_describe" : "居住、商业（二期）",
		            "sales_no" : "19025",
		            "sales_price_deltail_uri" : "javascript:looklpb('484446')",
		            "sales_price_company" : "沈阳润地房地产有限公司",
		            "sales_price_approve_date" : "2019-01-23",
		            "sales_price_program_localtion_detail" : "浑南区长安桥南街9-19号",
		            "sales_price_third_record_id" : "484446"
				 */
				JSONObject salesPriceJsonObject = (JSONObject)salesPrice;
				int third_record_id = salesPriceJsonObject.getInteger("sales_price_third_record_id");
				String program_localtion_detail = salesPriceJsonObject.getString("sales_price_program_localtion_detail");
				String approve_date = salesPriceJsonObject.getString("sales_price_approve_date");
				String company = salesPriceJsonObject.getString("sales_price_company");
				String sales_no = salesPriceJsonObject.getString("sales_no");
				String program_describe = salesPriceJsonObject.getString("sales_price_program_describe");
				
				Query updateQuery = new Query();
				updateQuery.addCriteria(Criteria.where("third_record_id").is(third_record_id));
				JSONObject salesNumRecord = mongoTemplate.findOne(updateQuery, JSONObject.class, recordCollectionName);
				if(salesNumRecord != null ) {
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
					insert.put("init_time", mongo_iso.format(new Date()));
					insert.put("third_record_id", third_record_id);
					insert.put("program_localtion_detail", program_localtion_detail);
					insert.put("approve_date", approve_date);
					insert.put("company", company);
					insert.put("sales_no", sales_no);
					insert.put("program_describe", program_describe);
					mongoTemplate.insert(insert,recordCollectionName);
				}
			}
		}
	}
}
