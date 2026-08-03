package com.medibridge.opinion;

import com.medibridge.common.security.CurrentUser;
import com.medibridge.common.security.SecurityUser;
import com.medibridge.opinion.dto.OpinionRequest;
import com.medibridge.opinion.dto.OpinionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Second Opinions", description = "Specialist review of an existing case")
public class OpinionController {

    private final OpinionService opinionService;

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Issue a second opinion", description = "Doctor writes up their review of an existing case.")
    public OpinionResponse create(@CurrentUser SecurityUser me,
                                  @Valid @RequestBody OpinionRequest request) {
        return opinionService.create(me.getId(), request);
    }

    @GetMapping
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "List my second opinions")
    public List<OpinionResponse> mine(@CurrentUser SecurityUser me) {
        return opinionService.listForPatient(me.idAsInt());
    }

    @GetMapping("/{opinionId}/pdf")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    @Operation(summary = "Download a second opinion PDF", description = "Patient gets their own, doctor gets ones they issued.")
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
