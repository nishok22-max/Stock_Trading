package com.stock.trading.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI stockTradingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Stock Trading Application API")
                        .version("1.0.0")
                        .description("""
                            Comprehensive REST API for the Stock Trading Application.
                            Integrates all 5 modules:
                            - Member 1: User & Account Management
                            - Member 2: Stock & Market Management
                            - Member 3: Order Management & Trading Engine
                            - Member 4: Portfolio & Watchlist Management
                            - Member 5: Dashboard, Integration & Quality Assurance
                            """)
                        .contact(new Contact()
                                .name("Trading System Integration Team")
                                .email("support@stocktrading.local"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development Server")
                ));
    }
}

