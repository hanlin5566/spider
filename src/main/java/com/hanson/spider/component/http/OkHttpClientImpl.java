package com.hanson.spider.component.http;

import java.io.IOException;

import com.alibaba.fastjson.JSONObject;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author Hanson
 * create on 2019年2月6日
 */
public class OkHttpClientImpl implements HttpClient{

	@Override
	public String get(String url, JSONObject param) {
		OkHttpClient okHttpClient = new OkHttpClient();
		Request request = new Request.Builder()
				.url(url)
				.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36\"")
				.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
				.addHeader("Accept-Language", "zh-CN,zh;q=0.8")
				.addHeader("ACCEPT-ENCODING", "gzip, deflate, br")
				.addHeader("ACCEPT-LANGUAGE", "zh-CN,zh")
				.build();
		Call call = okHttpClient.newCall(request);
		try {
			Response response = call.execute();
			byte[] bytes = response.body().bytes();
			String body = new String(bytes);
			return body;
		} catch (IOException e) {
		}
		return null;
	}

	@Override
	public String post(String url, JSONObject param) {
		return null;
	}

}

