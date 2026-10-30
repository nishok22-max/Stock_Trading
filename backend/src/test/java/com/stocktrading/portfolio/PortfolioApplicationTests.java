package com.stocktrading.portfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = PortfolioApplication.class)
class PortfolioApplicationTests {

    @Test
    @DisplayName("Verify Spring application context loads successfully")
    void contextLoads() {
        // Confirms that Spring Boot context bootstraps without database dependencies
    }
}
