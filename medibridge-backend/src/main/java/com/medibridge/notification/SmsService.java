package com.medibridge.notification;

import com.medibridge.common.util.PhoneNumbers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Sends SMS and WhatsApp messages via Twilio.
 *
 * <p>Mirrors {@link EmailService}: when {@code medibridge.sms.enabled=false}
 * (the dev default) the message is logged to the console instead of sent, so
 * the whole notification flow works with no Twilio account. Set the flag and
 * supply credentials to go live.
 *
 * <p>Uses the JDK HTTP client against Twilio's REST API rather than pulling in
 * the Twilio SDK — one fewer dependency, and the request is simple: a
 * form-encoded POST with basic auth. Sends are {@code @Async} so a slow SMS
 * gateway never delays the booking that triggered it, and failures are
 * swallowed — a missed text must not roll back a confirmed appointment.
 */
@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    @Value("${medibridge.sms.enabled}")
    private boolean enabled;

    /** SMS or WHATSAPP — decides which Twilio channel the message uses. */
    @Value("${medibridge.sms.channel:WHATSAPP}")
    private String channel;

    @Value("${medibridge.sms.twilio.account-sid:}")
    private String accountSid;

    @Value("${medibridge.sms.twilio.auth-token:}")
    private String authToken;

    /** Twilio number for plain SMS, e.g. +12025550123. */
    @Value("${medibridge.sms.twilio.from-sms:}")
    private String fromSms;

    /** WhatsApp-enabled sender. The Twilio sandbox default is +14155238886. */
    @Value("${medibridge.sms.twilio.whatsapp-from:+14155238886}")
    private String whatsappFrom;

    public boolean isWhatsApp() {
        return "WHATSAPP".equalsIgnoreCase(channel);
    }

    /**
     * @param toPhone recipient in any format; normalised to E.164 here
     * @param body    message text (keep short — SMS caps at 160 chars)
     */
    @Async
    public void send(String toPhone, String body) {
        String to = PhoneNumbers.toE164(toPhone);
        if (to == null) {
            log.warn("SMS/WhatsApp skipped: no valid phone number");
            return;
        }

        if (!enabled) {
            logInsteadOfSending(to, body);
            return;
        }

        try {
            deliverViaTwilio(to, body);
            log.info("{} sent to {}", isWhatsApp() ? "WhatsApp" : "SMS", to);
        } catch (Exception e) {
            // Never propagate: a failed text must not undo the action behind it.
            log.error("{} to {} failed: {}",
                    isWhatsApp() ? "WhatsApp" : "SMS", to, e.getMessage());
        }
    }

    /**
     * The same delivery, synchronous and loud.
     *
     * <p>{@link #send} is right for a booking notice: it happens behind an
     * action the user already completed, so a slow gateway must not hold up the
     * response and a failed text must not undo a confirmed appointment.
     *
     * <p>A login code is the opposite on both counts. The user is sitting on the
     * "enter the code" screen waiting for it, so a swallowed failure leaves them
     * waiting for something that is never coming - and the caller needs the
     * exception so it can roll back the code it just issued rather than leave a
     * live row nobody can satisfy.
     *
     * @throws IllegalArgumentException the number is not one we can text
     * @throws IllegalStateException    the gateway refused or was unreachable
     */
    public void sendNow(String toPhone, String body) {
        String to = PhoneNumbers.toE164(toPhone);
        if (to == null) {
            throw new IllegalArgumentException("Not a valid mobile number");
        }

        if (!enabled) {
            logInsteadOfSending(to, body);
            return;
        }

        try {
            deliverViaTwilio(to, body);
            log.info("{} sent to {}", isWhatsApp() ? "WhatsApp" : "SMS", to);
        } catch (Exception e) {
            throw new IllegalStateException(
                    (isWhatsApp() ? "WhatsApp" : "SMS") + " delivery failed", e);
        }
    }

    private void logInsteadOfSending(String to, String body) {
        log.info("""

                ---------- {} (not sent: medibridge.sms.enabled=false) ----------
                To   : {}
                {}
                -----------------------------------------------------------------""",
                isWhatsApp() ? "WHATSAPP" : "SMS", to, body);
    }

    private void deliverViaTwilio(String to, String body) throws Exception {
        if (accountSid.isBlank() || authToken.isBlank()) {
            throw new IllegalStateException("Twilio credentials are not configured");
        }

        String from = isWhatsApp() ? "whatsapp:" + whatsappFrom : fromSms;
        String recipient = isWhatsApp() ? "whatsapp:" + to : to;

        String form = "From=" + enc(from) + "&To=" + enc(recipient) + "&Body=" + enc(body);
        String basic = Base64.getEncoder()
                .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/"
                        + accountSid + "/Messages.json"))
                .header("Authorization", "Basic " + basic)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> res = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() >= 300) {
            throw new IllegalStateException("Twilio returned " + res.statusCode()
                    + ": " + res.body());
        }
    }

    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}
