package com.valderson.shoppingcart.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI gadernalApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Shopping Cart")
                        .description("")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Valderson")
                                .email("valdersonpontes@gmail.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org"))
                );
//                .components(new Components()
//                        .addSecuritySchemes("bearer-jwt",
//                                new SecurityScheme()
//                                        .type(SecurityScheme.Type.HTTP)
//                                        .scheme("bearer")
//                                        .bearerFormat("JWT")
//                        )
//                );
//                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/**")
                .build();
    }
}