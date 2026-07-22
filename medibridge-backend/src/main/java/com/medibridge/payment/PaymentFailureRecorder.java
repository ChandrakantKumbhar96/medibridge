package com.medibridge.payment;

import com.medibridge.common.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records a failed payment attempt in its own transaction.
 *
 * <p>This exists because of a subtle rollback trap: writing the FAILED status
 * and then throwing inside the same {@code @Transactional} method rolls the
 * write back along with everything else, so the failure vanishes. REQUIRES_NEW
 * commits it independently of whatever the caller does next.
 *
 * <p>It is a separate bean rather than a method on PaymentService because
 * Spring's transaction proxy is bypassed on self-invocation - calling
 * {@code this.recordFailure(...)} would silently run in the caller's
 * transaction and reintroduce the exact bug.
 */
@Component
@RequiredArgsConstructor
public class PaymentFailureRecorder {

    private static final Logger log = LoggerFactory.getLogger(PaymentFailureRecorder.class);

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Integer transactionId, String reason) {
        paymentRepository.findById(transactionId).ifPresent(payment -> {
            payment.setTransactionStatus(PaymentStatus.FAILED);
            payment.setFailureReason(reason);
            paymentRepository.save(payment);
            log.warn("Payment {} marked FAILED: {}", transactionId, reason);
        });
    }
}
