package com.medibridge.common.enums.converter;

import com.medibridge.common.enums.PaymentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;

@Converter(autoApply = true)
public class PaymentStatusConverter implements AttributeConverter<PaymentStatus, String> {

    @Override
    public String convertToDatabaseColumn(PaymentStatus status) {
        return status == null ? null : status.getDbValue();
    }

    @Override
    public PaymentStatus convertToEntityAttribute(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        return Arrays.stream(PaymentStatus.values())
                .filter(s -> s.getDbValue().equalsIgnoreCase(dbValue))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unknown payment status: " + dbValue));
    }
}
