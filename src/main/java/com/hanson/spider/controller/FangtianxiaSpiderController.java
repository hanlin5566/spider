package com.hanson.spider.controller;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.hanson.spider.misc.SpiderResponseCode;
import com.hanson.spider.service.FangtianxiaSpiderService;
import com.hzcf.base.exception.ControllerException;
import com.hzcf.base.response.ResponseData;

import io.swagger.annotations.ApiOperation;

/**
 * @author Hanson
 * create on 2018年3月11日
 */
//@RestController
//@RequestMapping(value = "/fangtianxia")
//@Api("房天下爬虫")
public class FangtianxiaSpiderController{
	@Autowired
	FangtianxiaSpiderService spiderService;
	
	
	@ApiOperation(value = "爬取新盘列表", notes = "根据传入的参数，地址，返回爬取内容")
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
        String ret = spiderService.getAllNews(recObject);
		return ResponseData.ok(ret);
	}
}

