package com.medibridge.payout;

import com.medibridge.payout.entity.DoctorPayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DoctorPayoutRepository extends JpaRepository<DoctorPayout, Integer> {

    List<DoctorPayout> findByDoctorIdOrderByCreatedAtDesc(String doctorId);

    List<DoctorPayout> findAllByOrderByCreatedAtDesc();

    Optional<DoctorPayout> findByDoctorIdAndPeriodStartAndPeriodEnd(
            String doctorId, LocalDate periodStart, LocalDate periodEnd);

    List<DoctorPayout> findByStatusOrderByCreatedAtAsc(DoctorPayout.Status status);

    @Query("""
           SELECT COALESCE(SUM(p.netAmount), 0) FROM DoctorPayout p
           WHERE p.status = :status
           """)
    BigDecimal sumNetByStatus(@Param("status") DoctorPayout.Status status);
}
