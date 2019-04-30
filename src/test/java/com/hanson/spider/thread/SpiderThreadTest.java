package com.hanson.spider.thread;

import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Ignore;
import org.junit.Test;

import com.alibaba.fastjson.JSONObject;

/**
 * @author Hanson create on 2019年2月7日
 */

public class SpiderThreadTest {

	@Test
	@Ignore
	public void test() throws IOException {
		FileInputStream fis;
		String body;
		try {
			fis = new FileInputStream("D:\\body\\fangtianxia\\ftx.html");
			body = IOUtils.toString(fis);
			Document doc = Jsoup.parse(body);
			Elements new_list = doc.select("#newhouse_loupai_list").get(0).getElementsByTag("li");
			for (Element element : new_list) {
				Elements detail = element.select(".nlc_details");
				String name = detail.select(".nlcd_name").text();
				String type = detail.select(".house_type").text();
				String relative_message = detail.select(".relative_message").text();
				String fangyuan = detail.select(".fangyuan").text();
				String nhouse_price = detail.select(".nhouse_price").text();
				JSONObject obj = new JSONObject();
				obj.put("name",name);
				obj.put("type",type);
				obj.put("relative_message",relative_message);
				obj.put("fangyuan",fangyuan);
				obj.put("nhouse_price",nhouse_price);
				System.out.println(obj.toJSONString());
			}
			fis.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

}
