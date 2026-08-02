package com.medibridge.auth;

import com.medibridge.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
// Spring Boot 4 moved MockMvc autoconfiguration into spring-boot-webmvc-test -
// see the note in AppointmentApiSecurityTest.
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Brute-force protection on the credential endpoints.
 *
 * <p>The limit is lowered to 3 for these tests. Asserting against the production
 * value would mean firing ten requests to prove one thing, and would silently
 * stop testing anything the day someone raises it.
 *
 * <p>Every test uses its own source address. The limiter keys on the caller, and
 * a shared key would make each test depend on which ran first.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "medibridge.ratelimit.auth.max-attempts=3",
        "medibridge.ratelimit.auth.window-seconds=60"
})
class AuthRateLimitTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;

    private static final String BAD_LOGIN = """
            {"email":"nobody@test.local","password":"wrong","role":"patient"}""";

    @Test
    @DisplayName("repeated failed logins are cut off at the limit")
    void bruteForceIsThrottled() throws Exception {
        String ip = "203.0.113.10";

        // Inside the allowance: rejected on credentials, which is the endpoint
        // doing its job - not on rate.
        for (int attempt = 1; attempt <= 3; attempt++) {
            mockMvc.perform(login(ip))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(login(ip))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429));
    }

    /**
     * The limit must not become a way to lock everyone else out. Keying on the
     * caller is what keeps one attacker from denying the whole service.
     */
    @Test
    @DisplayName("one attacker does not throttle everybody else")
    void limitIsPerCaller() throws Exception {
        String attacker = "203.0.113.20";
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(login(attacker));
        }
        mockMvc.perform(login(attacker)).andExpect(status().isTooManyRequests());

        mockMvc.perform(login("203.0.113.21"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Registration mints accounts, so it is guarded too - otherwise the login
     * limit just moves the abuse one endpoint sideways.
     */
    @Test
    @DisplayName("registration is guarded as well as login")
    void registrationIsThrottled() throws Exception {
        String ip = "203.0.113.30";
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/auth/register/patient")
                    .with(r -> { r.setRemoteAddr(ip); return r; })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"full_name\":\"X\",\"email\":\"x@test.local\",\"password\":\"Test@1234\"}"));
        }

        mockMvc.perform(post("/auth/register/patient")
                        .with(r -> { r.setRemoteAddr(ip); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"full_name\":\"X\",\"email\":\"y@test.local\",\"password\":\"Test@1234\"}"))
                .andExpect(status().isTooManyRequests());
    }

    /**
     * The filter must not sit in front of the whole application. A patient
     * refreshing a dashboard fires several requests at once and would trip a
     * limit sized for password guessing.
     */
    @Test
    @DisplayName("ordinary endpoints are not rate limited")
    void onlyCredentialEndpointsAreGuarded() throws Exception {
        String ip = "203.0.113.40";
        for (int i = 0; i < 8; i++) {
            mockMvc.perform(get("/specialties").with(r -> { r.setRemoteAddr(ip); return r; }))
                    .andExpect(status().isOk());
        }
    }

    private org.springframework.test.web.servlet.RequestBuilder login(String ip) {
        return post("/auth/login")
                .with(request -> { request.setRemoteAddr(ip); return request; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(BAD_LOGIN);
    }
}
