package com.medibridge.auth;

import com.medibridge.auth.dto.*;
import com.medibridge.common.security.CurrentUser;
import com.medibridge.common.security.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "name", user.getFullName(),
                "email", user.getEmail(),
                "role", user.getUserType().toFrontend()));
    }
}
