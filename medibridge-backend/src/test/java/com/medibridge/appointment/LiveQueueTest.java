package com.medibridge.appointment;

import com.medibridge.appointment.entity.Appointment;
import com.medibridge.common.enums.AppointmentStatus;
import com.medibridge.doctor.entity.Doctor;
import com.medibridge.doctor.entity.DoctorSchedule;
import com.medibridge.patient.entity.Patient;
import com.medibridge.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The live queue and the running-late estimate.
 *
 * <p>The delay is a claim the platform makes to a patient who is sitting there
 * waiting, so what it is derived from matters as much as the arithmetic: only
 * observed join timestamps, never a status anyone set by hand. These tests pin
 * the derivation, not just the number.
 *
 * <p><b>The clock is pinned to 09:35 today.</b> Every branch keys off whether a
 * slot falls before or after now, so a scenario built from real wall-clock time
 * would be testing something different depending on when the suite ran - and
 * near midnight could not be expressed at all, because the queue is scoped to a
 * single calendar day. The date stays real, so the day-scoping is still
 * exercised against rows the repository genuinely returns.
 *
 * <p>Doctors are created with a 30-minute consultation length, which is what
 * makes 09:00/09:30/10:00 a back-to-back run with no slack to absorb a delay.
 */
class LiveQueueTest extends AbstractIntegrationTest {

    @Autowired LiveQueueService liveQueue;

    @MockitoBean Clock clock;

    @BeforeEach
    void pinTheClock() {
        ZoneId zone = ZoneId.systemDefault();
        Mockito.when(clock.getZone()).thenReturn(zone);
        Mockito.when(clock.instant()).thenReturn(at(9, 35).atZone(zone).toInstant());
    }

    /**
     * The headline case, and the one the patient card renders verbatim: a
     * 25-minute overrun on a full morning, seen by the patient sitting third in
     * line - "You are #3 · running ~25 min late".
     */
    @Test
    @DisplayName("a late start pushes a back-to-back queue back by the same amount")
    void slippageCascadesThroughAFullSchedule() {
        Doctor doctor = newDoctor();

        // 09:00 began at 09:25 with both parties in the room - an observed
        // start, not a guess, and the only kind the estimate will accept.
        Appointment first = todaysAppointment(doctor, at(9, 0));
        attended(first, at(9, 25), at(9, 24));

        Appointment second = todaysAppointment(doctor, at(9, 30));
        Appointment third = todaysAppointment(doctor, at(10, 0));

        Map<Integer, LiveQueueService.QueueView> queue = queue(doctor);

        assertThat(queue.get(first.getId()).position()).isEqualTo(1);
        assertThat(queue.get(second.getId()).position()).isEqualTo(2);
        assertThat(queue.get(third.getId()).position()).isEqualTo(3);

        assertThat(queue.get(third.getId()).delayMinutes())
                .as("09:25 + two 30-minute consultations puts 10:00 at 10:25")
                .isEqualTo(25);
        assertThat(queue.get(third.getId()).etaMinutes())
                .as("09:35 to 10:25")
                .isEqualTo(50);

        assertThat(queue.get(first.getId()).etaMinutes())
                .as("the consultation in the room is not a wait")
                .isZero();
    }

    /**
     * The case a flat doctor-wide delay gets wrong. A gap in the schedule is
     * time the doctor recovers in, so an overrun before it must not be pinned
     * on a patient booked after it.
     */
    @Test
    @DisplayName("a gap in the schedule absorbs the delay")
    void slackRecoversTheSchedule() {
        Doctor doctor = newDoctor();

        Appointment first = todaysAppointment(doctor, at(9, 0));
        attended(first, at(9, 25), at(9, 24));

        // 09:25 + 30 minutes frees the doctor at 09:55, well before this slot.
        Appointment afterTheGap = todaysAppointment(doctor, at(11, 0));

        LiveQueueService.QueueView view = queue(doctor).get(afterTheGap.getId());

        assertThat(view.delayMinutes())
                .as("the doctor is free at 09:55; 11:00 is unaffected")
                .isZero();
        assertThat(view.etaMinutes()).isEqualTo(85);
    }

    /**
     * The signal that has to keep moving. If nobody has opened a room whose time
     * has passed, the estimate cannot sit on the last consultation that
     * finished - that is the exact moment the waiting patient is staring at it.
     */
    @Test
    @DisplayName("an unopened overdue slot grows the delay in real time")
    void overdueSlotDrivesTheEstimate() {
        Doctor doctor = newDoctor();

        Appointment overdue = todaysAppointment(doctor, at(9, 0));    // 35 min ago
        Appointment waiting = todaysAppointment(doctor, at(10, 0));

        Map<Integer, LiveQueueService.QueueView> queue = queue(doctor);

        assertThat(queue.get(overdue.getId()).delayMinutes())
                .as("nobody has opened the 09:00 room and it is now 09:35")
                .isEqualTo(35);
        assertThat(queue.get(overdue.getId()).etaMinutes())
                .as("it cannot start earlier than now")
                .isZero();

        assertThat(queue.get(waiting.getId()).delayMinutes())
                .as("09:35 + 30 minutes leaves the doctor free at 10:05")
                .isEqualTo(5);
    }

