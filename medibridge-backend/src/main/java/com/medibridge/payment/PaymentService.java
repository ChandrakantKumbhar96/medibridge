package com.medibridge.payment;

import com.medibridge.appointment.AppointmentRepository;
import com.medibridge.appointment.AppointmentService;
import com.medibridge.appointment.entity.Appointment;
import com.medibridge.common.enums.AppointmentStatus;
import com.medibridge.common.enums.PaymentStatus;
import com.medibridge.common.exception.BadRequestException;
import com.medibridge.common.exception.ConflictException;
import com.medibridge.common.exception.ResourceNotFoundException;
import com.medibridge.payment.dto.*;
import com.medibridge.payment.entity.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Simulated payment processing.
 *
 * <p>No gateway is called: a real Razorpay/Stripe integration needs business
 * verification and live keys. The transaction record, status transitions, and
 * refund flow are real, so swapping in a gateway later means replacing one
 * method rather than restructuring anything.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentService appointmentService;
    private final RazorpayGateway razorpayGateway;
    private final PaymentFailureRecorder failureRecorder;
    private final org.springframework.context.ApplicationEventPublisher events;

    // ------------------------------------------------------- gateway payment

    public boolean isGatewayEnabled() {
        return razorpayGateway.isEnabled();
    }

    /**
     * Step 1 of a real payment: create the gateway order and a PENDING
     * transaction row. The amount comes from the doctor's fee, never from the
     * client.
     */
    @Transactional
    public OrderResponse createOrder(Integer patientId, CreateOrderRequest request) {
        Appointment appointment = appointmentRepository
                .findByIdAndPatientId(request.appointmentId(), patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        paymentRepository.findByAppointmentId(appointment.getId()).ifPresent(existing -> {
            if (existing.getTransactionStatus() == PaymentStatus.PAID) {
                throw new ConflictException("This appointment has already been paid for");
            }
        });

        if (appointment.getStatus() == AppointmentStatus.CANCELLED
                || appointment.getStatus() == AppointmentStatus.REJECTED) {
            throw new BadRequestException("Cannot pay for a cancelled appointment");
        }

        // The amount agreed at booking, not the doctor's current rate - and the
        // full total including platform fee, which is what the UI quoted.
        BigDecimal amount = appointment.getTotalAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("This appointment has no payable amount");
        }

        String receipt = "MB-APPT-" + appointment.getId();
        String orderId = razorpayGateway.createOrder(amount, receipt);

        PaymentTransaction payment = paymentRepository.save(PaymentTransaction.builder()
                .appointment(appointment)
                .amount(amount)
                .consultationAmount(appointment.getBookedFee())
                .platformFee(appointment.getPlatformFee())
                .paymentMethod("RAZORPAY")
                .gateway(PaymentTransaction.Gateway.RAZORPAY)
                .gatewayOrderId(orderId)
                .transactionRef("MB-" + UUID.randomUUID().toString()
                        .replace("-", "").substring(0, 16).toUpperCase())
                .transactionStatus(PaymentStatus.PENDING)
                .build());

        var patient = appointment.getPatient();

        return new OrderResponse(
                orderId,
                razorpayGateway.getKeyId(),
                amount,
                amount.multiply(BigDecimal.valueOf(100)).longValueExact(),
                "INR",
                payment.getId(),
                appointment.getId(),
                appointment.getDoctor().getFullName(),
                patient.getFullName(),
                patient.getEmail(),
                patient.getPhone());
    }

    /**
     * Step 2: verify the signature before believing anything the browser says.
     *
     * <p>A failed verification is recorded as FAILED rather than ignored - a
     * burst of them is exactly what an attempt to forge payments looks like.
     */
    @Transactional
    public PaymentResponse verifyAndCapture(Integer patientId, VerifyPaymentRequest request) {
        PaymentTransaction payment = paymentRepository
                .findByGatewayOrderId(request.razorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));

        // The order must belong to the caller - otherwise one patient could
        // settle (or tamper with) another's payment.
        if (!payment.getAppointment().getPatient().getId().equals(patientId)) {
            throw new ResourceNotFoundException("Payment order not found");
        }

        if (payment.getTransactionStatus() == PaymentStatus.PAID) {
            return toDto(payment);   // idempotent: checkout can fire twice
        }

        boolean valid = razorpayGateway.verifySignature(
                request.razorpayOrderId(),
                request.razorpayPaymentId(),
                request.razorpaySignature());

        if (!valid) {
            // Committed in a separate transaction - see PaymentFailureRecorder.
            // Writing it here would be rolled back by the throw below, which is
            // precisely when we most want the record to survive.
            failureRecorder.recordFailure(payment.getId(), "Signature verification failed");
            throw new BadRequestException(
                    "Payment could not be verified. If you were charged, contact support.");
        }

        payment.setGatewayPaymentId(request.razorpayPaymentId());
        payment.setGatewaySignature(request.razorpaySignature());
        payment.setTransactionStatus(PaymentStatus.PAID);
        payment = paymentRepository.save(payment);

        appointmentService.markPaidAndConfirm(payment.getAppointment().getId());

        return toDto(payment);
    }

    /** Records an abandoned or gateway-rejected attempt. */
    @Transactional
    public void markFailed(Integer patientId, String orderId, String reason) {
        paymentRepository.findByGatewayOrderId(orderId).ifPresent(payment -> {
            if (payment.getAppointment().getPatient().getId().equals(patientId)
                    && payment.getTransactionStatus() == PaymentStatus.PENDING) {
                payment.setTransactionStatus(PaymentStatus.FAILED);
                payment.setFailureReason(
                        reason == null ? "Payment was not completed" : reason);
                paymentRepository.save(payment);
            }
        });
    }

    /**
     * Pays for an appointment and promotes it out of PENDING_PAYMENT.
     *
     * <p>Both writes share one transaction: money recorded without the
     * appointment advancing (or the reverse) would leave the patient paid but
     * unbooked. This is exactly the guarantee a split microservice would lose.
     */
    @Transactional
    public PaymentResponse pay(Integer patientId, PaymentRequest request) {
        Appointment appointment = appointmentRepository
                .findByIdAndPatientId(request.appointmentId(), patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        paymentRepository.findByAppointmentId(appointment.getId()).ifPresent(existing -> {
            if (existing.getTransactionStatus() == PaymentStatus.PAID) {
                throw new ConflictException("This appointment has already been paid for");
            }
        });

        if (appointment.getStatus() == AppointmentStatus.CANCELLED
                || appointment.getStatus() == AppointmentStatus.REJECTED) {
            throw new BadRequestException("Cannot pay for a cancelled appointment");
        }

        BigDecimal amount = appointment.getTotalAmount();

        PaymentTransaction payment = PaymentTransaction.builder()
                .appointment(appointment)
                .amount(amount)
                .consultationAmount(appointment.getBookedFee())
                .platformFee(appointment.getPlatformFee())
                .paymentMethod(request.paymentMethod())
                .gateway(PaymentTransaction.Gateway.SIMULATED)
                .transactionRef("MB-" + UUID.randomUUID().toString()
                        .replace("-", "").substring(0, 16).toUpperCase())
                .transactionStatus(PaymentStatus.PAID)
                .build();

        payment = paymentRepository.save(payment);

        appointmentService.markPaidAndConfirm(appointment.getId());

        return toDto(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getForAppointment(Integer patientId, Integer appointmentId) {
        appointmentRepository.findByIdAndPatientId(appointmentId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        return paymentRepository.findByAppointmentId(appointmentId)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("No payment for this appointment"));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listForPatient(Integer patientId) {
        return paymentRepository.findByAppointmentPatientIdOrderByProcessedAtDesc(patientId)
                .stream().map(this::toDto).toList();
    }

    /**
     * Admin-initiated full refund.
     *
     * <p>Calls the gateway for real gateway payments. Previously this only
     * flipped a status, which told the patient they had been repaid while
     * nothing left the account.
     */
    @Transactional
    public PaymentResponse refund(Integer transactionId, String reason) {
        PaymentTransaction payment = paymentRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (payment.getTransactionStatus() != PaymentStatus.PAID) {
            throw new BadRequestException(
                    "Only a paid transaction can be refunded (status: "
                            + payment.getTransactionStatus().getDbValue() + ")");
        }

        return toDto(doRefund(payment, payment.getAmount(), reason));
    }

    /**
     * Automatic refund on cancellation. Driven by the event the appointment
     * module publishes, so the money rules live with the money.
     *
     * @param refundPercent 100 for a full refund, less inside the
     *                      late-cancellation window
     */
    @Transactional
    public BigDecimal refundForCancellation(Integer appointmentId, int refundPercent,
                                            String reason) {
        PaymentTransaction payment = paymentRepository.findByAppointmentId(appointmentId)
                .orElse(null);

        // Nothing was paid - an unpaid hold that lapsed, for instance.
        if (payment == null || payment.getTransactionStatus() != PaymentStatus.PAID) {
            return BigDecimal.ZERO;
        }

        BigDecimal refundAmount = payment.getAmount()
                .multiply(BigDecimal.valueOf(refundPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        if (refundAmount.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        doRefund(payment, refundAmount, reason);
        return refundAmount;
    }

    private PaymentTransaction doRefund(PaymentTransaction payment, BigDecimal amount,
                                        String reason) {
        if (payment.getGateway() == PaymentTransaction.Gateway.RAZORPAY) {
            String refundId = razorpayGateway.refund(
                    payment.getGatewayPaymentId(), amount, reason);
            payment.setGatewayRefundId(refundId);
        }

        payment.setTransactionStatus(PaymentStatus.REFUNDED);
        payment.setRefundAmount(amount);
        payment.setRefundedAt(LocalDateTime.now());
        payment.setRefundReason(reason);

        PaymentTransaction saved = paymentRepository.save(payment);

        // Money returned to the patient must also stop being owed to the doctor.
        // Announced from here rather than only from the cancellation path,
        // because an admin can refund a completed consultation directly - and
        // without this the platform would refund the patient and still pay out.
        events.publishEvent(new PaymentRefundedEvent(
                saved.getAppointment().getId(), amount, reason));

        return saved;
    }

    /** Published whenever money goes back to a patient, from any path. */
    public record PaymentRefundedEvent(Integer appointmentId, BigDecimal amount,
                                       String reason) {
    }

    private PaymentResponse toDto(PaymentTransaction p) {
        return new PaymentResponse(
                p.getId(),
                p.getTransactionRef(),
                p.getAppointment().getId(),
                p.getAmount(),
                p.getPaymentMethod(),
                p.getTransactionStatus().getDbValue(),
                p.getProcessedAt() == null ? null : p.getProcessedAt().format(TS),
                p.getAppointment().getDoctor().getFullName(),
                "/payments/" + p.getId() + "/invoice");
    }
}
