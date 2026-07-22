package com.medibridge.record.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Matches what MedicalRecords.jsx renders: report_name, report_type, upload_date, size. */
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
        String downloadUrl
) {
}
