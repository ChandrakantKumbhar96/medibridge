package com.medibridge.appointment;

import com.medibridge.appointment.dto.AppointmentResponse;
import com.medibridge.appointment.entity.Appointment;
import com.medibridge.patient.entity.Patient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;

public final class AppointmentMapper {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Locale.ENGLISH pinned, then upper-cased: Java 21's CLDR data renders the
     * am/pm marker lowercase, but the frontend's mock data and layout expect
     * "02:30 PM".
     */
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.ENGLISH);

    private AppointmentMapper() {
    }

    /**
     * @param includeMeetingLink only the patient and the treating doctor may see
     *                           the join URL - it is withheld from admin listings.
     */
    public static AppointmentResponse toDto(Appointment a, boolean includeMeetingLink) {
        LocalDateTime when = a.getAppointmentDate();
        Patient p = a.getPatient();

        return new AppointmentResponse(
                a.getId(),
                a.getDoctor().getFullName(),
                a.getDoctor().getId(),
                a.getDoctor().getSpecialization().getName(),
                p.getFullName(),
                p.getId(),
                ageOf(p.getDateOfBirth()),
                when.format(DATE),
                when.format(TIME).toUpperCase(java.util.Locale.ENGLISH),
                a.getConsultType(),
                a.getStatus().toFrontend(),
                a.getReason(),
                a.getDoctor().getConsultationFee(),
                includeMeetingLink ? a.getMeetingLink() : null);
    }

    public static Integer ageOf(LocalDate dateOfBirth) {
        return dateOfBirth == null ? null : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    public static String describe(Appointment a) {
        return a.getAppointmentDate().format(DATE) + " at "
                + a.getAppointmentDate().format(TIME).toUpperCase(java.util.Locale.ENGLISH);
    }
}
