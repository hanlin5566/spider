package com.hanson.spider.misc;

import com.hanson.base.enums.EnumType;

/**
 * @author Hanson
 * create on 2019年2月27日
 */

public enum SaleStateEnum implements EnumType{
		UNKNOWN(0, "未知"),
		NOT_INCLUDED(10, "#ccffff"),//未纳入网上销售 #ccffff
	    CAN_SALE(1, "#00ff00"),//#可售00ff00
	    SALEED(2, "#ffff00"),//#已售ffff00
	    NOW_SALE(3, "#CCFF00"),//#现售CCFF00
	    CERTIFIED(4, "#0099ff"),//#已发证0099ff
	    CLOSE_DOWN(1, "#ff0000"),//#查封ff0000
	    ;
		


	    private final int code;
	    private final String text;

	    private SaleStateEnum(int code, String text) {
	        this.code = code;
	        this.text = text;
	    }

	    @Override
	    public int code() {
	        return code;
	    }

	    @Override
	    public String text() {
	        return text;
	    }

	    public static SaleStateEnum codeOf(int code) {
	        for (SaleStateEnum value : values()) {
	            if (value.code == code) {
	                return value;
	            }
	        }

	        throw new IllegalArgumentException("Invalid SaleStateEnum code: " + code);
	    }
	    
	    public static SaleStateEnum textOf(String text) {
	        for (SaleStateEnum value : values()) {
	            if (value.text.toLowerCase().equals(text.toLowerCase())) {
	                return value;
	            }
	        }
	        throw new IllegalArgumentException("Invalid SaleStateEnum code: " + text);
	    }
}
