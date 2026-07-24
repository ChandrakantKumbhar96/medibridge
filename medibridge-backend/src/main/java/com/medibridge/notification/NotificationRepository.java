package com.medibridge.notification;

import com.medibridge.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByTypeAndEntityTypeAndEntityIdAndRecipientIdAndChannel(
            String type, String entityType, String entityId, String recipientId,
            com.medibridge.notification.entity.Notification.Channel channel);
}
