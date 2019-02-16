package com.hanson.spider.parser;

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
 * @author Hanson
 * create on 2019年2月6日
 */

public class SYFCParserTest {
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
	public void testParseSalesNoDetail() {
		FileInputStream fis;
		String body;
		try {
			fis = new FileInputStream("D:\\body\\syfc_sales_num_detail2019-02-09\\syfc_salesNoDetail_1_2019-02-09_04-22-50");
			body = IOUtils.toString(fis);
			Document doc = Jsoup.parse(body);
			Elements elements = doc.getElementsByTag("td");
//			String third_no = elements.get(1).text(); //三方库中的序号（随着采集可能重复）
			String third_record_id = elements.get(1).text(); //第三方记录ID
			String sales_no = elements.get(57).text(); //房产预售号
			String  company = elements.get(7).text(); //开发商
//			String  program_name  '' //项目名称
			String  program_localtion  = elements.get(13).text();//项目位置
			String build_count = elements.get(19).text();  //商品房栋数
			String  total_build_area  = elements.get(25).text(); //总建筑面积
			String  sales_area  = elements.get(31).text(); //销售面积
			String  dwelling_area  = elements.get(37).text(); //住宅面积
			String  shop_area  = elements.get(43).text(); //网点面积
			String  public_area = elements.get(49).text(); //公建面积
			String  other_area  = elements.get(55).text(); //其他面积
			String  approve_date = elements.get(51).text(); //审批日期
			String  dwelling_build_no = elements.get(3).text();  //住宅栋号
			String  shop_build_no  = elements.get(9).text(); //网点栋号
			String  public_build_no  = elements.get(15).text(); //公建栋号
			String  other_build_no  = elements.get(21).text(); //其他栋号
			String  dwelling_build_count = elements.get(27).text();  //住宅套数
			String  shop_build_count = elements.get(33).text();  //网点套数
			String  public_build_count = elements.get(39).text();  //公建套数
			String  other_build_count = elements.get(45).text(); //其他套数
			String  remark = elements.get(61).text();
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("third_record_id", third_record_id);
			jsonObject.put("sales_no", sales_no);
			jsonObject.put("company", company);
			jsonObject.put("program_localtion", program_localtion);
			jsonObject.put("build_count", build_count);
			jsonObject.put("total_build_area", total_build_area);
			jsonObject.put("sales_area", sales_area);
			jsonObject.put("dwelling_area", dwelling_area);
			jsonObject.put("shop_area", shop_area);
			jsonObject.put("public_area", public_area);
			jsonObject.put("other_area", other_area);
			jsonObject.put("approve_date", approve_date);
			jsonObject.put("dwelling_build_no", dwelling_build_no);
			jsonObject.put("shop_build_no", shop_build_no);
			jsonObject.put("public_build_no", public_build_no);
			jsonObject.put("other_build_no", other_build_no);
			jsonObject.put("dwelling_build_count", dwelling_build_count);
			jsonObject.put("shop_build_count", shop_build_count);
			jsonObject.put("public_build_count", public_build_count);
			jsonObject.put("other_build_count", other_build_count);
			jsonObject.put("remark", remark);
			System.out.println(jsonObject.toJSONString());
			fis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	@Ignore
	public void testBuildList() {
		FileInputStream fis;
		String body;
		try {
			fis = new FileInputStream("D:\\body\\syfc\\syfc.html");
			body = IOUtils.toString(fis);
			Document doc = Jsoup.parse(body);
			Elements elements = doc.getElementsByAttributeValueContaining("bgcolor", "#a4abb5");
			for (Element tables : elements) {
				Elements trs = tables.getElementsByTag("tr");
				for (Element tr : trs) {
					Elements td = tr.getElementsByTag("td");
					String text = tr.text();
					if(text.indexOf("项目名称") >= 0) {
						//title then continue;
						continue;
					}
					String deltail_uri = td.get(0).getElementsByTag("a").attr("href");//详情页连接
					String program_describe = td.get(0).text();//项目描述
					String district = td.get(1).text();//项目区域
					String build_count = td.get(2).text();//总栋数
					String company = td.get(3).text();//开发商
					String start_sales_date = td.get(4).text();//开盘日期
					String subordinate_district = td.get(5).text();//所属区
					String third_pard_id = deltail_uri.substring(deltail_uri.indexOf("=")+1, deltail_uri.indexOf("&"));//第三方记录id;//第三方ID;//所属区
					JSONObject json = new JSONObject();
					json.put("deltail_uri", deltail_uri);
					json.put("third_pard_id", third_pard_id);
					json.put("program_describe", program_describe);
					json.put("district", district);
					json.put("build_count", build_count);
					json.put("company", company);
					json.put("start_sales_date", start_sales_date);
					json.put("subordinate_district", subordinate_district);
					System.out.println(json.toJSONString());
				}
			}
//			System.out.println(jsonObject.toJSONString());
			fis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * 预售许可证价钱详情解析
	 */
	@Test
	@Ignore
	public void testSalesPrice() {
		FileInputStream fis;
		String body;
		try {
			fis = new FileInputStream("D:\\body\\syfc\\syfc_sales_price_1549728767198");
			body = IOUtils.toString(fis);
			Document searchDoc = Jsoup.parse(body);
			Elements elements = searchDoc.select(".tablestyle");
			for (Element tables : elements) {
				Elements trs = tables.getElementsByTag("tr");
				for (Element tr : trs) {
					Elements td = tr.getElementsByTag("td");
					String text = tr.text();
					if(text.indexOf("项目名称") >= 0) {
						//title then continue;
						continue;
					}
					String sales_price_sub_no = td.get(0).text();//子序号
					String sales_price_approve_date = td.get(1).text();//审批日期
					String sales_price_company = td.get(2).text();//开发商
					String sales_price_program_describe = td.get(3).text();//项目描述
					String sales_no = td.get(4).text();//预售许可证
					String sales_price_deltail_uri = td.get(5).getElementsByTag("a").attr("href");//详情页连接
					String sales_price_program_localtion_detail = td.get(5).getElementsByTag("a").text();//销售金额-项目地址-详情
					String sales_price_third_pard_id = sales_price_deltail_uri.substring(sales_price_deltail_uri.indexOf("('")+1, sales_price_deltail_uri.indexOf("')"));//第三方记录id;//第三方ID;//所属区
					JSONObject json = new JSONObject();
					json.put("sales_price_sub_no", sales_price_sub_no);
					json.put("sales_price_approve_date", sales_price_approve_date);
					json.put("sales_price_company", sales_price_company);
					json.put("sales_price_program_describe", sales_price_program_describe);
					json.put("sales_no", sales_no);
					json.put("sales_price_deltail_uri", sales_price_deltail_uri);
					json.put("sales_price_program_localtion_detail", sales_price_program_localtion_detail);
					json.put("sales_price_third_pard_id", sales_price_third_pard_id);
					System.out.println(json.toJSONString());
				}
			}
//			System.out.println(jsonObject.toJSONString());
			fis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	@Ignore
	public void testParseSalesNoList() {
		FileInputStream fis;
		String body;
		try {
			fis = new FileInputStream("D:\\body\\syfc\\syfc.html");
			body = IOUtils.toString(fis);
			Document doc = Jsoup.parse(body);
//			Element elementById = doc.getElementById("ysxkzh");
			Elements details = doc.select(".style1");
//			Elements elementsByAttributeValue = doc.getElementsByAttributeValue("colspan", "6");
//			Element first = elementsByAttributeValue.first();//div
//			Elements elementsByTag = first.getElementsByTag("b");
//			String text2 = elementsByTag.first().text();
//			for (Element element : details) {
//				String href = element.attr("href");
//				System.out.println("href:"+href);
//				/**
//				 * <td bgcolor="white"> 
//					   <div align="center"> 
//					    <a href="/work/ysxk/ysxkzinfo.jsp?id=23238356" target="_blank" class="style1">查看详细</a> 
//					   </div> 
//				   </td> 
//				 */
//				//div--td--tr
//				Element parent = element.parent().parent().parent();
//				Elements tds = parent.getElementsByTag("td");
//				for (Element td : tds) {
//					String text = td.text();
//					System.out.print(text+"|||");
//				}
//				System.out.println();
//			}
			
			for (Element element : details) {
				JSONObject obj = new JSONObject();
				String href = element.attr("href");//详情连接
				/**
				 * <td bgcolor="white"> 
					   <div align="center"> 
					    <a href="/work/ysxk/ysxkzinfo.jsp?id=23238356" target="_blank" class="style1">查看详细</a> 
					   </div> 
				   </td> 
				 */
				//div--td--tr
				Element parent = element.parent().parent().parent();
				Elements tds = parent.getElementsByTag("td");
				String list_no = tds.get(0).text();//列表编号
				String approve_date = tds.get(1).text();//审批日期
				String develop_company = tds.get(2).text();//开发商
				String program_localtion = tds.get(3).text();//项目地址
				String sales_no = tds.get(4).text();//预售许可证号
				String deltail_uri = tds.get(5).getElementsByTag("a").attr("href");//详情页连接
				String third_record_id = deltail_uri.substring(deltail_uri.indexOf("=")+1, deltail_uri.length());//第三方记录id
				
				obj.put("third_record_id", third_record_id);
				obj.put("list_no", list_no);
				obj.put("approve_date", approve_date);
				obj.put("develop_company", develop_company);
				obj.put("program_localtion", program_localtion);
				obj.put("sales_no", sales_no);
				obj.put("deltail_uri", deltail_uri);
				obj.put("href", href);
				
				System.out.println(obj.toJSONString());
			}
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

