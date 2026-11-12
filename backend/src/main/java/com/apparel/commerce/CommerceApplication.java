package com.apparel.commerce;

import com.apparel.commerce.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class CommerceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CommerceApplication.class, args);
    }
}
