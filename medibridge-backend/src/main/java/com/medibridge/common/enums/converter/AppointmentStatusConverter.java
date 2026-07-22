package com.medibridge.common.enums.converter;

import com.medibridge.common.enums.AppointmentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;

@Converter(autoApply = true)
public class AppointmentStatusConverter implements AttributeConverter<AppointmentStatus, String> {

    @Override
    public String convertToDatabaseColumn(AppointmentStatus status) {
        return status == null ? null : status.getDbValue();
    }

    @Override
    public AppointmentStatus convertToEntityAttribute(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        return Arrays.stream(AppointmentStatus.values())
                .filter(s -> s.getDbValue().equalsIgnoreCase(dbValue))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unknown appointment status: " + dbValue));
    }
}
