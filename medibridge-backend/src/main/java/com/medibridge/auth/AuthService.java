package com.medibridge.auth;

import com.medibridge.admin.AdminRepository;
import com.medibridge.admin.entity.Admin;
import com.medibridge.auth.dto.*;
import com.medibridge.auth.entity.RefreshToken;
import com.medibridge.common.enums.AccountStatus;
import com.medibridge.common.enums.UserType;
import com.medibridge.admin.SettingsProvider;
import com.medibridge.common.exception.BadRequestException;
import com.medibridge.common.exception.ConflictException;
import com.medibridge.common.exception.InvalidOtpException;
import com.medibridge.common.exception.OAuthException;
import com.medibridge.common.security.JwtService;
import com.medibridge.common.util.PhoneNumbers;
import com.medibridge.doctor.DoctorRepository;
import com.medibridge.doctor.SpecializationRepository;
import com.medibridge.doctor.entity.Doctor;
import com.medibridge.doctor.entity.Specialization;
import com.medibridge.patient.PatientRepository;
import com.medibridge.patient.entity.Patient;
import com.medibridge.admin.ActivityLogService;
import com.medibridge.admin.entity.ActivityLog;
import com.medibridge.common.security.AuthEventListener;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Authentication across the three identity tables.
 *
 * <p>This module deliberately reaches into the patient/doctor/admin
 * repositories rather than going through their services: authentication is
 * inherently cross-cutting over all three identities, and an extra indirection
 * layer would add nothing. It is the one documented exception to the
 * "modules talk through services" rule.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AdminRepository adminRepository;
    private final SpecializationRepository specializationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher events;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final PhoneOtpService phoneOtpService;
    private final SettingsProvider settings;

    /** Placeholder until the patient completes their profile. */
    private static final String NEW_PHONE_PATIENT_NAME = "New Patient";

    public boolean isGoogleEnabled() {
        return googleTokenVerifier.isEnabled();
    }

    // ---------------------------------------------------------------- login

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserType type = UserType.fromRequest(request.role());

        return switch (type) {
            case PATIENT -> loginPatient(request);
            case DOCTOR -> loginDoctor(request);
            case ADMIN -> loginAdmin(request);
        };
    }

    private AuthResponse loginPatient(LoginRequest request) {
        Patient patient = patientRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // Neither a Google nor a phone account has a password hash. Without this
        // the encoder is handed a null, and the caller gets an unexplained
        // failure on an account that is perfectly fine.
        //
        // OAuthException, not BadCredentialsException: the handler answers the
        // latter with a fixed "Invalid email or password" to avoid enumerating
        // accounts, which would swallow the one message that actually helps
        // here. Naming the sign-in method does confirm the address is
        // registered - accepted, because the alternative is a user stuck
        // retyping a password that does not exist.
        if (patient.getAuthProvider() != Patient.AuthProvider.LOCAL) {
            throw new OAuthException(switch (patient.getAuthProvider()) {
                case GOOGLE -> "This account uses Google Sign-In. Please continue with Google.";
                case PHONE -> "This account signs in with a code sent to your mobile number.";
                default -> "This account uses a different sign-in method.";
            });
        }

        verifyPassword(request.password(), patient.getPasswordHash());

        if (patient.getStatus() != AccountStatus.ACTIVE) {
            throw new DisabledException("Your account is inactive. Please contact support.");
        }

        return issueTokens(
                String.valueOf(patient.getId()), patient.getEmail(),
                patient.getFullName(), UserType.PATIENT, patientUser(patient));
    }

    private AuthResponse loginDoctor(LoginRequest request) {
        Doctor doctor = doctorRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        verifyPassword(request.password(), doctor.getPasswordHash());

        // Password was correct, so a specific message here is safe and useful.
        if (doctor.getStatus() == AccountStatus.PENDING) {
            throw new DisabledException(
                    "Your doctor account is awaiting admin approval.");
        }
        if (doctor.getStatus() != AccountStatus.ACTIVE) {
            throw new DisabledException(
                    "Your account has been " + doctor.getStatus().getDbValue()
                            + ". Please contact support.");
        }

        return issueTokens(
                doctor.getId(), doctor.getEmail(), doctor.getFullName(), UserType.DOCTOR,
                new AuthResponse.User(
                        doctor.getId(), doctor.getFullName(), doctor.getEmail(), "doctor",
                        doctor.getSpecialization().getName(), null,
                        doctor.getConsultationFee(), doctor.getStatus().getDbValue(),
                        null, null));
    }

    private AuthResponse loginAdmin(LoginRequest request) {
        Admin admin = adminRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        verifyPassword(request.password(), admin.getPasswordHash());

        if (admin.getStatus() != AccountStatus.ACTIVE) {
            throw new DisabledException("This admin account is inactive.");
        }

        return issueTokens(
                String.valueOf(admin.getId()), admin.getEmail(),
                admin.getFullName(), UserType.ADMIN,
                new AuthResponse.User(
                        String.valueOf(admin.getId()), admin.getFullName(),
                        admin.getEmail(), "admin",
                        null, admin.getTitle(), null, admin.getStatus().getDbValue(),
                        null, null));
    }

    private void verifyPassword(String raw, String hash) {
        if (!passwordEncoder.matches(raw, hash)) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    // -------------------------------------------------------- google sign-in

    /**
     * Signs in (or registers) a patient with a verified Google ID token.
     *
     * <p>Patients only. Doctors need a licence number verified by an admin, and
     * admin accounts are seeded - letting either be created by anyone with a
     * Google account would hand out privileged access for free.
     *
     * <p>Existing local accounts are linked rather than duplicated: signing in
     * with Google using an email that already registered with a password
     * attaches the Google identity to that same patient row.
     */
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        if (request.role() != null && !request.role().isBlank()
                && !"patient".equalsIgnoreCase(request.role().trim())) {
            throw new OAuthException(
                    "Google Sign-In is available for patient accounts only");
        }

        GoogleTokenVerifier.GoogleUser googleUser =
                googleTokenVerifier.verify(request.credential());

        Patient patient = patientRepository.findByGoogleSub(googleUser.sub())
                .or(() -> patientRepository.findByEmailIgnoreCase(googleUser.email()))
                .orElse(null);

        if (patient == null) {
            patient = patientRepository.save(Patient.builder()
                    .fullName(googleUser.name() == null ? googleUser.email() : googleUser.name())
                    .email(googleUser.email().toLowerCase())
                    .passwordHash(null)
                    .authProvider(Patient.AuthProvider.GOOGLE)
                    .googleSub(googleUser.sub())
                    .avatarUrl(googleUser.pictureUrl())
                    .status(AccountStatus.ACTIVE)
                    .build());
        } else {
            // Link an existing local account, or refresh a changed avatar.
            if (patient.getGoogleSub() == null) {
                patient.setGoogleSub(googleUser.sub());
            }
            patient.setAvatarUrl(googleUser.pictureUrl());
            patient = patientRepository.save(patient);
        }

        if (patient.getStatus() != AccountStatus.ACTIVE) {
            throw new DisabledException("Your account is inactive. Please contact support.");
        }

        return issueTokens(
                String.valueOf(patient.getId()), patient.getEmail(),
                patient.getFullName(), UserType.PATIENT, patientUser(patient));
    }

    // --------------------------------------------------------- phone sign-in

    /**
     * Texts a login code to the number, whoever it belongs to.
     *
     * <p>The response is identical for a registered number and an unknown one.
     * Anything else - a different message, a 404, even a visibly faster reply -
     * makes this endpoint a way to test whether a given person is a MediBridge
     * patient, one number at a time.
     *
     * <p>No account is created here. Registration happens on VERIFY, so a number
     * that was typed by mistake, or by someone probing, never leaves a row
     * behind.
     */
    @Transactional
    public PhoneOtpSentResponse requestPhoneOtp(PhoneOtpRequest request) {
        phoneOtpService.issue(toLookupKey(request.phone()));

        return new PhoneOtpSentResponse(
                "If that number can receive messages, a verification code is on its way.",
                settings.otpLength(),
                settings.otpResendCooldownSeconds(),
                settings.otpTtlMinutes() * 60);
    }

    /**
     * Signs in the owner of a verified number, creating the account if this is
     * the first time it has been seen.
     *
     * <p>Patients only, for the reason Google Sign-In is patients only: a doctor
     * account is a licence an admin checked, and an admin account is seeded.
     * Neither should be obtainable by holding a SIM.
     */
    @Transactional
    public AuthResponse loginWithPhoneOtp(PhoneOtpVerifyRequest request) {
        String e164 = toLookupKey(request.phone());

        // One message for a wrong code, an expired one, one already used, one
        // burnt by too many guesses, and a number that was never sent anything.
        // Same rule as "Invalid email or password": each of those, told apart,
        // is a hint about what to change next. Not a BadCredentialsException,
        // because that handler replaces the message with the password-login one.
        if (!phoneOtpService.consume(e164, request.code())) {
            throw new InvalidOtpException("That code is not valid. Please request a new one.");
        }

        Patient patient = patientRepository.findByPhoneE164(e164)
                .orElseGet(() -> registerPhonePatient(e164, request.phone()));

        if (patient.getStatus() != AccountStatus.ACTIVE) {
            throw new DisabledException("Your account is inactive. Please contact support.");
        }

        return issueTokens(
                String.valueOf(patient.getId()), patient.getEmail(),
                patient.getFullName(), UserType.PATIENT, patientUser(patient));
    }

    /**
     * The verified-number account: no password, no email, a placeholder name.
     *
     * <p>Exactly the half-built state V3 introduced for Google users rather than
     * a second one - {@code auth_provider} says how they got in and
     * {@code isProfileComplete()} says what is still missing.
     */
    private Patient registerPhonePatient(String e164, String asTyped) {
        return patientRepository.save(Patient.builder()
                .fullName(NEW_PHONE_PATIENT_NAME)
                .email(null)
                .passwordHash(null)
                .authProvider(Patient.AuthProvider.PHONE)
                // Both forms: one to show them, one to find them by.
                .phone(asTyped.trim())
                .phoneE164(e164)
                .status(AccountStatus.ACTIVE)
                .build());
    }

    /**
     * Rejects here rather than letting a null reach the lookup, which would
     * quietly match nothing and read as a wrong code.
     */
    private String toLookupKey(String phone) {
        String e164 = PhoneNumbers.toE164(phone);
        if (e164 == null) {
            throw new BadRequestException("Enter a valid mobile number");
        }
        return e164;
    }

    // ------------------------------------------------------------- register

    @Transactional
    public AuthResponse registerPatient(PatientRegisterRequest request) {
        if (patientRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("An account with this email already exists");
        }

        Patient patient = Patient.builder()
                .fullName(request.fullName())
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                // Populated on every path that writes a phone, so an
                // email-registered patient can still sign in by code later
                // instead of auto-registering a second account on one number.
                .phoneE164(PhoneNumbers.toE164(request.phone()))
                .anotherNumber(blankToNull(request.anotherNumber()))
                .address(blankToNull(request.address()))
                .dateOfBirth(request.dateOfBirth())
                .gender(Patient.Gender.valueOf(request.gender()))
                .bloodGroup(request.bloodGroup())
                .reasonOfConsult(blankToNull(request.reasonOfConsult()))
                .status(AccountStatus.ACTIVE)
                .build();

        patient = patientRepository.save(patient);

        return issueTokens(
                String.valueOf(patient.getId()), patient.getEmail(),
                patient.getFullName(), UserType.PATIENT, patientUser(patient));
    }

    /**
     * Doctors land in PENDING and cannot log in until an admin approves them,
     * which is what the admin dashboard's "Doctor account approved" activity
     * entry refers to.
     */
    @Transactional
    public AuthResponse registerDoctor(DoctorRegisterRequest request) {
        if (doctorRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("An account with this email already exists");
        }
        if (doctorRepository.existsByLicenseNumberIgnoreCase(request.licenseNumber())) {
            throw new ConflictException("This license number is already registered");
        }

        Specialization specialization = specializationRepository
                .findByNameIgnoreCase(request.specialization())
                .orElseThrow(() -> new ConflictException(
                        "Unknown specialization: " + request.specialization()));

        Doctor doctor = Doctor.builder()
                .fullName(request.fullName())
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .specialization(specialization)
                .licenseNumber(request.licenseNumber())
                .experienceYears(request.experienceYears())
                .consultationFee(request.consultationFee())
                .consultationDurationMin(request.consultationDurationMin())
                .bio(blankToNull(request.bio()))
                .status(AccountStatus.PENDING)
                .build();

        doctor = doctorRepository.save(doctor);

        // No tokens: a pending doctor has nothing to access yet. The frontend
        // shows the message and keeps them on the login screen.
        return new AuthResponse(null, null, new AuthResponse.User(
                doctor.getId(), doctor.getFullName(), doctor.getEmail(), "doctor",
                specialization.getName(), null, doctor.getConsultationFee(), "pending",
                null, null));
    }

    // -------------------------------------------------------------- refresh

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String hash = jwtService.hashRefreshToken(request.refreshToken());

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (!stored.isUsable()) {
            throw new BadCredentialsException("Refresh token has expired. Please log in again.");
        }

        // Rotate: the presented token is burned so a stolen copy cannot be reused.
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return switch (stored.getUserType()) {
            case PATIENT -> {
                Patient p = patientRepository.findById(Integer.valueOf(stored.getUserId()))
                        .orElseThrow(() -> new BadCredentialsException("Account no longer exists"));
                yield issueTokens(String.valueOf(p.getId()), p.getEmail(), p.getFullName(),
                        UserType.PATIENT, patientUser(p));
            }
            case DOCTOR -> {
                Doctor d = doctorRepository.findById(stored.getUserId())
                        .orElseThrow(() -> new BadCredentialsException("Account no longer exists"));
                yield issueTokens(d.getId(), d.getEmail(), d.getFullName(), UserType.DOCTOR,
                        new AuthResponse.User(d.getId(), d.getFullName(), d.getEmail(), "doctor",
                                d.getSpecialization().getName(), null, d.getConsultationFee(),
                                d.getStatus().getDbValue(), null, null));
            }
            case ADMIN -> {
                Admin a = adminRepository.findById(Integer.valueOf(stored.getUserId()))
                        .orElseThrow(() -> new BadCredentialsException("Account no longer exists"));
                yield issueTokens(String.valueOf(a.getId()), a.getEmail(), a.getFullName(),
                        UserType.ADMIN,
                        new AuthResponse.User(String.valueOf(a.getId()), a.getFullName(),
                                a.getEmail(), "admin", null, a.getTitle(), null,
                                a.getStatus().getDbValue(), null, null));
            }
        };
    }

    /**
     * Revokes every refresh token for the user, so logout actually ends the
     * session rather than just clearing localStorage. The current access token
     * still works until it expires (max 15 minutes).
     */
    @Transactional
    public void logout(UserType type, String userId, String fullName) {
        refreshTokenRepository.revokeAllForUser(type, userId);
        events.publishEvent(new AuthEventListener.LogoutEvent(userId, fullName, type));
    }

    // --------------------------------------------------------------- helper

    private AuthResponse issueTokens(String userId, String email, String fullName,
                                     UserType type, AuthResponse.User user) {

        events.publishEvent(new AuthEventListener.LoginEvent(userId, fullName, type));

        String accessToken = jwtService.generateAccessToken(userId, email, fullName, type);
        String rawRefresh = jwtService.generateRefreshToken();

        refreshTokenRepository.save(RefreshToken.builder()
                .tokenHash(jwtService.hashRefreshToken(rawRefresh))
                .userType(type)
                .userId(userId)
                .expiresAt(LocalDateTime.now().plusDays(jwtService.getRefreshTokenDays()))
                .revoked(false)
                .build());

        return new AuthResponse(accessToken, rawRefresh, user);
    }

    /**
     * One place patients turn into a response body. There are four paths in and
     * they were drifting - registration reported a hardcoded "active" while the
     * others read the column.
     */
    private AuthResponse.User patientUser(Patient p) {
        return new AuthResponse.User(
                String.valueOf(p.getId()), p.getFullName(), p.getEmail(), "patient",
                null, null, null, p.getStatus().getDbValue(),
                p.getPhone(), p.isProfileComplete());
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
