package com.medibridge.opinion;

import com.medibridge.common.security.CurrentUser;
import com.medibridge.common.security.SecurityUser;
import com.medibridge.opinion.dto.OpinionRequest;
import com.medibridge.opinion.dto.OpinionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/opinions")
@RequiredArgsConstructor
public class OpinionController {

    private final OpinionService opinionService;

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public OpinionResponse create(@CurrentUser SecurityUser me,
                                  @Valid @RequestBody OpinionRequest request) {
        return opinionService.create(me.getId(), request);
    }

    @GetMapping
    @PreAuthorize("hasRole('PATIENT')")
    public List<OpinionResponse> mine(@CurrentUser SecurityUser me) {
        return opinionService.listForPatient(me.idAsInt());
    }

    /** The document itself - patient gets their own, doctor gets ones they issued. */
    @GetMapping("/{opinionId}/pdf")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    public ResponseEntity<byte[]> pdf(@CurrentUser SecurityUser me,
                                      @PathVariable Integer opinionId) {
        byte[] pdf = switch (me.getUserType()) {
            case PATIENT -> opinionService.pdfForPatient(me.idAsInt(), opinionId);
            case DOCTOR -> opinionService.pdfForDoctor(me.getId(), opinionId);
            default -> throw new IllegalStateException("Unsupported role");
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"second-opinion-" + opinionId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}
