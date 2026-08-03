package com.medibridge.payment;

import com.medibridge.admin.SettingsProvider;
import com.medibridge.common.security.CurrentUser;
import com.medibridge.common.security.SecurityUser;
import com.medibridge.payment.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Payments", description = "Checkout, gateway verification and refunds")
public class PaymentController {

    private final PaymentService paymentService;
    private final SettingsProvider settings;

    @GetMapping("/config")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get checkout config", description = "Whether to show the real gateway or the simulated form, and the platform/second-opinion fees so the UI never hardcodes them.")
    public Map<String, Object> config() {
        return Map.of(
                "gatewayEnabled", paymentService.isGatewayEnabled(),
                "platformFee", settings.platformFee(),
                // The checkout screens need this to quote a second opinion at the
                // price that will actually be charged. Without it they show the
                // doctor's base fee and the gateway then takes the premium - a
                // billing surprise at the worst possible moment.
                "secondOpinionFeePercent", settings.secondOpinionFeePercent());
    }

    @PostMapping("/order")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Create a payment order", description = "Step 1 of a gateway payment: server creates the order and fixes the amount.")
    public OrderResponse createOrder(@CurrentUser SecurityUser me,
                                     @Valid @RequestBody CreateOrderRequest request) {
        return paymentService.createOrder(me.idAsInt(), request);
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Verify a gateway payment", description = "Step 2: signature verified server-side before the payment counts.")
    public PaymentResponse verify(@CurrentUser SecurityUser me,
                                  @Valid @RequestBody VerifyPaymentRequest request) {
        return paymentService.verifyAndCapture(me.idAsInt(), request);
    }

    @PostMapping("/failed")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Report a failed payment")
    public ResponseEntity<Void> markFailed(@CurrentUser SecurityUser me,
                                           @RequestBody Map<String, String> body) {
        paymentService.markFailed(me.idAsInt(), body.get("order_id"), body.get("reason"));
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Simulated payment", description = "Kept so the app still works without gateway keys.")
    public PaymentResponse pay(@CurrentUser SecurityUser me,
                               @Valid @RequestBody PaymentRequest request) {
        return paymentService.pay(me.idAsInt(), request);
    }

    @GetMapping
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "List my payments")
    public List<PaymentResponse> myPayments(@CurrentUser SecurityUser me) {
        return paymentService.listForPatient(me.idAsInt());
    }

    @GetMapping("/appointment/{appointmentId}")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get the payment for an appointment")
    public PaymentResponse forAppointment(@CurrentUser SecurityUser me,
                                          @PathVariable Integer appointmentId) {
        return paymentService.getForAppointment(me.idAsInt(), appointmentId);
    }

    @PostMapping("/{transactionId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Issue a manual refund", description = "Admin-only, for disputes and goodwill - cancellation refunds already happen automatically.")
    public PaymentResponse refund(@PathVariable Integer transactionId,
                                  @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? "Refunded by administrator" : body.get("reason");
        return paymentService.refund(transactionId, reason);
    }
}
