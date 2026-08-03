package com.medibridge.admin;

import com.medibridge.admin.dto.PolicyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Policies", description = "Platform policy numbers (cancellation, reschedule, follow-up, second opinion)")
public class PolicyController {

    private final SettingsProvider settings;

    @GetMapping("/policies")
    @Operation(summary = "Get current policy settings", description = "Public, read-only. Backs the chat assistant's policy tool.")
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
