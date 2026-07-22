package com.medibridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync    // EmailService sends off the request thread
public class MediBridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediBridgeApplication.class, args);
    }
}
