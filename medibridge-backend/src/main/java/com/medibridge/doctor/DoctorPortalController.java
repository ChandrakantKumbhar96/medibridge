package com.medibridge.doctor;

import com.medibridge.common.security.CurrentUser;
import com.medibridge.common.security.SecurityUser;
import com.medibridge.doctor.dto.DoctorProfileUpdateRequest;
import com.medibridge.doctor.dto.DoctorResponse;
import com.medibridge.doctor.dto.ScheduleDayDto;
import com.medibridge.record.dto.RecordResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Doctor Portal", description = "A doctor managing their own profile, schedule and patients")
public class DoctorPortalController {

    private final DoctorService doctorService;
    private final com.medibridge.record.RecordService recordService;

    @GetMapping("/profile")
    @Operation(summary = "Get my profile")
    public DoctorResponse getProfile(@CurrentUser SecurityUser me) {
        return doctorService.getOwnProfile(me.getId());
    }

    @PutMapping("/profile")
    @Operation(summary = "Update my profile")
    public DoctorResponse updateProfile(@CurrentUser SecurityUser me,
                                        @Valid @RequestBody DoctorProfileUpdateRequest request) {
        return doctorService.updateOwnProfile(me.getId(), request);
    }

    @GetMapping("/schedule")
    @Operation(summary = "Get my weekly schedule")
    public List<ScheduleDayDto> getSchedule(@CurrentUser SecurityUser me) {
        return doctorService.getWeeklySchedule(me.getId());
    }

    @GetMapping("/patients")
    @Operation(summary = "List patients I have treated",
            description = "Deliberately not \"all patients\": a doctor has no business seeing "
                    + "someone they have never had an appointment with.")
    public List<Map<String, Object>> getPatients(@CurrentUser SecurityUser me) {
        return doctorService.getTreatedPatients(me.getId());
    }

    @GetMapping("/patients/{patientId}/records")
    @Operation(summary = "Get a treated patient's records",
            description = "Verifies a treating relationship first - holding ROLE_DOCTOR is not "
                    + "enough to read an arbitrary patient's records.")
    public List<RecordResponse> getPatientRecords(@CurrentUser SecurityUser me,
                                                  @PathVariable Integer patientId) {
        return recordService.listForDoctorsPatient(me.getId(), patientId);
    }

    @PutMapping("/schedule")
    @Operation(summary = "Update my weekly schedule")
    public List<ScheduleDayDto> updateSchedule(@CurrentUser SecurityUser me,
                                               @RequestBody List<ScheduleDayDto> days) {
        return doctorService.updateWeeklySchedule(me.getId(), days);
    }
}
