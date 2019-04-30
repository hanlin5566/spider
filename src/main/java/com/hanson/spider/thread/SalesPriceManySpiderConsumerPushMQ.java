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

public class SalesPriceManySpiderConsumerPushMQ implements Runnable {
	Logger logger = LoggerFactory.getLogger(this.getClass());
    private RabbitMQSender sender;
	
	private String path = "/newbargain/download/findys/ys_info.jsp";
	private String imagePath = "/newbargain/download/findys/image.jsp";
	
	
	private BlockingQueue<Spider> consumerQueue = null;
	private String verifyCodeFolder;
	private String queueName;
	private String datapath;

	public SalesPriceManySpiderConsumerPushMQ(BlockingQueue<Spider> consumerQueue,RabbitMQSender sender,String queueName,String verifyCodeFolder,String datapath) {
		super();
		this.consumerQueue = consumerQueue;
		this.queueName = queueName;
		this.verifyCodeFolder = verifyCodeFolder;
		this.sender = sender;
		this.datapath = datapath;
	}
	private long sleepTime = 1000*3;//五分钟
	private int connectTimeout = 1000*30;//五分钟
	
	@Override
	public void run() {
		while(true) {
			try {
				Spider spider = consumerQueue.take();
				Thread.sleep(sleepTime);
				execute(spider);
			} catch (InterruptedException e) {
				throw new ServiceException(SpiderResponseCode.SPIDER_GET_CONSUMER_ERROR,String.format(SpiderResponseCode.SPIDER_GET_CONSUMER_ERROR.detailMsg(), e.getMessage()), e);
			}
		}
	}


	private void execute(Spider spider) throws InterruptedException {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				boolean success = false;
				JSONObject ret = new JSONObject();
				String content = null;
				int pageNum = spider.getPageNum();
				String url = spider.getUrl();
				String name = spider.getName();
				logger.info("name:{},NO:{},休眠{}",name,pageNum,sleepTime);
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
				    // 读取文件存储位置
				    //保存验证码
				    File folder = new File(verifyCodeFolder);
				    if(!folder.exists()) {
				    	folder.mkdirs();
				    }
				    
				    File file = new File(folder.getPath()+"\\"+pageNum+"_"+System.currentTimeMillis()+".jpg");
				    FileOutputStream fos = new FileOutputStream(file);
				    fos.write(img);
				    fos.close();
				    ITesseract instance = new Tesseract();
				    instance.setLanguage("chi_sim");
				    instance.setDatapath(datapath);
				    //识别验证码
				    String ocrResult = instance.doOCR(file);
				    logger.info(ocrResult);
					//根据预售许可证模拟查询
					//"ys_info.jsp?
				    //http://218.25.83.4:7003/newbargain/download/findys/ys_info.jsp?pages=2&count=16924&kfs=&xmxq=&ldz=&ysid=&yzmcode=nnnn&flagcx=1
				    //pages=2&count=16924&kfs=&xmxq=&ldz=&ysid=&yzmcode=nnnn&flagcx=1
					String searchUrl = "http://"+url+path+"?pages="+pageNum+"&count=16924&kfs=&xmxq=&ldz=&ysid=&yzmcode="+ocrResult+"&flagcx=1";
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
					logger.error("http URI:{} NO:{},请求发生错误",url,pageNum,e);
				}
				logger.info("采集{}",spider.getUrl());
				ret.put("body", content);
				ret.put("pageNum", pageNum);
				ret.put("name", name);
				ret.put("success", success);
				//发送给消息队列
				sender.send(queueName,ret);
			}
		});
		
		t.start();
	}
	
	
	public static class Spider implements Serializable{
		private static final long serialVersionUID = 6811416960086949633L;
		// 页码
		private int pageNum;
		// 序号
		private String name;
		// url
		private String url;
		// param
		private String param;
		public int getPageNum() {
			return pageNum;
		}
		public void setPageNum(int pageNum) {
			this.pageNum = pageNum;
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
		public Spider(int pageNum,String name, String url, String param) {
			super();
			this.pageNum = pageNum;
			this.name = name;
			this.url = url;
			this.param = param;
		}
	}
	
}


