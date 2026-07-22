package com.medibridge.common.enums.converter;

import com.medibridge.review.entity.Rating.Highlight;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Java constant BEDSIDE_MANNER <-> MySQL ENUM literal 'Bedside Manner'.
 * Enum names cannot contain spaces or hyphens, so a converter is unavoidable.
 */
@Converter(autoApply = true)
public class HighlightConverter implements AttributeConverter<Highlight, String> {

    @Override
    public String convertToDatabaseColumn(Highlight highlight) {
        return highlight == null ? null : highlight.getLabel();
    }

    @Override
    public Highlight convertToEntityAttribute(String dbValue) {
        return dbValue == null ? null : Highlight.fromLabel(dbValue);
    }
}
