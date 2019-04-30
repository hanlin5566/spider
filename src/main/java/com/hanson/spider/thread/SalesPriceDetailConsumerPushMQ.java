package com.hanson.spider.thread;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.hanson.spider.component.rabbitmq.RabbitMQSender;
import com.hanson.spider.misc.SpiderResponseCode;
import com.hanson.base.exception.ServiceException;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

/**
 * @author Hanson create on 2019年2月8日
 */

public class SalesPriceDetailConsumerPushMQ implements Runnable {
	Logger logger = LoggerFactory.getLogger(this.getClass());
    private RabbitMQSender sender;
    //http://218.25.83.4:7003/newbargain/download/findys/showPrice.jsp?buildingID=444323
	private String path = "/newbargain/download/findys/showPrice.jsp";
	private String imagePath = "/newbargain/download/findys/image.jsp";
	
	
	private BlockingQueue<Spider> consumerQueue = null;
	private String verifyCodeFolder;
	private String queueName;
	private String datapath;

	public SalesPriceDetailConsumerPushMQ(BlockingQueue<Spider> consumerQueue,RabbitMQSender sender,String queueName,String verifyCodeFolder,String datapath) {
		super();
		this.consumerQueue = consumerQueue;
		this.queueName = queueName;
		this.verifyCodeFolder = verifyCodeFolder;
		this.sender = sender;
		this.datapath = datapath;
	}
	private long sleepTime = 1000*2;//五分钟
	private int connectTimeout = 1000*30;//五分钟
	
	@Override
	public void run() {
		while(true) {
			try {
				Spider spider = consumerQueue.take();
				boolean success = false;
				JSONObject ret = new JSONObject();
				String content = null;
				int id = spider.getId();
				String url = spider.getUrl();
				String name = spider.getName();
				logger.info("name:{},NO:{},休眠{}",name,id,sleepTime);
				Thread.sleep(sleepTime);
				try {
					
					//首次获取页面
			        Connection connect = Jsoup.connect("http://"+url+path);
			        // 伪造请求头
			        connect.header("Accept", "application/json, text/javascript, */*; q=0.01").header("Accept-Encoding",
			                "gzip, deflate");
			        connect.header("Accept-Language", "zh-CN,zh;q=0.9").header("Connection", "keep-alive");
			        connect.header("Content-Length", "213").header("Content-Type",
			                "application/x-www-form-urlencoded; charset=UTF-8");
			        connect.header("Host", url+path).header("Referer", "http://"+url+path);
			        connect.header("User-Agent",
			                "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36")
			                .header("X-Requested-With", "XMLHttpRequest");
			        connect.timeout(connectTimeout);
			        // 请求url获取响应信息
			        Response res = connect.ignoreContentType(true).method(Method.GET).execute();// 执行请求
			        // 获取返回的cookie
			        Map<String, String> cookies = res.cookies();
			        //携带cooike获取验证码
			        String image_url = "http://"+url+imagePath+"?"+System.currentTimeMillis();
			        Connection image_connect = Jsoup.connect(image_url);
			        image_connect.cookies(cookies);// 携带cookies爬取图片
			        image_connect.timeout(5 * 10000);
			        Connection.Response iamge_response = image_connect.ignoreContentType(true).execute();
			        byte[] img = iamge_response.bodyAsBytes();
			        //读取文件存储位置
			        //保存验证码
			        File folder = new File(verifyCodeFolder+"\\"+"price_detail");
			        if(!folder.exists()) {
			        	folder.mkdirs();
			        }
			        
			        File file = new File(folder.getPath()+"\\"+id+"_"+System.currentTimeMillis()+".jpg");
			        FileOutputStream fos = new FileOutputStream(file);
			        fos.write(img);
			        fos.close();
			        ITesseract instance = new Tesseract();
				    instance.setLanguage("chi_sim");
			        instance.setDatapath(datapath);
			        //识别验证码
			        String ocrResult = instance.doOCR(file);
			        logger.info("verifyCode:{}",ocrResult);
			    	//模拟查询
			    	String searchUrl = "http://"+url+path+"?buildingID="+id+"&yzmcode="+ocrResult+"&flagcx=1";
			    	searchUrl = searchUrl.replaceAll("\r|\n","");
			    	Connection searchConnection = Jsoup.connect(searchUrl);  
			    	
			    	searchConnection.header("Accept", "text/html, application/xhtml+xml, image/jxr, */*")
			    	.header("Accept-Encoding","gzip, deflate");
			    	searchConnection.header("Accept-Language", "zh-CN,zh;q=0.9").header("Connection", "keep-alive");
			    	searchConnection.cookies(cookies);// 携带cookies爬取图片
			    	searchConnection.timeout(connectTimeout);
			    	Connection.Response searchResponse = searchConnection.ignoreContentType(true).method(Method.GET).cookies(cookies).execute();
			    	//返回结果
			    	content = searchResponse.body();
					success = true;
				} catch (Exception e) {
					logger.error("http URI:{} NO:{},请求发生错误",url,id,e);
				}
				logger.info("采集{}",spider.getUrl());
				ret.put("body", content);
				ret.put("id", id);
				ret.put("name", name);
				ret.put("success", success);
				//发送给消息队列
				sender.send(queueName,ret);
			} catch (InterruptedException e) {
				throw new ServiceException(SpiderResponseCode.SPIDER_GET_CONSUMER_ERROR,String.format(SpiderResponseCode.SPIDER_GET_CONSUMER_ERROR.detailMsg(), e.getMessage()), e);
			}
		}
	}
	
	
	public static class Spider implements Serializable{
		private static final long serialVersionUID = 6811416960086949633L;
		// 标识
		private int id;
		// 序号
		private String name;
		// url
		private String url;
		// param
		private String param;
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
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
		public Spider(int id,String name, String url, String param) {
			super();
			this.id = id;
			this.name = name;
			this.url = url;
			this.param = param;
		}
	}
	
}


