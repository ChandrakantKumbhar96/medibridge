package com.medibridge.patient;

import com.medibridge.appointment.AppointmentRepository;
import com.medibridge.auth.RefreshTokenRepository;
import com.medibridge.common.enums.UserType;
import com.medibridge.common.exception.BadRequestException;
import com.medibridge.common.exception.ConflictException;
import com.medibridge.common.exception.ResourceNotFoundException;
import com.medibridge.common.util.PhoneNumbers;
import com.medibridge.patient.dto.ChangePasswordRequest;
import com.medibridge.patient.dto.PatientProfileResponse;
import com.medibridge.patient.dto.PatientProfileUpdateRequest;
import com.medibridge.patient.entity.Patient;
import com.medibridge.record.MedicalReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PatientService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalReportRepository reportRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PatientProfileResponse getProfile(Integer patientId) {
        return toDto(load(patientId));
    }

    @Transactional
    public PatientProfileResponse updateProfile(Integer patientId,
                                                PatientProfileUpdateRequest request) {
        Patient patient = load(patientId);

        // Set-once. A phone-first account has no email until here; an account
        // that already has one keeps it, whatever the body says.
        if (patient.getEmail() == null && request.email() != null
                && !request.email().isBlank()) {
            String email = request.email().trim().toLowerCase();
            if (patientRepository.existsByEmailIgnoreCase(email)) {
                throw new ConflictException("An account with this email already exists");
            }
            patient.setEmail(email);
        }

        patient.setFullName(request.fullName());
        patient.setPhone(request.phone());
        // The E.164 form is what phone login matches on, so it has to move with
        // `phone`. Left behind, the patient would sign in by code to whatever
        // their OLD number resolved to - or auto-register a second account on
        // the new one.
        patient.setPhoneE164(claimPhone(patient, request.phone()));
        patient.setAnotherNumber(request.anotherNumber());
        patient.setAddress(request.address());
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setGender(Patient.Gender.valueOf(request.gender()));
        patient.setBloodGroup(request.bloodGroup());

        return toDto(patientRepository.save(patient));
    }

    /**
     * Changing the password revokes every refresh token, so a session opened
     * with the old password cannot outlive it. This is the main reason a
     * password change is worth anything.
     */
    @Transactional
    public void changePassword(Integer patientId, ChangePasswordRequest request) {
        Patient patient = load(patientId);

        if (!passwordEncoder.matches(request.currentPassword(), patient.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), patient.getPasswordHash())) {
            throw new BadRequestException("New password must differ from the current one");
        }

        patient.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        patientRepository.save(patient);

        refreshTokenRepository.revokeAllForUser(UserType.PATIENT, String.valueOf(patientId));
    }

    /** Backs the stat tiles on the patient overview screen. */
    @Transactional(readOnly = true)
    public Map<String, Object> getStats(Integer patientId) {
        var all = appointmentRepository.findByPatientIdAndStatusInOrderByAppointmentDateAsc(
                patientId, java.util.Arrays.asList(
                        com.medibridge.common.enums.AppointmentStatus.values()));

        long upcoming = all.stream().filter(a -> a.getStatus().isActive()).count();
        long completed = all.stream()
                .filter(a -> a.getStatus()
                        == com.medibridge.common.enums.AppointmentStatus.COMPLETED)
                .count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("upcomingAppointments", upcoming);
        stats.put("medicalRecords", reportRepository.countByPatientId(patientId));
        stats.put("completedConsultations", completed);
        return stats;
    }

    /**
     * The normalised number this patient may hold, or null when it is not a
     * number we can text.
     *
     * <p>Checked rather than left to the UNIQUE index: the index would reject
     * the save with a constraint violation and a 500, and "that number belongs
     * to another account" is something the patient can act on.
     */
    private String claimPhone(Patient patient, String phone) {
        String e164 = PhoneNumbers.toE164(phone);
        if (e164 != null && !e164.equals(patient.getPhoneE164())
                && patientRepository.existsByPhoneE164(e164)) {
            throw new ConflictException("This mobile number is already registered");
        }
        return e164;
    }

    private Patient load(Integer patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
    }

    private PatientProfileResponse toDto(Patient p) {
        return new PatientProfileResponse(
                p.getId(),
                p.getFullName(),
                p.getEmail(),
                p.getPhone(),
                p.getAnotherNumber(),
                p.getAddress(),
                p.getDateOfBirth() == null ? null : p.getDateOfBirth().format(DATE),
                // Null on any account that has not completed its profile -
                // Google since V3, phone since V14. This threw an NPE on the
                // one screen those users are sent to in order to fix it.
                p.getGender() == null ? null : p.getGender().name(),
                p.getBloodGroup(),
                p.getReasonOfConsult(),
                p.getStatus().getDbValue());
    }
}
