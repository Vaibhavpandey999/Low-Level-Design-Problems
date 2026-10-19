package com.carrental;

// Spring Boot auto-configuration entry point
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;          // activates @Cacheable, @CacheEvict proxy
import org.springframework.scheduling.annotation.EnableAsync;       // allows @Async methods (observer notifications)

/**
 * CarRentalApplication – Main entry point for the Car Rental microservice.
 *
 * @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan
 * @EnableCaching         = Activates Spring's cache proxy (backed by Redis in this service)
 * @EnableAsync           = Allows observer notification methods to run in a thread pool (non-blocking)
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class CarRentalApplication {

    public static void main(String[] args) {
        // Bootstrap Spring context, scan all @Component/@Service/@Repository/@Controller beans
        SpringApplication.run(CarRentalApplication.class, args);
    }
}

