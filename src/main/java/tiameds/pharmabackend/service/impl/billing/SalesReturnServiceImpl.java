package tiameds.pharmabackend.service.impl.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import tiameds.pharmabackend.context.CurrentPharmacyContext;
import tiameds.pharmabackend.dto.billing.SalesReturnDetailsDto;
import tiameds.pharmabackend.dto.billing.SalesReturnDto;
import tiameds.pharmabackend.dto.billing.SalesReturnKpiResponse;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.billing.Billing;
import tiameds.pharmabackend.entity.billing.BillingDetails;
import tiameds.pharmabackend.entity.billing.SalesReturn;
import tiameds.pharmabackend.entity.billing.SalesReturnDetails;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.purchase.Inventory;
import tiameds.pharmabackend.entity.purchase.InventoryAudit;
import tiameds.pharmabackend.enums.BillReturnStatus;
import tiameds.pharmabackend.enums.SalesReturnStatus;
import tiameds.pharmabackend.enums.StockMovement;
import tiameds.pharmabackend.enums.TransactionType;
import tiameds.pharmabackend.mapper.billing.SalesReturnMapper;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.billing.BillingRepository;
import tiameds.pharmabackend.repository.billing.SalesReturnDetailsRepository;
import tiameds.pharmabackend.repository.billing.SalesReturnRepository;
import tiameds.pharmabackend.repository.purchase.InventoryAuditRepository;
import tiameds.pharmabackend.repository.purchase.InventoryRepository;
import tiameds.pharmabackend.service.billing.SalesReturnService;

@Service
@RequiredArgsConstructor
@Transactional
public class SalesReturnServiceImpl implements SalesReturnService {

    private final SalesReturnRepository salesReturnRepository;
    private final SalesReturnDetailsRepository salesReturnDetailsRepository;
    private final BillingRepository billingRepository;
    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryAuditRepository inventoryAuditRepository;
    private final CurrentPharmacyContext pharmacyContext;


