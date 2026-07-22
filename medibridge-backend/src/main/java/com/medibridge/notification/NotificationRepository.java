package com.medibridge.notification;

import com.medibridge.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByTypeAndEntityTypeAndEntityIdAndRecipientId(
            String type, String entityType, String entityId, String recipientId);
}
