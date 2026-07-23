package com.medibridge.payout;

import com.medibridge.admin.SettingsProvider;
import com.medibridge.appointment.entity.Appointment;
import com.medibridge.common.exception.BadRequestException;
import com.medibridge.common.exception.ConflictException;
import com.medibridge.common.exception.ResourceNotFoundException;
import com.medibridge.doctor.DoctorRepository;
import com.medibridge.payout.dto.EarningResponse;
import com.medibridge.payout.dto.PayoutResponse;
import com.medibridge.payout.entity.DoctorEarning;
import com.medibridge.payout.entity.DoctorPayout;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Doctor earnings and settlement.
 *
 * <p>The money model:
 * <pre>
 *   patient pays    consultation fee + platform fee     e.g. 800 + 5 = 805
 *   platform keeps  platform fee + commission%          5 + 160 = 165
 *   doctor is owed  consultation fee - commission%      800 - 160 = 640
 * </pre>
 *
 * <p>Note the platform fee never enters the doctor's ledger - it is charged to
 * the patient for using the platform, not deducted from the doctor.
 *
 * <p>Earnings accrue per completed consultation and are paid in batches. Bank
 * transfers cost money and reconcile per-transfer, which is why real
 * marketplaces settle on a cycle rather than per transaction.
 */
@Service
@RequiredArgsConstructor
public class PayoutService {

