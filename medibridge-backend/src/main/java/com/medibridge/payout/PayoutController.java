package com.medibridge.payout;

import com.medibridge.common.security.CurrentUser;
import com.medibridge.common.security.SecurityUser;
import com.medibridge.payout.dto.EarningResponse;
import com.medibridge.payout.dto.PayoutResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Payouts", description = "Doctor earnings and platform settlement runs")
public class PayoutController {

    private final PayoutService payoutService;

    // -------------------------------------------------------------- doctor

    @GetMapping("/doctor/earnings/summary")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Get my earnings summary")
    public Map<String, Object> mySummary(@CurrentUser SecurityUser me) {
        return payoutService.getEarningsSummary(me.getId());
    }

    @GetMapping("/doctor/earnings")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "List my earnings")
    public List<EarningResponse> myEarnings(@CurrentUser SecurityUser me) {
        return payoutService.getEarnings(me.getId());
    }

    @GetMapping("/doctor/payouts")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "List my payouts")
    public List<PayoutResponse> myPayouts(@CurrentUser SecurityUser me) {
        return payoutService.getPayoutsForDoctor(me.getId());
    }

    // --------------------------------------------------------------- admin

    @GetMapping("/admin/payouts")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all payouts")
    public List<PayoutResponse> allPayouts() {
        return payoutService.getAllPayouts();
    }

    @GetMapping("/admin/payouts/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get platform settlement summary")
    public Map<String, Object> platformSummary() {
        return payoutService.getPlatformSummary();
    }

    @PostMapping("/admin/payouts/run")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Run a settlement batch", description = "Creates payout batches for every doctor with unsettled earnings for the given period, defaulting to the last payout_cycle_days. Safe to re-run: the unique key on (doctor, period) prevents double-paying.")
    public List<PayoutResponse> runSettlement(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate periodEnd = to == null ? LocalDate.now() : to;
        LocalDate periodStart = from == null ? periodEnd.minusDays(30) : from;

        return payoutService.runSettlementForAll(periodStart, periodEnd);
    }

    @PatchMapping("/admin/payouts/{payoutId}/paid")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mark a payout paid", description = "Records that the bank transfer actually happened.")
    public PayoutResponse markPaid(@PathVariable Integer payoutId,
                                   @RequestBody Map<String, String> body) {
        return payoutService.markPaid(payoutId, body.get("reference"), body.get("notes"));
    }
}
