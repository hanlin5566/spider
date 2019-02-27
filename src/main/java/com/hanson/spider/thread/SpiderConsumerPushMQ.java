package com.hanson.spider.thread;

import java.io.Serializable;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONObject;
import com.hanson.spider.component.rabbitmq.RabbitMQSender;
import com.hanson.spider.misc.SpiderResponseCode;
import com.hzcf.base.exception.ServiceException;

/**
 * @author Hanson create on 2019年2月8日
 */

public class SpiderConsumerPushMQ implements Runnable {
	
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private BlockingQueue<Spider> consumerQueue = null;
	private RabbitMQSender sender;
	private String queueName;

	public SpiderConsumerPushMQ(BlockingQueue<Spider> consumerQueue,RabbitMQSender sender,String queueName) {
		super();
		this.consumerQueue = consumerQueue;
		this.queueName = queueName;
		this.sender = sender;
	}
	private long sleepTime = 1000*5;//五分钟
	private int connectTimeout = 1000*30;//五分钟
	private int readTimeout = 1000*30;//五分钟
	
	@Override
	public void run() {
		while(true) {
			try {
				Spider spider = consumerQueue.take();
				boolean success = false;
				JSONObject ret = new JSONObject();
				String content = null;
				int no = spider.getNo();
				String id = spider.getId();
				String url = spider.getUrl();
				String name = spider.getName();
				logger.info("name:{},NO:{},休眠{}",name,no,sleepTime);
				Thread.sleep(sleepTime);
				try {
					SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();  
					requestFactory.setConnectTimeout(connectTimeout);// 设置超时  
					requestFactory.setReadTimeout(readTimeout);  
					//设置代理
//					SocketAddress address = new InetSocketAddress("115.151.1.96", 808);
//					Proxy proxy = new Proxy(Proxy.Type.HTTP, address);
//					requestFactory.setProxy(proxy);
					//利用复杂构造器可以实现超时设置，内部实际实现为 HttpClient  
					RestTemplate restTemplate = new RestTemplate(requestFactory);  
					content = restTemplate.getForObject(spider.getUrl(),String.class);
//					FileInputStream fis = new FileInputStream("D:\\body\\syfc_sales_num_detail2019-02-09\\syfc_salesNoDetail_1_2019-02-09_04-22-50");
//					content = IOUtils.toString(fis);
					success = true;
				} catch (Exception e) {
					logger.error("http URI:{} NO:{},请求发生错误",url,no,e);
				}
				logger.info("采集{},NO{},ID{}",spider.getUrl(),no,id);
				ret.put("body", content);
				ret.put("no", no);
				ret.put("id", id);
				ret.put("name", name);
				ret.put("success", success);
//				producerQueue.put(ret);
				//发送给消息队列
				sender.send(queueName,ret);
			} catch (InterruptedException e) {
				throw new ServiceException(SpiderResponseCode.SPIDER_GET_CONSUMER_ERROR,String.format(SpiderResponseCode.SPIDER_GET_CONSUMER_ERROR.detailMsg(), e.getMessage()), e);
			}
		}
	}
	
	
	public static class Spider implements Serializable{
		private static final long serialVersionUID = 2097027720043257694L;
		// 序号
		private int no;
		//标识
		private String id;
		// 序号
		private String name;
		// url
		private String url;
		// param
		private String param;
		
		public int getNo() {
			return no;
		}
		public void setNo(int no) {
			this.no = no;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getUrl() {
			return url;
		}
		public void setUrl(String url) {
			this.url = url;
		}
		public String getParam() {
			return param;
		}
		public void setParam(String param) {
			this.param = param;
		}
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public Spider(int no, String id, String name, String url, String param) {
			super();
			this.no = no;
			this.id = id;
			this.name = name;
			this.url = url;
			this.param = param;
		}
	}
	
}


