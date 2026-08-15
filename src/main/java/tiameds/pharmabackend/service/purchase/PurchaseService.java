package tiameds.pharmabackend.service.purchase;

import tiameds.pharmabackend.dto.purchase.PurchaseDto;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.List;

public interface PurchaseService {

    PurchaseDto createPurchase(PurchaseDto purchaseDto, UserDetails user);

    List<PurchaseDto> getAllPurchases(UserDetails user);

    // true when this supplier already has that invoice number in that year
    boolean checkInvoiceExists(
            Long supplierId,
            String invoiceNo,
            Integer year,
            UserDetails user);

}
