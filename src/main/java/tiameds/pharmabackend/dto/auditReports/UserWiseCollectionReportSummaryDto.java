package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;

// KPI cards, over every collection matching the filters (not just the
// current page). Also serves the Total row of the User-wise Collection Summary.
@Data
@JsonPropertyOrder({
        "totalCollections", "totalCollection",
        "totalCashCollection", "totalUpiCollection", "totalCardCollection",
        "cashPercentage", "upiPercentage", "cardPercentage"})
public class UserWiseCollectionReportSummaryDto {

    // "30 collections recorded".
    private Long totalCollections;
    private BigDecimal totalCollection;

    private BigDecimal totalCashCollection;
    private BigDecimal totalUpiCollection;
    private BigDecimal totalCardCollection;

    // Share of totalCollection, 0-100: "33% of total collection".
    private BigDecimal cashPercentage;
    private BigDecimal upiPercentage;
    private BigDecimal cardPercentage;
}
