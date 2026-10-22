package tiameds.pharmabackend.dto.supplier;

import lombok.Data;

import java.math.BigDecimal;

// KPI card figures for the supplier payment list, scoped to the caller's
// current warehouse or pharmacy. All values are system-calculated and read-only.
@Data
public class SupplierPaymentKpiResponse {

    // Sum of current outstanding amounts of credit purchase invoices
    // (net amount - supplier payments - confirmed purchase returns, floored at 0).
    private BigDecimal totalSupplierPayable;

    // Part of totalSupplierPayable whose payment due date
    // (invoice date + credit days) has passed.
    private BigDecimal overduePayable;

    // Amount due back from suppliers because confirmed purchase returns
    // exceed what is still owed on the invoice.
    private BigDecimal supplierCredit;
}
