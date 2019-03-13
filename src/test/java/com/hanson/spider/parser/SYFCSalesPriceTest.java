package com.hanson.spider.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

/**
 * @author Hanson create on 2019年2月6日
 */
@Ignore
public class SYFCSalesPriceTest {
	Logger logger = LoggerFactory.getLogger(this.getClass());
	private String uri = "218.25.83.4:7003/newbargain/download/findys/ys_info.jsp";
	private String urlLogin = "http://" + uri;

	@Test
	@Ignore
	public void test() throws Exception {
		ITesseract instance = new Tesseract();
		instance.setLanguage("chi_sim");
		String datapath = this.getClass().getResource("/").getPath();
		datapath = datapath.substring(1, datapath.length());
		instance.setDatapath(datapath);
		// 识别验证码
		String ocrResult = instance.doOCR(new File("D:\\body\\syfc\\verifyCode\\19025.jpg"));
		logger.info(ocrResult);
	}

	@Ignore
	@Test
	public void testPriceListByJsoup() throws Exception {
		Connection connect = Jsoup.connect(urlLogin);
		// 伪造请求头
		connect.header("Accept", "application/json, text/javascript, */*; q=0.01").header("Accept-Encoding",
				"gzip, deflate");
		connect.header("Accept-Language", "zh-CN,zh;q=0.9").header("Connection", "keep-alive");
		connect.header("Content-Length", "213").header("Content-Type",
				"application/x-www-form-urlencoded; charset=UTF-8");
		connect.header("Host", uri).header("Referer", urlLogin);
		connect.header("User-Agent",
				"Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36")
				.header("X-Requested-With", "XMLHttpRequest");

		// 请求url获取响应信息
		Response res = connect.ignoreContentType(true).method(Method.GET).execute();// 执行请求
		// 获取返回的cookie
		Map<String, String> cookies = res.cookies();
		for (Entry<String, String> entry : cookies.entrySet()) {
			System.out.println(entry.getKey() + "-" + entry.getValue());
		}
		// 获取响应体
		// String body = res.body();
		// Document doc = new Document(body);

		// 调用下载验证码的工具类下载验证码

		String image_url = "http://218.25.83.4:7003/newbargain/download/findys/image.jsp?" + System.currentTimeMillis();
		Connection image_connect = Jsoup.connect(image_url);
		image_connect.cookies(cookies);// 携带cookies爬取图片
		image_connect.timeout(5 * 10000);
		Connection.Response iamge_response = image_connect.ignoreContentType(true).execute();
		byte[] img = iamge_response.bodyAsBytes();
		// 读取文件存储位置
		File file = new File("D:\\body\\syfc\\yzm.jpg");
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(img);
		fos.close();
		ITesseract instance = new Tesseract();
		instance.setLanguage("chi_sim");
		instance.setDatapath("D:/mine/workspace/spider/src/main/resources");
		// long startTime = System.currentTimeMillis();
		String ocrResult = instance.doOCR(file);
		// char[] charArray = ocrResult.toCharArray();
		// char[] charCount = new char[4];
		// for (char c : charArray) {
		//
		// }
		System.out.println(ocrResult);
		try {
			// 模拟查询
			// "ys_info.jsp?
			String searchUrl = urlLogin + "?ysid=" + 18429 + "&yzmcode=" + ocrResult + "&flagcx=1";
			searchUrl = searchUrl.replaceAll("\r|\n", "");
			Connection searchConnection = Jsoup.connect(searchUrl);

			searchConnection.header("Accept", "text/html, application/xhtml+xml, image/jxr, */*")
					.header("Accept-Encoding", "gzip, deflate");
			searchConnection.header("Accept-Language", "zh-CN,zh;q=0.9").header("Connection", "keep-alive");
			// searchConnection.header("Host", uri).header("Referer", urlLogin);
			// searchConnection.header("User-Agent",
			// "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like
			// Gecko) Chrome/65.0.3325.181 Safari/537.36")
			// .header("X-Requested-With", "XMLHttpRequest");
			searchConnection.cookies(cookies);// 携带cookies爬取图片
			searchConnection.timeout(5 * 10000);
			Connection.Response searchResponse = searchConnection.ignoreContentType(true).method(Method.GET)
					.cookies(cookies).execute();
			// 设置cookie和post上面的map数据
			// Response
			// login=searchConnection.ignoreContentType(true).method(Method.GET).data(datas).cookies(rs.cookies()).execute();
			// Response searchResponse =
			// searchConnection.ignoreContentType(true).method(Method.GET).cookies(cookies).execute();
			// 打印，登陆成功后的信息
			File salesPriceFile = new File("D:\\body\\syfc\\syfc_sales_price_" + System.currentTimeMillis());
			String searchBody = searchResponse.body();
			fos = new FileOutputStream(salesPriceFile);
			fos.write(searchBody.getBytes());
			fos.close();
			Document searchDoc = Jsoup.parse(searchBody);
			Elements elements = searchDoc.select(".tablestyle");
			for (Element tables : elements) {
				Elements trs = tables.getElementsByTag("tr");
				for (Element tr : trs) {
					Elements td = tr.getElementsByTag("td");
					String text = tr.text();
					if (text.indexOf("项目名称") >= 0) {
						// title then continue;
						continue;
					}
					String sales_price_sub_no = td.get(0).text();// 子序号
					String sales_price_approve_date = td.get(1).text();// 审批日期
					String sales_price_company = td.get(2).text();// 开发商
					String sales_price_program_describe = td.get(3).text();// 项目描述
					String sales_no = td.get(4).text();// 预售许可证
					String sales_price_deltail_uri = td.get(5).getElementsByTag("a").attr("href");// 详情页连接
					String sales_price_program_localtion_detail = td.get(5).getElementsByTag("a").text();// 销售金额-项目地址-详情
					String sales_price_third_record_id = sales_price_deltail_uri.substring(
							sales_price_deltail_uri.indexOf("('") + 1, sales_price_deltail_uri.indexOf("')"));// 第三方记录id;//第三方ID;//所属区
					JSONObject json = new JSONObject();
					json.put("sales_price_sub_no", sales_price_sub_no);
					json.put("sales_price_approve_date", sales_price_approve_date);
					json.put("sales_price_company", sales_price_company);
					json.put("sales_price_program_describe", sales_price_program_describe);
					json.put("sales_no", sales_no);
					json.put("sales_price_deltail_uri", sales_price_deltail_uri);
					json.put("sales_price_program_localtion_detail", sales_price_program_localtion_detail);
					json.put("sales_price_third_record_id", sales_price_third_record_id);
					System.out.println(json.toJSONString());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	@Ignore
	public void testPriceManyList() {
		try {
			File salesPriceFile = new File(
					"D:\\body\\syfc_sales_price_many_listmany_list2019-03-05\\syfc_sales_price_many_listmany_list2_2019-03-05_14-49-19.html");
			FileInputStream fis = new FileInputStream(salesPriceFile);
			String body = IOUtils.toString(fis);
			fis.close();
			Document searchDoc = Jsoup.parse(body);
			Elements elements = searchDoc.select(".tablestyle");
			for (Element tables : elements) {
				Elements trs = tables.getElementsByTag("tr");
				for (Element tr : trs) {
					Elements td = tr.getElementsByTag("td");
					String text = tr.text();
					if (text.indexOf("项目名称") >= 0) {
						// title then continue;
						continue;
					}
					String sales_price_sub_no = td.get(0).text();// 子序号
					String sales_price_approve_date = td.get(1).text();// 审批日期
					String sales_price_company = td.get(2).text();// 开发商
					String sales_price_program_describe = td.get(3).text();// 项目描述
					String sales_no = td.get(4).text();// 预售许可证
					String sales_price_deltail_uri = td.get(5).getElementsByTag("a").attr("href");// 详情页连接
					String sales_price_program_localtion_detail = td.get(5).getElementsByTag("a").text();// 销售金额-项目地址-详情
					String sales_price_third_record_id = sales_price_deltail_uri.substring(
							sales_price_deltail_uri.indexOf("('") + 1, sales_price_deltail_uri.indexOf("')"));// 第三方记录id;//第三方ID;//所属区
					JSONObject json = new JSONObject();
					json.put("sales_price_sub_no", sales_price_sub_no);
					json.put("sales_price_approve_date", sales_price_approve_date);
					json.put("sales_price_company", sales_price_company);
					json.put("sales_price_program_describe", sales_price_program_describe);
					json.put("sales_no", sales_no);
					json.put("sales_price_deltail_uri", sales_price_deltail_uri);
					json.put("sales_price_program_localtion_detail", sales_price_program_localtion_detail);
					json.put("sales_price_third_record_id", sales_price_third_record_id);
					System.out.println(json.toJSONString());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testPriceHouse(){
		try {
			File salesPriceFile = new File("D:\\body\\syfc\\syfc_sales_price_detail.html");
			FileInputStream fis = new FileInputStream(salesPriceFile);
			String body = IOUtils.toString(fis);
			fis.close();
			Document searchDoc = Jsoup.parse(body);
			Elements levels = searchDoc.select(".T_song12bk2 .td2");
			Map<String,String> changeKeyMap = new HashMap<String,String>();
			changeKeyMap.put("房屋地址", "house_localtion");
			changeKeyMap.put("预售许可证号", "sales_no");
			changeKeyMap.put("房屋状态", "sales_state_enum");
			changeKeyMap.put("总价", "total_price");
			changeKeyMap.put("单价", "unit_price");
			changeKeyMap.put("房屋用途", "house_use");
			changeKeyMap.put("建筑面积", "house_build_area");
			changeKeyMap.put("房屋结构", "house_structure");
			changeKeyMap.put("可售", "1");
			changeKeyMap.put("已售", "2");
			//不循环最后一行
			for (int x = levels.size()-2; x >= 0; x--) {
				Element level = levels.get(x);
				//每层
				JSONArray tierArray = new JSONArray();
				Elements tds = level.children();
				JSONArray houseArray = new JSONArray();
				//第几层
				Element td0 = tds.get(0);
				String house_tier = td0.text();//第几层
				for (int j = 1 ; j < tds.size() ; j++) {
					//每个单元
					Element td = tds.get(j);
					//每单元的房间
					Elements housesTd = td.getElementsByTag("tr").get(0).children();
					for (Element houseTd : housesTd) {
						//每个房间
						JSONObject house = new JSONObject();
						//每层房屋销售情况
						Element element = houseTd.getElementsByTag("tr").get(0);
						String state = element.children().get(0).getElementsByTag("input").val();
						//房屋描述
						String houseDescribe = houseTd.getElementsByTag("tr").get(0).children().get(1).attr("xxx");
						String[] split = houseDescribe.split("<br>");
						//不检索最后一条详情数据
						for (int i = 0; i < split.length-1; i++) {
							String string = split[i];
							try {
								String[] describe = string.split("：");
								String key = describe[0].substring(3, describe[0].length());
								String value = describe[1].substring(4, describe[1].length());
								//去除单位
								value = value.replaceAll("元", "");
								value = value.replaceAll("平方米", "");
								if(changeKeyMap.containsKey(key)) {
									key = changeKeyMap.get(key);
								}
								if(changeKeyMap.containsKey(value)) {
									value = changeKeyMap.get(value);
								}
								
								house.put(key, value);
							} catch (Exception e) {
								logger.error("解析售价描述出现异常:{}",string,e);
							}
						}
						house.put("house_tier", house_tier.substring(1, house_tier.length()-1));
						house.put("house_state", state);
//						house.put("sales_state_enum", JSONObject.toJSON(sales_state_enum).toString());
//						house.put("house_detail_uri", house_detail_uri);
//						house.put("third_record_id", third_record_id);
//						house.put("house_no", house_no);
//						house.put("house_localtion", house_localtion);
						logger.info(house.toJSONString());
						//每层添加房屋
						houseArray.add(house);
					}
					
				}
				//楼栋添加每层
				tierArray.add(houseArray);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
