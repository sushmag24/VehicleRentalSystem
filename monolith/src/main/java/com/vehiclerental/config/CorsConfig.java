package com.vehiclerental.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${FRONTEND_URL:}")
    private String customFrontendUrl;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                List<String> origins = new ArrayList<>(List.of(
                        "http://localhost:4200",
                        "http://localhost:3000",
                        "http://localhost:8080",
                        "https://*.vercel.app"
                ));

                if (customFrontendUrl != null && !customFrontendUrl.isBlank()) {
                    origins.add(customFrontendUrl.trim());
                }

                registry.addMapping("/**")
                        .allowedOriginPatterns(origins.toArray(new String[0]))
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}
