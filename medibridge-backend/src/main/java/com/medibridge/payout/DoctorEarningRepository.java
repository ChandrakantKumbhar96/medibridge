package com.medibridge.payout;

import com.medibridge.payout.entity.DoctorEarning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DoctorEarningRepository extends JpaRepository<DoctorEarning, Integer> {

    Optional<DoctorEarning> findByAppointmentId(Integer appointmentId);

    boolean existsByAppointmentId(Integer appointmentId);

    List<DoctorEarning> findByDoctorIdOrderByEarnedAtDesc(String doctorId);

    List<DoctorEarning> findByPayoutIdOrderByEarnedAtAsc(Integer payoutId);

    /**
     * Unsettled earnings for one doctor up to a cut-off - the input to a payout
     * run. REVERSED rows are excluded because a refunded consultation is no
     * longer owed.
     */
    @Query("""
           SELECT e FROM DoctorEarning e
           WHERE e.doctor.id = :doctorId
             AND e.status = com.medibridge.payout.entity.DoctorEarning$Status.PENDING
             AND e.earnedAt <= :until
           ORDER BY e.earnedAt ASC
           """)
    List<DoctorEarning> findSettleable(@Param("doctorId") String doctorId,
                                       @Param("until") LocalDateTime until);

    /** Doctors with anything to settle - avoids creating empty payout batches. */
    @Query("""
           SELECT DISTINCT e.doctor.id FROM DoctorEarning e
           WHERE e.status = com.medibridge.payout.entity.DoctorEarning$Status.PENDING
             AND e.earnedAt <= :until
           """)
    List<String> findDoctorIdsWithPendingEarnings(@Param("until") LocalDateTime until);

    @Query("""
           SELECT COALESCE(SUM(e.netAmount), 0) FROM DoctorEarning e
           WHERE e.doctor.id = :doctorId AND e.status = :status
           """)
    BigDecimal sumNetByDoctorAndStatus(@Param("doctorId") String doctorId,
                                       @Param("status") DoctorEarning.Status status);

    @Query("""
           SELECT COALESCE(SUM(e.commissionAmount), 0) FROM DoctorEarning e
           WHERE e.status <> com.medibridge.payout.entity.DoctorEarning$Status.REVERSED
           """)
    BigDecimal totalPlatformCommission();

    long countByDoctorIdAndStatus(String doctorId, DoctorEarning.Status status);
}
