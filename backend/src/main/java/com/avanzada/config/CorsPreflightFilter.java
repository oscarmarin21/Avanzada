package com.avanzada.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Responds to CORS preflight (OPTIONS) requests immediately with 200 and CORS headers,
 * before the security chain runs. Ensures preflight works behind proxies (e.g. Railway).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsPreflightFilter extends OncePerRequestFilter {

    private static final List<String> ALLOWED_ORIGINS = List.of(
            "https://avanzada.vercel.app",
            "https://*.vercel.app",
            "http://localhost:4000",
            "http://127.0.0.1:4000"
    );

    private static final String ALLOWED_METHODS = "GET, POST, PUT, PATCH, DELETE, OPTIONS";
    private static final String ALLOWED_HEADERS = "*";
    private static final String EXPOSED_HEADERS = "Authorization";
    private static final long MAX_AGE = 3600;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!"OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String origin = request.getHeader("Origin");
        if (origin != null && isAllowedOrigin(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        }
        response.setHeader("Access-Control-Allow-Methods", ALLOWED_METHODS);
        response.setHeader("Access-Control-Allow-Headers", ALLOWED_HEADERS);
        response.setHeader("Access-Control-Expose-Headers", EXPOSED_HEADERS);
        response.setHeader("Access-Control-Max-Age", String.valueOf(MAX_AGE));
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private static boolean isAllowedOrigin(String origin) {
        if (ALLOWED_ORIGINS.contains(origin)) {
            return true;
        }
        return origin.startsWith("https://") && origin.endsWith(".vercel.app");
    }
}
