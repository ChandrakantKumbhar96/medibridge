package com.medibridge.payment;

import com.medibridge.common.enums.PaymentStatus;
import com.medibridge.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentTransaction, Integer> {

    Optional<PaymentTransaction> findByAppointmentId(Integer appointmentId);

    Optional<PaymentTransaction> findByTransactionRef(String transactionRef);

    Optional<PaymentTransaction> findByGatewayOrderId(String gatewayOrderId);

    List<PaymentTransaction> findByAppointmentPatientIdOrderByProcessedAtDesc(Integer patientId);

    @Query("""
           SELECT COALESCE(SUM(p.amount), 0) FROM PaymentTransaction p
           WHERE p.transactionStatus = :status
             AND p.processedAt >= :from AND p.processedAt < :to
           """)
    BigDecimal sumAmountBetween(@Param("status") PaymentStatus status,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentTransaction p "
            + "WHERE p.transactionStatus = :status")
    BigDecimal sumAmountByStatus(@Param("status") PaymentStatus status);
}
