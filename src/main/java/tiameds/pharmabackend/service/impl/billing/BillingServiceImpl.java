package tiameds.pharmabackend.service.impl.billing;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import tiameds.pharmabackend.context.CurrentPharmacyContext;
import tiameds.pharmabackend.dto.billing.BillingDetailsDto;
import tiameds.pharmabackend.dto.billing.BillingDto;
import tiameds.pharmabackend.dto.billing.BillingPaymentDto;
import tiameds.pharmabackend.dto.billing.PrescriptionUploadDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.billing.Billing;
import tiameds.pharmabackend.entity.billing.BillingDetails;
import tiameds.pharmabackend.entity.billing.BillingPayment;
import tiameds.pharmabackend.entity.billing.CustomerManagement;
import tiameds.pharmabackend.entity.billing.DoctorDetails;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.purchase.Inventory;
import tiameds.pharmabackend.entity.purchase.InventoryAudit;
import tiameds.pharmabackend.enums.PaymentType;
import tiameds.pharmabackend.enums.StockMovement;
import tiameds.pharmabackend.enums.TransactionType;
import tiameds.pharmabackend.mapper.billing.BillingDetailsMapper;
import tiameds.pharmabackend.mapper.billing.BillingMapper;
import tiameds.pharmabackend.mapper.billing.BillingPaymentMapper;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.billing.BillingRepository;
import tiameds.pharmabackend.repository.billing.CustomerManagementRepository;
import tiameds.pharmabackend.repository.billing.DoctorDetailsRepository;
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
    private final DoctorDetailsRepository doctorDetailsRepository;
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

        BillingContext context = resolveContext(user);

        requireLines(billingDto);

        CustomerManagement customer = resolveCustomer(
                billingDto,
                context.pharmacy(),
                context.currentUserId());

        Billing billing = BillingMapper.toEntity(billingDto);

        billing.setPharmacy(context.pharmacy());
        billing.setCustomer(customer);
        billing.setDoctor(resolveDoctor(billingDto, context.pharmacyId()));
        billing.setBillNo(generateBillNo(context.pharmacyId()));
        billing.setCreatedBy(context.currentUserId());
        billing.setCreatedAt(LocalDateTime.now());
        billing.setModifiedBy(null);
        billing.setModifiedAt(null);

        // Resolve every line and validate stock BEFORE anything is written, so a
        // single short line aborts the whole bill instead of half-issuing stock.
        List<Inventory> lineInventories = attachAndValidateLines(billing, billingDto, context);

        attachPayments(billing, context.currentUserId());

        Billing savedBilling = billingRepository.save(billing);

        issueStockOut(savedBilling, lineInventories, context);

        return BillingMapper.toDto(savedBilling);
    }


    @Override
    public List<BillingDto> getAllBillings(UserDetails user) {

        BillingContext context = resolveContext(user);

        return billingRepository.findByPharmacy_PharmacyId(context.pharmacyId())
                .stream()
                .map(BillingMapper::toDto)
                .collect(Collectors.toList());
    }


    @Override
    public BillingDto getBillingById(Long billingId, UserDetails user) {

        BillingContext context = resolveContext(user);

        Billing billing = requireBilling(billingId, context.pharmacyId());

        return BillingMapper.toDto(billing);
    }


    @Override
    public BillingDto updateBilling(Long billingId, BillingDto billingDto, UserDetails user) {

        BillingContext context = resolveContext(user);

        requireLines(billingDto);

        Billing billing = requireBilling(billingId, context.pharmacyId());

        // Put the previously sold quantities back before re-applying the new lines,
        // so the stock check for the edited bill sees the stock it had released.
        restoreStock(billing, billing, context);

        billing.getBillingDetails().clear();
        billing.getBillingPayments().clear();

        // Flush the removals so the replacement rows are inserted cleanly.
        billingRepository.saveAndFlush(billing);

        billing.setCustomer(resolveCustomer(
                billingDto,
                context.pharmacy(),
                context.currentUserId()));

        billing.setDoctor(resolveDoctor(billingDto, context.pharmacyId()));
        billing.setCustomerType(billingDto.getCustomerType());
        billing.setPaymentType(billingDto.getPaymentType());
        billing.setOpIpNumber(billingDto.getOpIpNumber());
        billing.setSellingType(billingDto.getSellingType());
        billing.setTotalGrossAmount(billingDto.getTotalGrossAmount());
        billing.setTotalDiscountPercentage(billingDto.getTotalDiscountPercentage());
        billing.setTotalDiscountAmount(billingDto.getTotalDiscountAmount());
        billing.setTotalGstAmount(billingDto.getTotalGstAmount());
        billing.setTotalNetAmount(billingDto.getTotalNetAmount());

        // An edit that omits the prescription keeps the already uploaded one.
        if (billingDto.getPrescriptionUrl() != null
                && !billingDto.getPrescriptionUrl().isBlank()) {
            billing.setPrescriptionUrl(billingDto.getPrescriptionUrl());
        }

        billing.setModifiedBy(context.currentUserId());
        billing.setModifiedAt(LocalDateTime.now());

        for (BillingDetailsDto lineDto : billingDto.getBillingDetails()) {

            BillingDetails detail = BillingDetailsMapper.toEntity(lineDto);
            detail.setBillingDetailsId(null);
            detail.setBilling(billing);

            billing.getBillingDetails().add(detail);
        }

        if (billingDto.getBillingPayments() != null) {

            for (BillingPaymentDto paymentDto : billingDto.getBillingPayments()) {

                BillingPayment payment = BillingPaymentMapper.toEntity(paymentDto);
                payment.setPaymentId(null);
                payment.setBilling(billing);

                billing.getBillingPayments().add(payment);
            }
        }

        List<Inventory> lineInventories = attachAndValidateLines(billing, billingDto, context);

        attachPayments(billing, context.currentUserId());

        Billing savedBilling = billingRepository.save(billing);

        issueStockOut(savedBilling, lineInventories, context);

        return BillingMapper.toDto(savedBilling);
    }


    @Override
    public void deleteBilling(Long billingId, UserDetails user) {

        BillingContext context = resolveContext(user);

        Billing billing = requireBilling(billingId, context.pharmacyId());

        // Give the sold stock back before the bill disappears.
        restoreStock(billing, null, context);

        // The audit trail outlives the bill, so its billing reference is cleared
        // instead of the history rows being deleted along with it.
        List<InventoryAudit> audits =
                inventoryAuditRepository.findByBilling_BillingId(billingId);

        for (InventoryAudit audit : audits) {
            audit.setBilling(null);
        }

        inventoryAuditRepository.saveAll(audits);

        String prescriptionUrl = billing.getPrescriptionUrl();

        billingRepository.delete(billing);

        deleteOldPrescriptionQuietly(prescriptionUrl);
    }


    @Override
    public BillingDto addPayment(
            Long billingId,
            BillingPaymentDto paymentDto,
            UserDetails user) {

        BillingContext context = resolveContext(user);

        Billing billing = requireBilling(billingId, context.pharmacyId());

        if (paymentDto == null
                || paymentDto.getReceivedAmount() == null
                || paymentDto.getReceivedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Received amount must be greater than zero");
        }

        BigDecimal netAmount = billing.getTotalNetAmount() != null
                ? billing.getTotalNetAmount()
                : BigDecimal.ZERO;

        BigDecimal alreadyReceived = totalReceived(billing);

        BigDecimal outstanding = netAmount.subtract(alreadyReceived);

        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(
                    "Bill " + billing.getBillNo() + " is already settled");
        }

        if (paymentDto.getReceivedAmount().compareTo(outstanding) > 0) {
            throw new RuntimeException(
                    "Received amount " + paymentDto.getReceivedAmount()
                            + " exceeds the outstanding amount " + outstanding);
        }

        BigDecimal pendingAmount = outstanding.subtract(paymentDto.getReceivedAmount());

        BillingPayment payment = new BillingPayment();

        payment.setBilling(billing);
        payment.setPaymentMode(paymentDto.getPaymentMode());
        payment.setTransactionId(paymentDto.getTransactionId());
        payment.setReceivedAmount(paymentDto.getReceivedAmount());

        // Derived here rather than trusted from the client: an outstanding balance
        // is what the customer still owes.
        payment.setPendingAmount(pendingAmount);

        payment.setCreatedBy(context.currentUserId());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setModifiedBy(null);
        payment.setModifiedAt(null);

        billing.getBillingPayments().add(payment);

        billing.setPaymentType(resolvePaymentType(
                netAmount,
                alreadyReceived.add(paymentDto.getReceivedAmount())));

        billing.setModifiedBy(context.currentUserId());
        billing.setModifiedAt(LocalDateTime.now());

        Billing savedBilling = billingRepository.save(billing);

        return BillingMapper.toDto(savedBilling);
    }


    private BigDecimal totalReceived(Billing billing) {

        if (billing.getBillingPayments() == null) {
            return BigDecimal.ZERO;
        }

        return billing.getBillingPayments()
                .stream()
                .map(payment -> payment.getReceivedAmount() != null
                        ? payment.getReceivedAmount()
                        : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    private PaymentType resolvePaymentType(BigDecimal netAmount, BigDecimal received) {

        if (received.compareTo(BigDecimal.ZERO) <= 0) {
            return PaymentType.UNPAID;
        }

        if (received.compareTo(netAmount) >= 0) {
            return PaymentType.PAID;
        }

        return PaymentType.PARTIAL;
    }


    @Override
    public PrescriptionUploadDto uploadPrescription(
            Long billingId,
            MultipartFile file,
            UserDetails user) {

        BillingContext context = resolveContext(user);

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Prescription file is required");
        }

        String contentType = file.getContentType() != null
                ? file.getContentType().toLowerCase()
                : "";

        if (!contentType.startsWith("image/") && !contentType.equals("application/pdf")) {
            throw new RuntimeException("Only image or PDF files are allowed");
        }

        Billing billing = requireBilling(billingId, context.pharmacyId());

        String key = buildPrescriptionKey(
                context.pharmacyId(),
                billingId,
                file.getOriginalFilename());

        String prescriptionUrl;

        try {
            prescriptionUrl = s3Service.uploadFile(key, file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload prescription", e);
        }

        String oldPrescriptionUrl = billing.getPrescriptionUrl();

        billing.setPrescriptionUrl(prescriptionUrl);
        billing.setModifiedBy(context.currentUserId());
        billing.setModifiedAt(LocalDateTime.now());

        billingRepository.save(billing);

        deleteOldPrescriptionQuietly(oldPrescriptionUrl);

        return new PrescriptionUploadDto(billing.getBillingId(), prescriptionUrl);
    }


    /**
     * Resolves each bill line onto managed product/batch entities and verifies the
     * requested quantity against locked stock. Nothing is written here.
     */
    private List<Inventory> attachAndValidateLines(
            Billing billing,
            BillingDto billingDto,
            BillingContext context) {

        List<Inventory> lineInventories = new ArrayList<>();

        // A batch can appear on more than one line of the same bill, so the
        // requested quantity is accumulated per inventory row before comparing.
        Map<Long, Long> requestedPerInventory = new LinkedHashMap<>();

        for (int i = 0; i < billing.getBillingDetails().size(); i++) {

            BillingDetails detail = billing.getBillingDetails().get(i);
            BillingDetailsDto dto = billingDto.getBillingDetails().get(i);

            ProductDetails product = productDetailsRepository
                    .findById(dto.getProductId())
                    .orElseThrow(() ->
                            new RuntimeException("Product not found: " + dto.getProductId()));

            // OLD: single-pharmacy check, broke when ProductDetails.pharmacy (ManyToOne)
            // was replaced by pharmacies (ManyToMany).
            // if (product.getPharmacies() == null
            //         || !context.pharmacyId().equals(product.getPharmacy().getPharmacyId())) {
            //     throw new RuntimeException(
            //             "Product does not belong to this pharmacy: " + dto.getProductId());
            // }
            boolean belongsToPharmacy = product.getPharmacies() != null
                    && product.getPharmacies().stream()
                            .anyMatch(p -> context.pharmacyId().equals(p.getPharmacyId()));

            if (!belongsToPharmacy) {
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
            detail.setCreatedBy(context.currentUserId());
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

            Inventory inventory = requireInventory(product, batch, context.pharmacyId());

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

        return lineInventories;
    }


    private void attachPayments(Billing billing, String currentUserId) {

        if (billing.getBillingPayments() == null) {
            return;
        }

        for (BillingPayment payment : billing.getBillingPayments()) {

            payment.setBilling(billing);
            payment.setCreatedBy(currentUserId);
            payment.setCreatedAt(LocalDateTime.now());
            payment.setModifiedBy(null);
            payment.setModifiedAt(null);
        }
    }


    /**
     * Issues stock out once every line has passed validation, writing one
     * OUT / SALE audit row per line.
     */
    private void issueStockOut(
            Billing savedBilling,
            List<Inventory> lineInventories,
            BillingContext context) {

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
            inventory.setModifiedBy(context.currentUserId());
            inventory.setModifiedAt(LocalDateTime.now());

            inventory = inventoryRepository.save(inventory);

            writeAudit(
                    inventory,
                    savedBilling,
                    StockMovement.OUT,
                    TransactionType.SALE,
                    billQuantity,
                    context);
        }
    }


    /**
     * Puts the quantities of the bill's current lines back into stock, writing one
     * IN / STOCK_ADJUSTMENT audit row per line. {@code auditBilling} is null when
     * the bill itself is being deleted.
     */
    private void restoreStock(
            Billing billing,
            Billing auditBilling,
            BillingContext context) {

        if (billing.getBillingDetails() == null) {
            return;
        }

        for (BillingDetails detail : billing.getBillingDetails()) {

            Long billQuantity = detail.getBillQuantity() != null
                    ? detail.getBillQuantity()
                    : 0L;

            if (billQuantity <= 0L) {
                continue;
            }

            Inventory inventory = requireInventory(
                    detail.getProduct(),
                    detail.getBatch(),
                    context.pharmacyId());

            Long currentStock = inventory.getTotalStock() != null
                    ? inventory.getTotalStock()
                    : 0L;

            inventory.setTotalStock(currentStock + billQuantity);
            inventory.setModifiedBy(context.currentUserId());
            inventory.setModifiedAt(LocalDateTime.now());

            inventory = inventoryRepository.save(inventory);

            writeAudit(
                    inventory,
                    auditBilling,
                    StockMovement.IN,
                    TransactionType.STOCK_ADJUSTMENT,
                    billQuantity,
                    context);
        }
    }


    private void writeAudit(
            Inventory inventory,
            Billing billing,
            StockMovement stockMovement,
            TransactionType transactionType,
            Long changeStock,
            BillingContext context) {

        InventoryAudit audit = new InventoryAudit();

        audit.setInventory(inventory);
        audit.setPharmacy(context.pharmacy());
        audit.setBilling(billing);
        audit.setPurchaseDetails(null);
        audit.setStockMovement(stockMovement);
        audit.setTransactionType(transactionType);
        audit.setChangeStock(changeStock);
        audit.setRemainingStock(inventory.getTotalStock());
        audit.setChangedBy(context.currentUserId());
        audit.setChangedAt(LocalDateTime.now());

        inventoryAuditRepository.save(audit);
    }


    private Inventory requireInventory(
            ProductDetails product,
            BatchDetails batch,
            String pharmacyId) {

        PackagingDetails packaging = batch != null
                ? batch.getPackagingDetails()
                : null;

        return inventoryRepository
                .findByPharmacy_PharmacyIdAndProductAndPackagingAndBatch(
                        pharmacyId, product, packaging, batch)
                .orElseThrow(() -> new RuntimeException(
                        "No stock available for product "
                                + (product != null ? product.getProductName() : null)
                                + ", batch "
                                + (batch != null ? batch.getBatchNumber() : null)));
    }


    private Billing requireBilling(Long billingId, String pharmacyId) {

        return billingRepository
                .findByBillingIdAndPharmacy_PharmacyId(billingId, pharmacyId)
                .orElseThrow(() -> new RuntimeException(
                        "Bill not found in this pharmacy with id : " + billingId));
    }


    private void requireLines(BillingDto billingDto) {

        if (billingDto.getBillingDetails() == null
                || billingDto.getBillingDetails().isEmpty()) {
            throw new RuntimeException("A bill must contain at least one product.");
        }
    }


    /**
     * The prescribing doctor is picked from the doctor master, the same way a
     * purchase picks its supplier. Bills without a doctor simply omit the id.
     */
    private DoctorDetails resolveDoctor(BillingDto billingDto, String pharmacyId) {

        if (billingDto.getDoctorId() == null) {
            return null;
        }

        DoctorDetails doctor = doctorDetailsRepository
                .findByDoctorIdAndPharmacyId(billingDto.getDoctorId(), pharmacyId)
                .orElseThrow(() -> new RuntimeException(
                        "Doctor not found in this pharmacy with id : "
                                + billingDto.getDoctorId()));

        return doctor;
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

            // The customer was picked deliberately, so a patient number sent with
            // it is stored against that customer.
            String pickedPatientNumber = billingDto.getPatientNumber() != null
                    ? billingDto.getPatientNumber().trim()
                    : null;

            if (pickedPatientNumber != null
                    && !pickedPatientNumber.isEmpty()
                    && !pickedPatientNumber.equals(customer.getPatientNumber())) {

                customer.setPatientNumber(pickedPatientNumber);
                customer.setModifiedBy(currentUserId);
                customer.setModifiedAt(LocalDateTime.now());

                return customerManagementRepository.save(customer);
            }

            return customer;
        }

        String phoneNo = billingDto.getCustomerPhoneNo() != null
                ? billingDto.getCustomerPhoneNo().trim()
                : null;

        String name = billingDto.getCustomerName() != null
                ? billingDto.getCustomerName().trim()
                : null;

        boolean hasPhone = phoneNo != null && !phoneNo.isEmpty();
        boolean hasName = name != null && !name.isEmpty();

        // Anonymous walk-in: no customer row is created.
        if (!hasPhone && !hasName) {
            return null;
        }

        String patientNumber = billingDto.getPatientNumber() != null
                ? billingDto.getPatientNumber().trim()
                : null;

        boolean hasPatientNumber = patientNumber != null && !patientNumber.isEmpty();

        // A customer is identified by phone AND name, so one number can carry
        // several people. The same pair is reused; a new name on a known number
        // becomes a new customer row.
        if (hasPhone && hasName) {

            List<CustomerManagement> matches = customerManagementRepository
                    .findByPharmacy_PharmacyIdAndCustomerPhoneNoAndCustomerNameIgnoreCase(
                            pharmacyId, phoneNo, name);

            if (!hasPatientNumber) {

                if (!matches.isEmpty()) {
                    return matches.get(0);
                }
            }

            else {

                // Same person, already carrying this patient number.
                for (CustomerManagement match : matches) {
                    if (patientNumber.equalsIgnoreCase(match.getPatientNumber())) {
                        return match;
                    }
                }

                // Known person who has no patient number yet: fill it in rather
                // than creating a second row for them.
                for (CustomerManagement match : matches) {

                    if (match.getPatientNumber() == null
                            || match.getPatientNumber().isBlank()) {

                        match.setPatientNumber(patientNumber);
                        match.setModifiedBy(currentUserId);
                        match.setModifiedAt(LocalDateTime.now());

                        return customerManagementRepository.save(match);
                    }
                }

                // Every match already has a different patient number, so this is a
                // different person -> fall through and register a new customer.
            }
        }

        // Phone with no name: reuse whoever is already on that number rather than
        // piling up nameless rows.
        else if (hasPhone) {

            List<CustomerManagement> matches = customerManagementRepository
                    .findByPharmacy_PharmacyIdAndCustomerPhoneNo(pharmacyId, phoneNo);

            if (!matches.isEmpty()) {
                return matches.get(0);
            }
        }

        CustomerManagement customer = new CustomerManagement();

        customer.setPharmacy(pharmacy);
        customer.setCustomerName(name);
        customer.setCustomerPhoneNo(hasPhone ? phoneNo : null);
        customer.setCustomerAddress(billingDto.getCustomerAddress());
        customer.setPatientNumber(hasPatientNumber ? patientNumber : null);
        customer.setCreatedBy(currentUserId);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setModifiedBy(null);
        customer.setModifiedAt(null);

        return customerManagementRepository.save(customer);
    }


    private BillingContext resolveContext(UserDetails user) {

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

        return new BillingContext(
                pharmacy,
                pharmacyId,
                String.valueOf(persistentUser.getUserId()));
    }


    private String generateBillNo(String pharmacyId) {

        int year = LocalDate.now().getYear();
        String prefix = "BILL-" + year + "-";

        List<String> latest = billingRepository.findLatestBillNo(
                prefix,
                pharmacyId,
                PageRequest.of(0, 1)
        );

        int nextNumber = 1;

        if (!latest.isEmpty()) {

            String latestBillNo = latest.get(0);

            String numberPart = latestBillNo.substring(prefix.length());

            nextNumber = Integer.parseInt(numberPart) + 1;
        }

        return prefix + String.format("%05d", nextNumber);
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


    private record BillingContext(
            PharmacyDetails pharmacy,
            String pharmacyId,
            String currentUserId) {
    }
}
