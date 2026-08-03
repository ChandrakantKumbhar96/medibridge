package com.medibridge.admin;

import com.medibridge.admin.entity.ActivityLog;
import com.medibridge.common.security.CurrentUser;
import com.medibridge.common.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin console. Called by services/adminService.js.
 *
 * <p>Gated twice on purpose: the URL rule {@code /admin/** -> hasRole('ADMIN')}
 * in SecurityConfig, plus the class-level {@code @PreAuthorize}. Either alone
 * would do; both means a future edit to one cannot silently open the console.
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin console: dashboard, user management, analytics and settings")
public class AdminController {

    private final AdminService adminService;
    private final ActivityLogService activityLogService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get the admin dashboard")
    public Map<String, Object> dashboard() {
        return adminService.getDashboard();
    }

    @GetMapping("/patients")
    @Operation(summary = "List all patients")
    public List<Map<String, Object>> patients() {
        return adminService.listPatients();
    }

    @GetMapping("/doctors")
    @Operation(summary = "List all doctors")
    public List<Map<String, Object>> doctors() {
        return adminService.listDoctors();
    }

    @GetMapping("/appointments")
    @Operation(summary = "List all appointments")
    public List<Map<String, Object>> appointments() {
        return adminService.listAppointments();
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get platform analytics")
    public Map<String, Object> analytics() {
        return adminService.getAnalytics();
    }

    @GetMapping("/activity")
    @Operation(summary = "Get recent activity log")
    public List<Map<String, Object>> activity(@RequestParam(defaultValue = "20") int limit) {
        return adminService.getRecentActivity(limit);
    }

    @PatchMapping("/doctors/{doctorId}/status")
    @Operation(summary = "Set a doctor's account status", description = "Approve a pending doctor: { \"status\": \"active\" }")
    public Map<String, Object> setDoctorStatus(@CurrentUser SecurityUser me,
                                               @PathVariable String doctorId,
                                               @RequestBody Map<String, String> body) {
        Map<String, Object> result = adminService.setDoctorStatus(doctorId, body.get("status"));

        activityLogService.record(me, "DOCTOR_STATUS_CHANGED",
                "Doctor account " + result.get("status") + ": " + result.get("full_name"),
                "DOCTOR", doctorId);

        return result;
    }

    @PatchMapping("/patients/{patientId}/status")
    @Operation(summary = "Set a patient's account status")
    public Map<String, Object> setPatientStatus(@CurrentUser SecurityUser me,
                                                @PathVariable Integer patientId,
                                                @RequestBody Map<String, String> body) {
        Map<String, Object> result = adminService.setPatientStatus(patientId, body.get("status"));

        activityLogService.record(me, "PATIENT_STATUS_CHANGED",
                "Patient account " + result.get("status") + ": " + result.get("full_name"),
                "PATIENT", String.valueOf(patientId));

        return result;
    }

    @GetMapping("/settings")
    @Operation(summary = "Get system settings")
    public Map<String, Object> settings() {
        return adminService.getSettings();
    }

    @PutMapping("/settings")
    @Operation(summary = "Update system settings")
    public Map<String, Object> updateSettings(@CurrentUser SecurityUser me,
                                             @RequestBody Map<String, String> updates) {
        Map<String, Object> result = adminService.updateSettings(updates);

        activityLogService.record(me, "SETTINGS_UPDATED",
                "System settings updated: " + String.join(", ", updates.keySet()),
                "SETTINGS", null);

        return result;
    }
}
