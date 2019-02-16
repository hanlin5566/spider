package com.hanson.spider.component.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hanson.spider.misc.SpiderResponseCode;
import com.hzcf.base.exception.ServiceException;

/**
 * @author Hanson
 * create on 2018年3月11日
 */
@Component
public class SYFCParser {
	/**
	 * 解析预售许可证列表页
	 * @param body
	 * @return
	 */
	public JSONArray parseSalesNoList(String body) {
		JSONArray ret = new JSONArray();
		Document doc = Jsoup.parse(body);
		Elements details = doc.select(".style1");
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
			String no = tds.get(0).text();//编号
			String date = tds.get(1).text();//审批日期
			String company = tds.get(2).text();//开发商
			String program_localtion = tds.get(3).text();//项目地址
			String sales_no = tds.get(4).text();//预售许可证号
			String deltail_uri = tds.get(5).getElementsByTag("a").attr("href");//详情页连接
			String third_record_id = deltail_uri.substring(deltail_uri.indexOf("=")+1, deltail_uri.length());//第三方记录id
			
			obj.put("third_record_id", third_record_id);
			obj.put("no", no);
			obj.put("date", date);
			obj.put("company", company);
			obj.put("program_localtion", program_localtion);
			obj.put("sales_no", sales_no);
			obj.put("deltail_uri", deltail_uri);
			obj.put("href", href);
			ret.add(obj);
		}
		return ret;
	}
	/**
	 * 解析预售许可证详情页
	 * @param body
	 * @return
	 */
	public JSONObject parseSalesNoDetail(String body) {
		JSONObject ret = new JSONObject();
		Document doc = Jsoup.parse(body);
		Elements elements = doc.getElementsByTag("td");
//		String third_no = elements.get(1).text(); //三方库中的序号（随着采集可能重复）
		String third_record_id = elements.get(1).text(); //第三方记录ID
		String sales_no = elements.get(57).text(); //房产预售号
		String  company = elements.get(7).text(); //开发商
//		String  program_name  '' //项目名称
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
		ret.put("third_record_id", third_record_id);
		ret.put("sales_no", sales_no);
		ret.put("company", company);
		ret.put("program_localtion", program_localtion);
		ret.put("build_count", build_count);
		ret.put("total_build_area", total_build_area);
		ret.put("sales_area", sales_area);
		ret.put("dwelling_area", dwelling_area);
		ret.put("shop_area", shop_area);
		ret.put("public_area", public_area);
		ret.put("other_area", other_area);
		ret.put("approve_date", approve_date);
		ret.put("dwelling_build_no", dwelling_build_no);
		ret.put("shop_build_no", shop_build_no);
		ret.put("public_build_no", public_build_no);
		ret.put("other_build_no", other_build_no);
		ret.put("dwelling_build_count", dwelling_build_count);
		ret.put("shop_build_count", shop_build_count);
		ret.put("public_build_count", public_build_count);
		ret.put("other_build_count", other_build_count);
		ret.put("remark", remark);
		return ret;
	}
	
	
	public int getPageCount(String body) {
		int pageCount = 0;
		try {
			Document doc = Jsoup.parse(body);
			Elements elementsByAttributeValue = doc.getElementsByAttributeValue("colspan", "6");
			Element first = elementsByAttributeValue.first();// div
			Elements elementsByTag = first.getElementsByTag("b");
			pageCount = Integer.parseInt(elementsByTag.first().text());
		} catch (Exception e) {
			throw new ServiceException(SpiderResponseCode.PARSE_SALES_PAGE_COUNT_ERROR,String.format(SpiderResponseCode.PARSE_SALES_PAGE_COUNT_ERROR.detailMsg(), e.getMessage()),e);
		}
		return pageCount;
	}
	
	
	/**
	 * 解析	商品房楼盘销售情况、户型、面积列表页
	 * @param body
	 * @return
	 */
	public JSONArray parseBuildList(String body) {
		JSONArray ret = new JSONArray();
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
				json.put("third_pard_id", third_pard_id);
				json.put("deltail_uri", deltail_uri);
				json.put("program_describe", program_describe);
				json.put("district", district);
				json.put("build_count", build_count);
				json.put("company", company);
				json.put("start_sales_date", start_sales_date);
				json.put("subordinate_district", subordinate_district);
				ret.add(json);
			}
		}
		return ret;
	}
	
	/**
	 * 解析	根据预售许可证查询价格列表页
	 * @param body
	 * @return
	 */
	public JSONArray parseSalesPriceList(String body) {
		JSONArray ret = new JSONArray();
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
				ret.add(json);
			}
		}
		return ret;
	}
}

