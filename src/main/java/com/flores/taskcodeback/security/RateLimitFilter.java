package com.flores.taskcodeback.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flores.taskcodeback.config.SecurityProperties;
import com.flores.taskcodeback.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory rate limiter per client IP.
 * Protects auth endpoints (brute-force) and general API (DDoS mitigation at app layer).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, WindowCounter> authBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WindowCounter> apiBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        boolean isAuthPath = path.startsWith("/api/auth/");

        int limit = isAuthPath
                ? securityProperties.getAuthRateLimit()
                : securityProperties.getApiRateLimit();

        ConcurrentHashMap<String, WindowCounter> bucket = isAuthPath ? authBuckets : apiBuckets;

        if (!tryConsume(bucket, clientIp, limit)) {
            writeTooManyRequests(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean tryConsume(ConcurrentHashMap<String, WindowCounter> buckets, String key, int limit) {
        WindowCounter counter = buckets.computeIfAbsent(key, k -> new WindowCounter());
        return counter.tryConsume(limit);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("Demasiadas solicitudes")
                .message("Has superado el límite de peticiones. Intenta de nuevo en un minuto.")
                .path(null)
                .build();

        objectMapper.writeValue(response.getOutputStream(), error);
    }

    private static final class WindowCounter {
        private static final long WINDOW_MS = 60_000L;

        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        synchronized boolean tryConsume(int limit) {
            long now = System.currentTimeMillis();
            if (now - windowStart >= WINDOW_MS) {
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() <= limit;
        }
    }
}
