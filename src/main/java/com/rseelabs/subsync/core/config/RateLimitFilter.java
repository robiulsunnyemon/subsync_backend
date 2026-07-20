package com.rseelabs.subsync.core.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> authCache = new ConcurrentHashMap<>();
    private final Map<String, Bucket> apiCache = new ConcurrentHashMap<>();

    private Bucket resolveAuthBucket(String ip) {
        return authCache.computeIfAbsent(ip, this::newAuthBucket);
    }

    private Bucket resolveApiBucket(String ip) {
        return apiCache.computeIfAbsent(ip, this::newApiBucket);
    }

    private Bucket newAuthBucket(String ip) {
        // Strict limit for auth endpoints: 5 requests per minute
        Bandwidth limit = Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket newApiBucket(String ip) {
        // Normal limit for other API endpoints: 100 requests per minute
        Bandwidth limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        
        // Only apply rate limits to /api/ endpoints
        if (requestURI.startsWith("/api/")) {
            String ip = request.getRemoteAddr();
            Bucket bucket;

            if (requestURI.startsWith("/api/v1/auth")) {
                bucket = resolveAuthBucket(ip);
            } else {
                bucket = resolveApiBucket(ip);
            }

            if (bucket.tryConsume(1)) {
                filterChain.doFilter(request, response);
            } else {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Too many requests. Please try again later.");
                response.setContentType("text/plain");
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
