package com.phrolova.vitaelensbackend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI vitaelensOpenAPI() {
        System.out.println("Generating OpenAPI documentation...");
        return new OpenAPI()
                .info(new Info()
                        .title("Vitaelens API Documentation")
                        .description("vitaelens后端接口文档")
                        .version("v1")
                        .license(new License().name("Apache 2.0")));
    }
}