    @Override
    public SalesReturnDto createSalesReturn(SalesReturnDto salesReturnDto, UserDetails user) {

        UserDetails persistentUser = requireUser(user);

        String pharmacyId = requirePharmacy(persistentUser);

        requireLines(salesReturnDto);

        if (salesReturnDto.getBillingId() == null) {
            throw new RuntimeException("Bill id is required for a sales return.");
        }

        // Locked so a concurrent return against the same bill waits for this one
        // and then sees its quantities as already returned.
        Billing billing = billingRepository
                .findForUpdate(salesReturnDto.getBillingId(), pharmacyId)
                .orElseThrow(() -> new RuntimeException(
                        "Bill not found in this pharmacy with id : "
                                + salesReturnDto.getBillingId()));

        String actor = String.valueOf(persistentUser.getUserId());
        LocalDateTime now = LocalDateTime.now();

        Map<LineKey, BilledLine> billedLines = summarizeBill(billing);
        Map<LineKey, Long> returnedBefore = returnedSoFar(billing.getBillingId());

        // Quantity requested on this return so far, so the same product + batch
        // sent on two lines is checked against the bill as a whole.
        Map<LineKey, Long> requestedNow = new HashMap<>();

        SalesReturn salesReturn = new SalesReturn();

        salesReturn.setBilling(billing);
        salesReturn.setPharmacyId(pharmacyId);
        salesReturn.setSalesReturnNo(generateSalesReturnNo(pharmacyId));
        salesReturn.setSalesReturnDate(salesReturnDto.getSalesReturnDate() != null
                ? salesReturnDto.getSalesReturnDate()
                : now);
        salesReturn.setSalesReturnStatus(SalesReturnStatus.COMPLETED);
        salesReturn.setCreatedBy(actor);
        salesReturn.setCreatedAt(now);
        salesReturn.setModifiedBy(null);
        salesReturn.setModifiedAt(null);

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalGst = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        for (SalesReturnDetailsDto lineDto : salesReturnDto.getSalesReturnDetails()) {

            if (lineDto.getProductId() == null || lineDto.getBatchId() == null) {
                throw new RuntimeException("Product id and batch id are required on every line.");
            }

            LineKey key = new LineKey(lineDto.getProductId(), lineDto.getBatchId());

            BilledLine billed = billedLines.get(key);

            if (billed == null) {
                throw new RuntimeException(
                        "Product " + lineDto.getProductId()
                                + ", batch " + lineDto.getBatchId()
                                + " is not on bill " + billing.getBillNo());
            }

            Long quantity = lineDto.getSalesReturnQuantity() != null
                    ? lineDto.getSalesReturnQuantity()
                    : 0L;

            if (quantity <= 0L) {
                throw new RuntimeException(
                        "Return quantity must be greater than zero for product: "
                                + billed.product().getProductName());
            }

            long previouslyReturned = returnedBefore.getOrDefault(key, 0L)
                    + requestedNow.getOrDefault(key, 0L);

            long cumulativeReturned = previouslyReturned + quantity;

            if (cumulativeReturned > billed.quantity()) {
                throw new RuntimeException(
                        "Cannot return " + quantity + " of product "
                                + billed.product().getProductName()
                                + ", batch " + billed.batch().getBatchNumber()
                                + ". Billed: " + billed.quantity()
                                + ", already returned: " + previouslyReturned);
            }

            requestedNow.merge(key, quantity, Long::sum);

            SalesReturnDetails detail = new SalesReturnDetails();

            detail.setSalesReturn(salesReturn);
            detail.setProduct(billed.product());
            detail.setBatch(billed.batch());
            detail.setSalesReturnQuantity(quantity);
            detail.setSalesReturnReason(lineDto.getSalesReturnReason());

            // Refunded at what the customer actually paid on the bill, not at a
            // client-sent price.
            detail.setGrossAmount(share(billed.grossAmount(), previouslyReturned, cumulativeReturned, billed.quantity()));
            detail.setGstAmount(share(billed.gstAmount(), previouslyReturned, cumulativeReturned, billed.quantity()));
            detail.setNetAmount(share(billed.netAmount(), previouslyReturned, cumulativeReturned, billed.quantity()));

            detail.setCreatedBy(actor);
            detail.setCreatedAt(now);
            detail.setModifiedBy(null);
            detail.setModifiedAt(null);

            salesReturn.getSalesReturnDetails().add(detail);

            totalGross = totalGross.add(detail.getGrossAmount());
            totalGst = totalGst.add(detail.getGstAmount());
            totalNet = totalNet.add(detail.getNetAmount());
        }

        salesReturn.setTotalGrossAmount(totalGross);
        salesReturn.setTotalGstAmount(totalGst);
        salesReturn.setTotalNetAmount(totalNet);

        SalesReturn savedSalesReturn = salesReturnRepository.save(salesReturn);

        restock(savedSalesReturn, billing, actor, now);

        billing.setSalesReturnStatus(resolveBillReturnStatus(billedLines, returnedBefore, requestedNow));
        billingRepository.save(billing);

        return SalesReturnMapper.toDto(savedSalesReturn);
    }


    @Override
    public List<SalesReturnDto> getAllSalesReturns(UserDetails user) {

        String pharmacyId = requirePharmacy(requireUser(user));

        return salesReturnRepository.findByPharmacyId(pharmacyId)
                .stream()
                .map(SalesReturnMapper::toDto)
                .collect(Collectors.toList());
    }


    @Override
    public SalesReturnDto getSalesReturnById(Long salesReturnId, UserDetails user) {

        String pharmacyId = requirePharmacy(requireUser(user));

        SalesReturn salesReturn = salesReturnRepository
                .findBySalesReturnIdAndPharmacyId(salesReturnId, pharmacyId)
                .orElseThrow(() -> new RuntimeException(
                        "Sales return not found in this pharmacy with id : " + salesReturnId));

        return SalesReturnMapper.toDto(salesReturn);
    }


