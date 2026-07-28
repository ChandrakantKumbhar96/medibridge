package com.medibridge.appointment;

import com.medibridge.appointment.dto.BookAppointmentRequest;
import com.medibridge.doctor.entity.Doctor;
import com.medibridge.doctor.entity.DoctorSchedule;
import com.medibridge.patient.entity.Patient;
import com.medibridge.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The double-booking guarantee.
 *
 * <p>This is the test the whole slot design exists for. {@code is_booked} is a
 * read cache and cannot be the guard: every thread can read {@code false}
 * before any of them writes. What actually holds the line is the UNIQUE
 * constraint on {@code appointment.schedule_id}, backed by a row lock taken in
 * {@code book()}.
 *
 * <p>Twenty patients are used rather than one, because one patient booking the
 * same slot twenty times could be stopped by an ownership check and would prove
 * nothing about concurrency.
 */
class ConcurrentBookingTest extends AbstractIntegrationTest {

    private static final int THREADS = 20;

    @Test
    @DisplayName("20 patients booking the same slot: exactly one succeeds")
    void onlyOneBookingSurvivesForASingleSlot() throws Exception {
        Doctor doctor = newDoctor();
        DoctorSchedule slot = slotInDays(doctor, 3);

        List<Patient> patients = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            patients.add(newPatient());
        }

        AtomicInteger booked = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Throwable> unexpected = new CopyOnWriteArrayList<>();

        // A latch rather than staggered starts: the threads must contend, and
        // starting them in a loop would let the first finish before the last
        // begins, which is exactly the race this test needs to provoke.
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(THREADS);

        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            for (Patient patient : patients) {
                pool.submit(() -> {
                    try {
                        startGun.await();
                        appointmentService.book(patient.getId(), new BookAppointmentRequest(
                                doctor.getId(), slot.getId(), "Consultation", "race"));
                        booked.incrementAndGet();
                    } catch (Exception e) {
                        // Either guard is a correct outcome: the pessimistic
                        // lock making the loser see is_booked=true, or the
                        // UNIQUE index rejecting the insert. Both surface as a
                        // clean domain conflict rather than a driver error,
                        // which is the behaviour being asserted.
                        if (isSlotTaken(e)) {
                            rejected.incrementAndGet();
                        } else {
                            unexpected.add(e);
                        }
                    } finally {
                        finished.countDown();
                    }
                });
            }

            startGun.countDown();
            assertThat(finished.await(60, TimeUnit.SECONDS))
                    .as("all booking attempts finished")
                    .isTrue();
        }

        assertThat(unexpected)
                .as("every rejection should be a clean conflict, not a leaked driver error")
                .isEmpty();
        assertThat(booked.get()).as("successful bookings").isEqualTo(1);
        assertThat(rejected.get()).as("rejected bookings").isEqualTo(THREADS - 1);

        // The database is the real assertion: one row for this slot, regardless
        // of what the application threads believed happened.
        assertThat(appointmentRepository.existsByScheduleId(slot.getId())).isTrue();
        assertThat(appointmentRepository.findAll().stream()
                .filter(a -> a.getSchedule() != null
                        && a.getSchedule().getId().equals(slot.getId()))
                .count())
                .as("appointments holding this slot")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("is_booked is only a cache: the constraint still refuses a second booking")
    void constraintHoldsEvenWhenTheCacheLies() {
        Doctor doctor = newDoctor();
        DoctorSchedule slot = slotInDays(doctor, 4);

        paidAppointment(newPatient(), slot);

        // Simulate the cache being wrong - a crashed sweep, a stale write, an
        // admin fixing something by hand. The booking must still be refused,
        // because correctness never depended on this flag.
        DoctorSchedule stale = scheduleRepository.findById(slot.getId()).orElseThrow();
        stale.setIsBooked(false);
        scheduleRepository.save(stale);

        Patient other = newPatient();
        Throwable thrown = catchBooking(other.getId(), doctor.getId(), slot.getId());

        assertThat(thrown).isNotNull();
        assertThat(isSlotTaken(thrown))
                .as("second booking refused despite is_booked=false, thrown: %s", thrown)
                .isTrue();
    }

    private Throwable catchBooking(Integer patientId, String doctorId, Integer scheduleId) {
        try {
            appointmentService.book(patientId, new BookAppointmentRequest(
                    doctorId, scheduleId, "Consultation", "second attempt"));
            return null;
        } catch (Exception e) {
            return e;
        }
    }

    /**
     * Matched on the message rather than the exception type so the test does
     * not care which of the two guards fired - only that the caller was told
     * the slot is gone.
     */
    private boolean isSlotTaken(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String message = t.getMessage();
            if (message != null && message.toLowerCase().contains("just been taken")) {
                return true;
            }
        }
        return false;
    }
}
