package ca.bcit.infosys.workpackage;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class WpStatusConverter implements AttributeConverter<WpStatus, String> {

    @Override
    public String convertToDatabaseColumn(WpStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getDisplayName();
    }

    @Override
    public WpStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        for (WpStatus status : WpStatus.values()) {
            if (status.getDisplayName().equals(dbData)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown database value: " + dbData);
    }
}