    @Override
    public SalesReturnKpiResponse getSalesReturnKpis(UserDetails user) {

        String pharmacyId = requirePharmacy(requireUser(user));

        SalesReturnKpiResponse response = new SalesReturnKpiResponse();

        response.setTotalSalesReturns(salesReturnRepository
                .countByPharmacyIdAndSalesReturnStatus(pharmacyId, SalesReturnStatus.COMPLETED));

        response.setTotalReturnAmount(salesReturnRepository
                .sumTotalNetAmount(pharmacyId, SalesReturnStatus.COMPLETED));

        return response;
    }


    private UserDetails requireUser(UserDetails user) {

        return userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }


    // The selected pharmacy, after checking the user is mapped to it.
    private String requirePharmacy(UserDetails persistentUser) {

        String pharmacyId = pharmacyContext.getCurrentPharmacy();

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        return pharmacyId;
    }


    /**
     * Puts each returned quantity back into the pharmacy stock for its product
     * + batch, writing one IN / SALES_RETURN audit row per line.
     */
    private void restock(
            SalesReturn salesReturn,
            Billing billing,
            String actor,
            LocalDateTime now) {

        for (SalesReturnDetails detail : salesReturn.getSalesReturnDetails()) {

            Inventory inventory = requireInventory(
                    detail.getProduct(),
                    detail.getBatch(),
                    salesReturn.getPharmacyId());

            Long currentStock = inventory.getTotalStock() != null
                    ? inventory.getTotalStock()
                    : 0L;

            // salesReturnQuantity is in smallest units, the same unit as
            // bill_quantity and Inventory.totalStock.
            inventory.setTotalStock(currentStock + detail.getSalesReturnQuantity());
            inventory.setModifiedBy(actor);
            inventory.setModifiedAt(now);

            inventory = inventoryRepository.save(inventory);

            InventoryAudit audit = new InventoryAudit();

            audit.setInventory(inventory);
            audit.setPharmacy(billing.getPharmacy());
            audit.setBilling(billing);
            audit.setPurchaseDetails(null);
            audit.setStockMovement(StockMovement.IN);
            audit.setTransactionType(TransactionType.SALES_RETURN);
            audit.setChangeStock(detail.getSalesReturnQuantity());
            audit.setRemainingStock(inventory.getTotalStock());
            audit.setChangedBy(actor);
            audit.setChangedAt(now);

            inventoryAuditRepository.save(audit);
        }
    }


    /**
     * Not Returned while nothing has come back, Returned once every billed unit
     * has, Partially Returned in between. Lines are capped at their billed
     * quantity, so equal totals means every line is fully returned.
     */
    private BillReturnStatus resolveBillReturnStatus(
            Map<LineKey, BilledLine> billedLines,
            Map<LineKey, Long> returnedBefore,
            Map<LineKey, Long> requestedNow) {

        long totalBilled = billedLines.values()
                .stream()
                .mapToLong(BilledLine::quantity)
                .sum();

        long totalReturned = returnedBefore.values().stream().mapToLong(Long::longValue).sum()
                + requestedNow.values().stream().mapToLong(Long::longValue).sum();

        if (totalReturned <= 0L) {
            return BillReturnStatus.NOT_RETURNED;
        }

        if (totalReturned >= totalBilled) {
            return BillReturnStatus.RETURNED;
        }

        return BillReturnStatus.PARTIALLY_RETURNED;
    }


