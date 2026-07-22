package com.medibridge.payment.entity;

import com.medibridge.appointment.entity.Appointment;
import com.medibridge.common.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;

    /** Unique - doubles as an idempotency key against duplicate submissions. */
    @Column(name = "transaction_ref", nullable = false, unique = true, length = 64)
    private String transactionRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gateway gateway;

    /** Created server-side before checkout opens. */
    @Column(name = "gateway_order_id", length = 100, unique = true)
    private String gatewayOrderId;

    /** Returned by the gateway after the customer pays. */
    @Column(name = "gateway_payment_id", length = 100)
    private String gatewayPaymentId;

    /** Kept so a disputed payment can be re-verified later. */
    @Column(name = "gateway_signature", length = 255)
    private String gatewaySignature;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "transaction_status", nullable = false)
    private PaymentStatus transactionStatus;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "processed_at", insertable = false, updatable = false)
    private LocalDateTime processedAt;

    @PrePersist
    void applyDefaults() {
        if (transactionStatus == null) transactionStatus = PaymentStatus.PENDING;
        if (gateway == null) gateway = Gateway.SIMULATED;
    }

    public enum Gateway {
        SIMULATED, RAZORPAY
    }
}
