package tiameds.pharmabackend.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// Stores DamagedReturnStatus by its label ("Not Returned") rather than its
// constant name, so the column reads the same as the UI.
@Converter
public class DamagedReturnStatusConverter implements AttributeConverter<DamagedReturnStatus, String> {

    @Override
    public String convertToDatabaseColumn(DamagedReturnStatus status) {
        return status != null ? status.getLabel() : null;
    }

    @Override
    public DamagedReturnStatus convertToEntityAttribute(String value) {
        return DamagedReturnStatus.fromValue(value);
    }
}
