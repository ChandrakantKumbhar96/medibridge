package com.medibridge.doctor;

import com.medibridge.doctor.entity.DoctorSchedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Integer> {

    List<DoctorSchedule> findByDoctorIdAndAvailableDateOrderByStartTime(
            String doctorId, LocalDate date);

    boolean existsByDoctorIdAndAvailableDateAndStartTime(
            String doctorId, LocalDate date, LocalTime startTime);

    /**
     * Pessimistic lock taken when confirming a booking. The UNIQUE constraint on
     * appointment.schedule_id is the real guarantee, but locking here turns a
     * constraint violation into a clean "slot taken" message instead of a
     * 500 from the database driver.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM DoctorSchedule s WHERE s.id = :id")
    Optional<DoctorSchedule> findByIdForUpdate(@Param("id") Integer id);

    long countByDoctorIdAndAvailableDateGreaterThanEqual(String doctorId, LocalDate from);

    /**
     * Clears future slots nobody has booked, so they can be regenerated at a new
     * consultation length. Booked slots are excluded - a patient has already
     * been promised that time.
     */
    @Modifying
    @Query("DELETE FROM DoctorSchedule s WHERE s.doctor.id = :doctorId "
            + "AND s.availableDate >= :from AND s.isBooked = false")
    int deleteUnbookedFrom(@Param("doctorId") String doctorId, @Param("from") LocalDate from);

    /**
     * The soonest open slot within a window, optionally narrowed to one
     * specialization. Backs "next available" - a patient who does not want to
     * browse doctors and pick a time themselves.
     *
     * <p>{@code Pageable} rather than a literal LIMIT: JPQL has no LIMIT clause,
     * and capping to one row through paging keeps this portable.
     */
    @Query("""
           SELECT s FROM DoctorSchedule s
           WHERE s.isBooked = false
             AND s.doctor.status = com.medibridge.common.enums.AccountStatus.ACTIVE
             AND (:specializationId IS NULL OR s.doctor.specialization.id = :specializationId)
             AND FUNCTION('TIMESTAMP', s.availableDate, s.startTime) BETWEEN :from AND :until
           ORDER BY s.availableDate ASC, s.startTime ASC
           """)
    List<DoctorSchedule> findEarliestOpenSlots(@Param("specializationId") Integer specializationId,
                                                @Param("from") LocalDateTime from,
                                                @Param("until") LocalDateTime until,
                                                Pageable page);
}
