package com.medibridge.payment;

import com.medibridge.common.security.CurrentUser;
import com.medibridge.common.security.SecurityUser;
import com.medibridge.payment.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** Tells the checkout screen whether to show the real gateway or the simulated form. */
    @GetMapping("/config")
    @PreAuthorize("hasRole('PATIENT')")
    public Map<String, Object> config() {
        return Map.of("gatewayEnabled", paymentService.isGatewayEnabled());
    }

    /** Step 1 of a gateway payment: server creates the order and fixes the amount. */
    @PostMapping("/order")
    @PreAuthorize("hasRole('PATIENT')")
    public OrderResponse createOrder(@CurrentUser SecurityUser me,
                                     @Valid @RequestBody CreateOrderRequest request) {
        return paymentService.createOrder(me.idAsInt(), request);
    }

    /** Step 2: signature verified server-side before the payment counts. */
    @PostMapping("/verify")
    @PreAuthorize("hasRole('PATIENT')")
    public PaymentResponse verify(@CurrentUser SecurityUser me,
                                  @Valid @RequestBody VerifyPaymentRequest request) {
        return paymentService.verifyAndCapture(me.idAsInt(), request);
    }

    @PostMapping("/failed")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Void> markFailed(@CurrentUser SecurityUser me,
                                           @RequestBody Map<String, String> body) {
        paymentService.markFailed(me.idAsInt(), body.get("order_id"), body.get("reason"));
        return ResponseEntity.noContent().build();
    }

    /** Simulated payment - kept so the app still works without gateway keys. */
    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse pay(@CurrentUser SecurityUser me,
                               @Valid @RequestBody PaymentRequest request) {
        return paymentService.pay(me.idAsInt(), request);
    }

    @GetMapping
    @PreAuthorize("hasRole('PATIENT')")
    public List<PaymentResponse> myPayments(@CurrentUser SecurityUser me) {
        return paymentService.listForPatient(me.idAsInt());
    }

    @GetMapping("/appointment/{appointmentId}")
    @PreAuthorize("hasRole('PATIENT')")
    public PaymentResponse forAppointment(@CurrentUser SecurityUser me,
                                          @PathVariable Integer appointmentId) {
        return paymentService.getForAppointment(me.idAsInt(), appointmentId);
    }

    /**
     * Manual full refund. Admin-only - a patient must not be able to reverse
     * their own charge. Cancellation refunds happen automatically; this is for
     * disputes and goodwill.
     */
    @PostMapping("/{transactionId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponse refund(@PathVariable Integer transactionId,
                                  @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? "Refunded by administrator" : body.get("reason");
        return paymentService.refund(transactionId, reason);
    }
}
