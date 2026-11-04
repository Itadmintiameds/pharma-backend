package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;

// One user of the User-wise Collection Summary table.
// totalCollection = cash + upi + card.
@Data
@JsonPropertyOrder({
        "userId", "userName", "noOfCollections",
        "cash", "upi", "card", "totalCollection"})
public class UserWiseCollectionUserRowDto {

    private String userId;
    private String userName;

    private Long noOfCollections;

    private BigDecimal cash;
    private BigDecimal upi;
    private BigDecimal card;
    private BigDecimal totalCollection;
}
