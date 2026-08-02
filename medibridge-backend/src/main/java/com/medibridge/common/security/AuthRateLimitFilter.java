package com.medibridge.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Throttles the credential-accepting endpoints.
 *
 * <p>Everything else in this application's security is about what an
 * authenticated caller may touch. This covers the step before that: an
 * unauthenticated caller guessing. Without it, bcrypt at strength 12 is the only
 * thing between an attacker and an account, and ~250ms per attempt still allows
 * thousands of guesses an hour against a weak password.
 *
 * <p><b>Sliding window, not a fixed one.</b> A fixed window resets on a clock
 * boundary, so an attacker who lines up with it gets double the quota in the
 * instant either side. The deque holds only the timestamps still inside the
 * window, which caps its own size at the limit.
 *
 * <p><b>In-memory, and therefore per-instance.</b> Two instances behind a load
 * balancer each allow the full quota. That is the right trade for this
 * deployment - a single JVM - and the wrong one the moment it scales out, at
 * which point the counter belongs in Redis. Stated here rather than discovered
 * later.
 *
 * <p><b>Keyed on the socket address by default.</b> {@code X-Forwarded-For} is a
 * request header like any other: trusting it unconditionally lets an attacker
 * mint a fresh identity per request and walk straight past this filter. It is
 * only read when {@code medibridge.ratelimit.trust-forwarded-for} is on, which
 * is correct exclusively behind a proxy that overwrites the header.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitFilter.class);

    /**
     * Only the endpoints that accept credentials or mint accounts.
     *
     * <p>{@code /auth/refresh} is deliberately absent: it is called on a timer by
     * every open tab, and it already carries a single-use token that the server
     * burns on rotation - so a guess is not cheap the way a password guess is.
     */
    private static final List<String> GUARDED = List.of(
            "/auth/login",
            "/auth/google",
            "/auth/register/patient",
            "/auth/register/doctor",
            // Phone login is guarded here AND by a per-number cooldown in the
            // database. Neither alone is enough: this one keys on the caller, so
            // a rotating source address walks past it and bills us for an SMS
            // per request; the cooldown keys on the number, so one attacker can
            // still spray a different number every time.
            "/auth/otp/request",
            "/auth/otp/verify");

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    @Value("${medibridge.ratelimit.auth.max-attempts:10}")
    private int maxAttempts;

    @Value("${medibridge.ratelimit.auth.window-seconds:60}")
    private long windowSeconds;

    @Value("${medibridge.ratelimit.trust-forwarded-for:false}")
    private boolean trustForwardedFor;

    /**
     * The context path is stripped before matching, so these paths are the same
     * ones {@code SecurityConfig} uses - {@code /auth/login}, not
     * {@code /api/auth/login}.
     *
     * <p>Derived from the request URI rather than {@code getServletPath()},
     * which MockMvc leaves empty - the filter silently matched nothing under
     * test while appearing to work in the running application.
     */
    private String pathWithinApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        return context != null && !context.isEmpty() && uri.startsWith(context)
                ? uri.substring(context.length())
                : uri;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = pathWithinApplication(request);
        return GUARDED.stream().noneMatch(path::equals);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String key = clientId(request) + " " + pathWithinApplication(request);

        if (!allow(key)) {
            log.warn("Rate limit hit on {} from {}", pathWithinApplication(request), clientId(request));
            reject(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Records an attempt and reports whether it is inside the limit.
     *
     * <p>Synchronised on the individual deque rather than the map: two requests
     * from different callers never contend, and two from the same caller must
     * not both read a count of {@code limit - 1} and both proceed.
     */
    private boolean allow(String key) {
        long now = System.currentTimeMillis();
        long cutoff = now - Duration.ofSeconds(windowSeconds).toMillis();

        Deque<Long> window = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst() < cutoff) {
                window.pollFirst();
            }
            if (window.size() >= maxAttempts) {
                // Not recorded. Otherwise a caller hammering the endpoint would
                // keep pushing their own window forward and never be let back in.
                return false;
            }
            window.addLast(now);
            return true;
        }
    }

    /**
     * Says how long to wait, and nothing else.
     *
     * <p>No hint about whether the email existed or the password was close - the
     * same reasoning as the deliberately vague "Invalid email or password".
     */
    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(windowSeconds));
        response.getWriter().write(
                "{\"message\":\"Too many attempts. Please wait "
                        + windowSeconds + " seconds and try again.\",\"status\":429}");
    }

    private String clientId(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                // Left-most entry is the original client; the rest are proxies.
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * Drops keys whose window has fully elapsed.
     *
     * <p>Without this the map is an unbounded, attacker-controlled data
     * structure: one entry per source address, retained forever. That is a slow
     * memory leak in normal use and a denial of service under a spoofed-source
     * flood.
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 300_000)
    void evictStale() {
        long cutoff = System.currentTimeMillis() - Duration.ofSeconds(windowSeconds).toMillis();
        int before = hits.size();

        hits.entrySet().removeIf(entry -> {
            Deque<Long> window = entry.getValue();
            synchronized (window) {
                return window.isEmpty() || window.peekLast() < cutoff;
            }
        });

        if (before != hits.size()) {
            log.debug("Rate limit table: {} -> {} entries", before, hits.size());
        }
    }
}