    /**
     * The bill's lines grouped by product + batch. A batch can appear on more
     * than one line of a bill, so quantities and amounts are summed per key.
     */
    private Map<LineKey, BilledLine> summarizeBill(Billing billing) {

        Map<LineKey, BilledLine> billedLines = new LinkedHashMap<>();

        for (BillingDetails detail : billing.getBillingDetails()) {

            if (detail.getProduct() == null || detail.getBatch() == null) {
                continue;
            }

            LineKey key = new LineKey(
                    detail.getProduct().getProductId(),
                    detail.getBatch().getBatchId());

            BilledLine line = new BilledLine(
                    detail.getProduct(),
                    detail.getBatch(),
                    detail.getBillQuantity() != null ? detail.getBillQuantity() : 0L,
                    orZero(detail.getGrossAmount()),
                    orZero(detail.getGstAmount()),
                    orZero(detail.getNetAmount()));

            billedLines.merge(key, line, BilledLine::plus);
        }

        return billedLines;
    }


    private Map<LineKey, Long> returnedSoFar(Long billingId) {

        Map<LineKey, Long> returned = new HashMap<>();

        List<Object[]> rows = salesReturnDetailsRepository.sumReturnedQuantityByProductAndBatch(
                billingId,
                SalesReturnStatus.COMPLETED);

        for (Object[] row : rows) {

            LineKey key = new LineKey((String) row[0], (String) row[1]);
            Long quantity = row[2] != null ? ((Number) row[2]).longValue() : 0L;

            returned.put(key, quantity);
        }

        return returned;
    }


    /**
     * The part of a bill line's amount that belongs to units
     * (previouslyReturned, cumulativeReturned]. Taken as the difference of two
     * cumulative shares so that a line returned in several goes adds up to
     * exactly the billed amount, with no rounding drift.
     */
    private BigDecimal share(
            BigDecimal lineAmount,
            long previouslyReturned,
            long cumulativeReturned,
            long billedQuantity) {

        if (billedQuantity <= 0L) {
            return BigDecimal.ZERO;
        }

        return cumulativeShare(lineAmount, cumulativeReturned, billedQuantity)
                .subtract(cumulativeShare(lineAmount, previouslyReturned, billedQuantity));
    }


    private BigDecimal cumulativeShare(BigDecimal lineAmount, long quantity, long billedQuantity) {

        return lineAmount
                .multiply(BigDecimal.valueOf(quantity))
                .divide(BigDecimal.valueOf(billedQuantity), 2, RoundingMode.HALF_UP);
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
                        "No inventory found for product "
                                + (product != null ? product.getProductName() : null)
                                + ", batch "
                                + (batch != null ? batch.getBatchNumber() : null)));
    }


    private void requireLines(SalesReturnDto salesReturnDto) {

        if (salesReturnDto.getSalesReturnDetails() == null
                || salesReturnDto.getSalesReturnDetails().isEmpty()) {
            throw new RuntimeException("A sales return must contain at least one product.");
        }
    }


    private String generateSalesReturnNo(String pharmacyId) {

        int year = LocalDate.now().getYear();
        String prefix = "SLR-" + year + "-";

        List<String> latest = salesReturnRepository.findLatestSalesReturnNo(
                prefix,
                pharmacyId,
                PageRequest.of(0, 1)
        );

        int nextNumber = 1;

        if (!latest.isEmpty()) {

            String latestSalesReturnNo = latest.get(0);

            String numberPart = latestSalesReturnNo.substring(prefix.length());

            nextNumber = Integer.parseInt(numberPart) + 1;
        }

        return prefix + String.format("%05d", nextNumber);
    }


    private static BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }


    private record LineKey(String productId, String batchId) {
    }


    private record BilledLine(
            ProductDetails product,
            BatchDetails batch,
            long quantity,
            BigDecimal grossAmount,
            BigDecimal gstAmount,
            BigDecimal netAmount) {

        BilledLine plus(BilledLine other) {
            return new BilledLine(
                    product,
                    batch,
                    quantity + other.quantity,
                    grossAmount.add(other.grossAmount),
                    gstAmount.add(other.gstAmount),
                    netAmount.add(other.netAmount));
        }
    }
}
