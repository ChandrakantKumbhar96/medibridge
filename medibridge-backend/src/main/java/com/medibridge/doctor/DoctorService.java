package com.medibridge.doctor;

import com.medibridge.common.enums.AccountStatus;
import com.medibridge.common.enums.AppointmentStatus;
import com.medibridge.common.exception.BadRequestException;
import com.medibridge.common.exception.ResourceNotFoundException;
import com.medibridge.doctor.dto.*;
import com.medibridge.doctor.entity.Doctor;
import com.medibridge.doctor.entity.DoctorAvailability;
import com.medibridge.doctor.entity.DoctorSchedule;
import com.medibridge.doctor.entity.Specialization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DoctorService {

    /** Slot generation windows for the morning/afternoon flags on the schedule screen. */
    private static final LocalTime MORNING_START = LocalTime.of(9, 0);
    private static final LocalTime MORNING_END = LocalTime.of(12, 0);
    private static final LocalTime AFTERNOON_START = LocalTime.of(14, 0);
    private static final LocalTime AFTERNOON_END = LocalTime.of(17, 0);

    /** How far ahead slots are materialised when a doctor saves their schedule. */
    private static final int SLOT_HORIZON_DAYS = 30;

    private final DoctorRepository doctorRepository;
    private final SpecializationRepository specializationRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final com.medibridge.appointment.AppointmentRepository appointmentRepository;

    // ------------------------------------------------------------- listings

    /** Only ACTIVE doctors are ever exposed publicly - pending/suspended stay hidden. */
    @Transactional(readOnly = true)
    public List<DoctorResponse> listDoctors(String specialization, String search) {
        Integer specializationId = null;
        if (specialization != null && !specialization.isBlank()) {
            specializationId = specializationRepository.findByNameIgnoreCase(specialization)
                    .map(Specialization::getId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Unknown specialization: " + specialization));
        }

        return doctorRepository
                .search(AccountStatus.ACTIVE, specializationId,
                        (search == null || search.isBlank()) ? null : search)
                .stream()
                .map(DoctorMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DoctorResponse getDoctor(String doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        if (doctor.getStatus() != AccountStatus.ACTIVE) {
            throw new ResourceNotFoundException("Doctor not found");
        }
        return DoctorMapper.toDto(doctor);
    }

    @Transactional(readOnly = true)
    public List<SpecialtyResponse> listSpecialties() {
        return specializationRepository.findAll().stream()
                .map(s -> new SpecialtyResponse(
                        s.getId(), s.getName(), s.getEmoji(), s.getDescription(),
                        doctorRepository.search(AccountStatus.ACTIVE, s.getId(), null).size()))
                .toList();
    }

    /** Flat name list for the specialization dropdown on the doctor register form. */
    @Transactional(readOnly = true)
    public List<String> listSpecializationNames() {
        return specializationRepository.findAll().stream()
                .map(Specialization::getName)
                .toList();
    }

    // ---------------------------------------------------------------- slots

    /**
     * Free slots for a doctor on a date.
     *
     * <p>Slots are generated on demand from the weekly availability pattern if
     * they do not exist yet, so a patient can book any date within the horizon
     * without the doctor having pre-materialised it.
     */
    @Transactional
    public List<Map<String, Object>> getAvailableSlots(String doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        if (date == null) {
            throw new BadRequestException("date query parameter is required");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot book a slot in the past");
        }

        ensureSlotsGenerated(doctor, date);

        return scheduleRepository
                .findByDoctorIdAndAvailableDateOrderByStartTime(doctorId, date)
                .stream()
                .filter(s -> !Boolean.TRUE.equals(s.getIsBooked()))
                .map(s -> {
                    Map<String, Object> slot = new LinkedHashMap<>();
                    slot.put("schedule_id", s.getId());
                    slot.put("start_time", s.getStartTime().toString());
                    slot.put("end_time", s.getEndTime().toString());
                    slot.put("label", formatTime(s.getStartTime()));
                    return slot;
                })
                .toList();
    }

    /** Materialises slots for one date from the weekly pattern, if not already present. */
    private void ensureSlotsGenerated(Doctor doctor, LocalDate date) {
        Optional<DoctorAvailability> availability = availabilityRepository
                .findByDoctorIdAndDayOfWeek(doctor.getId(), date.getDayOfWeek());

        if (availability.isEmpty() || !Boolean.TRUE.equals(availability.get().getIsAvailable())) {
            return;   // doctor does not work this weekday
        }

        DoctorAvailability week = availability.get();
        int duration = doctor.getConsultationDurationMin() == null
                ? 30 : doctor.getConsultationDurationMin();

        List<LocalTime> starts = new ArrayList<>();
        if (Boolean.TRUE.equals(week.getMorning())) {
            starts.addAll(sliceWindow(MORNING_START, MORNING_END, duration));
        }
        if (Boolean.TRUE.equals(week.getAfternoon())) {
            starts.addAll(sliceWindow(AFTERNOON_START, AFTERNOON_END, duration));
        }

        for (LocalTime start : starts) {
            if (scheduleRepository.existsByDoctorIdAndAvailableDateAndStartTime(
                    doctor.getId(), date, start)) {
                continue;
            }
            scheduleRepository.save(DoctorSchedule.builder()
                    .doctor(doctor)
                    .availableDate(date)
                    .startTime(start)
                    .endTime(start.plusMinutes(duration))
                    .isBooked(false)
                    .build());
        }
    }

    private List<LocalTime> sliceWindow(LocalTime from, LocalTime to, int minutes) {
        List<LocalTime> slots = new ArrayList<>();
        LocalTime cursor = from;
        while (!cursor.plusMinutes(minutes).isAfter(to)) {
            slots.add(cursor);
            cursor = cursor.plusMinutes(minutes);
        }
        return slots;
    }

    /** '09:00' -> '09:00 AM', matching the labels the booking wizard renders. */
    private String formatTime(LocalTime time) {
        int hour = time.getHour();
        String suffix = hour < 12 ? "AM" : "PM";
        int display = hour % 12 == 0 ? 12 : hour % 12;
        return String.format("%02d:%02d %s", display, time.getMinute(), suffix);
    }

    // ------------------------------------------------------ doctor's own data

    @Transactional(readOnly = true)
    public DoctorResponse getOwnProfile(String doctorId) {
        return DoctorMapper.toDto(doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found")));
    }

    @Transactional
    public DoctorResponse updateOwnProfile(String doctorId, DoctorProfileUpdateRequest request) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        Specialization specialization = specializationRepository
                .findByNameIgnoreCase(request.specialization())
                .orElseThrow(() -> new BadRequestException(
                        "Unknown specialization: " + request.specialization()));

        doctor.setFullName(request.fullName());
        doctor.setPhone(request.phone());
        doctor.setSpecialization(specialization);
        doctor.setExperienceYears(request.experienceYears());
        doctor.setConsultationFee(request.consultationFee());
        doctor.setConsultationDurationMin(request.consultationDurationMin());
        doctor.setBio(request.bio());

        return DoctorMapper.toDto(doctorRepository.save(doctor));
    }

    /**
     * One row per distinct patient this doctor has seen, with their most recent
     * visit and next upcoming appointment. Built from the appointment history
     * rather than a patient query, so it can only ever contain patients this
     * doctor has a treating relationship with.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTreatedPatients(String doctorId) {
        Map<Integer, Map<String, Object>> byPatient = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        for (var appointment : appointmentRepository.findAllForDoctor(doctorId)) {
            var patient = appointment.getPatient();
            LocalDate when = appointment.getAppointmentDate().toLocalDate();

            Map<String, Object> row = byPatient.computeIfAbsent(patient.getId(), id -> {
                Map<String, Object> fresh = new LinkedHashMap<>();
                fresh.put("patient_id", patient.getId());
                fresh.put("name", patient.getFullName());
                fresh.put("age", com.medibridge.appointment.AppointmentMapper
                        .ageOf(patient.getDateOfBirth()));
                fresh.put("gender", patient.getGender().name());
                fresh.put("blood_group", patient.getBloodGroup());
                fresh.put("last_visit", null);
                fresh.put("next", null);
                fresh.put("condition", "N/A");
                return fresh;
            });

            // "Last visit" is decided by status, not by date: a COMPLETED
            // appointment is a visit that happened, and a future-dated row that
            // was never attended is not. findAllForDoctor returns newest first,
            // so the first COMPLETED row we see is the most recent one.
            if (appointment.getStatus() == AppointmentStatus.COMPLETED
                    && row.get("last_visit") == null) {
                row.put("last_visit", when.toString());
            }

            // Keep the *soonest* upcoming appointment, not the last one seen -
            // the list is ordered newest-first, so later iterations are earlier.
            if (when.isAfter(today) && appointment.getStatus().isActive()) {
                row.put("next", when.toString());
            }

            if (appointment.getReason() != null && "N/A".equals(row.get("condition"))) {
                row.put("condition", appointment.getReason());
            }
        }

        byPatient.values().forEach(row -> {
            if (row.get("last_visit") == null) row.put("last_visit", "New Patient");
            if (row.get("next") == null) row.put("next", "-");
        });

        return List.copyOf(byPatient.values());
    }

    @Transactional(readOnly = true)
    public List<ScheduleDayDto> getWeeklySchedule(String doctorId) {
        Map<DayOfWeek, DoctorAvailability> saved = new EnumMap<>(DayOfWeek.class);
        availabilityRepository.findByDoctorIdOrderByDayOfWeek(doctorId)
                .forEach(a -> saved.put(a.getDayOfWeek(), a));

        // Always return all seven days so the UI can render a complete week,
        // defaulting the ones the doctor has never configured.
        return Arrays.stream(DayOfWeek.values())
                .map(day -> {
                    DoctorAvailability a = saved.get(day);
                    return new ScheduleDayDto(
                            day.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                            a != null && Boolean.TRUE.equals(a.getIsAvailable()),
                            a != null && Boolean.TRUE.equals(a.getMorning()),
                            a != null && Boolean.TRUE.equals(a.getAfternoon()));
                })
                .toList();
    }

    @Transactional
    public List<ScheduleDayDto> updateWeeklySchedule(String doctorId, List<ScheduleDayDto> days) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        for (ScheduleDayDto day : days) {
            DayOfWeek dow;
            try {
                dow = DayOfWeek.valueOf(day.day().toUpperCase(Locale.ENGLISH));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid day: " + day.day());
            }

            DoctorAvailability availability = availabilityRepository
                    .findByDoctorIdAndDayOfWeek(doctorId, dow)
                    .orElseGet(() -> DoctorAvailability.builder()
                            .doctor(doctor).dayOfWeek(dow).build());

            availability.setIsAvailable(Boolean.TRUE.equals(day.available()));
            availability.setMorning(Boolean.TRUE.equals(day.morning()));
            availability.setAfternoon(Boolean.TRUE.equals(day.afternoon()));
            availabilityRepository.save(availability);
        }

        // Pre-generate the next month so patients see slots immediately.
        LocalDate today = LocalDate.now();
        for (int i = 0; i < SLOT_HORIZON_DAYS; i++) {
            ensureSlotsGenerated(doctor, today.plusDays(i));
        }

        return getWeeklySchedule(doctorId);
    }
}
