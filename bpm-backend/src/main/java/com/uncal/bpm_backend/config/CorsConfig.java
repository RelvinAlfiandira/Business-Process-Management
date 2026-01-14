package com.uncal.bpm_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Izinkan semua endpoint di backend ('/**')
        registry.addMapping("/**")
                // Izinkan permintaan dari server frontend React (port 3000)
                .allowedOrigins("http://localhost:3000")
                // Izinkan semua metode (GET, POST, PUT, DELETE)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                // Izinkan header standar dan custom
                .allowedHeaders("*")
                // Penting untuk otentikasi (mengirim cookies/authorization header)
                .allowCredentials(true);
    }
}