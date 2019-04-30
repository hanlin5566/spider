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
public class AnjukeParser {
	public JSONObject parseNewsList(String html) {
		JSONObject ret = new JSONObject();
		Document doc = Jsoup.parse(html);
		Elements items = doc.select(".list-contents .list-results .key-list .item-mod");
		for (Element element : items) {
			String name = element.select(".lp-name .items-name").text();
			String price = element.select(".price").text();
			String address = element.select(".address .list-map").text();
			String huxing = element.select(".huxing").text();
			String tags = element.select(".tags-wrap").text();
			String top = element.select(".group-mark").text();
			//data-link="https://shen.fang.anjuke.com/loupan/415033.html"
			String dataLink = element.attr("data-link");//详情页连接
			String thirdPartId = dataLink.substring(dataLink.lastIndexOf("/")+1, dataLink.lastIndexOf("\\\\.html"));//第三方ID
			ret.put("name", name);//楼盘名称
			ret.put("detailLink", dataLink);//楼盘名称
			ret.put("thirdPartId", thirdPartId);//第三方ID
			ret.put("price", price);//楼盘均价
			ret.put("address", address);//地址
			ret.put("huxing", huxing);//户型
			ret.put("promotion", huxing);//优惠
			ret.put("tag", tags);//标签
			ret.put("top", top);//人气榜
		}
		return ret;
	}
}

