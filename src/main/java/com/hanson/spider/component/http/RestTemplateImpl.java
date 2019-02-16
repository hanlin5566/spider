package com.hanson.spider.component.http;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONObject;

/**
 * @author Hanson create on 2018年3月11日
 */
@Component
public class RestTemplateImpl implements HttpClient {
	@Override
	public String get(String url, JSONObject param) {
		RestTemplate restTemplate = new RestTemplate();
		return restTemplate.getForObject(url,String.class);
	}

	@Override
	public String post(String url, JSONObject param) {
		return null;
	}
}
