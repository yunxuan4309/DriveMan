package com.homework.driveman.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j (SpringDoc OpenAPI) 接口文档配置
 * 启动后访问: http://localhost:9080/doc.html
 */
@Configuration
public class Knife4jConfiguration {

    @Value("${knife4j.title:驾校报名管理系统接口文档}")
    private String title;

    @Value("${knife4j.description:驾校报名管理系统在线API文档}")
    private String description;

    @Value("${knife4j.contact.name:开发团队}")
    private String contactName;

    @Value("${knife4j.contact.email:dev@example.com}")
    private String contactEmail;

    /** 扫描 controller 包，生成接口分组 */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("driving-school")
                .packagesToScan("com.homework.driveman.controller")
                .pathsToMatch("/**")
                .build();
    }

    /** 自定义 API 文档信息（标题、描述、联系方式等） */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .description(description)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name(contactName)
                                .url("http://localhost:9080/doc.html")
                                .email(contactEmail))
                        .termsOfService("http://www.apache.org/licenses/LICENSE-2.0")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0")));
    }
}
