package com.paychecker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI payCheckerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PayChecker API")
                        .version("v1")
                        .description("""
                                PayChecker is a fintech backend API for payment authorization,
                                fraud risk scoring, risk alerts and append-only financial event logging.
                                """));
    }
}