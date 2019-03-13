package com.hanson.spider.component;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author Hanson create on 2019年2月12日
 */

@Configuration // 标记配置类
@EnableSwagger2 // 开启在线接口文档
public class SwaggerConfig {
	/**
	 * 添加摘要信息(Docket)
	 */
	@Bean
	public Docket controllerApi() {
		return new Docket(DocumentationType.SWAGGER_2)
				.apiInfo(new ApiInfoBuilder().title("标题：翰林")
						.description("描述：...").contact("hanson")
						.version("版本号:1.0").build())
				.select().apis(RequestHandlerSelectors.basePackage("com.hanson.spider.controller")).paths(PathSelectors.any())
				.build();
	}
}
