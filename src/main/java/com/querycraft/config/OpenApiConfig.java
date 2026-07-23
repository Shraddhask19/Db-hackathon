package com.querycraft.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI queryCraftOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("QueryCraft AI Text-to-SQL API")
                        .description("Enterprise Spring Boot 3 Backend Platform for AI-driven Text-to-SQL Generation with strict JSqlParser AST safety validation.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("QueryCraft Architecture Team")
                                .email("architecture@querycraft.ai"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
