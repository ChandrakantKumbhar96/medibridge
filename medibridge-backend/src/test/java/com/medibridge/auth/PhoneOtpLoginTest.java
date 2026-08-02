package com.medibridge.auth;

import com.medibridge.admin.SettingsProvider;
import com.medibridge.admin.SystemSettingRepository;
import com.medibridge.admin.entity.SystemSetting;
import com.medibridge.auth.entity.PhoneOtp;
import com.medibridge.common.enums.AccountStatus;
import com.medibridge.common.util.PhoneNumbers;
import com.medibridge.notification.SmsService;
import com.medibridge.patient.PatientService;
import com.medibridge.patient.dto.PatientProfileUpdateRequest;
import com.medibridge.patient.entity.Patient;
import com.medibridge.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
// Spring Boot 4 moved MockMvc autoconfiguration into spring-boot-webmvc-test.
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phone OTP login, end to end through the HTTP layer.
 *
 * <p>Driven through MockMvc rather than the service, because half of what is
 * under test lives outside {@code AuthService}: the rate-limit filter, the
 * uniform-response rule, and the fact that four different rejections have to be
 * indistinguishable in the actual response.
 *
 * <p>Every test uses its own source address and its own phone number. The
 * limiter keys on the caller and the cooldown keys on the number, so sharing
 * either would make each test depend on which ran first.
 *
 * <p>The attempt cap is lowered to 3 for the same reason AuthRateLimitTest
 * lowers its limit, and the resend cooldown is switched off so a test can ask
 * for a second code without sleeping through a real minute - the test that
 * actually cares about the cooldown turns it back on through a
 * {@code system_settings} row, which is how an admin would.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "medibridge.otp.max-attempts=3",
        "medibridge.otp.resend-cooldown-seconds=0",
        "medibridge.ratelimit.auth.max-attempts=8",
        "medibridge.ratelimit.auth.window-seconds=60"
})
class PhoneOtpLoginTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired PhoneOtpRepository otpRepository;
    @Autowired SystemSettingRepository settingRepository;
    @Autowired PatientService patientService;

    /**
     * The gateway is replaced, which is how the test reads a code it is
     * otherwise never told - only the hash is stored. It also guarantees no
     * test can send a real message, whatever the configuration says.
     */
    @MockitoBean SmsService smsService;

    private static final Pattern CODE = Pattern.compile("\\b(\\d{4,8})\\b");

    // ------------------------------------------------------------- signing in

    @Test
    @DisplayName("an unknown number verifies and becomes a patient")
    void unknownNumberAutoRegisters() throws Exception {
        String ip = "198.51.100.10";
        String phone = uniquePhone();
        String e164 = PhoneNumbers.toE164(phone);

        assertThat(patientRepository.findByPhoneE164(e164)).isEmpty();

        String code = requestCode(phone, ip);

        mockMvc.perform(verify(phone, code, ip))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("patient"))
                // What sends them to finish their profile instead of to a
                // dashboard of blanks.
                .andExpect(jsonPath("$.user.profile_complete").value(false));

        Patient created = patientRepository.findByPhoneE164(e164).orElseThrow();
        assertThat(created.getAuthProvider()).isEqualTo(Patient.AuthProvider.PHONE);
        assertThat(created.getEmail()).isNull();
        assertThat(created.getPasswordHash()).isNull();
        assertThat(created.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    /**
     * The whole reason registration happens on verify rather than on request:
     * anyone can type a number, and typing one must not mint an account.
     */
    @Test
    @DisplayName("asking for a code does not create anything")
    void requestingACodeCreatesNoAccount() throws Exception {
        String phone = uniquePhone();

        requestCode(phone, "198.51.100.11");

        assertThat(patientRepository.findByPhoneE164(PhoneNumbers.toE164(phone))).isEmpty();
    }

    /**
     * A different answer for a registered number would turn this endpoint into
     * a way to ask "is this person a patient here?", one number at a time.
     */
    @Test
    @DisplayName("the response is the same for a known and an unknown number")
    void requestResponseDoesNotRevealWhetherTheNumberIsKnown() throws Exception {
        String ip = "198.51.100.12";
        Patient existing = newPatient();

        String known = body(mockMvc.perform(request(existing.getPhone(), ip))
                .andExpect(status().isOk()).andReturn());
        String unknown = body(mockMvc.perform(request(uniquePhone(), ip))
                .andExpect(status().isOk()).andReturn());

        assertThat(known).isEqualTo(unknown);
    }

    @Test
    @DisplayName("every way of writing one number reaches one account")
    void allWrittenFormsResolveToTheSameAccount() throws Exception {
        String ip = "198.51.100.13";
        String plain = uniquePhone();                                  // 9000011111
        String spaced = "+91 " + plain.substring(0, 5) + " " + plain.substring(5);
        String trunk = "0" + plain;                                    // 09000011111

        Integer first = signIn(spaced, ip);
        Integer second = signIn(plain, ip);
        Integer third = signIn(trunk, ip);

        assertThat(second).isEqualTo(first);
        assertThat(third).isEqualTo(first);
    }

    // ---------------------------------------------------------- code lifetime

    @Test
    @DisplayName("a wrong guess is counted but does not burn the code")
    void wrongGuessCountsAnAttemptAndTheRealCodeStillWorks() throws Exception {
        String ip = "198.51.100.20";
        String phone = uniquePhone();
        String code = requestCode(phone, ip);

        mockMvc.perform(verify(phone, wrongVersionOf(code), ip))
                .andExpect(status().isUnauthorized());

        assertThat(latestOtp(phone).getAttempts()).isEqualTo(1);

        mockMvc.perform(verify(phone, code, ip))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    /**
     * Backdated on the row rather than waited out: the expiry is a column
     * checked against {@code now()}, so moving the column is the honest way to
     * test it and the only one that does not add five minutes to the suite.
     */
    @Test
    @DisplayName("an expired code is refused")
    void expiredCodeIsRefused() throws Exception {
        String ip = "198.51.100.21";
        String phone = uniquePhone();
        String code = requestCode(phone, ip);

        PhoneOtp otp = latestOtp(phone);
        otp.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        otpRepository.save(otp);

        mockMvc.perform(verify(phone, code, ip))
                .andExpect(status().isUnauthorized());

        assertThat(patientRepository.findByPhoneE164(PhoneNumbers.toE164(phone))).isEmpty();
    }

    /**
     * Single use is what stops a code read over someone's shoulder, or left in
     * a notification history, from being worth anything after the fact.
     */
    @Test
    @DisplayName("a spent code cannot be replayed")
    void spentCodeCannotBeReplayed() throws Exception {
        String ip = "198.51.100.22";
        String phone = uniquePhone();
        String code = requestCode(phone, ip);

        mockMvc.perform(verify(phone, code, ip)).andExpect(status().isOk());

        mockMvc.perform(verify(phone, code, ip))
                .andExpect(status().isUnauthorized());
    }

    /**
     * The cap is what makes six digits defensible, so it has to hold against
     * the correct code too - otherwise an attacker simply keeps guessing and
     * the counter is decoration.
     */
    @Test
    @DisplayName("the attempt cap burns the code, correct or not")
    void attemptCapBurnsTheCode() throws Exception {
        String ip = "198.51.100.23";
        String phone = uniquePhone();
        String code = requestCode(phone, ip);

        for (int guess = 0; guess < 3; guess++) {
            mockMvc.perform(verify(phone, wrongVersionOf(code), ip))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(verify(phone, code, ip))
                .andExpect(status().isUnauthorized());

        // Still 3: a burnt row stops counting rather than climbing forever.
        assertThat(latestOtp(phone).getAttempts()).isEqualTo(3);
        assertThat(patientRepository.findByPhoneE164(PhoneNumbers.toE164(phone))).isEmpty();
    }

    /**
     * The refusals above all assert only the status, which is how the wording
     * drifted unnoticed: every one of them was answered with the password
     * login's "Invalid email or password" on a screen that has no password
     * field. Uniform across reasons is the security requirement; being about
     * the code the caller actually typed is the usability one, and this pins
     * both at once.
     */
    @Test
    @DisplayName("every refused code is answered with the same message, about the code")
    void refusedCodesSayTheSameThingAboutTheCode() throws Exception {
        String expected = "That code is not valid. Please request a new one.";

        // An IP each: the per-IP cap in this class is 8, and one address for
        // all four would sit exactly on it.
        // Wrong code.
        String wrongPhone = uniquePhone();
        String wrongCode = requestCode(wrongPhone, "198.51.100.31");
        mockMvc.perform(verify(wrongPhone, wrongVersionOf(wrongCode), "198.51.100.31"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(expected));

        // Expired code.
        String expiredPhone = uniquePhone();
        String expiredCode = requestCode(expiredPhone, "198.51.100.32");
        PhoneOtp expired = latestOtp(expiredPhone);
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        otpRepository.save(expired);
        mockMvc.perform(verify(expiredPhone, expiredCode, "198.51.100.32"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(expected));

        // Replayed code.
        String spentPhone = uniquePhone();
        String spentCode = requestCode(spentPhone, "198.51.100.33");
        mockMvc.perform(verify(spentPhone, spentCode, "198.51.100.33")).andExpect(status().isOk());
        mockMvc.perform(verify(spentPhone, spentCode, "198.51.100.33"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(expected));

        // Never sent anything.
        mockMvc.perform(verify(uniquePhone(), "000000", "198.51.100.34"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(expected));
    }

    // ------------------------------------------------------------- other paths

    /**
     * A phone-first account has no password hash. Before the provider check it
     * reached the encoder as a null; the requirement is a message that tells
     * the patient how to get in, not a crash and not the generic
     * "Invalid email or password" that leaves them retyping.
     */
    @Test
    @DisplayName("a phone account is turned away from password login, and told why")
    void phoneAccountCannotUseAPasswordLogin() throws Exception {
        String ip = "198.51.100.30";
        String phone = uniquePhone();
        Integer patientId = signIn(phone, ip);

        // They finish their profile, which is where the account gets an email -
        // and therefore where a password login can find it at all.
        String email = "phone-" + patientId + "@test.local";
        patientService.updateProfile(patientId, new PatientProfileUpdateRequest(
                "Phone Patient", email, phone, null, null,
                LocalDate.of(1990, 1, 1), "Male", "O+"));

        mockMvc.perform(post("/auth/login")
                        .with(r -> { r.setRemoteAddr(ip); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email
                                + "\",\"password\":\"Test@1234\",\"role\":\"patient\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("This account signs in with a code sent to your mobile number."));
    }

    // -------------------------------------------------------------- throttling

    /**
     * The per-number cooldown, which is the one that stops rotating source
     * addresses from billing us for an SMS per request.
     *
     * <p>Turned on through a {@code system_settings} row rather than a property,
     * which also proves the SettingsProvider fallback is actually wired: the
     * class-level property says 0 and the row has to win.
     */
    @Test
    @DisplayName("the same number cannot ask for codes back to back")
    void resendCooldownIsEnforcedPerNumber() throws Exception {
        String ip = "198.51.100.40";
        String phone = uniquePhone();

        settingRepository.save(SystemSetting.builder()
                .key(SettingsProvider.OTP_RESEND_COOLDOWN_SECONDS)
                .value("60")
                .valueType(SystemSetting.ValueType.INT)
                .build());
        try {
            Mockito.clearInvocations(smsService);

            mockMvc.perform(request(phone, ip)).andExpect(status().isOk());
            mockMvc.perform(request(phone, ip))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().exists("Retry-After"));

            // The point of the guard is the message that was not sent.
            Mockito.verify(smsService, Mockito.times(1))
                    .sendNow(Mockito.anyString(), Mockito.anyString());
        } finally {
            settingRepository.deleteById(SettingsProvider.OTP_RESEND_COOLDOWN_SECONDS);
        }
    }

    /**
     * Both new endpoints are in AuthRateLimitFilter.GUARDED. The IP limit and
     * the per-number cooldown guard different things - one attacker spraying a
     * different number every time never trips the cooldown.
     */
    @Test
    @DisplayName("both OTP endpoints are rate limited per caller")
    void bothOtpEndpointsAreRateLimited() throws Exception {
        String requestIp = "198.51.100.41";
        for (int i = 0; i < 8; i++) {
            mockMvc.perform(request(uniquePhone(), requestIp)).andExpect(status().isOk());
        }
        mockMvc.perform(request(uniquePhone(), requestIp))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));

        String verifyIp = "198.51.100.42";
        for (int i = 0; i < 8; i++) {
            mockMvc.perform(verify(uniquePhone(), "000000", verifyIp));
        }
        mockMvc.perform(verify(uniquePhone(), "000000", verifyIp))
                .andExpect(status().isTooManyRequests());
    }

    // ----------------------------------------------------------------- helpers

    /** Requests a code and reads it back out of the message that was sent. */
    private String requestCode(String phone, String ip) throws Exception {
        Mockito.clearInvocations(smsService);
        mockMvc.perform(request(phone, ip))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code_length").value(6));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        Mockito.verify(smsService).sendNow(Mockito.anyString(), message.capture());

        Matcher matcher = CODE.matcher(message.getValue());
        assertThat(matcher.find())
                .as("the text message should contain the code")
                .isTrue();
        return matcher.group(1);
    }

    /** Full round trip; @return the id of the account the session belongs to. */
    private Integer signIn(String phone, String ip) throws Exception {
        String code = requestCode(phone, ip);
        mockMvc.perform(verify(phone, code, ip)).andExpect(status().isOk());

        return patientRepository.findByPhoneE164(PhoneNumbers.toE164(phone))
                .orElseThrow().getId();
    }

    private PhoneOtp latestOtp(String phone) {
        return otpRepository
                .findFirstByPhoneE164OrderByIdDesc(PhoneNumbers.toE164(phone))
                .orElseThrow();
    }

    /** Same shape, definitely not the same value. */
    private String wrongVersionOf(String code) {
        char first = code.charAt(0);
        return (first == '0' ? '1' : '0') + code.substring(1);
    }

    private RequestBuilder request(String phone, String ip) {
        return post("/auth/otp/request")
                .with(r -> { r.setRemoteAddr(ip); return r; })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"" + phone + "\"}");
    }

    private RequestBuilder verify(String phone, String code, String ip) {
        return post("/auth/otp/verify")
                .with(r -> { r.setRemoteAddr(ip); return r; })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"" + phone + "\",\"code\":\"" + code + "\"}");
    }

    private String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }
}
