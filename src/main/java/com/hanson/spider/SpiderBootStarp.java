package com.hanson.spider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author Hanson create on 2018年3月11日
 */
@SpringBootApplication
@EnableSwagger2
// 暂时不使用数据库配置
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class, })
public class SpiderBootStarp {
	public static void main(String[] args) {
		SpringApplication.run(SpiderBootStarp.class, args);
	}
}