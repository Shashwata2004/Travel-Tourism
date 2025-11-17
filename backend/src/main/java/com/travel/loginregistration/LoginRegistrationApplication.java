package com.travel.loginregistration;

import com.travel.loginregistration.config.EnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/*
 * Main application class for Login and Registration service.
 * Loads environment variables and starts the Spring Boot application.
 * The @ComponentScan annotation ensures that all the files in the specified package are scanned and registered as Spring Beans.
 * starts a tomcat server and hosts the application.
 * Envloader loads .env file and puts all data (like database url, username,password,jwt secret etc) into spring properties
 * it connects to the database using these properties and runs the application.
 * after running the application, it listens for incoming http requests from frontend.
 */

@SpringBootApplication
@ComponentScan(basePackages = "com.travel.loginregistration")
public class LoginRegistrationApplication {

    public static void main(String[] args) {
        // Load .env (if present) and map known keys to Spring properties
        EnvLoader.load();
        SpringApplication.run(LoginRegistrationApplication.class, args);
    }
}
