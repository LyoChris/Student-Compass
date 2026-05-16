package org.backendcompas.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Student Compass API")
                        .version("1.0.0")
                        .description("""
                                REST API for the **Student Compass** platform.

                                ## Authentication
                                Most endpoints require a **Bearer JWT** access token in the `Authorization` header.
                                Tokens are obtained via `/api/v1/auth/login` or `/api/v1/auth/register`.

                                The access token expires in **10 minutes**. Use `/api/v1/auth/refresh` to obtain a
                                new one — the refresh token is transported via an HttpOnly cookie (`refresh_token`)
                                and rotated on every use. Its lifetime is **7 days**.

                                ## CSRF
                                The `/auth/refresh` and `/auth/logout` endpoints require a valid CSRF token.
                                The server sets a `XSRF-TOKEN` cookie on every response; browsers should send it
                                back as the `X-XSRF-TOKEN` request header (handled automatically by Angular, Axios, etc.).
                                """)
                        .contact(new Contact()
                                .name("Student Compass Team")
                                .email("contact@student-compass.ro")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the access token returned by /auth/login or /auth/register.")));
    }
}
