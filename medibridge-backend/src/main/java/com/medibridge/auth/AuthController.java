package com.medibridge.auth;

import com.medibridge.auth.dto.*;
import com.medibridge.common.security.CurrentUser;
import com.medibridge.common.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Auth", description = "Login, registration and session management across all three identity tables")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Log in", description = "Body carries a role (patient/doctor/admin) that only selects which table to look up; authority comes from the row actually loaded.")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/google")
    @Operation(summary = "Log in with Google", description = "Body: { \"credential\": \"<Google ID token>\" }")
    public AuthResponse googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        return authService.loginWithGoogle(request);
    }

    @PostMapping("/otp/request")
    @Operation(summary = "Request a phone OTP", description = "Step one of phone login. Always answers the same way, registered number or not.")
    public PhoneOtpSentResponse requestOtp(@Valid @RequestBody PhoneOtpRequest request) {
        return authService.requestPhoneOtp(request);
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Verify a phone OTP", description = "Step two: signs in or registers depending on whether the phone number is known.")
    public AuthResponse verifyOtp(@Valid @RequestBody PhoneOtpVerifyRequest request) {
        return authService.loginWithPhoneOtp(request);
    }

    @GetMapping("/providers")
    @Operation(summary = "Get enabled login providers", description = "Lets the login page hide the Google button when the server has no client id.")
    public Map<String, Object> providers() {
        return Map.of("google", authService.isGoogleEnabled());
    }

    @PostMapping("/register/patient")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a patient account")
    public AuthResponse registerPatient(@Valid @RequestBody PatientRegisterRequest request) {
        return authService.registerPatient(request);
    }

    @PostMapping("/register/doctor")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a doctor account")
    public AuthResponse registerDoctor(@Valid @RequestBody DoctorRegisterRequest request) {
        return authService.registerDoctor(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh an access token")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out")
    public ResponseEntity<Map<String, String>> logout(@CurrentUser SecurityUser user) {
        if (user != null) {
            authService.logout(user.getUserType(), user.getId(), user.getFullName());
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the signed-in user", description = "Lets the frontend re-hydrate the user without a fresh login.")
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
