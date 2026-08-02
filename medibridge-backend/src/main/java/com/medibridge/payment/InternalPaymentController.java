package com.medibridge.payment;

import com.medibridge.payment.dto.PaymentResponse;
import com.medibridge.payment.dto.RefundStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Called only by trusted internal services via the {@code X-Internal-Api-Key}
 * header - see InternalApiKeyAuthFilter. Same trust model as
 * {@link com.medibridge.appointment.InternalAppointmentController}: no
 * ownership scoping by design, safe only because reaching this controller at
 * all requires the shared secret, never a user-supplied JWT.
 */
@RestController
@RequestMapping("/internal/payments")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final PaymentService paymentService;

    /** Backs the chat-service's get_refund_status tool - see spring_client.py. */
    @GetMapping("/patient/{patientId}/latest-refund")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public RefundStatusResponse getLatestRefund(@PathVariable Integer patientId) {
        return paymentService.latestRefundForPatient(patientId);
    }

    /** Backs the chat-service's get_payment_history tool - see spring_client.py. */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public List<PaymentResponse> listForPatient(@PathVariable Integer patientId) {
        return paymentService.listForPatient(patientId);
    }
}
