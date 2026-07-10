package com.phrolova.vitaelensbackend.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Data
@Component
@ConfigurationProperties(prefix = "vitaelens")
public class UploadConfig {

    private String uploadDir;

    @PostConstruct
    public void init() {
        Path path = Paths.get(uploadDir);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (Exception e) {
                throw new RuntimeException("Could not create upload directory: " + uploadDir, e);
            }
        }
    }
}