    private static final Logger log = LoggerFactory.getLogger(PayoutService.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final DoctorEarningRepository earningRepository;
    private final DoctorPayoutRepository payoutRepository;
    private final DoctorRepository doctorRepository;
    private final SettingsProvider settings;

    // ------------------------------------------------------------- accrual

    /**
     * Records what a completed consultation earned.
     *
     * <p>REQUIRES_NEW so a ledger failure cannot roll back the clinical record
     * that triggered it - a prescription must not vanish because bookkeeping
     * hiccuped. The unique key on appointment_id makes a retry safe.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEarning(Appointment appointment) {
        if (appointment.getBookedFee() == null
                || appointment.getBookedFee().signum() <= 0) {
            return;   // nothing was charged, so nothing is owed
        }
        if (earningRepository.existsByAppointmentId(appointment.getId())) {
            return;   // already accrued
        }

        BigDecimal commissionPercent =
                BigDecimal.valueOf(settings.getInt("doctor_commission_percent", 20));

        try {
            DoctorEarning earning = earningRepository.save(
                    DoctorEarning.of(appointment, commissionPercent));

            log.info("Earning recorded: appointment {} -> doctor {} net Rs.{}",
                    appointment.getId(), appointment.getDoctor().getId(),
                    earning.getNetAmount());

        } catch (DataIntegrityViolationException e) {
            // Two completion paths raced; the unique key settled it.
            log.debug("Earning for appointment {} already exists", appointment.getId());
        }
    }

    /**
     * Reverses an earning when the consultation is refunded.
     *
     * <p>An already-settled earning is NOT silently un-paid: the money has left
     * the building. It is marked REVERSED and flagged for the admin, which is
     * how a real system handles a clawback.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reverseEarning(Integer appointmentId, String reason) {
        earningRepository.findByAppointmentId(appointmentId).ifPresent(earning -> {
            if (earning.getStatus() == DoctorEarning.Status.REVERSED) {
                return;
            }

            boolean alreadyPaid = earning.getStatus() == DoctorEarning.Status.SETTLED;

            earning.setStatus(DoctorEarning.Status.REVERSED);
            earning.setReversalReason(reason == null ? "Consultation refunded" : reason);
            earningRepository.save(earning);

            if (alreadyPaid) {
                log.warn("Earning {} was already settled in payout {} - "
                        + "Rs.{} must be recovered from the doctor's next cycle",
                        earning.getId(),
                        earning.getPayout() == null ? "?" : earning.getPayout().getId(),
                        earning.getNetAmount());
            } else {
                log.info("Earning {} reversed before settlement", earning.getId());
            }
        });
    }

    // ---------------------------------------------------------- settlement

    /**
     * Creates a payout batch for one doctor covering everything earned up to
     * {@code periodEnd}.
     *
     * <p>Totals are summed from the ledger rows being settled rather than
     * recomputed from appointments, so the batch can never disagree with the
     * rows it contains.
     */
    @Transactional
    public PayoutResponse runSettlement(String doctorId, LocalDate periodStart,
                                        LocalDate periodEnd) {
        var doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        if (periodEnd.isBefore(periodStart)) {
            throw new BadRequestException("Period end cannot be before period start");
        }

        payoutRepository.findByDoctorIdAndPeriodStartAndPeriodEnd(
                doctorId, periodStart, periodEnd).ifPresent(existing -> {
            throw new ConflictException(
                    "A payout for this doctor and period already exists (#"
                            + existing.getId() + ")");
        });

        List<DoctorEarning> earnings = earningRepository.findSettleable(
                doctorId, periodEnd.plusDays(1).atStartOfDay());

        if (earnings.isEmpty()) {
            throw new BadRequestException("No unsettled earnings for this doctor in that period");
        }

        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal commission = BigDecimal.ZERO;
        BigDecimal net = BigDecimal.ZERO;

        for (DoctorEarning e : earnings) {
            gross = gross.add(e.getGrossAmount());
            commission = commission.add(e.getCommissionAmount());
            net = net.add(e.getNetAmount());
        }

        DoctorPayout payout = payoutRepository.save(DoctorPayout.builder()
                .doctor(doctor)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .consultations(earnings.size())
                .grossAmount(gross)
                .commission(commission)
                .netAmount(net)
                .status(DoctorPayout.Status.PENDING)
                .build());

        LocalDateTime now = LocalDateTime.now();
        for (DoctorEarning e : earnings) {
            e.setStatus(DoctorEarning.Status.SETTLED);
            e.setPayout(payout);
            e.setSettledAt(now);
        }
        earningRepository.saveAll(earnings);

        log.info("Payout {} created for doctor {}: {} consultations, net Rs.{}",
                payout.getId(), doctorId, earnings.size(), net);

        return toDto(payout);
    }

    /** Settles every doctor with pending earnings. Backs the admin "Run payout" button. */
    @Transactional
    public List<PayoutResponse> runSettlementForAll(LocalDate periodStart, LocalDate periodEnd) {
        List<String> doctorIds = earningRepository.findDoctorIdsWithPendingEarnings(
                periodEnd.plusDays(1).atStartOfDay());

        List<PayoutResponse> created = new ArrayList<>();
        for (String doctorId : doctorIds) {
            try {
                created.add(runSettlement(doctorId, periodStart, periodEnd));
            } catch (ConflictException | BadRequestException e) {
                // Already settled, or nothing to settle - skip rather than abort
                // the whole run for the remaining doctors.
                log.debug("Skipping doctor {}: {}", doctorId, e.getMessage());
            }
        }
        return created;
    }

    /**
     * Marks a batch as actually transferred.
     *
     * <p>Separate from creating the batch because the bank transfer happens
     * outside this system: an admin makes the payment, then records the
     * reference here. Collapsing the two would mean the database claiming money
     * moved when it might not have.
     */
    @Transactional
    public PayoutResponse markPaid(Integer payoutId, String reference, String notes) {
        DoctorPayout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout not found"));

        if (payout.getStatus() == DoctorPayout.Status.PAID) {
            throw new ConflictException("This payout has already been marked paid");
        }
        if (reference == null || reference.isBlank()) {
            throw new BadRequestException("A payment reference is required");
        }

        payout.setStatus(DoctorPayout.Status.PAID);
        payout.setPayoutRef(reference.trim());
        payout.setNotes(notes);
        payout.setPaidAt(LocalDateTime.now());

        return toDto(payoutRepository.save(payout));
    }

    // --------------------------------------------------------------- views

    /** The doctor's own earnings summary. */
    @Transactional(readOnly = true)
    public Map<String, Object> getEarningsSummary(String doctorId) {
        BigDecimal pending = earningRepository.sumNetByDoctorAndStatus(
                doctorId, DoctorEarning.Status.PENDING);
        BigDecimal settled = earningRepository.sumNetByDoctorAndStatus(
                doctorId, DoctorEarning.Status.SETTLED);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("pendingAmount", pending);
        summary.put("paidAmount", settled);
        summary.put("lifetimeEarnings", pending.add(settled));
        summary.put("pendingConsultations",
                earningRepository.countByDoctorIdAndStatus(
                        doctorId, DoctorEarning.Status.PENDING));
        summary.put("commissionRate", settings.getInt("doctor_commission_percent", 20));
        summary.put("payoutCycleDays", settings.getInt("payout_cycle_days", 15));
        return summary;
    }

    @Transactional(readOnly = true)
    public List<EarningResponse> getEarnings(String doctorId) {
        return earningRepository.findByDoctorIdOrderByEarnedAtDesc(doctorId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PayoutResponse> getPayoutsForDoctor(String doctorId) {
        return payoutRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PayoutResponse> getAllPayouts() {
        return payoutRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toDto).toList();
    }

    /** Platform-side view: what has been earned, owed and paid out overall. */
    @Transactional(readOnly = true)
    public Map<String, Object> getPlatformSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalCommission", earningRepository.totalPlatformCommission());
        summary.put("pendingPayouts",
                payoutRepository.sumNetByStatus(DoctorPayout.Status.PENDING));
        summary.put("paidPayouts",
                payoutRepository.sumNetByStatus(DoctorPayout.Status.PAID));
        summary.put("commissionRate", settings.getInt("doctor_commission_percent", 20));
        return summary;
    }

    // ------------------------------------------------------------- mapping

    private EarningResponse toDto(DoctorEarning e) {
        return new EarningResponse(
                e.getId(),
                e.getAppointment().getId(),
                e.getAppointment().getPatient().getFullName(),
                e.getAppointment().getAppointmentDate().format(DATE),
                e.getGrossAmount(),
                e.getCommissionRate(),
                e.getCommissionAmount(),
                e.getNetAmount(),
                e.getStatus().name(),
                e.getPayout() == null ? null : e.getPayout().getId(),
                e.getEarnedAt() == null ? null : e.getEarnedAt().format(DATE));
    }

    private PayoutResponse toDto(DoctorPayout p) {
        return new PayoutResponse(
                p.getId(),
                p.getDoctor().getId(),
                p.getDoctor().getFullName(),
                p.getPeriodStart().format(DATE),
                p.getPeriodEnd().format(DATE),
                p.getConsultations(),
                p.getGrossAmount(),
                p.getCommission(),
                p.getNetAmount(),
                p.getStatus().name(),
                p.getPayoutRef(),
                p.getCreatedAt() == null ? null : p.getCreatedAt().format(DATE),
                p.getPaidAt() == null ? null : p.getPaidAt().format(DATE));
    }
}
