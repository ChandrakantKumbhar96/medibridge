package com.medibridge.patient;

import com.medibridge.common.security.CurrentUser;
import com.medibridge.common.security.SecurityUser;
import com.medibridge.patient.dto.ChangePasswordRequest;
import com.medibridge.patient.dto.PatientProfileResponse;
import com.medibridge.patient.dto.PatientProfileUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * A patient acting on their own record. No path variable carries a patient id -
 * it always comes from the token, so there is nothing to tamper with.
 */
@RestController
@RequestMapping("/patient")
@PreAuthorize("hasRole('PATIENT')")
@RequiredArgsConstructor
@Tag(name = "Patient", description = "A patient managing their own profile, password and stats")
public class PatientController {

    private final PatientService patientService;

    @GetMapping("/profile")
    @Operation(summary = "Get my profile")
    public PatientProfileResponse getProfile(@CurrentUser SecurityUser me) {
        return patientService.getProfile(me.idAsInt());
    }

    @PutMapping("/profile")
    @Operation(summary = "Update my profile")
    public PatientProfileResponse updateProfile(@CurrentUser SecurityUser me,
                                                @Valid @RequestBody PatientProfileUpdateRequest request) {
        return patientService.updateProfile(me.idAsInt(), request);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change my password")
    public ResponseEntity<Map<String, String>> changePassword(
            @CurrentUser SecurityUser me,
            @Valid @RequestBody ChangePasswordRequest request) {

        patientService.changePassword(me.idAsInt(), request);
        return ResponseEntity.ok(Map.of(
                "message", "Password updated. Please sign in again on your other devices."));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get my dashboard stats")
    public Map<String, Object> stats(@CurrentUser SecurityUser me) {
        return patientService.getStats(me.idAsInt());
    }
}
