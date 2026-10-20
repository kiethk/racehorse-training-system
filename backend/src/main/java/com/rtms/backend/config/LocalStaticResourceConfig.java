package com.rtms.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class LocalStaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.storage.local-dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        // Ánh xạ: URL /api/v1/files/** -> Trỏ vào thư mục ./uploads/ trên máy tính
        registry.addResourceHandler("/api/v1/files/**")
                .addResourceLocations("file:" + uploadPath.toString() + "/");
    }
}
