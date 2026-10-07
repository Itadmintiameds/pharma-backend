package tiameds.pharmabackend.service.billing;

import tiameds.pharmabackend.dto.billing.SalesReturnDto;
import tiameds.pharmabackend.entity.UserDetails;

public interface SalesReturnService {

    // Returns stock against an existing bill: puts it back into inventory and
    // re-derives the bill's sales return status.
    SalesReturnDto createSalesReturn(SalesReturnDto salesReturnDto, UserDetails user);
}
