package com.medibridge.notification;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Email delivery.
 *
 * <p>When {@code medibridge.mail.enabled=false} (the dev default) the message
 * is logged instead of sent, so the whole booking flow works without SMTP
 * credentials. Flip the flag and set MAIL_USERNAME/MAIL_PASSWORD to go live.
 *
 * <p>Sends are {@code @Async}: a slow or unreachable SMTP server must never
 * make a patient's booking request hang or fail.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${medibridge.mail.enabled}")
    private boolean mailEnabled;

    @Value("${medibridge.mail.from}")
    private String from;

    @Async
    public void send(String to, String subject, String body) {
        if (!mailEnabled) {
            log.info("""

                    ---------- EMAIL (not sent: medibridge.mail.enabled=false) ----------
                    To      : {}
                    Subject : {}
                    {}
                    ---------------------------------------------------------------------""",
                    to, subject, body);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            // Never propagate: a failed notification must not roll back the
            // booking that triggered it.
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    public void sendMeetingLink(String to, String patientName, String doctorName,
                                String when, String meetingLink) {
        send(to,
                "Your MediBridge consultation is confirmed",
                """
                Hello %s,

                Your consultation with %s is confirmed for %s.

                Join using this link at your appointment time:
                %s

                No installation or account is required - the link opens in your browser.
                Please join a few minutes early.

                - MediBridge
                """.formatted(patientName, doctorName, when, meetingLink));
    }

    public void sendBookingRequested(String to, String patientName, String doctorName, String when) {
        send(to,
                "MediBridge booking request received",
                """
                Hello %s,

                Your appointment request with %s for %s has been received and is
                awaiting the doctor's confirmation. You will get the consultation
                link by email once it is accepted.

                - MediBridge
                """.formatted(patientName, doctorName, when));
    }

    public void sendCancellation(String to, String name, String doctorName, String when) {
        send(to,
                "Your MediBridge appointment was cancelled",
                """
                Hello %s,

                Your appointment with %s scheduled for %s has been cancelled.
                Any payment made will be refunded to the original payment method.

                - MediBridge
                """.formatted(name, doctorName, when));
    }
}
