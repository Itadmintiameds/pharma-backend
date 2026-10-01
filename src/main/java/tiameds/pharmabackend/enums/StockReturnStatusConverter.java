package tiameds.pharmabackend.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// Stores StockReturnStatus by its label ("Pending Receipt") rather than its
// constant name, so the column reads the same as the UI.
@Converter
public class StockReturnStatusConverter implements AttributeConverter<StockReturnStatus, String> {

    @Override
    public String convertToDatabaseColumn(StockReturnStatus status) {
        return status != null ? status.getLabel() : null;
    }

    @Override
    public StockReturnStatus convertToEntityAttribute(String value) {
        return StockReturnStatus.fromValue(value);
    }
}
