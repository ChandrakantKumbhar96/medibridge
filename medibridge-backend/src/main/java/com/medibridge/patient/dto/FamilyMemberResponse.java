package com.medibridge.patient.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Snake_case to match the rest of the entity-shaped payloads. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FamilyMemberResponse(

        @JsonProperty("family_member_id")
        Integer familyMemberId,

        @JsonProperty("full_name")
        String fullName,

        @JsonProperty("date_of_birth")
        String dateOfBirth,

        /** Derived, so the booking dropdown can render "Aarav (8)" without maths. */
        Integer age,

        String gender,

        String relation,

        @JsonProperty("blood_group")
        String bloodGroup,

        String phone,

        /** Only sent when true - archived profiles are absent from the normal list. */
        Boolean archived
) {
}
