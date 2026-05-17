package org.backendcompas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class BackendCompasApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendCompasApplication.class, args);
    }

}
