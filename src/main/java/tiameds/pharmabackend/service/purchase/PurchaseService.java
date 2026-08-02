package tiameds.pharmabackend.service.purchase;

import tiameds.pharmabackend.dto.purchase.PurchaseDto;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.List;

public interface PurchaseService {

    PurchaseDto createPurchase(PurchaseDto purchaseDto, UserDetails user);

    List<PurchaseDto> getAllPurchases(UserDetails user);

}