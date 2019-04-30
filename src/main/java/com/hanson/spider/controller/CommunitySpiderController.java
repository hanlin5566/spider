//package com.hanson.spider.controller;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.hanson.base.response.ResponseData;
//import com.hanson.spider.service.CommunitySpiderService;
//
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
//
///**
// * @author Hanson
// * create on 2019年4月3日
// */
//@RestController
//@RequestMapping(value = "/syfc/community")
//@Api("沈阳房产-楼盘信息")
//public class CommunitySpiderController {
//	@Autowired
//	private CommunitySpiderService spiderService;
//	
//	@ApiOperation(value = "采集新楼盘", notes = "")
//	@PostMapping("/new/")
//	public ResponseData incrementCommunity() {
//		spiderService.incrementCommunity();
//		return ResponseData.ok();
//	}
//}
//
