package com.medibridge.common.enums.converter;

import com.medibridge.common.enums.AccountStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;

/**
 * Java constants are UPPER_CASE; the MySQL ENUM values are lowercase. Relying
 * on the case-insensitive collation to bridge that would be accidental, so the
 * mapping is explicit.
 */
@Converter(autoApply = true)
public class AccountStatusConverter implements AttributeConverter<AccountStatus, String> {

    @Override
    public String convertToDatabaseColumn(AccountStatus status) {
        return status == null ? null : status.getDbValue();
    }

    @Override
    public AccountStatus convertToEntityAttribute(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        return Arrays.stream(AccountStatus.values())
                .filter(s -> s.getDbValue().equalsIgnoreCase(dbValue))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unknown account status: " + dbValue));
    }
}
