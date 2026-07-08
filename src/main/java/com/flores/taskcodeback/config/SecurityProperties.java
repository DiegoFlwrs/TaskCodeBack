package com.flores.taskcodeback.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class SecurityProperties {

    /** Requests per minute allowed on auth endpoints (login, register, reset). */
    private int authRateLimit = 15;

    /** Requests per minute allowed on general API endpoints per IP. */
    private int apiRateLimit = 120;

    /** Max JSON request body size in bytes (default 1 MB). */
    private int maxBodySize = 1_048_576;

    /** Allowed CORS origins (comma-separated in properties). */
    private List<String> allowedOrigins = List.of("http://localhost:3000");

    /** Disable Swagger UI in production. */
    private boolean swaggerEnabled = true;

    /** Max failed login attempts before account lockout. */
    private int maxLoginAttempts = 5;

    /** Lockout duration in minutes after max failed attempts. */
    private int lockoutDurationMinutes = 15;
}
