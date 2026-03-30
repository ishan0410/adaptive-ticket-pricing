package com.adaptiveticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AdaptiveTicketPricingApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdaptiveTicketPricingApplication.class, args);
    }
}
