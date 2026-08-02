package com.medibridge.patient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.medibridge.patient.entity.FamilyMember;
import com.medibridge.patient.entity.Patient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Note there is no owner field. The owning patient always comes from the token
 * via {@code @CurrentUser} - accepting it in the body would be handing the
 * caller the one value the whole authorization model rests on.
 */
public record FamilyMemberRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100)
        @JsonProperty("full_name")
        String fullName,

        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        @JsonProperty("date_of_birth")
        LocalDate dateOfBirth,

        @NotNull(message = "Gender is required")
        Patient.Gender gender,

        @NotNull(message = "Relationship is required")
        FamilyMember.Relation relation,

        @Size(max = 5)
        @JsonProperty("blood_group")
        String bloodGroup,

        @Size(max = 20)
        String phone
) {
}
