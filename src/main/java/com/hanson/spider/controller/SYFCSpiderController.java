package com.hanson.spider.controller;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.hanson.spider.misc.SpiderResponseCode;
import com.hanson.spider.service.SYFCNewBuildListSpiderService;
import com.hanson.spider.service.SYFCSalesNumDetailSpiderService;
import com.hanson.spider.service.SYFCSalesNumListSpiderService;
import com.hanson.spider.service.SYFCSalesPriceListSpiderService;
import com.hzcf.base.exception.ControllerException;
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
	SYFCSalesNumListSpiderService spiderService;
	@Autowired
	SYFCSalesNumDetailSpiderService detailSpiderService;
	@Autowired
	SYFCNewBuildListSpiderService newBuildSpiderService;
	@Autowired
	SYFCSalesPriceListSpiderService salesPricelistSpiderService;
	
	
	@ApiOperation(value = "全量爬取沈阳房产预售证", notes = "根据传入的参数，地址，返回爬取内容")
	@PostMapping()
	public ResponseData execute(HttpServletRequest request) {
		InputStream inputStream = null;
		JSONObject recObject;
        try {
            inputStream = request.getInputStream();
            String rec = IOUtils.toString(inputStream, "UTF-8");
            recObject = JSONObject.parseObject(rec);
            IOUtils.closeQuietly(inputStream);
		} catch (IOException ioException) {
			throw new ControllerException(SpiderResponseCode.PARAM_ERROR,String.format(SpiderResponseCode.PARAM_ERROR.detailMsg(), ioException.getMessage()),ioException);
		}catch (JSONException jsonException) {
			throw new ControllerException(SpiderResponseCode.PARAM_ERROR,String.format(SpiderResponseCode.PARAM_ERROR.detailMsg(), jsonException.getMessage()),jsonException);
		}catch (Exception exception) {
			throw new ControllerException(SpiderResponseCode.PARAM_ERROR,String.format(SpiderResponseCode.PARAM_ERROR.detailMsg(), exception.getMessage()),exception);
		}
        spiderService.getSlaesNo(recObject);
		return ResponseData.ok();
	}
	
	
	@ApiOperation(value = "继续爬取沈阳房产预售证", notes = "根据传入的taskId增量爬取数据")
	@PostMapping("/{taskId}")
	public ResponseData continueExecute(@PathVariable String taskId) {
        spiderService.continueGetSlaesNoByBlocked(taskId);
		return ResponseData.ok();
	}
	
	@ApiOperation(value = "继续爬取沈阳房产预售证", notes = "根据传入的taskId增量爬取数据")
	@PostMapping("/incrementSalesNo")
	public ResponseData incrementCollectSalesNo() {
        detailSpiderService.incrementCollectSalesNo();
		return ResponseData.ok();
	}
	
	@ApiOperation(value = "爬取沈阳房产预售证详情", notes = "根据传入的taskId增量爬取数据")
	@PostMapping("/collectDetail")
	public ResponseData collectSalesNumDetail() {
		detailSpiderService.collectSalesNumDetail();
		return ResponseData.ok();
	}
	
	@ApiOperation(value = "爬取沈阳房产新建楼盘列表", notes = "根据传入的taskId增量爬取数据")
	@PostMapping("/collectNewBuildList")
	public ResponseData collectNewBuildList() {
		newBuildSpiderService.collectNewBuildList();
		return ResponseData.ok();
	}
	
	@ApiOperation(value = "爬取沈阳房产销售价格列表", notes = "根据预售许可证爬取数据")
	@PostMapping("/collectPriceList")
	public ResponseData collectPriceList() {
		salesPricelistSpiderService.collectPriceList();
		return ResponseData.ok();
	}
}

