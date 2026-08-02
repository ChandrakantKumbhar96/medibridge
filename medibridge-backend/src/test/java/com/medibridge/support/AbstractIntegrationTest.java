package com.medibridge.support;

import com.medibridge.appointment.AppointmentRepository;
import com.medibridge.appointment.AppointmentScheduler;
import com.medibridge.appointment.AppointmentService;
import com.medibridge.appointment.dto.BookAppointmentRequest;
import com.medibridge.appointment.entity.Appointment;
import com.medibridge.common.enums.AccountStatus;
import com.medibridge.common.enums.UserType;
import com.medibridge.common.security.SecurityUser;
import com.medibridge.common.util.PhoneNumbers;
import com.medibridge.doctor.DoctorRepository;
import com.medibridge.doctor.DoctorScheduleRepository;
import com.medibridge.doctor.SpecializationRepository;
import com.medibridge.doctor.entity.Doctor;
import com.medibridge.doctor.entity.DoctorSchedule;
import com.medibridge.patient.FamilyMemberRepository;
import com.medibridge.patient.FamilyMemberService;
import com.medibridge.patient.PatientRepository;
import com.medibridge.patient.dto.FamilyMemberRequest;
import com.medibridge.patient.entity.FamilyMember;
import com.medibridge.patient.entity.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Shared setup for the integration suite.
 *
 * <p>Tests run against a real MySQL schema built by the real Flyway migrations.
 * That is deliberate rather than convenient: the guarantees under test are
 * database guarantees - a UNIQUE index, a CHECK constraint, row locking - and
 * an in-memory database would quietly not have them, leaving a green suite that
 * proves nothing.
 *
 * <p>Fixtures are created fresh with unique identifiers per test rather than
 * rolled back. The concurrency test needs real commits from several threads, so
 * a transactional test wrapper would defeat it; making every test additive
 * keeps one strategy for the whole suite.
 */
@SpringBootTest
@ActiveProfiles({"local", "test"})
public abstract class AbstractIntegrationTest {

    @Autowired protected AppointmentService appointmentService;
    @Autowired protected AppointmentRepository appointmentRepository;
    @Autowired protected DoctorRepository doctorRepository;
    @Autowired protected DoctorScheduleRepository scheduleRepository;
    @Autowired protected PatientRepository patientRepository;
    @Autowired protected SpecializationRepository specializationRepository;
    @Autowired protected FamilyMemberService familyMemberService;
    @Autowired protected FamilyMemberRepository familyMemberRepository;

    /**
     * The background sweeps are replaced for the whole suite.
     *
     * <p>They fire on a timer, and a slow run would let the hold-expiry or
     * no-show job mutate a fixture between arrange and assert - an intermittent
     * failure with no obvious cause. The jobs are not skipped as a result: the
     * service methods they call are invoked directly by the tests that cover
     * them, which is also faster than waiting for a schedule.
     */
    @MockitoBean
    protected AppointmentScheduler scheduler;

    // ------------------------------------------------------------- fixtures

    protected Patient newPatient() {
        Patient p = new Patient();
        p.setFullName("Test Patient");
        p.setEmail("patient-" + UUID.randomUUID() + "@test.local");
        p.setPasswordHash("$2a$10$notarealhashnotarealhashnotarealhashnotarealhash00");
        p.setPhone(uniquePhone());
        p.setPhoneE164(PhoneNumbers.toE164(p.getPhone()));
        p.setAuthProvider(Patient.AuthProvider.LOCAL);
        p.setStatus(AccountStatus.ACTIVE);
        return patientRepository.save(p);
    }

