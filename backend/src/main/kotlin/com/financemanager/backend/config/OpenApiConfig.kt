package com.financemanager.backend.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        val securitySchemeName = "BearerAuth"
        return OpenAPI()
            .info(
                Info()
                    .title("سیستم جامع مدیریت مالی، یادآور و پیامک هوشمند (Finance & Reminder API)")
                    .description("REST API برای احراز هویت، اعتبارسنجی شماره همراه ایرانی، زمان‌بندی یادآورها، اقساط، خودرو و صف پیامک")
                    .version("1.0.0")
            )
            .addSecurityItem(SecurityRequirement().addList(securitySchemeName))
            .components(
                Components().addSecuritySchemes(
                    securitySchemeName,
                    SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
            )
    }
}
