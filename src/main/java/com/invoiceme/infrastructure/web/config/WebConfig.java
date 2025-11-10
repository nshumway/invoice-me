package com.invoiceme.infrastructure.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                    "http://localhost:5173",
                    "https://invoice-me-frontend.onrender.com"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // Restrict allowed headers to only what we need (security best practice)
                .allowedHeaders("Authorization", "Content-Type", "X-Requested-With")
                // Expose specific response headers to the frontend
                .exposedHeaders("Authorization")
                // Credentials disabled - we use stateless JWT in Authorization header, not cookies
                // This prevents CSRF attacks if cookies were accidentally added in the future
                .allowCredentials(false)
                // Cache preflight response for 1 hour
                .maxAge(3600);
    }
}
