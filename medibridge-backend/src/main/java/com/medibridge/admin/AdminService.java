package com.medibridge.admin;

import com.medibridge.admin.entity.ActivityLog;
import com.medibridge.admin.entity.SystemSetting;
import com.medibridge.appointment.AppointmentMapper;
import com.medibridge.appointment.AppointmentRepository;
import com.medibridge.common.enums.AccountStatus;
import com.medibridge.common.enums.AppointmentStatus;
import com.medibridge.common.enums.PaymentStatus;
import com.medibridge.common.exception.BadRequestException;
import com.medibridge.common.exception.ResourceNotFoundException;
import com.medibridge.doctor.DoctorRepository;
import com.medibridge.doctor.entity.Doctor;
import com.medibridge.patient.PatientRepository;
import com.medibridge.patient.entity.Patient;
import com.medibridge.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final ActivityLogRepository activityLogRepository;
    private final SystemSettingRepository settingRepository;

    // ------------------------------------------------------------ dashboard

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard() {
        LocalDate today = LocalDate.now();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalPatients", patientRepository.count());
        stats.put("activeDoctors", doctorRepository.countByStatus(AccountStatus.ACTIVE));
        stats.put("totalAppointments", appointmentRepository.count());
        stats.put("activeToday", appointmentRepository.countOnDay(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay()));
        stats.put("completedToday", appointmentRepository.countByStatus(
                AppointmentStatus.COMPLETED));
        stats.put("revenueMTD", paymentRepository.sumAmountBetween(
                PaymentStatus.PAID, monthStart, LocalDateTime.now()));
        stats.put("pendingDoctorApprovals",
                doctorRepository.countByStatus(AccountStatus.PENDING));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stats", stats);
        result.put("activity", getRecentActivity(10));
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentActivity(int limit) {
        return activityLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                .stream()
                .map(a -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", a.getId());
                    row.put("name", a.getActorName() == null ? "System" : a.getActorName());
                    row.put("text", a.getDescription());
                    row.put("type", a.getActorType().name().toLowerCase());
                    row.put("action", a.getAction());
                    row.put("time", relativeTime(a.getCreatedAt()));
                    return row;
                })
                .toList();
    }

    /** "2 hours ago" - the format the admin activity feed already renders. */
    private String relativeTime(LocalDateTime when) {
        if (when == null) return "just now";
        Duration since = Duration.between(when, LocalDateTime.now());

        if (since.toMinutes() < 1) return "just now";
        if (since.toMinutes() < 60) return since.toMinutes() + " minutes ago";
        if (since.toHours() < 24) return since.toHours() + " hours ago";
        if (since.toDays() == 1) return "yesterday";
        return since.toDays() + " days ago";
    }

    // ------------------------------------------------------------- listings

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPatients() {
        return patientRepository.findAll().stream().map(p -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("patient_id", p.getId());
            row.put("full_name", p.getFullName());
            row.put("email", p.getEmail());
            row.put("phone", p.getPhone());
            row.put("join_date", p.getCreatedAt() == null ? null : p.getCreatedAt().format(DATE));
            row.put("appointments", appointmentRepository
                    .findByPatientIdAndStatusInOrderByAppointmentDateAsc(
                            p.getId(), Arrays.asList(AppointmentStatus.values())).size());
            row.put("status", p.getStatus().getDbValue());
            return row;
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listDoctors() {
        return doctorRepository.findAll().stream().map(d -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("doctor_id", d.getId());
            row.put("full_name", d.getFullName());
            row.put("email", d.getEmail());
            row.put("specialization", d.getSpecialization().getName());
            row.put("license_number", d.getLicenseNumber());
            row.put("patients", appointmentRepository.countDistinctPatientsForDoctor(d.getId()));
            row.put("rating", d.getRatingAvg());
            row.put("status", d.getStatus().getDbValue());
            return row;
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAppointments() {
        return appointmentRepository.findAll().stream().map(a -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("appointment_id", a.getId());
            row.put("patient", a.getPatient().getFullName());
            row.put("doctor", a.getDoctor().getFullName());
            row.put("date", a.getAppointmentDate().format(DATE));
            row.put("time", AppointmentMapper.toDto(a, false).time());
            row.put("type", a.getConsultType());
            row.put("status", a.getStatus().toFrontend());
            return row;
        }).toList();
    }

    // -------------------------------------------------------- account admin

    /**
     * Approving a pending doctor. This is the action behind the
     * "Doctor account approved" entry in the activity feed.
     */
    @Transactional
    public Map<String, Object> setDoctorStatus(String doctorId, String status) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        AccountStatus target = parseStatus(status,
                AccountStatus.ACTIVE, AccountStatus.INACTIVE,
                AccountStatus.SUSPENDED, AccountStatus.PENDING);

        // Taking a doctor offline hides them from search, but it does not undo
        // the appointments patients have already paid for. Flipping the flag
        // alone would leave those patients holding a booking with a doctor the
        // platform has just told to stop working - and nobody would be notified.
        // The queue has to be cleared first, deliberately, one refund at a time.
        if (target != AccountStatus.ACTIVE && target != AccountStatus.PENDING) {
            long committed = appointmentRepository
                    .countByDoctorIdAndStatusAndAppointmentDateAfter(
                            doctor.getId(), AppointmentStatus.ACCEPTED, LocalDateTime.now());

            if (committed > 0) {
                throw new BadRequestException(
                        "This doctor has " + committed + " confirmed upcoming appointment"
                                + (committed == 1 ? "" : "s")
                                + " that patients have already paid for. Cancel or reschedule "
                                + "them first - each cancellation refunds the patient in full.");
            }
        }

        doctor.setStatus(target);
        doctorRepository.save(doctor);

        return Map.of(
                "doctor_id", doctor.getId(),
                "full_name", doctor.getFullName(),
                "status", target.getDbValue());
    }

    @Transactional
    public Map<String, Object> setPatientStatus(Integer patientId, String status) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        AccountStatus target = parseStatus(status,
                AccountStatus.ACTIVE, AccountStatus.INACTIVE);

        patient.setStatus(target);
        patientRepository.save(patient);

        return Map.of(
                "patient_id", patient.getId(),
                "full_name", patient.getFullName(),
                "status", target.getDbValue());
    }

    private AccountStatus parseStatus(String raw, AccountStatus... allowed) {
        if (raw == null) {
            throw new BadRequestException("status is required");
        }
        for (AccountStatus candidate : allowed) {
            if (candidate.getDbValue().equalsIgnoreCase(raw.trim())) {
                return candidate;
            }
        }
        throw new BadRequestException("status must be one of: "
                + Arrays.stream(allowed).map(AccountStatus::getDbValue).toList());
    }

    // ------------------------------------------------------------ analytics

    @Transactional(readOnly = true)
    public Map<String, Object> getAnalytics() {
        LocalDate today = LocalDate.now();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();

        long total = appointmentRepository.count();
        long completed = appointmentRepository.countByStatus(AppointmentStatus.COMPLETED);
        long cancelled = appointmentRepository.countByStatus(AppointmentStatus.CANCELLED);

        Map<String, Object> monthly = new LinkedHashMap<>();
        monthly.put("newPatients", patientRepository.count());
        monthly.put("newDoctors", doctorRepository.count());
        monthly.put("totalAppointments", total);
        monthly.put("cancelledAppointments", cancelled);
        monthly.put("completionRate", total == 0
                ? "0%"
                : String.format("%.1f%%", (completed * 100.0) / total));

        BigDecimal paid = paymentRepository.sumAmountByStatus(PaymentStatus.PAID);
        BigDecimal refunded = paymentRepository.sumAmountByStatus(PaymentStatus.REFUNDED);

        Map<String, Object> revenue = new LinkedHashMap<>();
        revenue.put("total", paid);
        revenue.put("refunded", refunded);
        revenue.put("net", paid.subtract(refunded));
        revenue.put("monthToDate", paymentRepository.sumAmountBetween(
                PaymentStatus.PAID, monthStart, LocalDateTime.now()));

        Map<String, Object> daily = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            daily.put(day.format(DATE), paymentRepository.sumAmountBetween(
                    PaymentStatus.PAID, day.atStartOfDay(), day.plusDays(1).atStartOfDay()));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("monthly", monthly);
        result.put("revenue", revenue);
        result.put("dailyRevenue", daily);
        return result;
    }

    // ------------------------------------------------------------- settings

    @Transactional(readOnly = true)
    public Map<String, Object> getSettings() {
        Map<String, Object> settings = new LinkedHashMap<>();
        settingRepository.findAll().forEach(s -> settings.put(s.getKey(), typed(s)));
        return settings;
    }

    @Transactional
    public Map<String, Object> updateSettings(Map<String, String> updates) {
        updates.forEach((key, value) -> {
            SystemSetting setting = settingRepository.findById(key)
                    .orElseThrow(() -> new BadRequestException("Unknown setting: " + key));
            validate(setting, value);
            setting.setValue(value);
            settingRepository.save(setting);
        });
        return getSettings();
    }

    private void validate(SystemSetting setting, String value) {
        switch (setting.getValueType()) {
            case INT -> {
                try {
                    Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new BadRequestException(setting.getKey() + " must be a number");
                }
            }
            case BOOLEAN -> {
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    throw new BadRequestException(setting.getKey() + " must be true or false");
                }
            }
            case STRING -> {
                if (value.isBlank()) {
                    throw new BadRequestException(setting.getKey() + " cannot be empty");
                }
            }
        }
    }

    /** Returns the setting as its declared type so the JSON is not all strings. */
    private Object typed(SystemSetting s) {
        return switch (s.getValueType()) {
            case INT -> Integer.parseInt(s.getValue());
            case BOOLEAN -> Boolean.parseBoolean(s.getValue());
            case STRING -> s.getValue();
        };
    }
}
