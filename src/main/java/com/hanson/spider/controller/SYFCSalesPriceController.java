package com.hanson.spider.controller;

import java.io.File;
import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.hanson.spider.service.SYFCSalesPriceDetailSpiderService;
import com.hanson.spider.service.SYFCSalesPriceListSpiderService;
import com.hanson.spider.service.SYFCSalesPriceManyListSpiderService;
import com.hzcf.base.response.ResponseData;

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
	SYFCSalesPriceListSpiderService salesPricelistSpiderService;
	@Autowired
	SYFCSalesPriceManyListSpiderService salesPriceManylistSpiderService;
	@Autowired
	SYFCSalesPriceDetailSpiderService salesPriceDetailSpiderService;
	
	Logger logger = LoggerFactory.getLogger(this.getClass());
	@Value("${syfc.tesseract.path:/opt/}")
	private String datapath;
	
	@ApiOperation(value = "从文件恢复预售许可详情", notes = "从文件恢复预售许可详情")
	@PostMapping("/tesseract")
	public ResponseData recoverSalesNumDetail(@RequestBody JSONObject param) throws FileNotFoundException {
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
	
	
	@ApiOperation(value = "爬取沈阳房产销售价格列表", notes = "根据预售许可证爬取数据")
	@PostMapping("/collectPriceList")
	public ResponseData collectPriceList() {
		salesPricelistSpiderService.collectPriceList();
		return ResponseData.ok();
	}
	
	
	@ApiOperation(value = "从文件恢复沈阳房产销售价格列表", notes = "只需要输入日起即可 2019-02-10")
	@PostMapping("/recoverPriceList/{folderPath}")
	public ResponseData recoverPriceList(@PathVariable String folderPath) {
		salesPricelistSpiderService.recoverSalesPriceList(folderPath);
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
	
	@ApiOperation(value = "初始化采集售价详情")
	@PostMapping("/initSalesPriceDetail")
	public ResponseData initSalesPriceDetail() {
		salesPriceDetailSpiderService.initSalesPriceDetail();
		return ResponseData.ok();
	}
	
	@ApiOperation(value = "采集售价详情")
	@PostMapping("/collectSalesPriceDetail")
	public ResponseData collectSalesPriceDetail() {
		salesPriceDetailSpiderService.collect();
		return ResponseData.ok();
	}
}

