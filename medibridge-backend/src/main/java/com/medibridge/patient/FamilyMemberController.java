package com.medibridge.patient;

import com.medibridge.common.security.CurrentUser;
import com.medibridge.common.security.SecurityUser;
import com.medibridge.patient.dto.FamilyMemberRequest;
import com.medibridge.patient.dto.FamilyMemberResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The owner is always the caller. No path variable carries a patient id, exactly
 * as in {@link PatientController} - {@code familyMemberId} in the path is
 * meaningless without the token's owner id, since every lookup is by the pair.
 */
@RestController
@RequestMapping("/patient/family")
@PreAuthorize("hasRole('PATIENT')")
@RequiredArgsConstructor
public class FamilyMemberController {

    private final FamilyMemberService familyMemberService;

    @GetMapping
    public List<FamilyMemberResponse> list(@CurrentUser SecurityUser me) {
        return familyMemberService.list(me.idAsInt());
    }

    @PostMapping
    public FamilyMemberResponse create(@CurrentUser SecurityUser me,
                                       @Valid @RequestBody FamilyMemberRequest request) {
        return familyMemberService.create(me.idAsInt(), request);
    }

    @PutMapping("/{familyMemberId}")
    public FamilyMemberResponse update(@CurrentUser SecurityUser me,
                                       @PathVariable Integer familyMemberId,
                                       @Valid @RequestBody FamilyMemberRequest request) {
        return familyMemberService.update(me.idAsInt(), familyMemberId, request);
    }

    @DeleteMapping("/{familyMemberId}")
    public ResponseEntity<Void> archive(@CurrentUser SecurityUser me,
                                        @PathVariable Integer familyMemberId) {
        familyMemberService.archive(me.idAsInt(), familyMemberId);
        return ResponseEntity.noContent().build();
    }
}
