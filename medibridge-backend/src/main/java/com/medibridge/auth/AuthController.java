package com.medibridge.auth;

import com.medibridge.auth.dto.*;
import com.medibridge.common.security.CurrentUser;
import com.medibridge.common.security.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoints called by src/services/authService.js. Context path is /api, so
 * these resolve to /api/auth/*.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Body: {@code { "credential": "<Google ID token>" }} */
    @PostMapping("/google")
    public AuthResponse googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        return authService.loginWithGoogle(request);
    }

    /**
     * Step one of phone login. Body: {@code { "phone": "+91 90000 11111" }}
     *
     * <p>Always answers the same way, registered number or not - see
     * {@link AuthService#requestPhoneOtp}.
     */
    @PostMapping("/otp/request")
    public PhoneOtpSentResponse requestOtp(@Valid @RequestBody PhoneOtpRequest request) {
        return authService.requestPhoneOtp(request);
    }

    /** Step two: {@code { "phone": "...", "code": "123456" }} - signs in or registers. */
    @PostMapping("/otp/verify")
    public AuthResponse verifyOtp(@Valid @RequestBody PhoneOtpVerifyRequest request) {
        return authService.loginWithPhoneOtp(request);
    }

    /** Lets the login page hide the Google button when the server has no client id. */
    @GetMapping("/providers")
    public Map<String, Object> providers() {
        return Map.of("google", authService.isGoogleEnabled());
    }

    @PostMapping("/register/patient")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerPatient(@Valid @RequestBody PatientRegisterRequest request) {
        return authService.registerPatient(request);
    }

    @PostMapping("/register/doctor")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerDoctor(@Valid @RequestBody DoctorRegisterRequest request) {
        return authService.registerDoctor(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@CurrentUser SecurityUser user) {
        if (user != null) {
            authService.logout(user.getUserType(), user.getId(), user.getFullName());
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    /** Lets the frontend re-hydrate the user without a fresh login. */
    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me(@CurrentUser SecurityUser user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // LinkedHashMap rather than Map.of, which throws on a null value: a
        // phone-first account has no email until it completes its profile, and
        // Map.of turned that into a 500 on the endpoint the frontend calls to
        // find out who it is talking to.
        Map<String, String> me = new LinkedHashMap<>();
        me.put("id", user.getId());
        me.put("name", user.getFullName());
        me.put("email", user.getEmail());
        me.put("role", user.getUserType().toFrontend());
        return ResponseEntity.ok(me);
    }
}
