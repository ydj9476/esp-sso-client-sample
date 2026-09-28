package com.example.sso;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SsoClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(SsoClientApplication.class, args);
    }
}
