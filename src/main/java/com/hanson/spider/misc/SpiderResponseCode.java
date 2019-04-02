package com.hanson.spider.misc;

import com.hanson.base.enums.IResponseCode;

/**
 * Create by hanlin on 2018年10月11日
 **/

public enum SpiderResponseCode implements IResponseCode {
	PARAM_ERROR(200001, "接收参数异常","接收参数发生%s异常,"),
	WRITE_FILE_ERROR(200401, "生成文件发生异常","生成文件发生%s异常,"),
	PARSE_SALES_PAGE_COUNT_ERROR(200501, "获取预售许可证总页数发生异常","获取预售许可证总页数发生%s异常,"),
	Spider_SALES_PAGE_ERROR(200502, "爬取预售许可证总页数发生异常","爬取预售许可证总页数发生%s异常,"),
	SPIDER_PUT_CONSUMER_ERROR(200601, "爬取预售许可证总页数发生异常","爬取预售许可证总页数发生%s异常,"),
	SPIDER_GET_CONSUMER_ERROR(200602, "爬取预售许可证总页数发生异常","爬取预售许可证总页数发生%s异常,"),
	SPIDER_PUT_PRODUCER_ERROR(200603, "爬取预售许可证总页数发生异常","爬取预售许可证总页数发生%s异常,"),
	SPIDER_GET_PRODUCER_ERROR(200604, "爬取预售许可证总页数发生异常","爬取预售许可证总页数发生%s异常,"),
	;
	private final int code;
	private final String friendlyMsg;
	private final String detailMsg;
	
	private SpiderResponseCode(int code, String friendlyMsg) {
		this.code = code;
		this.friendlyMsg = friendlyMsg;
		this.detailMsg = friendlyMsg;
	}

	private SpiderResponseCode(int code, String friendlyMsg, String detailMsg) {
		this.code = code;
		this.friendlyMsg = friendlyMsg;
		this.detailMsg = detailMsg;
	}

	@Override
	public int code() {
		return code;
	}

	@Override
	public String friendlyMsg() {
		return friendlyMsg;
	}

	@Override
	public String detailMsg() {
		return detailMsg;
	}

	public static SpiderResponseCode codeOf(int code) {
        for (SpiderResponseCode value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid SpiderResponseCode code: " + code);
    }
}