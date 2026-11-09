package tiameds.pharmabackend.dto.purchase;

import lombok.Data;

@Data
public class PurchaseReturnCancelRequest {

    // Required; stored on the return as cancelReason.
    private String cancelReason;
}
