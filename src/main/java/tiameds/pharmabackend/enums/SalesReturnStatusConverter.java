package tiameds.pharmabackend.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// Stores SalesReturnStatus by its label ("Completed") rather than its
// constant name, so the column reads the same as the UI.
@Converter
public class SalesReturnStatusConverter implements AttributeConverter<SalesReturnStatus, String> {

    @Override
    public String convertToDatabaseColumn(SalesReturnStatus status) {
        return status != null ? status.getLabel() : null;
    }

    @Override
    public SalesReturnStatus convertToEntityAttribute(String value) {
        return SalesReturnStatus.fromValue(value);
    }
}
