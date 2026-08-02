package com.medibridge.payout;

import com.medibridge.payout.dto.EarningResponse;
import com.medibridge.payout.dto.PayoutResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Called only by trusted internal services via the {@code X-Internal-Api-Key}
 * header - see InternalApiKeyAuthFilter. Same trust model as
 * {@link com.medibridge.appointment.InternalAppointmentController}: no
 * ownership scoping by design, safe only because reaching this controller at
 * all requires the shared secret, never a user-supplied JWT.
 */
@RestController
@RequestMapping("/internal/payouts")
@RequiredArgsConstructor
public class InternalPayoutController {

    private final PayoutService payoutService;

    /** Backs the chat-service's get_earnings_summary tool - see spring_client.py. */
    @GetMapping("/doctor/{doctorId}/summary")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public Map<String, Object> getEarningsSummary(@PathVariable String doctorId) {
        return payoutService.getEarningsSummary(doctorId);
    }

    /** Backs the chat-service's get_earnings_list tool - see spring_client.py. */
    @GetMapping("/doctor/{doctorId}/earnings")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public List<EarningResponse> getEarnings(@PathVariable String doctorId) {
        return payoutService.getEarnings(doctorId);
    }

    /** Backs the chat-service's get_payouts tool - see spring_client.py. */
    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public List<PayoutResponse> getPayouts(@PathVariable String doctorId) {
        return payoutService.getPayoutsForDoctor(doctorId);
    }
}
