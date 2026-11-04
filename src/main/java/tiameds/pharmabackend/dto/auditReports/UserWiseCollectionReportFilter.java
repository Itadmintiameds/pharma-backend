package tiameds.pharmabackend.dto.auditReports;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

// Query-param filters for the User-wise Collection Report. Every list is a
// multi-select; null or empty means "no restriction" on that field.
@Data
public class UserWiseCollectionReportFilter {

    // Collection date period, both inclusive. Today / Specific Day / By Month /
    // By Date Range on the screen all resolve to this pair on the client.
    private LocalDate fromDate;
    private LocalDate toDate;

    // Empty = every pharmacy the logged-in user can access.
    private List<String> pharmacyIds;

    // User associated with the collection (the receipt's created_by).
    private List<String> userIds;

    // CASH, UPI, CARD. CREDIT is not a collection.
    private List<String> paymentModes;

    // Customer/Patient Type: WALK_IN, OP_PATIENT, IP_PATIENT, DAYCARE.
    private List<String> customerTypes;

    // Collection Transaction Details paging; 0-based, offset = page * size.
    private Integer page;
    private Integer size;

    // collectionDate, receiptNo, billNo, userName, customerType, paymentMode,
    // collectedAmount. Defaults to collectionDate DESC.
    private String sortBy;
    private String sortDir;

    // User-wise Collection Summary order: userName, noOfCollections, cash,
    // upi, card, totalCollection. Defaults to totalCollection DESC.
    private String summarySortBy;
    private String summarySortDir;
}
