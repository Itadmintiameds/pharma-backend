package tiameds.pharmabackend.service.purchase;

import tiameds.pharmabackend.dto.purchase.PurchaseReturnDto;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.List;

public interface PurchaseReturnService {

    PurchaseReturnDto createPurchaseReturn(PurchaseReturnDto purchaseReturnDto, UserDetails user);

    List<PurchaseReturnDto> getAllPurchaseReturns(UserDetails user);

    PurchaseReturnDto getPurchaseReturnById(Long purchaseReturnId, UserDetails user);

}
