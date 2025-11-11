package com.kavyashree.bfh;

import com.kavyashree.bfh.service.WebhookService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BfhApplication {

    public static void main(String[] args) {
        SpringApplication.run(BfhApplication.class, args);
    }

    @Bean
    CommandLineRunner run(WebhookService service) {
        return args -> service.executeWorkflow();
    }
}

