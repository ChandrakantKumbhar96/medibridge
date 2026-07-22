package com.medibridge.admin;

import com.medibridge.admin.entity.ActivityLog;
import com.medibridge.common.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogService.class);

    private final ActivityLogRepository repository;

    /**
     * REQUIRES_NEW so an audit write never rolls back - or is rolled back by -
     * the business transaction it is recording. An audit trail that disappears
     * when the recorded action fails is worse than none.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(ActivityLog.ActorType actorType, String actorId, String actorName,
                       String action, String description,
                       String entityType, String entityId) {
        try {
            repository.save(ActivityLog.builder()
                    .actorType(actorType)
                    .actorId(actorId)
                    .actorName(actorName)
                    .action(action)
                    .description(description)
                    .entityType(entityType)
                    .entityId(entityId)
                    .build());
        } catch (Exception e) {
            log.error("Failed to write activity log [{}]: {}", action, e.getMessage());
        }
    }

    public void record(SecurityUser user, String action, String description,
                       String entityType, String entityId) {
        if (user == null) {
            record(ActivityLog.ActorType.SYSTEM, null, null,
                    action, description, entityType, entityId);
            return;
        }
        record(ActivityLog.ActorType.from(user.getUserType()), user.getId(),
                user.getFullName(), action, description, entityType, entityId);
    }
}
