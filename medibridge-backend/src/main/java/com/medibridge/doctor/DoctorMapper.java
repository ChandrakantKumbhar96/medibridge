package com.medibridge.doctor;

import com.medibridge.common.enums.AccountStatus;
import com.medibridge.doctor.dto.DoctorResponse;
import com.medibridge.doctor.entity.Doctor;

import java.math.BigDecimal;

/**
 * Entity -> DTO. Static methods rather than MapStruct: with this few mappings a
 * codegen dependency costs more than it saves.
 */
public final class DoctorMapper {

    private DoctorMapper() {
    }

    public static DoctorResponse toDto(Doctor d, boolean hasUpcomingSlots) {
        return new DoctorResponse(
                d.getId(),
                d.getFullName(),
                d.getSpecialization().getName(),
                d.getEmail(),
                d.getPhone(),
                d.getLicenseNumber(),
                d.getExperienceYears(),
                d.getConsultationFee(),
                d.getConsultationDurationMin(),
                d.getBio(),
                d.getQualifications(),
                d.getLanguages(),
                d.getRatingAvg() == null ? 0.0 : d.getRatingAvg().doubleValue(),
                d.getRatingCount() == null ? 0 : d.getRatingCount(),
                d.getStatus() == AccountStatus.ACTIVE && hasUpcomingSlots,
                d.getStatus().getDbValue());
    }

    /** Listing variant - avoids a slot-count query per doctor. */
    public static DoctorResponse toDto(Doctor d) {
        return toDto(d, d.getStatus() == AccountStatus.ACTIVE);
    }

    public static BigDecimal feeOf(Doctor d) {
        return d.getConsultationFee() == null ? BigDecimal.ZERO : d.getConsultationFee();
    }
}
