package com.medibridge.payment;

import com.medibridge.common.exception.BadRequestException;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Razorpay integration.
 *
 * <p>The flow, and why each step is server-side:
 * <ol>
 *   <li><b>Create order</b> here, not in the browser - the amount must be fixed
 *       by the server. If the client sent the amount, a user could pay ₹1 for a
 *       ₹800 consultation.</li>
 *   <li>The browser opens Razorpay checkout with that order id.</li>
 *   <li><b>Verify the signature</b> here. Razorpay returns
 *       {@code HMAC_SHA256(order_id + "|" + payment_id, key_secret)}. Only
 *       someone holding the key secret can produce it, so a matching signature
 *       proves Razorpay really took the money. Marking a payment paid on the
 *       browser's say-so instead would let anyone book for free by posting a
 *       fake success callback.</li>
 * </ol>
 *
 * <p>The key secret never leaves the server. Only the key id is public.
 */
@Component
public class RazorpayGateway {

    private static final Logger log = LoggerFactory.getLogger(RazorpayGateway.class);

    private final String keyId;
    private final String keySecret;
    private final RazorpayClient client;

    public RazorpayGateway(
            @Value("${medibridge.razorpay.key-id:}") String keyId,
            @Value("${medibridge.razorpay.key-secret:}") String keySecret) {

        this.keyId = keyId;
        this.keySecret = keySecret;

        RazorpayClient created = null;
        if (isConfigured()) {
            try {
                created = new RazorpayClient(keyId, keySecret);
                log.info("Razorpay enabled (key id {}…)",
                        keyId.substring(0, Math.min(12, keyId.length())));
            } catch (Exception e) {
                log.error("Razorpay initialisation failed: {}", e.getMessage());
            }
        } else {
            log.warn("Razorpay not configured - payments fall back to simulated mode");
        }
        this.client = created;
    }

    public boolean isEnabled() {
        return client != null;
    }

    public String getKeyId() {
        return keyId;
    }

    private boolean isConfigured() {
        return keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank();
    }

    /**
     * @param amount rupees; Razorpay works in paise, so this is multiplied by 100
     * @return the created order id (e.g. {@code order_ABC123})
     */
    public String createOrder(BigDecimal amount, String receipt) {
        if (!isEnabled()) {
            throw new BadRequestException("Online payments are not configured on this server");
        }

        try {
            JSONObject request = new JSONObject();
            request.put("amount", amount.multiply(BigDecimal.valueOf(100)).longValueExact());
            request.put("currency", "INR");
            request.put("receipt", receipt);
            request.put("payment_capture", true);

            Order order = client.orders.create(request);
            return order.get("id");

        } catch (Exception e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new BadRequestException(
                    "Could not start the payment. Please try again.");
        }
    }

    /**
     * Issues a real refund through Razorpay.
     *
     * @param paymentId the gateway payment id captured at verification time
     * @param amount    rupees to return; converted to paise
     * @return the gateway's refund id - evidence the money actually moved
     */
    public String refund(String paymentId, BigDecimal amount, String reason) {
        if (!isEnabled()) {
            throw new BadRequestException("Online payments are not configured on this server");
        }
        if (paymentId == null || paymentId.isBlank()) {
            throw new BadRequestException(
                    "This payment has no gateway reference and cannot be refunded online");
        }

        try {
            JSONObject request = new JSONObject();
            request.put("amount", amount.multiply(BigDecimal.valueOf(100)).longValueExact());
            request.put("speed", "normal");

            JSONObject notes = new JSONObject();
            notes.put("reason", reason == null ? "Appointment cancelled" : reason);
            request.put("notes", notes);

            var refund = client.payments.refund(paymentId, request);
            return refund.get("id");

        } catch (Exception e) {
            log.error("Razorpay refund failed for payment {}: {}", paymentId, e.getMessage());
            throw new BadRequestException(
                    "The refund could not be processed. Please contact support.");
        }
    }

    /**
     * Recomputes the expected signature and compares it in constant time.
     *
     * <p>{@link MessageDigest#isEqual} rather than {@code String.equals}: a
     * short-circuiting comparison leaks, through timing, how many leading bytes
     * were correct, which is enough to forge a signature byte by byte.
     */
    public boolean verifySignature(String orderId, String paymentId, String signature) {
        if (!isConfigured() || orderId == null || paymentId == null || signature == null) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

            byte[] expected = mac.doFinal(
                    (orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));

            return MessageDigest.isEqual(
                    HexFormat.of().formatHex(expected).getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }
}
