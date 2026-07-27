package tiameds.pharmabackend.service.impl.supplier;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tiameds.pharmabackend.dto.supplier.SupplierPaymentDto;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.purchase.Purchase;
import tiameds.pharmabackend.entity.supplier.SupplierPayment;
import tiameds.pharmabackend.mapper.supplier.SupplierPaymentMapper;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.purchase.PurchaseRepository;
import tiameds.pharmabackend.repository.supplier.SupplierPaymentRepository;
import tiameds.pharmabackend.service.supplier.SupplierPaymentService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierPaymentServiceImpl implements SupplierPaymentService {

    private final SupplierPaymentRepository supplierPaymentRepository;
    private final SupplierPaymentMapper supplierPaymentMapper;
    private final PurchaseRepository purchaseRepository;
    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;

    @Override
    public SupplierPaymentDto createPayment(
            SupplierPaymentDto paymentDto,
            UserDetails user) {

        if (paymentDto.getPurchaseId() == null) {
            throw new RuntimeException("Purchase id is required.");
        }

        if (paymentDto.getPaidAmount() == null || paymentDto.getPaidAmount() <= 0) {
            throw new RuntimeException("Paid amount must be greater than zero.");
        }

        UserDetails persistentUser = getPersistentUser(user);
        Purchase purchase = getAuthorizedPurchase(paymentDto.getPurchaseId(), persistentUser);

        SupplierPayment payment = supplierPaymentMapper.toEntity(paymentDto);
        payment.setSupplierPaymentId(null);
        payment.setPurchase(purchase);

        double alreadyPaid = totalActivePaid(purchase.getPurchaseId(), null);
        double outstanding = totalNetAmount(purchase) - (alreadyPaid + paymentDto.getPaidAmount());
        payment.setOutstandingAmount(outstanding);

        payment.setCreatedBy(String.valueOf(persistentUser.getUserId()));
        payment.setCreatedAt(LocalDateTime.now().toString());
        payment.setModifiedBy(null);
        payment.setModifiedAt(null);

        SupplierPayment savedPayment = supplierPaymentRepository.save(payment);

        updatePurchasePaymentStatus(purchase, outstanding);

        return supplierPaymentMapper.toDto(savedPayment);
    }

    @Override
    public List<SupplierPaymentDto> getPaymentsByPurchase(Long purchaseId, UserDetails user) {

        UserDetails persistentUser = getPersistentUser(user);
        getAuthorizedPurchase(purchaseId, persistentUser);

        return supplierPaymentRepository.findByPurchase_PurchaseId(purchaseId)
                .stream()
                .map(supplierPaymentMapper::toDto)
                .toList();
    }

    @Override
    public SupplierPaymentDto getPaymentById(Long supplierPaymentId, UserDetails user) {

        UserDetails persistentUser = getPersistentUser(user);
        SupplierPayment payment = getPayment(supplierPaymentId);
        getAuthorizedPurchase(payment.getPurchase().getPurchaseId(), persistentUser);

        return supplierPaymentMapper.toDto(payment);
    }

    @Override
    public SupplierPaymentDto updatePayment(
            Long supplierPaymentId,
            SupplierPaymentDto paymentDto,
            UserDetails user) {

        if (paymentDto.getPaidAmount() == null || paymentDto.getPaidAmount() <= 0) {
            throw new RuntimeException("Paid amount must be greater than zero.");
        }

        UserDetails persistentUser = getPersistentUser(user);
        SupplierPayment payment = getPayment(supplierPaymentId);
        Purchase purchase = getAuthorizedPurchase(
                payment.getPurchase().getPurchaseId(), persistentUser);

        payment.setPaymentDate(paymentDto.getPaymentDate());
        payment.setPaymentMode(paymentDto.getPaymentMode());
        payment.setReferenceNumber(paymentDto.getReferenceNumber());
        payment.setPaidAmount(paymentDto.getPaidAmount());

        double otherPaid = totalActivePaid(purchase.getPurchaseId(), supplierPaymentId);
        double paidNow = Boolean.TRUE.equals(payment.getIsActive()) ? paymentDto.getPaidAmount() : 0.0;
        double outstanding = totalNetAmount(purchase) - (otherPaid + paidNow);
        payment.setOutstandingAmount(outstanding);

        payment.setModifiedBy(String.valueOf(persistentUser.getUserId()));
        payment.setModifiedAt(LocalDateTime.now().toString());

        SupplierPayment savedPayment = supplierPaymentRepository.save(payment);

        updatePurchasePaymentStatus(purchase, outstanding);

        return supplierPaymentMapper.toDto(savedPayment);
    }

    @Override
    public SupplierPaymentDto updatePaymentStatus(
            Long supplierPaymentId,
            Boolean isActive,
            UserDetails user) {

        if (isActive == null) {
            throw new RuntimeException("isActive is required.");
        }

        UserDetails persistentUser = getPersistentUser(user);
        SupplierPayment payment = getPayment(supplierPaymentId);
        Purchase purchase = getAuthorizedPurchase(
                payment.getPurchase().getPurchaseId(), persistentUser);

        payment.setIsActive(isActive);
        payment.setModifiedBy(String.valueOf(persistentUser.getUserId()));
        payment.setModifiedAt(LocalDateTime.now().toString());

        SupplierPayment savedPayment = supplierPaymentRepository.save(payment);

        double outstanding = totalNetAmount(purchase)
                - totalActivePaid(purchase.getPurchaseId(), null);
        updatePurchasePaymentStatus(purchase, outstanding);

        return supplierPaymentMapper.toDto(savedPayment);
    }

    private UserDetails getPersistentUser(UserDetails user) {
        return userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Purchase getAuthorizedPurchase(Long purchaseId, UserDetails persistentUser) {

        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new RuntimeException("Purchase not found"));

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                purchase.getPharmacyId(),
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        return purchase;
    }

    private SupplierPayment getPayment(Long supplierPaymentId) {
        return supplierPaymentRepository.findById(supplierPaymentId)
                .orElseThrow(() -> new RuntimeException("Supplier payment not found"));
    }

    private double totalActivePaid(Long purchaseId, Long excludePaymentId) {
        return supplierPaymentRepository
                .findByPurchase_PurchaseIdAndIsActiveTrue(purchaseId)
                .stream()
                .filter(p -> !p.getSupplierPaymentId().equals(excludePaymentId))
                .mapToDouble(p -> p.getPaidAmount() != null ? p.getPaidAmount() : 0.0)
                .sum();
    }

    private double totalNetAmount(Purchase purchase) {
        return purchase.getTotalNetAmount() != null
                ? purchase.getTotalNetAmount().doubleValue()
                : 0.0;
    }

    private void updatePurchasePaymentStatus(Purchase purchase, double outstanding) {
        purchase.setSupplierPaymentStatus(outstanding <= 0 ? "PAID" : "PARTIALLY_PAID");
        purchaseRepository.save(purchase);
    }
}
