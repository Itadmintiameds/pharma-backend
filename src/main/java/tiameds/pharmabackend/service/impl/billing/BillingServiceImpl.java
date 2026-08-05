package tiameds.pharmabackend.service.impl.billing;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import tiameds.pharmabackend.context.CurrentPharmacyContext;
import tiameds.pharmabackend.dto.billing.BillingDto;
import tiameds.pharmabackend.dto.billing.PrescriptionUploadDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.billing.Billing;
import tiameds.pharmabackend.entity.billing.BillingDetails;
import tiameds.pharmabackend.entity.billing.BillingPayment;
import tiameds.pharmabackend.entity.billing.CustomerManagement;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.purchase.Inventory;
import tiameds.pharmabackend.entity.purchase.InventoryAudit;
import tiameds.pharmabackend.enums.StockMovement;
import tiameds.pharmabackend.enums.TransactionType;
import tiameds.pharmabackend.mapper.billing.BillingMapper;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.billing.BillingRepository;
import tiameds.pharmabackend.repository.billing.CustomerManagementRepository;
import tiameds.pharmabackend.repository.product.BatchDetailsRepository;
import tiameds.pharmabackend.repository.product.ProductDetailsRepository;
import tiameds.pharmabackend.repository.purchase.InventoryAuditRepository;
import tiameds.pharmabackend.repository.purchase.InventoryRepository;
import tiameds.pharmabackend.service.S3Service;
import tiameds.pharmabackend.service.billing.BillingService;

@Service
@RequiredArgsConstructor
@Transactional
public class BillingServiceImpl implements BillingService {

    private static final DateTimeFormatter PRESCRIPTION_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final BillingRepository billingRepository;
    private final CustomerManagementRepository customerManagementRepository;
    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final ProductDetailsRepository productDetailsRepository;
    private final BatchDetailsRepository batchDetailsRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryAuditRepository inventoryAuditRepository;
    private final CurrentPharmacyContext pharmacyContext;
    private final S3Service s3Service;


