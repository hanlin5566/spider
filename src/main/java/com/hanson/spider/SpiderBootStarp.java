package com.hanson.spider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

import net.sourceforge.tess4j.TesseractException;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author Hanson create on 2018年3月11日
 */
@SpringBootApplication
@EnableSwagger2
// 暂时不使用数据库配置
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class, })
public class SpiderBootStarp {
	static Logger logger = LoggerFactory.getLogger(SpiderBootStarp.class);
	
	public static void main(String[] args) throws TesseractException {
		SpringApplication.run(SpiderBootStarp.class, args);
	}
}