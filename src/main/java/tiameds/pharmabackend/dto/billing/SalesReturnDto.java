package tiameds.pharmabackend.dto.billing;

import lombok.Data;
import tiameds.pharmabackend.enums.BillReturnStatus;
import tiameds.pharmabackend.enums.CustomerType;
import tiameds.pharmabackend.enums.SalesReturnStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SalesReturnDto {

    private Long salesReturnId;
    private Long billingId;

    // Read-only: read off the parent bill so the response can show which bill
    // was returned against and where that bill now stands.
    private String billNo;
    private BillReturnStatus billReturnStatus;

    // Read-only: the bill's customer. Null for an anonymous walk-in bill.
    private Long customerId;
    private String customerName;
    private String customerPhoneNo;
    private CustomerType customerType;

    private String pharmacyId;

    // Read-only: generated and set by the server.
    private String salesReturnNo;
    private SalesReturnStatus salesReturnStatus;

    // Optional on create; defaults to now.
    private LocalDateTime salesReturnDate;

    // Read-only: computed from the lines, which are priced off the bill.
    private BigDecimal totalGrossAmount;
    private BigDecimal totalGstAmount;
    private BigDecimal totalNetAmount;

    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;

    private Integer itemCount;

    private List<SalesReturnDetailsDto> salesReturnDetails;
}
