package com.example.note.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

// Wires the OpenAPI spec served by springdoc at /v3/api-docs (and the Swagger UI
// at /swagger-ui.html). Declares a global "bearerAuth" JWT scheme so generated
// Postman collections and Swagger UI know how to attach the Authorization header.
//- Swagger UI: http://localhost:8181/swagger-ui.html — browse and try endpoints
//- Postman import: Import → Link → http://localhost:8181/v3/api-docs → full collection grouped by
//
// Note on Swagger UI responses: the "Response content type" dropdown in the
// "Try it out" panel does NOT auto-select application/json — pick it manually
// before sending the request, otherwise the response body is rendered as plain
// text instead of formatted JSON.
@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI {
        val schemeName = "bearerAuth"
        return OpenAPI()
            .info(
                Info()
                    .title("Notes API")
                    .description("Notes API with JWT auth, i18n, and token revocation")
                    .version("0.0.1-SNAPSHOT"),
            )
            .addSecurityItem(SecurityRequirement().addList(schemeName))
            .components(
                Components().addSecuritySchemes(
                    schemeName,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"),
                ),
            )
    }
}
