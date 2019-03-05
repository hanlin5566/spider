package com.hanson.spider.parser;

import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Ignore;
import org.junit.Test;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hanson.spider.misc.SaleStateEnum;

/**
 * @author Hanson
 * create on 2019年2月6日
 */

public class SYFCNewBuildParserTest {
	/**
	 * {
	"public_build_no": "8",
	"program_localtion": "于洪区黑山路",
	"other_build_no": "2、4、9",
	"shop_area": "4922",
	"public_build_count": "1",
	"sales_area": "99635",
	"other_area": "车库1047",
	"shop_build_count": "40",
	"other_build_count": "40",
	"sales_no": "经2001004",
	"total_build_area": "99635",
	"shop_build_no": "1、7",
	"remark": "",
	"dwelling_area": "89180",
	"dwelling_build_count": "796",
	"public_area": "2334",
	"company": "沈阳市南泰房屋开发有限公司",
	"approve_date": "1990-01-01",
	"build_count": 14,
	"third_record_id": "xkz_1080",
	"dwelling_build_no": "1-9"
}
	 */
	@Test
	@Ignore
	public void testParseNewBuildDetail() {
		FileInputStream fis;
		String body;
		try {
			fis = new FileInputStream("D:\\body\\syfc\\syfc_new_build_detail");
			body = IOUtils.toString(fis);
			Document doc = Jsoup.parse(body);
			/**
			 * <tr>
	            <td width="199" height="25" align="center" bgcolor="#F1FAFF">楼栋地址</td>
	            <td width="124" align="center" bgcolor="#F1FAFF">当前可售套数</td>
	            <td width="122" align="center" bgcolor="#F1FAFF">不可售套数</td>
	            <td width="111" align="center" bgcolor="#F1FAFF">出售套数</td>
	            <td width="157" align="center" bgcolor="#F1FAFF">纳入网上销售总套数</td>
	          </tr>
			 */
			Elements trs = doc.getElementsByAttributeValueContaining("bgcolor", "#cccccc").get(0).getElementsByTag("tr");
			for (int i = 1 ; i < trs.size() ; i++) {
				Element element = trs.get(i);
				Elements td = element.getElementsByTag("td");
				String build_location = td.get(0).text();//楼栋地址
				String sales_detail_uri = td.get(0).getElementsByTag("a").attr("href");//销售情况URL
				String third_record_id = sales_detail_uri.substring(sales_detail_uri.indexOf("=")+1, sales_detail_uri.indexOf("&"));//第三方记录id;//第三方ID;//所属区
				String sales_available_count = td.get(1).text();//当前可售套数
				String sales_unvailable_count = td.get(2).text();//不可售套数
				String saled_count = td.get(3).text();//出售套数
				String can_sales_count = td.get(4).text();//纳入网上销售总套数
				
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("third_record_id", third_record_id);
				jsonObject.put("build_location", build_location);
				jsonObject.put("sales_detail_uri", sales_detail_uri);
				jsonObject.put("sales_available_count", sales_available_count);
				jsonObject.put("sales_unvailable_count", sales_unvailable_count);	
				jsonObject.put("saled_count", saled_count);
				jsonObject.put("can_sales_count", can_sales_count);
				System.out.println(jsonObject.toJSONString());
			}
			fis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	@Ignore
	public void testParseNewBuildHouse() {
		FileInputStream fis;
		String body;
		try {
			fis = new FileInputStream("D:\\body\\syfc_build_house_2019-02-28\\syfc_build_house_515643_2019-02-28_18-28-09");
			body = IOUtils.toString(fis);
			Document doc = Jsoup.parse(body);
			Elements trs = doc.getElementsByTag("tr");
			JSONArray tierArray = new JSONArray();
			for (int i = 0 ; i < trs.size() ; i++) {
				Element element = trs.get(i);
				Elements tds = element.getElementsByTag("td");
				JSONArray houseArray = new JSONArray();
				for (int j = 1 ; j < tds.size() ; j++) {
					JSONObject house = new JSONObject();
					Element td0 = tds.get(0);
					//第几层
					String house_tier = td0.text();//第几层
					Element td = tds.get(j);
					//每层房屋销售情况
					String house_detail_uri = td.getElementsByTag("a").attr("href");//房屋公摊连接
					String third_record_id = "";
					if(StringUtils.isNotEmpty(house_detail_uri)) {
						third_record_id = house_detail_uri.substring(house_detail_uri.indexOf("=")+1, house_detail_uri.indexOf("&"));//第三方记录id;//第三方ID;//所属区
					}
					String house_no = td.text();//房屋门牌号
					//销售状态
					String sales_state = td.attr("bgcolor");
					String house_localtion = td.attr("xxx");
					SaleStateEnum sales_state_enum = SaleStateEnum.textOf(sales_state);
					house.put("house_tier", house_tier.substring(1, house_tier.length()-1));
					house.put("sales_state_enum", JSONObject.toJSON(sales_state_enum).toString());
					house.put("house_detail_uri", house_detail_uri);
					house.put("third_record_id", third_record_id);
					house.put("house_no", house_no);
					house.put("house_localtion", house_localtion);
					//每层添加房屋
					houseArray.add(house);
				}
				//楼栋添加每层
				tierArray.add(houseArray);
			}
			fis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

