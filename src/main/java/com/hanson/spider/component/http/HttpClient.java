package com.hanson.spider.component.http;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSONObject;

/**
 * @author Hanson create on 2018年3月11日
 */
@Resource
public interface HttpClient {
	public String get(String url, JSONObject param);
	public String post(String url, JSONObject param);
}
