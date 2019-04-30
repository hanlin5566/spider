package com.hanson.spider.component.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

/**
 * @author Hanson
 * create on 2018年3月11日
 */
@Component
public class FangtianxiaParser {
	public JSONObject parseNewList(String body) {
		JSONObject ret = new JSONObject();
		Document doc = Jsoup.parse(body);
		Elements items = doc.select(".list-contents .list-results .key-list .item-mod");
		for (Element element : items) {
			String name = element.select(".lp-name .items-name").text();
			String price = element.select(".price").text();
			String address = element.select(".address .list-map").text();
			String huxing = element.select(".huxing").text();
			ret.put("name", name);//楼盘名称
			ret.put("price", price);//楼盘价格
			ret.put("address", address);//地址
			ret.put("huxing", huxing);//户型
			ret.put("promotion", huxing);//优惠
		}
		return ret;
	}
}

