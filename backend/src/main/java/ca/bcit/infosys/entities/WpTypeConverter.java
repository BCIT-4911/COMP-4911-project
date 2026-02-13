package ca.bcit.infosys.entities;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class WpTypeConverter implements AttributeConverter<WpType, String> {

    @Override
    public String convertToDatabaseColumn(WpType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getDisplayName();
    }

    @Override
    public WpType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        for (WpType type : WpType.values()) {
            if (type.getDisplayName().equals(dbData)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown database value: " + dbData);
    }
}
