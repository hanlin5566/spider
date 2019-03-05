package com.hanson.spider.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hanson.spider.service.SYFCNewBuildDetailSpiderService;
import com.hanson.spider.service.SYFCNewBuildHouseSpiderService;
import com.hanson.spider.service.SYFCNewBuildListSpiderService;
import com.hanson.spider.service.SYFCSalesPriceListSpiderService;
import com.hzcf.base.response.ResponseData;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * @author Hanson
 * create on 2018年3月11日
 */
@RestController
@RequestMapping(value = "/syfc")
@Api("沈阳房产爬虫")
public class SYFCSpiderController{
	@Autowired
	SYFCNewBuildListSpiderService newBuildSpiderService;
	@Autowired
	SYFCNewBuildDetailSpiderService newBuildDetailSpiderService;
	@Autowired
	SYFCSalesPriceListSpiderService salesPricelistSpiderService;
	@Autowired
	SYFCNewBuildHouseSpiderService houseSpiderService;
	
	@ApiOperation(value = "爬取沈阳房产新建楼盘列表", notes = "根据传入的taskId增量爬取数据")
	@PostMapping("/collectNewBuildList")
	public ResponseData collectNewBuildList() {
		newBuildSpiderService.collectNewBuildList();
		return ResponseData.ok();
	}
	
	@ApiOperation(value = "抽取沈阳房产新建楼盘列表", notes = "根据传入的taskId增量爬取数据")
	@PostMapping("/transformBuildDetailTask")
	public ResponseData transformTask() {
		newBuildDetailSpiderService.transformTask();
		return ResponseData.ok();
	}
	
	@ApiOperation(value = "爬取沈阳房产新建楼盘详情")
	@PostMapping("/collectNewBuildDetail")
	public ResponseData collectNewBuildDetail() {
		newBuildDetailSpiderService.collectNewBuildDetail();
		return ResponseData.ok();
	}
	
	
	@ApiOperation(value = "采集房屋销售情况详情", notes = "")
	@PostMapping("/collectHouseList")
	public ResponseData collectHouseList() {
		houseSpiderService.collectNewHouseDetail();
		return ResponseData.ok();
	}
	
	@ApiOperation(value = "抽取生成房屋销售情况列表", notes = "")
	@PostMapping("/transformBuildHouseTask")
	public ResponseData transformHouseTask() {
		houseSpiderService.transformTask();
		return ResponseData.ok();
	}
}

