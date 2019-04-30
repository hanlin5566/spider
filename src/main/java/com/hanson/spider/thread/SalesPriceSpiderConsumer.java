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
import com.hanson.spider.misc.SpiderResponseCode;
import com.hanson.base.exception.ServiceException;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

/**
 * @author Hanson create on 2019年2月8日
 */

public class SalesPriceSpiderConsumer implements Runnable {
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private String path = "/newbargain/download/findys/ys_info.jsp";
	private String imagePath = "/newbargain/download/findys/image.jsp";
	
	
	private BlockingQueue<Spider> consumerQueue = null;
	private BlockingQueue<JSONObject> producerQueue = null;
	private String verifyCodeFolder;

	public SalesPriceSpiderConsumer(BlockingQueue<Spider> consumerQueue,BlockingQueue<JSONObject> producerQueue,String verifyCodeFolder) {
		super();
		this.consumerQueue = consumerQueue;
		this.producerQueue = producerQueue;
		this.verifyCodeFolder = verifyCodeFolder;
	}
	private long sleepTime = 1000*10;//五分钟
	private int connectTimeout = 1000*30;//五分钟
	
	@Override
	public void run() {
		while(true) {
			try {
				Spider spider = consumerQueue.take();
				boolean success = false;
				JSONObject ret = new JSONObject();
				String content = null;
				int no = spider.getNo();
				String salesNo = spider.getSalesNo();
				String url = spider.getUrl();
				String name = spider.getName();
				logger.info("name:{},NO:{},休眠{}",name,no,sleepTime);
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
			        // 读取文件存储位置
			        //保存验证码
			        File folder = new File(verifyCodeFolder);
			        if(!folder.exists()) {
			        	folder.mkdirs();
			        }
			        File file = new File(folder.getPath()+"\\"+salesNo+".jpg");
			        FileOutputStream fos = new FileOutputStream(file);
			        fos.write(img);
			        fos.close();
			        ITesseract instance = new Tesseract();
			        instance.setLanguage("chi_sim");
			        String datapath = this.getClass().getResource("/").getPath();
				    datapath = datapath.substring(1, datapath.length());
			        instance.setDatapath(datapath);
			        //识别验证码
			        String ocrResult = instance.doOCR(file);
			    	//根据预售许可证模拟查询
			    	//"ys_info.jsp?
			    	String searchUrl = "http://"+url+path+"?ysid="+salesNo+"&yzmcode="+ocrResult+"&flagcx=1";
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
					logger.error("http URI:{} NO:{},请求发生错误",url,no,e);
				}
				logger.info("采集{}",spider.getUrl());
				ret.put("body", content);
				ret.put("no", no);
				ret.put("sales_no", salesNo);
				ret.put("name", name);
				ret.put("success", success);
				producerQueue.put(ret);
			} catch (InterruptedException e) {
				throw new ServiceException(SpiderResponseCode.SPIDER_GET_CONSUMER_ERROR,String.format(SpiderResponseCode.SPIDER_GET_CONSUMER_ERROR.detailMsg(), e.getMessage()), e);
			}
		}
	}
	
	
	public static class Spider implements Serializable{
		private static final long serialVersionUID = 6811416960086949633L;
		// 序号
		private int no;
		// 预售许可证
		private String salesNo;
		// 序号
		private String name;
		// url
		private String url;
		// param
		private String param;
		public String getSalesNo() {
			return salesNo;
		}
		public void setSalesNo(String salesNo) {
			this.salesNo = salesNo;
		}
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
		public Spider(int no, String salesNo, String name, String url, String param) {
			super();
			this.no = no;
			this.salesNo = salesNo;
			this.name = name;
			this.url = url;
			this.param = param;
		}
	}
	
}


