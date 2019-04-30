package com.hanson.spider.thread;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONObject;

/**
 * @author Hanson
 * create on 2019年2月6日
 */
public class SpiderThread implements Callable<JSONObject>{
	Logger logger = LoggerFactory.getLogger(this.getClass());
	//序号
	private int no;
	//序号
	private String name;
	//url
	private String url;
	//param
	private String param;

	public SpiderThread(int no, String name, String url, String param) {
		super();
		this.no = no;
		this.name = name;
		this.url = url;
		this.param = param;
	}

	@Override
	public JSONObject call() throws Exception {
		//body
		//模拟岩石
		long sleepTime = (long) (Math.random()*1000);
		logger.debug("NO:{},休眠{}",no,sleepTime);
		Thread.sleep(sleepTime);
		
		JSONObject ret = new JSONObject();
		RestTemplate restTemplate = new RestTemplate();
		String content = restTemplate.getForObject(url,String.class);
		ret.put("body", content);
		ret.put("no", this.no);
		ret.put("name", this.name);
		return ret;
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
}

