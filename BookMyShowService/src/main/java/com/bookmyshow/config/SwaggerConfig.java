package com.bookmyshow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SwaggerConfig – OpenAPI / Swagger UI Configuration
 *
 * Configures:
 * 1. API metadata (title, description, version)
 * 2. JWT Bearer token security scheme for Swagger UI testing
 *
 * Access Swagger UI at: http://localhost:8080/swagger-ui.html
 * Access raw OpenAPI JSON at: http://localhost:8080/api-docs
 */
@Configuration
public class SwaggerConfig {

    /**
     * OpenAPI bean that configures the Swagger documentation.
     *
     * Security scheme setup allows "Authorize" button in Swagger UI
     * where you paste the JWT token and all requests use it automatically.
     *
     * @return Configured OpenAPI instance
     */
    @Bean
    public OpenAPI bookMyShowOpenAPI() {
        // Define the security scheme name (referenced below)
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                // API metadata for the Swagger UI header
                .info(new Info()
                        .title("BookMyShow – Movie Ticket Booking API")           // API title
                        .description("Production LLD implementation of BookMyShow.\n\n" +
                                "**Design Patterns Used:**\n" +
                                "- Strategy (PaymentProcessor)\n" +
                                "- Factory (PaymentProcessorFactory)\n" +
                                "- Observer (BookingEventPublisher)\n" +
                                "- Builder (Lombok @Builder on all entities)\n" +
                                "- Repository (Spring Data JPA/ES)\n" +
                                "- Template Method (BookingService.book())\n" +
                                "- Facade (BookingService as single entry point)\n" +
                                "- Optimistic Locking (@Version on ShowSeat)")   // Description
                        .version("1.0.0")                                          // API version
                        .contact(new Contact()
                                .name("BookMyShow LLD Team")
                                .email("dev@bookmyshow.com"))
                )
                // Add security requirement: all protected endpoints need bearerAuth
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                // Register the JWT bearer token security scheme
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)     // HTTP auth type
                                        .scheme("bearer")                   // Bearer scheme
                                        .bearerFormat("JWT")               // Token format hint
                        )
                );
    }
}

