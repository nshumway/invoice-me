package com.invoiceme.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiting filter to prevent brute force attacks and DoS.
 *
 * Rate limits:
 * - Auth endpoints (/api/auth/**): 5 requests per minute per IP
 * - All other API endpoints: 100 requests per minute per IP
 *
 * NOTE: This is a basic implementation using in-memory storage.
 * For production with multiple instances, consider using Redis or a dedicated
 * rate limiting service like Bucket4j with Redis backend.
 *
 * Disabled in test profile to avoid interference with integration tests.
 */
@Component
@org.springframework.context.annotation.Profile("!test")
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    // Rate limit windows (in seconds)
    private static final int WINDOW_SIZE_SECONDS = 60;

    // Rate limits per window
    private static final int AUTH_ENDPOINT_LIMIT = 5;  // 5 requests per minute for auth
    private static final int API_ENDPOINT_LIMIT = 100; // 100 requests per minute for other APIs

    // Storage for rate limit tracking: IP -> RateLimitEntry
    private final Map<String, RateLimitEntry> rateLimitStore = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        String clientIP = getClientIP(request);

        // Determine rate limit based on endpoint
        int rateLimit = requestURI.startsWith("/api/auth/") ? AUTH_ENDPOINT_LIMIT : API_ENDPOINT_LIMIT;

        // Create unique key for this IP + endpoint type
        String key = clientIP + ":" + (requestURI.startsWith("/api/auth/") ? "auth" : "api");

        // Check and update rate limit
        if (isRateLimitExceeded(key, rateLimit)) {
            logger.warn("Rate limit exceeded for IP: {}, endpoint type: {}, URI: {}",
                       clientIP, (requestURI.startsWith("/api/auth/") ? "auth" : "api"), requestURI);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"success\":false,\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }

        logger.debug("Rate limit check passed for IP: {}, endpoint: {}", clientIP, requestURI);
        filterChain.doFilter(request, response);
    }

    /**
     * Checks if rate limit is exceeded for the given key.
     * Uses sliding window algorithm.
     */
    private boolean isRateLimitExceeded(String key, int limit) {
        long now = Instant.now().getEpochSecond();

        rateLimitStore.compute(key, (k, entry) -> {
            if (entry == null || now - entry.windowStart >= WINDOW_SIZE_SECONDS) {
                // New window
                return new RateLimitEntry(now, 1);
            } else {
                // Same window, increment counter
                entry.count.incrementAndGet();
                return entry;
            }
        });

        RateLimitEntry entry = rateLimitStore.get(key);
        return entry.count.get() > limit;
    }

    /**
     * Gets the client IP address, considering X-Forwarded-For header for proxy situations.
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Cleanup old entries periodically to prevent memory leaks.
     * Called automatically by Spring's scheduled task if enabled.
     */
    public void cleanupOldEntries() {
        long now = Instant.now().getEpochSecond();
        int sizeBefore = rateLimitStore.size();
        rateLimitStore.entrySet().removeIf(entry ->
            now - entry.getValue().windowStart >= WINDOW_SIZE_SECONDS * 2
        );
        int sizeAfter = rateLimitStore.size();
        if (sizeBefore != sizeAfter) {
            logger.debug("Rate limit store cleanup: removed {} entries, {} remaining",
                        (sizeBefore - sizeAfter), sizeAfter);
        }
    }

    /**
     * Internal class to track rate limit state.
     */
    private static class RateLimitEntry {
        final long windowStart;
        final AtomicInteger count;

        RateLimitEntry(long windowStart, int initialCount) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(initialCount);
        }
    }
}
