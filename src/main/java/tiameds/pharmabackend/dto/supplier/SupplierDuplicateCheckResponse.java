package tiameds.pharmabackend.dto.supplier;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

// Result of checking a supplier's GST / DL / PAN / FSSAI numbers against the other
// suppliers of the caller's current warehouse or pharmacy.
@Data
public class SupplierDuplicateCheckResponse {

    // True when any of the numbers is already used by another supplier.
    private boolean duplicate;

    private boolean gstinNoExists;
    private boolean dlnoExists;
    private boolean panNoExists;
    private boolean fssaiNoExists;

    // One message per duplicated number, e.g. "GST number already exists for supplier ABC Pharma".
    private List<String> messages = new ArrayList<>();
}
