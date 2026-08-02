package com.medibridge.admin;

import com.medibridge.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /policies is permit-all - it exists so the chat assistant (and anything
 * else) can ground a policy number in live data instead of a hardcoded guess.
 * The contract under test is "no auth required" and "matches SettingsProvider",
 * not ownership - there is no per-user data behind this endpoint to protect.
 */
@AutoConfigureMockMvc
class PolicyControllerTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired SettingsProvider settings;

    @Test
    @DisplayName("anonymous callers get the current policy numbers, not 401")
    void anonymousCanReadPolicies() throws Exception {
        mockMvc.perform(get("/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.free_cancellation_hours").value(settings.freeCancellationHours()))
                .andExpect(jsonPath("$.partial_refund_percent").value(settings.partialRefundPercent()))
                .andExpect(jsonPath("$.max_reschedules").value(settings.maxReschedules()))
                .andExpect(jsonPath("$.reschedule_min_hours").value(settings.rescheduleMinHours()))
                .andExpect(jsonPath("$.no_show_grace_minutes").value(settings.noShowGraceMinutes()))
                .andExpect(jsonPath("$.follow_up_enabled").value(settings.followUpEnabled()))
                .andExpect(jsonPath("$.follow_up_window_days").value(settings.followUpWindowDays()))
                .andExpect(jsonPath("$.second_opinion_fee_percent").value(settings.secondOpinionFeePercent()))
                .andExpect(jsonPath("$.second_opinion_min_reports").value(settings.secondOpinionMinReports()));
    }
}