    /**
     * A phone number no other fixture holds.
     *
     * <p>Every fixture used to share "9000000000", which was harmless while
     * nothing was unique about a phone. V14 put a UNIQUE index on the
     * normalised form, so a constant here fails the second save - the same
     * reason email and licence number have been unique per fixture all along.
     *
     * <p>Random rather than a counter because fixtures are additive and never
     * rolled back: a counter restarts at 1 on the next run and collides with
     * everything the last one left behind.
     */
    protected static String uniquePhone() {
        long suffix = Math.floorMod(UUID.randomUUID().getMostSignificantBits(),
                100_000_000_000L);
        return String.format("9%011d", suffix);
    }

    protected Doctor newDoctor() {
        return newDoctor(new BigDecimal("500.00"));
    }

    protected Doctor newDoctor(BigDecimal fee) {
        Doctor d = new Doctor();
        d.setFullName("Dr Test");
        d.setEmail("doctor-" + UUID.randomUUID() + "@test.local");
        d.setPasswordHash("$2a$10$notarealhashnotarealhashnotarealhashnotarealhash00");
        d.setPhone(uniquePhone());
        d.setSpecialization(specializationRepository.findAll().getFirst());
        d.setLicenseNumber("LIC-" + UUID.randomUUID());
        d.setExperienceYears(5);
        d.setConsultationFee(fee);
        d.setConsultationDurationMin(30);
        // Must be ACTIVE: book() refuses any other status.
        d.setStatus(AccountStatus.ACTIVE);
        return doctorRepository.save(d);
    }

    protected DoctorSchedule newSlot(Doctor doctor, LocalDateTime when) {
        DoctorSchedule s = new DoctorSchedule();
        s.setDoctor(doctor);
        s.setAvailableDate(when.toLocalDate());
        s.setStartTime(when.toLocalTime());
        s.setEndTime(when.toLocalTime().plusMinutes(30));
        s.setIsBooked(false);
        return scheduleRepository.save(s);
    }

    /** A slot far enough ahead that no notice-period rule is triggered. */
    protected DoctorSchedule slotInDays(Doctor doctor, int days) {
        return newSlot(doctor, LocalDateTime.now().plusDays(days)
                .withHour(10).withMinute(0).withSecond(0).withNano(0));
    }

    /**
     * Books and confirms, i.e. an appointment that has been paid for. Goes
     * through the real service rather than inserting a row, so the fixture
     * cannot drift from what booking actually produces.
     */
    protected Appointment paidAppointment(Patient patient, DoctorSchedule slot) {
        return paidAppointment(patient, null, slot);
    }

    /** @param dependent null books for the account holder themselves. */
    protected Appointment paidAppointment(Patient patient, FamilyMember dependent,
                                          DoctorSchedule slot) {
        var booked = appointmentService.book(patient.getId(), new BookAppointmentRequest(
                slot.getDoctor().getId(), slot.getId(), "Consultation", "Test booking",
                dependent == null ? null : dependent.getId()));
        appointmentService.markPaidAndConfirm(booked.appointmentId());
        return appointmentRepository.findById(booked.appointmentId()).orElseThrow();
    }

    /** A dependent on {@code owner}'s account, created through the real service. */
    protected FamilyMember newDependent(Patient owner) {
        return newDependent(owner, "Child Test", FamilyMember.Relation.Child, 8);
    }

    protected FamilyMember newDependent(Patient owner, String name,
                                        FamilyMember.Relation relation, int ageYears) {
        var created = familyMemberService.create(owner.getId(), new FamilyMemberRequest(
                name, LocalDate.now().minusYears(ageYears), Patient.Gender.Male,
                relation, "O+", null));
        return familyMemberRepository.findByIdAndOwnerId(
                created.familyMemberId(), owner.getId()).orElseThrow();
    }

    // --------------------------------------------------------------- auth

    protected SecurityUser asPatient(Patient p) {
        return new SecurityUser(String.valueOf(p.getId()), p.getEmail(),
                p.getFullName(), UserType.PATIENT);
    }

    protected SecurityUser asDoctor(Doctor d) {
        return new SecurityUser(d.getId(), d.getEmail(), d.getFullName(), UserType.DOCTOR);
    }
}
