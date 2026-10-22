package tiameds.pharmabackend.service.supplier;

import tiameds.pharmabackend.dto.supplier.SupplierPaymentDto;
import tiameds.pharmabackend.dto.supplier.SupplierPaymentKpiResponse;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.List;

public interface SupplierPaymentService {

    SupplierPaymentDto createPayment(SupplierPaymentDto paymentDto, UserDetails user);

    List<SupplierPaymentDto> getAllPayments(UserDetails user);

    List<SupplierPaymentDto> getPaymentsByPurchase(Long purchaseId, UserDetails user);

    SupplierPaymentDto getPaymentById(Long supplierPaymentId, UserDetails user);

    SupplierPaymentKpiResponse getSupplierPaymentKpis(UserDetails user);
}
