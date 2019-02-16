package com.hanson.spider.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.hanson.spider.component.FileUtils;
import com.hanson.spider.component.http.HttpClient;
import com.hanson.spider.component.parser.AnjukeParser;

/**
 * @author Hanson
 * create on 2018年3月11日
 */
@Service
public class AnjukeSpiderService {
	@Autowired
	HttpClient httpclient;
	
	@Value("${anjuke.new.url}")
	String url;
	
	@Autowired
	FileUtils fileUtils;
	
	@Autowired
	AnjukeParser parser;
	
	public String getAllNews(JSONObject param){
		String body = httpclient.get(url, param);
		//保存文件
        fileUtils.saveFile("anjuke","anjuke", body);
        //解析文件
        parser.parseNewsList(body);
		return body;
	}
}

