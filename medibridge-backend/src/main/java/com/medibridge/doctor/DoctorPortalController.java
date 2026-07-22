package com.medibridge.doctor;

import com.medibridge.common.security.CurrentUser;
import com.medibridge.common.security.SecurityUser;
import com.medibridge.doctor.dto.DoctorProfileUpdateRequest;
import com.medibridge.doctor.dto.DoctorResponse;
import com.medibridge.doctor.dto.ScheduleDayDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * A doctor acting on their own record.
 *
 * <p>Every method derives the doctor id from {@link CurrentUser} rather than a
 * path variable, so there is no id for a caller to tamper with - one doctor
 * cannot edit another's profile or schedule.
 */
@RestController
@RequestMapping("/doctor")
@PreAuthorize("hasRole('DOCTOR')")
@RequiredArgsConstructor
public class DoctorPortalController {

    private final DoctorService doctorService;

    @GetMapping("/profile")
    public DoctorResponse getProfile(@CurrentUser SecurityUser me) {
        return doctorService.getOwnProfile(me.getId());
    }

    @PutMapping("/profile")
    public DoctorResponse updateProfile(@CurrentUser SecurityUser me,
                                        @Valid @RequestBody DoctorProfileUpdateRequest request) {
        return doctorService.updateOwnProfile(me.getId(), request);
    }

    @GetMapping("/schedule")
    public List<ScheduleDayDto> getSchedule(@CurrentUser SecurityUser me) {
        return doctorService.getWeeklySchedule(me.getId());
    }

    /**
     * Patients this doctor has actually treated - the doctor's Patient Records
     * screen. Deliberately not "all patients": a doctor has no business seeing
     * someone they have never had an appointment with.
     */
    @GetMapping("/patients")
    public List<Map<String, Object>> getPatients(@CurrentUser SecurityUser me) {
        return doctorService.getTreatedPatients(me.getId());
    }

    @PutMapping("/schedule")
    public List<ScheduleDayDto> updateSchedule(@CurrentUser SecurityUser me,
                                               @RequestBody List<ScheduleDayDto> days) {
        return doctorService.updateWeeklySchedule(me.getId(), days);
    }
}
