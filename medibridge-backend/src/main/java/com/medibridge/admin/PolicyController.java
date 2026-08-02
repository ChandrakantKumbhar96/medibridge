package com.medibridge.admin;

import com.medibridge.admin.dto.PolicyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only view of the policy numbers in {@link SettingsProvider}, for
 * anything that needs to state a rule rather than hardcode it - currently
 * the chat assistant's policy tool. Permit-all, same trust level as
 * {@code GET /specialties}: see {@code SecurityConfig}.
 */
@RestController
@RequiredArgsConstructor
public class PolicyController {

    private final SettingsProvider settings;

    @GetMapping("/policies")
    public PolicyResponse getPolicies() {
        return new PolicyResponse(
                settings.freeCancellationHours(),
                settings.partialRefundPercent(),
                settings.maxReschedules(),
                settings.rescheduleMinHours(),
                settings.noShowGraceMinutes(),
                settings.followUpEnabled(),
                settings.followUpWindowDays(),
                settings.secondOpinionFeePercent(),
                settings.secondOpinionMinReports());
    }
}
