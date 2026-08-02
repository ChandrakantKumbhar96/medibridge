package com.medibridge.common.config;

import com.medibridge.common.security.AuthRateLimitFilter;
import com.medibridge.common.security.InternalApiKeyAuthFilter;
import com.medibridge.common.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Stateless JWT security.
 *
 * <p>Paths here are relative to the {@code /api} context path, so
 * {@code "/auth/**"} matches {@code /api/auth/**}.
 *
 * <p>URL rules below are coarse role gates only. They cannot express "this
 * patient may read only their own records" - that ownership check lives in the
 * service layer, because only it can see who owns the row.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity      // turns on @PreAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthRateLimitFilter authRateLimitFilter;
    private final InternalApiKeyAuthFilter internalApiKeyAuthFilter;

    @Value("${medibridge.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // No cookies are used for auth, so there is no browser-attached
            // credential for an attacker's site to ride on. CSRF protection is
            // therefore unnecessary here - not merely inconvenient.
            .csrf(csrf -> csrf.disable())

            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Both handlers are set explicitly rather than left to defaults, so
            // the distinction is guaranteed: 401 = "who are you", 403 = "I know
            // who you are, you may not do this".
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                    .accessDeniedHandler((request, response, denied) -> {
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                        response.setContentType("application/json");
                        response.getWriter().write(
                                "{\"message\":\"You do not have permission to access this resource\","
                                        + "\"status\":403}");
                    }))

            .authorizeHttpRequests(auth -> auth
                    // --- public ---
                    .requestMatchers("/auth/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/specialties", "/specializations", "/policies").permitAll()
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                    // API docs. Fine to expose locally; behind a real deployment
                    // these should be disabled or restricted, since they
                    // enumerate every endpoint for an attacker too.
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                     "/v3/api-docs/**", "/api-docs/**").permitAll()

                    // --- machine-to-machine ---
                    // Only InternalApiKeyAuthFilter can ever grant this authority, and
                    // only on the shared secret - no user JWT, however privileged,
                    // carries ROLE_INTERNAL_SERVICE.
                    .requestMatchers("/internal/**").hasRole("INTERNAL_SERVICE")

                    // --- role gates ---
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    .requestMatchers("/doctor/**").hasRole("DOCTOR")
                    .requestMatchers("/patient/**").hasRole("PATIENT")

                    // /actuator/env and /actuator/heapdump would leak the DB
                    // password and JWT secret, so everything else is admin-only
                    .requestMatchers("/actuator/**").hasRole("ADMIN")

                    .anyRequest().authenticated())

            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(internalApiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // Ahead of the JWT filter, and therefore ahead of the controllers:
            // /auth/** is permitAll, so nothing else in this chain would stop a
            // caller hammering the login endpoint. Throttling has to happen
            // before any password is checked, not after.
            .addFilterBefore(authRateLimitFilter, JwtAuthFilter.class);

        return http.build();
    }

    /**
     * Strength 12 (~250ms per hash). Deliberately slow: it is what makes an
     * offline brute-force of a leaked hash impractical.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Explicit origin list, never "*" - the React dev server on 5173.
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept",
                "X-Requested-With"));
        config.setExposedHeaders(List.of("Content-Disposition"));  // PDF downloads
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
