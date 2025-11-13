package com.travel.loginregistration;

import com.travel.loginregistration.config.EnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.travel.loginregistration")
public class LoginRegistrationApplication {

    public static void main(String[] args) {
        // Load .env (if present) and map known keys to Spring properties
        EnvLoader.load();
        SpringApplication.run(LoginRegistrationApplication.class, args);
    }
}
