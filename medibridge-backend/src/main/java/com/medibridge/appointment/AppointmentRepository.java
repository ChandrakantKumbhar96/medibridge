package com.medibridge.appointment;

import com.medibridge.appointment.entity.Appointment;
import com.medibridge.common.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    List<Appointment> findByPatientIdAndStatusInOrderByAppointmentDateAsc(
            Integer patientId, Collection<AppointmentStatus> statuses);

    List<Appointment> findByPatientIdAndStatusInOrderByAppointmentDateDesc(
            Integer patientId, Collection<AppointmentStatus> statuses);

    List<Appointment> findByDoctorIdAndStatusInOrderByAppointmentDateAsc(
            String doctorId, Collection<AppointmentStatus> statuses);

    @Query("""
           SELECT a FROM Appointment a
           WHERE a.doctor.id = :doctorId
             AND a.appointmentDate >= :dayStart
             AND a.appointmentDate < :dayEnd
           ORDER BY a.appointmentDate ASC
           """)
    List<Appointment> findDoctorDay(@Param("doctorId") String doctorId,
                                    @Param("dayStart") LocalDateTime dayStart,
                                    @Param("dayEnd") LocalDateTime dayEnd);

    boolean existsByScheduleId(Integer scheduleId);

    /** Backs the hold-expiry job. */
    List<Appointment> findByStatusAndHoldExpiresAtBefore(
            AppointmentStatus status, LocalDateTime cutoff);

    /** Backs the reminder job. */
    List<Appointment> findByStatusAndAppointmentDateBetween(
            AppointmentStatus status, LocalDateTime from, LocalDateTime to);

    /**
     * Backs the no-show sweep.
     *
     * <p>Restricted to appointments that had a video room: attendance can only
     * be judged where joining was possible. Rows where both parties joined are
     * excluded here rather than in Java, so a doctor who consulted but has not
     * yet written their notes is never pulled into the sweep - that appointment
     * belongs in "Awaiting consultation notes", not in a no-show.
     */
    @Query("""
           SELECT a.id FROM Appointment a
           WHERE a.status = :status
             AND a.meetingLink IS NOT NULL
             AND a.meetingValidUntil IS NOT NULL
             AND a.meetingValidUntil < :cutoff
             AND (a.patientJoinedAt IS NULL OR a.doctorJoinedAt IS NULL)
           ORDER BY a.appointmentDate ASC
           """)
    List<Integer> findNoShowCandidateIds(@Param("status") AppointmentStatus status,
                                         @Param("cutoff") LocalDateTime cutoff);

    Optional<Appointment> findByIdAndPatientId(Integer id, Integer patientId);

    Optional<Appointment> findByIdAndDoctorId(Integer id, String doctorId);

    long countByStatus(AppointmentStatus status);

    /**
     * Paid appointments a doctor still owes. Taking their account offline while
     * this is non-zero would strand patients who have already been charged.
     */
    long countByDoctorIdAndStatusAndAppointmentDateAfter(
            String doctorId, AppointmentStatus status, LocalDateTime after);

    long countByDoctorId(String doctorId);

    @Query("SELECT COUNT(DISTINCT a.patient.id) FROM Appointment a WHERE a.doctor.id = :doctorId")
    long countDistinctPatientsForDoctor(@Param("doctorId") String doctorId);

    @Query("""
           SELECT COUNT(a) FROM Appointment a
           WHERE a.appointmentDate >= :dayStart AND a.appointmentDate < :dayEnd
           """)
    long countOnDay(@Param("dayStart") LocalDateTime dayStart,
                    @Param("dayEnd") LocalDateTime dayEnd);

    /** Patients a doctor has seen - backs the doctor's Patient Records screen. */
    @Query("""
           SELECT a FROM Appointment a
           WHERE a.doctor.id = :doctorId
           ORDER BY a.appointmentDate DESC
           """)
    List<Appointment> findAllForDoctor(@Param("doctorId") String doctorId);
}
