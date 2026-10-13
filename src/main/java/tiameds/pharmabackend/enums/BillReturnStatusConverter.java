package tiameds.pharmabackend.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// Stores BillReturnStatus by its label ("Not Returned") rather than its
// constant name, so the column reads the same as the UI.
@Converter
public class BillReturnStatusConverter implements AttributeConverter<BillReturnStatus, String> {

    @Override
    public String convertToDatabaseColumn(BillReturnStatus status) {
        return status != null ? status.getLabel() : null;
    }

    @Override
    public BillReturnStatus convertToEntityAttribute(String value) {
        return BillReturnStatus.fromValue(value);
    }
}
