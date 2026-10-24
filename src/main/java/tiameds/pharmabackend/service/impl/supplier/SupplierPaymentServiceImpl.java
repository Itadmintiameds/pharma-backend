package tiameds.pharmabackend.service.impl.supplier;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tiameds.pharmabackend.context.LocationContext;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.supplier.SupplierPaymentDto;
import tiameds.pharmabackend.dto.supplier.SupplierPaymentKpiResponse;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.purchase.Purchase;
import tiameds.pharmabackend.entity.supplier.SupplierMaster;
import tiameds.pharmabackend.entity.supplier.SupplierPayment;
import tiameds.pharmabackend.enums.PurchaseReturnStatus;
import tiameds.pharmabackend.mapper.supplier.SupplierPaymentMapper;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.purchase.PurchaseRepository;
import tiameds.pharmabackend.repository.purchase.PurchaseReturnRepository;
import tiameds.pharmabackend.repository.supplier.SupplierMasterRepository;
import tiameds.pharmabackend.repository.supplier.SupplierPaymentRepository;
import tiameds.pharmabackend.service.supplier.SupplierPaymentService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierPaymentServiceImpl implements SupplierPaymentService {

    private static final String PAYMENT_STATUS_PAID = "PAID";
    private static final String PURCHASE_PAYMENT_TYPE_CREDIT = "CREDIT";

    private final SupplierPaymentRepository supplierPaymentRepository;
    private final SupplierPaymentMapper supplierPaymentMapper;
    private final PurchaseRepository purchaseRepository;
    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final SupplierMasterRepository supplierMasterRepository;
    private final LocationContextResolver locationContextResolver;
    private final PurchaseReturnRepository purchaseReturnRepository;

    @Override
    public SupplierPaymentDto createPayment(SupplierPaymentDto paymentDto, UserDetails user) {

        if (paymentDto.getPurchaseId() == null) {
            throw new RuntimeException("Purchase id is required.");
        }

        if (paymentDto.getSupplierId() == null) {
            throw new RuntimeException("Supplier id is required.");
        }

        if (paymentDto.getPaymentAmount() == null || paymentDto.getPaymentAmount() <= 0) {
            throw new RuntimeException("Payment amount must be greater than zero.");
        }

        UserDetails persistentUser = getPersistentUser(user);

        // A warehouse manager pays suppliers for their warehouse's purchases,
        // everyone else for their selected pharmacy's.
        LocationContext location = resolveLocation(persistentUser);

        Purchase purchase = getPurchaseInLocation(paymentDto.getPurchaseId(), location);
        SupplierMaster supplier = getSupplierInLocation(paymentDto.getSupplierId(), location);

        if (purchase.getSupplier() == null
                || !supplier.getSupplierId().equals(purchase.getSupplier().getSupplierId())) {
            throw new RuntimeException("Supplier does not match the purchase supplier.");
        }

        SupplierPayment payment = supplierPaymentMapper.toEntity(paymentDto);

        payment.setSupplierPaymentId(null);
        payment.setSupplierMaster(supplier);
        payment.setPurchase(purchase);
        payment.setSupplierPaymentNo(generateSupplierPaymentNo(location));
        payment.setSupplierPaymentDate(LocalDate.now());

        payment.setCreatedBy(String.valueOf(persistentUser.getUserId()));
        payment.setCreatedAt(LocalDateTime.now().toString());
        payment.setModifiedBy(null);
        payment.setModifiedAt(null);

        SupplierPayment savedPayment = supplierPaymentRepository.save(payment);

        // No partial payments for now: any payment settles the purchase.
        purchase.setSupplierPaymentStatus(PAYMENT_STATUS_PAID);
        purchaseRepository.save(purchase);

        return supplierPaymentMapper.toDto(savedPayment);
    }

    @Override
    public List<SupplierPaymentDto> getAllPayments(UserDetails user) {

        UserDetails persistentUser = getPersistentUser(user);
        LocationContext location = resolveLocation(persistentUser);

        List<SupplierPayment> payments = location.isWarehouse()
                ? supplierPaymentRepository.findByPurchase_WarehouseId(location.getLocationId())
                : supplierPaymentRepository.findByPurchase_PharmacyId(location.getLocationId());

        return payments.stream()
                .map(supplierPaymentMapper::toDto)
                .toList();
    }

    @Override
    public List<SupplierPaymentDto> getPaymentsByPurchase(Long purchaseId, UserDetails user) {

        if (purchaseId == null) {
            throw new RuntimeException("Purchase id is required.");
        }

        UserDetails persistentUser = getPersistentUser(user);
        LocationContext location = resolveLocation(persistentUser);

        getPurchaseInLocation(purchaseId, location);

        return supplierPaymentRepository.findByPurchase_PurchaseId(purchaseId)
                .stream()
                .map(supplierPaymentMapper::toDto)
                .toList();
    }

    @Override
    public SupplierPaymentDto getPaymentById(Long supplierPaymentId, UserDetails user) {

        if (supplierPaymentId == null) {
            throw new RuntimeException("Supplier payment id is required.");
        }

        UserDetails persistentUser = getPersistentUser(user);
        LocationContext location = resolveLocation(persistentUser);

        if (location.isWarehouse()) {

            SupplierPayment payment = supplierPaymentRepository
                    .findBySupplierPaymentIdAndPurchase_WarehouseId(supplierPaymentId, location.getLocationId())
                    .orElseThrow(() -> new RuntimeException(
                            "Supplier payment not found in this warehouse with id : " + supplierPaymentId));

            return supplierPaymentMapper.toDto(payment);
        }

        SupplierPayment payment = supplierPaymentRepository
                .findBySupplierPaymentIdAndPurchase_PharmacyId(supplierPaymentId, location.getLocationId())
                .orElseThrow(() -> new RuntimeException(
                        "Supplier payment not found in this pharmacy with id : " + supplierPaymentId));

        return supplierPaymentMapper.toDto(payment);
    }

    // Supplier payment KPI cards (SR-SP-01-008..016), for the current warehouse or pharmacy.
    // Per credit purchase invoice:
    //   current outstanding = invoice amount - supplier payments - confirmed purchase returns, floored at 0.
    // Total supplier payable is the sum of those; overdue payable is the part whose due date
    // (invoice date + credit days) is before today. Supplier credit is what returns push past
    // the amount owed: the excess on credit purchases, and the full return on purchases
    // that were paid upfront.
    @Override
    public SupplierPaymentKpiResponse getSupplierPaymentKpis(UserDetails user) {

        UserDetails persistentUser = getPersistentUser(user);
        LocationContext location = resolveLocation(persistentUser);
        String locationId = location.getLocationId();

        List<Purchase> purchases;
        Map<Long, BigDecimal> paidByPurchase;
        Map<Long, BigDecimal> returnedByPurchase;

        if (location.isWarehouse()) {
            purchases = purchaseRepository.findByWarehouseId(locationId);
            paidByPurchase = toAmountMap(
                    supplierPaymentRepository.sumPaymentAmountPerPurchaseByWarehouseId(locationId));
            returnedByPurchase = toAmountMap(
                    purchaseReturnRepository.sumReturnAmountPerPurchaseByWarehouseId(
                            locationId, PurchaseReturnStatus.CONFIRMED));
        } else {
            purchases = purchaseRepository.findByPharmacyId(locationId);
            paidByPurchase = toAmountMap(
                    supplierPaymentRepository.sumPaymentAmountPerPurchaseByPharmacyId(locationId));
            returnedByPurchase = toAmountMap(
                    purchaseReturnRepository.sumReturnAmountPerPurchaseByPharmacyId(
                            locationId, PurchaseReturnStatus.CONFIRMED));
        }

        LocalDate today = LocalDate.now();

        BigDecimal totalSupplierPayable = BigDecimal.ZERO;
        BigDecimal overduePayable = BigDecimal.ZERO;
        BigDecimal supplierCredit = BigDecimal.ZERO;

        for (Purchase purchase : purchases) {

            BigDecimal returned = returnedByPurchase.getOrDefault(purchase.getPurchaseId(), BigDecimal.ZERO);

            if (!isCreditPurchase(purchase)) {
                // Paid upfront, so anything returned is owed back by the supplier.
                supplierCredit = supplierCredit.add(returned);
                continue;
            }

            BigDecimal paid = paidByPurchase.getOrDefault(purchase.getPurchaseId(), BigDecimal.ZERO);
            BigDecimal balance = invoiceAmount(purchase).subtract(paid).subtract(returned);

            if (balance.signum() <= 0) {
                // Fully settled: contributes nothing to payable or overdue (SR-SP-01-011/013).
                supplierCredit = supplierCredit.add(balance.negate());
                continue;
            }

            totalSupplierPayable = totalSupplierPayable.add(balance);

            LocalDate dueDate = paymentDueDate(purchase);

            if (dueDate != null && dueDate.isBefore(today)) {
                overduePayable = overduePayable.add(balance);
            }
        }

        SupplierPaymentKpiResponse response = new SupplierPaymentKpiResponse();
        response.setTotalSupplierPayable(totalSupplierPayable);
        response.setOverduePayable(overduePayable);
        response.setSupplierCredit(supplierCredit);

        return response;
    }

    private boolean isCreditPurchase(Purchase purchase) {
        return PURCHASE_PAYMENT_TYPE_CREDIT.equalsIgnoreCase(purchase.getPaymentType());
    }

    // Invoice amount as entered on the purchase, falling back to the computed net total.
    // What the supplier is owed for a purchase: its net amount, the same figure
    // the supplier payment screens show as the invoice amount.
    private BigDecimal invoiceAmount(Purchase purchase) {
        return purchase.getTotalNetAmount() != null ? purchase.getTotalNetAmount() : BigDecimal.ZERO;
    }

    private LocalDate paymentDueDate(Purchase purchase) {

        if (purchase.getInvoiceDate() == null) {
            return null;
        }

        long creditDays = purchase.getCreditDays() != null ? purchase.getCreditDays() : 0L;

        return purchase.getInvoiceDate().toLocalDate().plusDays(creditDays);
    }

    // Turns [purchaseId, amount] rows into a lookup by purchase id.
    private Map<Long, BigDecimal> toAmountMap(List<Object[]> rows) {

        Map<Long, BigDecimal> amounts = new HashMap<>();

        for (Object[] row : rows) {
            Long purchaseId = (Long) row[0];
            BigDecimal amount = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            amounts.put(purchaseId, amount);
        }

        return amounts;
    }

    private UserDetails getPersistentUser(UserDetails user) {
        return userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Resolves the caller's current warehouse or pharmacy; a pharmacy must also
    // be one the user belongs to.
    private LocationContext resolveLocation(UserDetails persistentUser) {

        LocationContext location = locationContextResolver.resolve(persistentUser);

        if (location.isPharmacy()) {

            boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                    location.getLocationId(),
                    persistentUser.getUserId());

            if (!valid) {
                throw new RuntimeException("You are not authorized to use this pharmacy.");
            }
        }

        return location;
    }

    private Purchase getPurchaseInLocation(Long purchaseId, LocationContext location) {

        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new RuntimeException("Purchase not found"));

        if (location.isWarehouse()) {

            if (!location.getLocationId().equals(purchase.getWarehouseId())) {
                throw new RuntimeException("Purchase not found in this warehouse with id : " + purchaseId);
            }

            return purchase;
        }

        if (!location.getLocationId().equals(purchase.getPharmacyId())) {
            throw new RuntimeException("Purchase not found in this pharmacy with id : " + purchaseId);
        }

        return purchase;
    }

    private SupplierMaster getSupplierInLocation(Long supplierId, LocationContext location) {

        if (location.isWarehouse()) {
            return supplierMasterRepository
                    .findBySupplierIdAndWarehouseId(supplierId, location.getLocationId())
                    .orElseThrow(() -> new RuntimeException(
                            "Supplier not found in this warehouse with id : " + supplierId));
        }

        return supplierMasterRepository
                .findBySupplierIdAndPharmacyId(supplierId, location.getLocationId())
                .orElseThrow(() -> new RuntimeException(
                        "Supplier not found in this pharmacy with id : " + supplierId));
    }

    private String generateSupplierPaymentNo(LocationContext location) {

        int year = LocalDate.now().getYear();
        String prefix = "SP-" + year + "-";

        List<String> latest = location.isWarehouse()
                ? supplierPaymentRepository.findLatestSupplierPaymentNoByWarehouse(
                        prefix,
                        location.getLocationId(),
                        PageRequest.of(0, 1))
                : supplierPaymentRepository.findLatestSupplierPaymentNo(
                        prefix,
                        location.getLocationId(),
                        PageRequest.of(0, 1));

        int nextNumber = 1;

        if (!latest.isEmpty()) {

            String latestSupplierPaymentNo = latest.get(0);

            String numberPart = latestSupplierPaymentNo.substring(prefix.length());

            nextNumber = Integer.parseInt(numberPart) + 1;
        }

        return prefix + String.format("%05d", nextNumber);
    }
}
