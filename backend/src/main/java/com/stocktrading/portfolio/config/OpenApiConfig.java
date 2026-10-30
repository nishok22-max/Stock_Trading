package com.stocktrading.portfolio.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for Swagger/OpenAPI 3 documentation and Cross-Origin Resource Sharing (CORS).
 */
@Configuration
public class OpenApiConfig implements WebMvcConfigurer {

    @Bean
    public OpenAPI portfolioOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Stock Trading App – Member 4: Portfolio & Watchlist API")
                        .description("REST API for managing users' stock holdings, portfolio valuations, weighted average purchase prices, and watchlists.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Member 4 Team Lead")
                                .email("portfolio@stocktrading.local"))
                        .license(new License().name("Apache 2.0")));
    }

    /**
     * Enables CORS for local frontend communication (Astro.js on port 4321 / 3000).
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:4321", "http://localhost:3000", "http://127.0.0.1:4321")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
