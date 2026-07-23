package com.medibridge.payout;

import com.medibridge.common.security.CurrentUser;
import com.medibridge.common.security.SecurityUser;
import com.medibridge.payout.dto.EarningResponse;
import com.medibridge.payout.dto.PayoutResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Doctor earnings and platform settlement.
 *
 * <p>Split by audience: a doctor sees only their own ledger (id taken from the
 * token, never a path variable), while settlement runs are admin-only - a
 * doctor must not be able to trigger their own payment.
 */
@RestController
@RequiredArgsConstructor
public class PayoutController {

    private final PayoutService payoutService;

    // -------------------------------------------------------------- doctor

    @GetMapping("/doctor/earnings/summary")
    @PreAuthorize("hasRole('DOCTOR')")
    public Map<String, Object> mySummary(@CurrentUser SecurityUser me) {
        return payoutService.getEarningsSummary(me.getId());
    }

    @GetMapping("/doctor/earnings")
    @PreAuthorize("hasRole('DOCTOR')")
    public List<EarningResponse> myEarnings(@CurrentUser SecurityUser me) {
        return payoutService.getEarnings(me.getId());
    }

    @GetMapping("/doctor/payouts")
    @PreAuthorize("hasRole('DOCTOR')")
    public List<PayoutResponse> myPayouts(@CurrentUser SecurityUser me) {
        return payoutService.getPayoutsForDoctor(me.getId());
    }

    // --------------------------------------------------------------- admin

    @GetMapping("/admin/payouts")
    @PreAuthorize("hasRole('ADMIN')")
    public List<PayoutResponse> allPayouts() {
        return payoutService.getAllPayouts();
    }

    @GetMapping("/admin/payouts/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> platformSummary() {
        return payoutService.getPlatformSummary();
    }

    /**
     * Creates payout batches for every doctor with unsettled earnings.
     *
     * <p>Defaults to the last {@code payout_cycle_days}. Safe to re-run: the
     * unique key on (doctor, period) means a second run for the same window
     * cannot pay anyone twice.
     */
    @PostMapping("/admin/payouts/run")
    @PreAuthorize("hasRole('ADMIN')")
    public List<PayoutResponse> runSettlement(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate periodEnd = to == null ? LocalDate.now() : to;
        LocalDate periodStart = from == null ? periodEnd.minusDays(30) : from;

        return payoutService.runSettlementForAll(periodStart, periodEnd);
    }

    /** Records that the bank transfer actually happened. */
    @PatchMapping("/admin/payouts/{payoutId}/paid")
    @PreAuthorize("hasRole('ADMIN')")
    public PayoutResponse markPaid(@PathVariable Integer payoutId,
                                   @RequestBody Map<String, String> body) {
        return payoutService.markPaid(payoutId, body.get("reference"), body.get("notes"));
    }
}
