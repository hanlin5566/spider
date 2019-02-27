package com.hanson.spider.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.hanson.spider.service.SYFCSalesNumDetailSpiderService;
import com.hzcf.base.response.ResponseData;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * @author Hanson
 * create on 2019年2月26日
 */
@RestController
@RequestMapping(value = "/syfc/salesNo")
@Api("沈阳房产预售许可证详情")
public class SYFCSalesNoDetailController {
	@Autowired
	SYFCSalesNumDetailSpiderService detailSpiderService;
	
	@ApiOperation(value = "从文件恢复预售许可详情", notes = "从文件恢复预售许可详情")
	@PostMapping("/recover")
	public ResponseData recoverSalesNumDetail(@RequestBody JSONObject param) {
		//D:\\body\\syfc_sales_num_detail2019-02-09
		String folderPath = param.getString("folderPath");
		detailSpiderService.recoverSalesNumDetail(folderPath);
		return ResponseData.ok();
	}
	
	@ApiOperation(value = "继续爬取沈阳房产预售证")
	@PostMapping("/incrementSalesNo")
	public ResponseData incrementCollectSalesNo() {
        detailSpiderService.incrementCollectSalesNo();
		return ResponseData.ok();
	}
	
	@ApiOperation(value = "爬取沈阳房产预售证详情")
	@PostMapping("/collectDetail")
	public ResponseData collectSalesNumDetail() {
		detailSpiderService.collectSalesNumDetail();
		return ResponseData.ok();
	}
	
	@ApiOperation(value = "去重沈阳房产预售证详情")
	@PostMapping("/distinctSalesNumDetail")
	public ResponseData distinctSalesNumDetail() {
		detailSpiderService.distinctSalesNumDetail();
		return ResponseData.ok();
	}
}

