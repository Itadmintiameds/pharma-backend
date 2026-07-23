package tiameds.pharmabackend.service.purchase;

import tiameds.pharmabackend.dto.purchase.PurchaseDto;
import tiameds.pharmabackend.entity.UserDetails;

public interface PurchaseService {

    PurchaseDto createPurchase(PurchaseDto purchaseDto, UserDetails user);

}