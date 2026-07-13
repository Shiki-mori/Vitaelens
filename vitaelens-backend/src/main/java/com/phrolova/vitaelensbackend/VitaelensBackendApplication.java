package com.phrolova.vitaelensbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class VitaelensBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(VitaelensBackendApplication.class, args);
    }

}
