package com.medibridge.doctor;

import com.medibridge.common.security.CurrentUser;
import com.medibridge.common.security.SecurityUser;
import com.medibridge.doctor.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Public doctor discovery. Called by services/doctorService.js.
 *
 * <p>Note the split: browsing lives here under /doctors and /specialties, while
 * a doctor managing their own data lives in {@link DoctorPortalController}
 * under /doctor/** - which the security config gates to ROLE_DOCTOR.
 */
@RestController
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping("/doctors")
    public List<DoctorResponse> listDoctors(
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) String search) {
        return doctorService.listDoctors(specialization, search);
    }

    @GetMapping("/doctors/{doctorId}")
    public DoctorResponse getDoctor(@PathVariable String doctorId) {
        return doctorService.getDoctor(doctorId);
    }

    @GetMapping("/doctors/{doctorId}/slots")
    public List<Map<String, Object>> getSlots(
            @PathVariable String doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return doctorService.getAvailableSlots(doctorId, date);
    }

    /** Cards on the booking wizard's first step: name, emoji, doctor count. */
    @GetMapping("/specialties")
    public List<SpecialtyResponse> listSpecialties() {
        return doctorService.listSpecialties();
    }

    /** Flat names for the register form dropdown. */
    @GetMapping("/specializations")
    public List<String> listSpecializations() {
        return doctorService.listSpecializationNames();
    }
}
