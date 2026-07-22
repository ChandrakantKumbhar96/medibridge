package com.medibridge.common.security;

import com.medibridge.admin.ActivityLogService;
import com.medibridge.admin.entity.ActivityLog;
import com.medibridge.common.enums.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Writes LOGIN / LOGOUT rows to the audit trail.
 *
 * <p>Driven by application events rather than calls inside AuthService, so the
 * audit concern stays out of the authentication logic and cannot be forgotten
 * when a new login path is added.
 */
@Component
@RequiredArgsConstructor
public class AuthEventListener {

    private final ActivityLogService activityLogService;

    @EventListener
    public void onLogin(LoginEvent event) {
        activityLogService.record(
                ActivityLog.ActorType.from(event.userType()),
                event.userId(),
                event.fullName(),
                "LOGIN",
                event.fullName() + " signed in",
                event.userType().name(),
                event.userId());
    }

    @EventListener
    public void onLogout(LogoutEvent event) {
        activityLogService.record(
                ActivityLog.ActorType.from(event.userType()),
                event.userId(),
                event.fullName(),
                "LOGOUT",
                event.fullName() + " signed out",
                event.userType().name(),
                event.userId());
    }

    public record LoginEvent(String userId, String fullName, UserType userType) {
    }

    public record LogoutEvent(String userId, String fullName, UserType userType) {
    }
}
