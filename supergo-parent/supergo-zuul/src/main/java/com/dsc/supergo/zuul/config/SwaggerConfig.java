package com.dsc.supergo.zuul.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
/**
 * Swagger配置
 * @author dsc
 */
public class SwaggerConfig {

    @Bean
    @Order(value = 1)
    public Docket groupRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(groupApiInfo());
    }

    private ApiInfo groupApiInfo() {
        return new ApiInfoBuilder()
                .title("swagger-bootstrap-ui zuul")
                .description("<div style='font-size:14px;color:red;'>swagger-bootstrap-ui-demo RESTful APIs</div>")
                .termsOfServiceUrl("http://localhost:9999")
                .contact("xx@qq.com")
                .version("1.0")
                .build();
    }

}