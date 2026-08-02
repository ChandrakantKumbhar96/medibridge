package com.medibridge.record.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Matches what MedicalRecords.jsx renders: report_name, report_type, upload_date, size. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecordResponse(

        @JsonProperty("report_id")
        Integer reportId,

        @JsonProperty("report_name")
        String reportName,

        @JsonProperty("report_type")
        String reportType,

        @JsonProperty("upload_date")
        String uploadDate,

        /** Pre-formatted for display, e.g. "2.4 MB". */
        String size,

        @JsonProperty("uploaded_by")
        String uploadedBy,

        @JsonProperty("download_url")
        String downloadUrl,

        /** Null when the document belongs to the account holder themselves. */
        @JsonProperty("family_member_id")
        Integer familyMemberId,

        /**
         * Whose document this is, already resolved to a name so the list can be
         * grouped without a second lookup per row.
         */
        @JsonProperty("patient_name")
        String patientName,

        /** Child, Spouse, Parent, Sibling, Other - null for the holder's own. */
        String relation
) {
}
