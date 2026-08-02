package com.medibridge.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Authenticates trusted internal callers (currently the .NET notify service)
 * on the {@code X-Internal-Api-Key} header against
 * {@code medibridge.internal.api-key}.
 *
 * <p>This is a machine identity, not a user - the principal is a fixed
 * string, never a {@link SecurityUser}, so {@code @CurrentUser} resolves to
 * null for it and no ownership check can mistake it for a patient or doctor.
 * It only ever reaches the dedicated {@code /internal/**} endpoints, gated
 * separately in SecurityConfig.
 *
 * <p>A missing, blank-configured, or wrong key is ignored, exactly like
 * JwtAuthFilter does for a bad bearer token - the request falls through as
 * anonymous and the URL/method rules decide whether that's enough.
 */
@Component
public class InternalApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Internal-Api-Key";
    static final String PRINCIPAL = "internal-service";

    @Value("${medibridge.internal.api-key:}")
    private String configuredKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String presented = request.getHeader(HEADER);

        if (presented != null && !configuredKey.isBlank()
                && SecurityContextHolder.getContext().getAuthentication() == null
                && constantTimeEquals(presented, configuredKey)) {

            var authentication = new UsernamePasswordAuthenticationToken(
                    PRINCIPAL, null, List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /** MessageDigest.isEqual is constant-time - a key comparison must not leak via timing. */
    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
