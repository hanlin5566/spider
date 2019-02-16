package com.hanson.spider.service;

import java.net.URLEncoder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.hanson.spider.component.FileUtils;
import com.hanson.spider.component.http.HttpClient;
import com.hanson.spider.component.parser.FangtianxiaParser;

/**
 * @author Hanson
 * create on 2018年3月11日
 */
@Service
public class FangtianxiaSpiderService {
	@Value("${fangtianxia.new.url}")
	String url;
	
	@Autowired
	HttpClient httpclient;
	
	@Autowired
	FileUtils fileUtils;
	
	@Autowired
	FangtianxiaParser parser;
	
	public String getAllNews(JSONObject param){
        String body = httpclient.get(url, param);
        System.out.println(body);
        try {
        	String result=new String(body.getBytes("GB2312"),"UTF-8");
        	System.out.println(result);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
        try {
        	String urlEncode = URLEncoder.encode (body, "UTF-8" );
        	System.out.println(urlEncode);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
        fileUtils.saveFile("fangtianxia","fangtianxia", body);
        parser.parseNewList(body);
		return body;
	}
}