    @Override
    public BillingDto createBilling(BillingDto billingDto, UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String pharmacyId = pharmacyContext.getCurrentPharmacy();

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        PharmacyDetails pharmacy = pharmacyDetailsRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));

        if (billingDto.getBillingDetails() == null
                || billingDto.getBillingDetails().isEmpty()) {
            throw new RuntimeException("A bill must contain at least one product.");
        }

        String currentUserId = String.valueOf(persistentUser.getUserId());

        CustomerManagement customer = resolveCustomer(billingDto, pharmacy, currentUserId);

        Billing billing = BillingMapper.toEntity(billingDto);

        billing.setPharmacy(pharmacy);
        billing.setCustomer(customer);
        billing.setCreatedBy(currentUserId);
        billing.setCreatedAt(LocalDateTime.now());
        billing.setModifiedBy(null);
        billing.setModifiedAt(null);

        // Resolve every line and validate stock BEFORE anything is written, so a
        // single short line aborts the whole bill instead of half-issuing stock.
        List<Inventory> lineInventories = new ArrayList<>();

        // A batch can appear on more than one line of the same bill, so the
        // requested quantity is accumulated per inventory row before comparing.
        Map<Long, Long> requestedPerInventory = new LinkedHashMap<>();

        for (int i = 0; i < billing.getBillingDetails().size(); i++) {

            BillingDetails detail = billing.getBillingDetails().get(i);
            var dto = billingDto.getBillingDetails().get(i);

            ProductDetails product = productDetailsRepository
                    .findById(dto.getProductId())
                    .orElseThrow(() ->
                            new RuntimeException("Product not found: " + dto.getProductId()));

            if (product.getPharmacy() == null
                    || !pharmacyId.equals(product.getPharmacy().getPharmacyId())) {
                throw new RuntimeException(
                        "Product does not belong to this pharmacy: " + dto.getProductId());
            }

            BatchDetails batch = batchDetailsRepository
                    .findById(dto.getBatchId())
                    .orElseThrow(() ->
                            new RuntimeException("Batch not found: " + dto.getBatchId()));

            if (batch.getProduct() == null
                    || !product.getProductId().equals(batch.getProduct().getProductId())) {
                throw new RuntimeException(
                        "Batch " + dto.getBatchId()
                                + " does not belong to product " + dto.getProductId());
            }

            detail.setProduct(product);
            detail.setBatch(batch);
            detail.setBilling(billing);
            detail.setCreatedBy(currentUserId);
            detail.setCreatedAt(LocalDateTime.now());
            detail.setModifiedBy(null);
            detail.setModifiedAt(null);

            // bill_quantity is already expressed in smallest units, which is the
            // same unit Inventory.totalStock is kept in on the purchase side.
            Long billQuantity = detail.getBillQuantity() != null
                    ? detail.getBillQuantity()
                    : 0L;

            if (billQuantity <= 0L) {
                throw new RuntimeException(
                        "Bill quantity must be greater than zero for product: "
                                + product.getProductName());
            }

            PackagingDetails packaging = batch.getPackagingDetails();

            Inventory inventory = inventoryRepository
                    .findByPharmacy_PharmacyIdAndProductAndPackagingAndBatch(
                            pharmacyId, product, packaging, batch)
                    .orElseThrow(() -> new RuntimeException(
                            "No stock available for product " + product.getProductName()
                                    + ", batch " + batch.getBatchNumber()));

            Long availableStock = inventory.getTotalStock() != null
                    ? inventory.getTotalStock()
                    : 0L;

            Long alreadyRequested = requestedPerInventory
                    .getOrDefault(inventory.getInventoryId(), 0L);

            Long totalRequested = alreadyRequested + billQuantity;

            if (availableStock < totalRequested) {
                throw new RuntimeException(
                        "Insufficient stock for product " + product.getProductName()
                                + ", batch " + batch.getBatchNumber()
                                + ". Requested: " + totalRequested
                                + ", Available: " + availableStock);
            }

            requestedPerInventory.put(inventory.getInventoryId(), totalRequested);

            lineInventories.add(inventory);
        }

        if (billing.getBillingPayments() != null) {

            for (BillingPayment payment : billing.getBillingPayments()) {

                payment.setBilling(billing);
                payment.setCreatedBy(currentUserId);
                payment.setCreatedAt(LocalDateTime.now());
                payment.setModifiedBy(null);
                payment.setModifiedAt(null);
            }
        }

        Billing savedBilling = billingRepository.save(billing);

        // Stock is issued out only after every line passed validation.
        for (int i = 0; i < savedBilling.getBillingDetails().size(); i++) {

            BillingDetails detail = savedBilling.getBillingDetails().get(i);
            Inventory inventory = lineInventories.get(i);

            Long billQuantity = detail.getBillQuantity() != null
                    ? detail.getBillQuantity()
                    : 0L;

            Long currentStock = inventory.getTotalStock() != null
                    ? inventory.getTotalStock()
                    : 0L;

            inventory.setTotalStock(currentStock - billQuantity);
            inventory.setModifiedBy(currentUserId);
            inventory.setModifiedAt(LocalDateTime.now());

            inventory = inventoryRepository.save(inventory);

            InventoryAudit audit = new InventoryAudit();

            audit.setInventory(inventory);
            audit.setPharmacy(pharmacy);
            audit.setBilling(savedBilling);
            audit.setPurchaseDetails(null);
            audit.setStockMovement(StockMovement.OUT);
            audit.setTransactionType(TransactionType.SALE);
            audit.setChangeStock(billQuantity);
            audit.setRemainingStock(inventory.getTotalStock());
            audit.setChangedBy(currentUserId);
            audit.setChangedAt(LocalDateTime.now());

            inventoryAuditRepository.save(audit);
        }

        return BillingMapper.toDto(savedBilling);
    }


    @Override
    public List<BillingDto> getAllBillings(UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String pharmacyId = pharmacyContext.getCurrentPharmacy();

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        return billingRepository.findByPharmacy_PharmacyId(pharmacyId)
                .stream()
                .map(BillingMapper::toDto)
                .collect(Collectors.toList());
    }


    @Override
    public PrescriptionUploadDto uploadPrescription(
            Long billingId,
            MultipartFile file,
            UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String pharmacyId = pharmacyContext.getCurrentPharmacy();

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Prescription file is required");
        }

        String contentType = file.getContentType() != null
                ? file.getContentType().toLowerCase()
                : "";

        if (!contentType.startsWith("image/") && !contentType.equals("application/pdf")) {
            throw new RuntimeException("Only image or PDF files are allowed");
        }

        Billing billing = billingRepository
                .findByBillingIdAndPharmacy_PharmacyId(billingId, pharmacyId)
                .orElseThrow(() -> new RuntimeException(
                        "Bill not found in this pharmacy with id : " + billingId));

        String key = buildPrescriptionKey(pharmacyId, billingId, file.getOriginalFilename());

        String prescriptionUrl;

        try {
            prescriptionUrl = s3Service.uploadFile(key, file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload prescription", e);
        }

        String oldPrescriptionUrl = billing.getPrescriptionUrl();

        billing.setPrescriptionUrl(prescriptionUrl);
        billing.setModifiedBy(String.valueOf(persistentUser.getUserId()));
        billing.setModifiedAt(LocalDateTime.now());

        billingRepository.save(billing);

        deleteOldPrescriptionQuietly(oldPrescriptionUrl);

        return new PrescriptionUploadDto(billing.getBillingId(), prescriptionUrl);
    }


    private void deleteOldPrescriptionQuietly(String oldPrescriptionUrl) {

        if (oldPrescriptionUrl == null || oldPrescriptionUrl.isBlank()) {
            return;
        }

        try {
            s3Service.deleteFile(s3Service.extractKeyFromUrl(oldPrescriptionUrl));
        } catch (Exception e) {
            // Old file may be external or already gone; replacing it should not fail the upload
        }
    }


    private CustomerManagement resolveCustomer(
            BillingDto billingDto,
            PharmacyDetails pharmacy,
            String currentUserId) {

        String pharmacyId = pharmacy.getPharmacyId();

        // An existing customer was picked on the frontend.
        if (billingDto.getCustomerId() != null) {

            CustomerManagement customer = customerManagementRepository
                    .findById(billingDto.getCustomerId())
                    .orElseThrow(() -> new RuntimeException(
                            "Customer not found: " + billingDto.getCustomerId()));

            if (customer.getPharmacy() == null
                    || !pharmacyId.equals(customer.getPharmacy().getPharmacyId())) {
                throw new RuntimeException(
                        "Customer does not belong to this pharmacy: "
                                + billingDto.getCustomerId());
            }

            return customer;
        }

        String phoneNo = billingDto.getCustomerPhoneNo();
        String name = billingDto.getCustomerName();

        boolean hasPhone = phoneNo != null && !phoneNo.isBlank();
        boolean hasName = name != null && !name.isBlank();

        // Anonymous walk-in: no customer row is created.
        if (!hasPhone && !hasName) {
            return null;
        }

        // Reuse the existing customer when the phone number is already known.
        if (hasPhone) {

            CustomerManagement existing = customerManagementRepository
                    .findByPharmacy_PharmacyIdAndCustomerPhoneNo(pharmacyId, phoneNo)
                    .orElse(null);

            if (existing != null) {
                return existing;
            }
        }

        CustomerManagement customer = new CustomerManagement();

        customer.setPharmacy(pharmacy);
        customer.setCustomerName(name);
        customer.setCustomerPhoneNo(hasPhone ? phoneNo : null);
        customer.setCreatedBy(currentUserId);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setModifiedBy(null);
        customer.setModifiedAt(null);

        return customerManagementRepository.save(customer);
    }


    private String buildPrescriptionKey(
            String pharmacyId,
            Long billingId,
            String originalFilename) {

        String extension = "";

        if (originalFilename != null) {
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = originalFilename.substring(dotIndex).toLowerCase();
            }
        }

        String timestamp = LocalDateTime.now().format(PRESCRIPTION_TIMESTAMP_FORMAT);

        return "pharmacy/" + pharmacyId + "/billing/" + billingId
                + "/prescription/RX_" + timestamp + extension;
    }
}
