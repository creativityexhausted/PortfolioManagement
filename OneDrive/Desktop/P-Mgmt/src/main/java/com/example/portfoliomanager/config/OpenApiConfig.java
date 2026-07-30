package com.example.portfoliomanager.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI portfolioManagerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Portfolio Management API")
                        .description("REST API for managing users, portfolios, assets, and bank transactions.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Portfolio Manager Team")
                                .email("support@portfoliomanager.local")
                                .url("https://example.com/portfolio-manager"))
                        .license(new License()
                                .name("Apache License 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
