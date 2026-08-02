package com.medibridge.record;

import com.medibridge.common.security.CurrentUser;
import com.medibridge.common.security.SecurityUser;
import com.medibridge.record.dto.RecordResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    /**
     * @param subject omit for everyone on the account, {@code self} for the
     *                holder alone, or a family member id. Whichever is given, the
     *                account is always the caller's own.
     */
    @GetMapping
    @PreAuthorize("hasRole('PATIENT')")
    public List<RecordResponse> myRecords(@CurrentUser SecurityUser me,
                                          @RequestParam(required = false) String subject) {
        return recordService.listForSubject(me.idAsInt(), subject);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PATIENT')")
    public RecordResponse upload(@CurrentUser SecurityUser me,
                                 @RequestPart("file") MultipartFile file,
                                 @RequestParam("report_name") String reportName,
                                 @RequestParam(value = "report_type", required = false) String reportType,
                                 @RequestParam(value = "family_member_id", required = false)
                                 Integer familyMemberId) {
        return recordService.upload(me.idAsInt(), familyMemberId, file, reportName, reportType);
    }

    @GetMapping("/{reportId}/download")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    public ResponseEntity<byte[]> download(@CurrentUser SecurityUser me,
                                           @PathVariable Integer reportId) {

        RecordService.DownloadedFile file = switch (me.getUserType()) {
            case PATIENT -> recordService.downloadAsPatient(me.idAsInt(), reportId);
            case DOCTOR -> recordService.downloadAsDoctor(me.getId(), reportId);
            default -> throw new IllegalStateException("Unsupported role");
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + safeName(file.filename()) + "\"")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    @DeleteMapping("/{reportId}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Void> delete(@CurrentUser SecurityUser me,
                                       @PathVariable Integer reportId) {
        recordService.delete(me.idAsInt(), reportId);
        return ResponseEntity.noContent().build();
    }

    /** Strips anything that could break out of the Content-Disposition header. */
    private String safeName(String name) {
        String cleaned = name.replaceAll("[\"\\r\\n\\\\]", "_");
        return URLEncoder.encode(cleaned, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
