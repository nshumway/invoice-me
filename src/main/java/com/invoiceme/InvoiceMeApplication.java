package com.invoiceme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class InvoiceMeApplication {
    public static void main(String[] args) {
        SpringApplication.run(InvoiceMeApplication.class, args);
    }
}
