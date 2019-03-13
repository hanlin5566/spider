package com.hanson.spider.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hanson.spider.service.SYFCNewBuildDetailSpiderService;
import com.hanson.spider.service.SYFCNewBuildHouseSpiderService;
import com.hanson.spider.service.SYFCNewBuildListSpiderService;
import com.hzcf.base.response.ResponseData;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * @author Hanson
 * create on 2018年3月11日
 */
@RestController
@RequestMapping(value = "/syfc/build")
@Api("沈阳房产-新建楼栋")
public class SYFCSalesBuildController{
	@Autowired
	SYFCNewBuildListSpiderService newBuildSpiderService;
	@Autowired
	SYFCNewBuildDetailSpiderService newBuildDetailSpiderService;
	@Autowired
	SYFCNewBuildHouseSpiderService houseSpiderService;
	
	@ApiOperation(value = "爬取沈阳房产新建楼盘列表", notes = "按页采集，并且将每页存入了一个document，每页的list为sales_build_list字段")
	@PostMapping("/collectNewBuildList")
	public ResponseData collectNewBuildList() {
		newBuildSpiderService.collectNewBuildList();
		return ResponseData.ok();
	}
	
	@ApiOperation(value = "抽取沈阳房产新建楼盘列表", notes = "根据build列表生成detail，抽取sales_build_list展开为单独的document")
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
	
	@ApiOperation(value = "增量爬取沈阳房产新建楼盘详情" , notes = "")
	@PostMapping("/incrementNewBuildDetail")
	public ResponseData incrementNewBuildDetail() {
		newBuildDetailSpiderService.incrementNewBuildDetail();
		return ResponseData.ok();
	}
	
	@ApiOperation(value = "初始化每日新建楼盘任务" , notes = "")
	@PostMapping("/initTodayNewBuildDetail")
	public ResponseData initTodayNewBuildDetail() {
		newBuildDetailSpiderService.initTodayNewBuildDetail();
		return ResponseData.ok();
	}
	
	
	@ApiOperation(value = "采集房屋销售情况详情", notes = "")
	@PostMapping("/collectNewHouseDetail")
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

