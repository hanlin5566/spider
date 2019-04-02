package com.hanson.spider.controller;

import java.io.File;
import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.hanson.spider.service.SYFCSalesPriceDetailSpiderService;
import com.hanson.spider.service.SYFCSalesPriceListSpiderService;
import com.hanson.spider.service.SYFCSalesPriceManyListSpiderService;
import com.hanson.base.response.ResponseData;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

/**
 * @author Hanson
 * create on 2019年2月26日
 */
@RestController
@RequestMapping(value = "/syfc/salesPirce")
@Api("沈阳售价列表")
public class SYFCSalesPriceController {
	@Autowired
	SYFCSalesPriceManyListSpiderService salesPriceManylistSpiderService;
	@Autowired
	SYFCSalesPriceListSpiderService salesPriceListSpiderService;
	@Autowired
	SYFCSalesPriceDetailSpiderService salesPriceDetailSpiderService;
	
	Logger logger = LoggerFactory.getLogger(this.getClass());
	@Value("${syfc.tesseract.path:/opt/}")
	private String datapath;
	
	@PostMapping("/tesseract")
	public ResponseData tesseract(@RequestBody JSONObject param) throws FileNotFoundException {
		ITesseract instance = new Tesseract();
	    instance.setLanguage("chi_sim");
		instance.setDatapath(datapath);
		// 识别验证码
		String ocrResult = "";
		try {
			ocrResult = instance.doOCR(new File("D:\\body\\syfc\\verifyCode\\19025.jpg"));
			logger.info(ocrResult);
		} catch (TesseractException e) {
			// TODO Auto-generated catch block
			logger.error("",e);
		}
		return ResponseData.ok(ocrResult);
	}
	
	
//	@ApiOperation(value = "爬取沈阳房产销售价格列表", notes = "根据预售许可证爬取数据")
//	@PostMapping("/collectPriceList")
//	@Deprecated
//	public ResponseData collectPriceList() {
//		salesPricelistSpiderService.collectPriceList();
//		return ResponseData.ok();
//	}
//	
//	
//	@ApiOperation(value = "从文件恢复沈阳房产销售价格列表", notes = "只需要输入日起即可 2019-02-10")
//	@PostMapping("/recoverPriceList/{folderPath}")
//	@Deprecated
//	public ResponseData recoverPriceList(@PathVariable String folderPath) {
//		salesPricelistSpiderService.recoverSalesPriceList(folderPath);
//		return ResponseData.ok();
//	}
	
	@ApiOperation(value = "从文件恢复售价详情", notes = "{\"folderPath\":\"D:\body\"}")
	@PostMapping("/recover")
	public ResponseData recoverSalesPriceDetail(@RequestBody JSONObject param) {
		//D:\\body\\syfc_sales_num_detail2019-02-09
		String folderPath = param.getString("folderPath");
		salesPriceDetailSpiderService.recoverSalesPriceDetail(folderPath);
		return ResponseData.ok();
	}
	
	@ApiOperation(value = "初始化根据页数采集售价列表")
	@PostMapping("/initSalesPriceManyList")
	public ResponseData initSalesPriceManyList() {
		salesPriceManylistSpiderService.initSalesPriceManyList();
		return ResponseData.ok();
	}
	
	@ApiOperation(value = "根据pageNo爬取售价列表", notes = "根据预售许可证爬取数据")
	@PostMapping("/collectPriceManyList")
	public ResponseData collectPriceManyList() {
		salesPriceManylistSpiderService.collectPriceList();
		return ResponseData.ok();
	}
	
	@ApiOperation(value = "去除重复采集的记录")
	@PostMapping("/discinctSalesPriceList")
	public ResponseData discinctSalesPriceList() {
		salesPriceDetailSpiderService.discinctSalesPriceList();
		return ResponseData.ok();
	}
	
	@ApiOperation(value = "生成预售许可证下的售价列表")
	@PostMapping("/slesNumPriceListGenerator")
	public ResponseData slesNumPriceListGenerator() {
		salesPriceDetailSpiderService.slesNumPriceListGenerator();
		return ResponseData.ok();
	}
	
	@ApiOperation(value = "增量采集售价详情")
	@PostMapping("/incrementtSalesPrice")
	public ResponseData incrementtSalesPrice() {
		salesPriceListSpiderService.incrementtSalesPrice();
		return ResponseData.ok();
	}
	@ApiOperation(value = "同步list到detail")
	@PostMapping("/syncSalesPriceDetail")
	public ResponseData syncSalesPriceDetail() {
		salesPriceDetailSpiderService.syncSalesPriceDetail();
		return ResponseData.ok();
	}
	
	@ApiOperation(value = "采集售价详情")
	@PostMapping("/collectSalesPriceDetail")
	public ResponseData collectSalesPriceDetail() {
		salesPriceDetailSpiderService.collect();
		return ResponseData.ok();
	}
}

