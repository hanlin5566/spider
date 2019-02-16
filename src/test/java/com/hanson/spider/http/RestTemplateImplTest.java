package com.hanson.spider.http;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;

import com.hanson.spider.misc.SpiderResponseCode;
import com.hzcf.base.exception.ServiceException;

/**
 * @author Hanson
 * create on 2018年3月11日
 */

public class RestTemplateImplTest {
	@Test
	@Ignore
	public void testSaveFile() {
		String floder = "D:\\body\\fangtianxia\\";
		File f = new File(floder);
		if(!f.exists()) {
			f.mkdirs();
		}
		String filePath = "fangtianxia_2019-24-06_04-24-53";
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(new File(f.getPath()+"\\"+filePath));
			IOUtils.write("abc",fos);
			fos.close();
		} catch (Exception exception) {
			throw new ServiceException(SpiderResponseCode.WRITE_FILE_ERROR,String.format(SpiderResponseCode.WRITE_FILE_ERROR.detailMsg(), exception.getMessage()),exception);
		}
	}
	
	@Test
	public void test() {
		try {
//			RestTemplateImpl rest = new RestTemplateImpl(); 
//			String url = "https://shen.fang.anjuke.com/loupan/all/";
//			String ret = rest.get(url, null);
//			FileOutputStream fos = new FileOutputStream(new File("D:\\body\\anjuke.txt"));
//			IOUtils.write(ret,fos);
//			System.out.println(ret);
			
//			String html = null;
			try {
				CountDownLatch countDownLatch = new CountDownLatch(10);
				for (int i = 0; i < 10; i++) {
					Thread t = new Thread(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							RestTemplate restTemplate = new RestTemplate();
							String forObject = restTemplate.getForObject("http://localhost:12345/hehe?name=hanson",String.class);
							System.out.println(Thread.currentThread().getName()+":"+forObject+"@"+System.currentTimeMillis());
							countDownLatch.countDown();
						}
					});
					t.setName("rec"+i);
					t.start();
				}
				countDownLatch.await();
//				html = IOUtils.toString(new FileInputStream(new File("D:\\body\\anjuke.html")));
//				Assert.hasText(html,"获取html失败");
//				Document doc = Jsoup.parse(html);
//				Elements items = doc.select(".list-contents .list-results .key-list .item-mod");
//				for (Element element : items) {
//					String lpName = element.select(".lp-name .items-name").text();
//					String price = element.select(".price").text();
//					String address = element.select(".address .list-map").text();
//					String huxing = element.select(".huxing").text();
//					
//					String output = String.format("楼盘:%s 地址:%s 户型:%s 价格:%s", lpName,address,huxing,price);
//					System.out.println(output);
//				}
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("failed :"+ e.getMessage());
		}
	}

}