    /**
     * A doctor sitting in an empty room is not late. Only both parties present
     * counts as a start, and until then nothing has been observed to measure.
     */
    @Test
    @DisplayName("a doctor alone in the room has not started the consultation")
    void aDoctorWaitingAloneIsNotAStart() {
        Doctor doctor = newDoctor();

        Appointment waitingRoom = todaysAppointment(doctor, at(9, 0));
        waitingRoom.setDoctorJoinedAt(at(8, 58));
        appointmentRepository.save(waitingRoom);

        assertThat(queue(doctor).get(waitingRoom.getId()).delayMinutes())
                .as("the doctor arrived early, but nothing has begun - so this "
                        + "falls through to the overdue branch, not to an "
                        + "observed start two minutes early")
                .isEqualTo(35);
    }

    /** Under the floor there is no story worth telling. */
    @Test
    @DisplayName("a few minutes behind is on time")
    void smallSlippageIsNotReported() {
        Doctor doctor = newDoctor();

        Appointment first = todaysAppointment(doctor, at(9, 30));
        attended(first, at(9, 33), at(9, 33));

        assertThat(queue(doctor).get(first.getId()).delayMinutes()).isZero();
    }

    /**
     * Positions have to shift down as the day is worked through. Otherwise the
     * patient told #3 is still told #3 after two people have been seen, and the
     * number stops meaning anything.
     */
    @Test
    @DisplayName("a completed consultation leaves the queue")
    void finishedConsultationsFreeTheirPosition() {
        Doctor doctor = newDoctor();

        Appointment done = todaysAppointment(doctor, at(9, 0));
        attended(done, at(9, 1), at(9, 1));
        Appointment next = todaysAppointment(doctor, at(10, 0));

        assertThat(queue(doctor).get(next.getId()).position()).isEqualTo(2);

        done.setStatus(AppointmentStatus.COMPLETED);
        done.setCompletedAt(at(9, 30));
        appointmentRepository.save(done);

        Map<Integer, LiveQueueService.QueueView> after = queue(doctor);
        assertThat(after).doesNotContainKey(done.getId());
        assertThat(after.get(next.getId()).position()).isEqualTo(1);
    }

    /**
     * A queue is a today thing. A position dated next week is wrong the moment
     * anyone else books, and there is nothing the patient could do with it.
     */
    @Test
    @DisplayName("appointments on other days get no queue standing")
    void onlyTodayIsQueued() {
        Doctor doctor = newDoctor();
        Appointment tomorrow = paidAppointment(newPatient(), slotInDays(doctor, 1));

        assertThat(liveQueue.viewsFor(List.of(tomorrow)))
                .as("a queue position dated tomorrow is noise")
                .isEmpty();
    }

    // ---------------------------------------------------------------- setup

    private Map<Integer, LiveQueueService.QueueView> queue(Doctor doctor) {
        return liveQueue.queueFor(doctor.getId(), LocalDate.now());
    }

    /**
     * A paid appointment sitting at a given time today.
     *
     * <p>Booking refuses a slot in the past, so it is booked in the future and
     * then moved - the same approach {@code NoShowSettlementTest} takes, and for
     * the same reason: the alternative is a test that sleeps.
     *
     * <p>The source slot borrows the target's time-of-day rather than using
     * {@code slotInDays}, which always produces 10:00. These tests put several
     * appointments on one doctor, and a fixed hour collides with
     * {@code uq_doctor_slot} on the second one.
     */
    private Appointment todaysAppointment(Doctor doctor, LocalDateTime when) {
        Patient patient = newPatient();
        DoctorSchedule slot = newSlot(doctor,
                LocalDate.now().plusDays(2).atTime(when.toLocalTime()));
        Appointment a = paidAppointment(patient, slot);

        a.setAppointmentDate(when);
        a.setMeetingJoinFrom(when.minusMinutes(15));
        a.setMeetingValidUntil(when.plusMinutes(60));
        return appointmentRepository.save(a);
    }

    /** Stamps the joins exactly as the join endpoint would have. */
    private void attended(Appointment a, LocalDateTime patientJoined,
                          LocalDateTime doctorJoined) {
        a.setPatientJoinedAt(patientJoined);
        a.setDoctorJoinedAt(doctorJoined);
        appointmentRepository.save(a);
    }

    private LocalDateTime at(int hour, int minute) {
        return LocalDate.now().atTime(hour, minute);
    }
}
