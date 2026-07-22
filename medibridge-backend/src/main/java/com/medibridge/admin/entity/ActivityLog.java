package com.medibridge.admin.entity;

import com.medibridge.common.enums.UserType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Audit trail and the source of the admin dashboard's recent-activity feed.
 *
 * <p>For a healthcare system, who *viewed* a record matters as much as who
 * changed it, so reads of patient data are logged too.
 */
@Entity
@Table(name = "activity_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false)
    private ActorType actorType;

    @Column(name = "actor_id", length = 36)
    private String actorId;

    @Column(name = "actor_name", length = 100)
    private String actorName;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(length = 255)
    private String description;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id", length = 36)
    private String entityId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Mirrors UserType but adds SYSTEM for scheduled/automatic actions. */
    public enum ActorType {
        PATIENT, DOCTOR, ADMIN, SYSTEM;

        public static ActorType from(UserType type) {
            return type == null ? SYSTEM : valueOf(type.name());
        }
    }
}
