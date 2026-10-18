package com.bookmyshow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * ============================================================
 * BookMyShowApplication – Main entry point
 * ============================================================
 * @SpringBootApplication combines:
 *   @Configuration     – marks this class as a Spring config source
 *   @EnableAutoConfiguration – auto-wires Spring Boot defaults
 *   @ComponentScan     – scans com.bookmyshow for beans
 *
 * @EnableCaching – activates Spring's proxy-based cache abstraction
 *                  (backed by Redis as configured in application.yml)
 * @EnableAsync   – allows @Async methods (used for notifications)
 */
@SpringBootApplication
@EnableCaching   // DESIGN PATTERN: Proxy Pattern – Spring wraps @Cacheable beans transparently
@EnableAsync     // allows non-blocking notification dispatch
public class BookMyShowApplication {

    /**
     * Application entry point – delegates to SpringApplication which:
     * 1. Creates ApplicationContext
     * 2. Loads all @Configuration classes
     * 3. Starts embedded Tomcat on port 8080
     */
    public static void main(String[] args) {
        SpringApplication.run(BookMyShowApplication.class, args);
    }
}

