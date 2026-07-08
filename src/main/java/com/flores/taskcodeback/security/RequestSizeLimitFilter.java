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

/**
 * Rejects oversized request bodies to mitigate payload-based DoS attacks.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long contentLength = request.getContentLengthLong();
        int maxSize = securityProperties.getMaxBodySize();

        if (contentLength > maxSize) {
            response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ErrorResponse error = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.PAYLOAD_TOO_LARGE.value())
                    .error("Payload demasiado grande")
                    .message("El cuerpo de la solicitud supera el tamaño máximo permitido")
                    .path(request.getRequestURI())
                    .build();

            objectMapper.writeValue(response.getOutputStream(), error);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
