package com.medibridge.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A record of every message the platform sent.
 *
 * <p>Exists mainly so reminders are idempotent: the unique key on
 * (type, entity, recipient) means a reminder job that runs twice - after a
 * restart, or on two instances - cannot mail the same patient twice.
 */
@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false)
    private RecipientType recipientType;

    @Column(name = "recipient_id", nullable = false, length = 36)
    private String recipientId;

    @Column(name = "recipient_email", nullable = false, length = 100)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "error_message", length = 255)
    private String errorMessage;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id", length = 36)
    private String entityId;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void applyDefaults() {
        if (status == null) status = Status.PENDING;
        if (channel == null) channel = Channel.EMAIL;
    }

    public enum RecipientType { PATIENT, DOCTOR, ADMIN }

    public enum Channel { EMAIL, SMS, PUSH }

    public enum Status { PENDING, SENT, FAILED }

    /** Notification types - also the de-duplication key. */
    public static final class Type {
        public static final String BOOKING_PENDING = "BOOKING_PENDING";
        public static final String BOOKING_CONFIRMED = "BOOKING_CONFIRMED";
        public static final String REMINDER_24H = "REMINDER_24H";
        public static final String APPOINTMENT_CANCELLED = "APPOINTMENT_CANCELLED";
        public static final String APPOINTMENT_RESCHEDULED = "APPOINTMENT_RESCHEDULED";
        public static final String REFUND_ISSUED = "REFUND_ISSUED";
        public static final String HOLD_EXPIRED = "HOLD_EXPIRED";
        public static final String PRESCRIPTION_READY = "PRESCRIPTION_READY";
        public static final String DOCTOR_APPROVED = "DOCTOR_APPROVED";

        private Type() {
        }
    }
}
