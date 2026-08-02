package tiameds.pharmabackend.service.supplier;

import tiameds.pharmabackend.dto.supplier.SupplierPaymentDto;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.List;

public interface SupplierPaymentService {

    SupplierPaymentDto createPayment(SupplierPaymentDto paymentDto, UserDetails user);

    List<SupplierPaymentDto> getPaymentsByPurchase(Long purchaseId, UserDetails user);

    SupplierPaymentDto getPaymentById(Long supplierPaymentId, UserDetails user);

    SupplierPaymentDto updatePayment(Long supplierPaymentId, SupplierPaymentDto paymentDto, UserDetails user);

    SupplierPaymentDto updatePaymentStatus(Long supplierPaymentId, Boolean isActive, UserDetails user);
}
