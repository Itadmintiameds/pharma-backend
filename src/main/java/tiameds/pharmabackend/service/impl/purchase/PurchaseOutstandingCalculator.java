package tiameds.pharmabackend.service.impl.purchase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tiameds.pharmabackend.dto.purchase.PurchaseDto;
import tiameds.pharmabackend.dto.purchase.PurchaseReturnDto;
import tiameds.pharmabackend.entity.purchase.Purchase;
import tiameds.pharmabackend.enums.PurchaseReturnStatus;
import tiameds.pharmabackend.repository.purchase.PurchaseRepository;
import tiameds.pharmabackend.repository.purchase.PurchaseReturnRepository;
import tiameds.pharmabackend.repository.supplier.SupplierPaymentRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Computes a purchase's outstanding balance on read. Nothing is stored: the
 * figures follow live supplier payments and confirmed purchase returns, so a
 * new payment or return shows up on the next GET.
 * <p>
 * Same rule as the supplier payment KPIs:
 * <ul>
 *   <li>credit purchase: outstanding = net amount - paid - confirmed returns,
 *       floored at 0; anything below 0 is supplier credit (the supplier owes
 *       it back)</li>
 *   <li>any other purchase was paid upfront: outstanding is 0 and confirmed
 *       returns are supplier credit</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class PurchaseOutstandingCalculator {

    private static final String PURCHASE_PAYMENT_TYPE_CREDIT = "CREDIT";

    private final SupplierPaymentRepository supplierPaymentRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;
    private final PurchaseRepository purchaseRepository;


    // Fills paid / returned / outstanding / supplier credit on each purchase.
    public void applyToPurchases(List<PurchaseDto> purchases) {

        if (purchases == null || purchases.isEmpty()) {
            return;
        }

        Set<Long> purchaseIds = purchases.stream()
                .map(PurchaseDto::getPurchaseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, BigDecimal> paid = paidByPurchase(purchaseIds);
        Map<Long, BigDecimal> returned = returnedByPurchase(purchaseIds);

        for (PurchaseDto purchase : purchases) {

            Balance balance = balance(
                    purchase.getTotalNetAmount(),
                    purchase.getPaymentType(),
                    paid.getOrDefault(purchase.getPurchaseId(), BigDecimal.ZERO),
                    returned.getOrDefault(purchase.getPurchaseId(), BigDecimal.ZERO));

            purchase.setPaidAmount(balance.paid());
            purchase.setReturnedAmount(balance.returned());
            purchase.setOutstandingAmount(balance.outstanding());
            purchase.setSupplierCreditAmount(balance.supplierCredit());
        }
    }


    // Fills the parent purchase's current balance on each return.
    public void applyToReturns(List<PurchaseReturnDto> returns) {

        if (returns == null || returns.isEmpty()) {
            return;
        }

        Set<Long> purchaseIds = returns.stream()
                .map(PurchaseReturnDto::getPurchaseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (purchaseIds.isEmpty()) {
            return;
        }

        Map<Long, Purchase> purchases = new HashMap<>();
        purchaseRepository.findAllById(purchaseIds).forEach(p -> purchases.put(p.getPurchaseId(), p));

        Map<Long, BigDecimal> paid = paidByPurchase(purchaseIds);
        Map<Long, BigDecimal> returned = returnedByPurchase(purchaseIds);

        for (PurchaseReturnDto purchaseReturn : returns) {

            Purchase purchase = purchases.get(purchaseReturn.getPurchaseId());

            if (purchase == null) {
                continue;
            }

            Balance balance = balance(
                    purchase.getTotalNetAmount(),
                    purchase.getPaymentType(),
                    paid.getOrDefault(purchase.getPurchaseId(), BigDecimal.ZERO),
                    returned.getOrDefault(purchase.getPurchaseId(), BigDecimal.ZERO));

            purchaseReturn.setPurchaseNetAmount(balance.netAmount());
            purchaseReturn.setPurchasePaidAmount(balance.paid());
            purchaseReturn.setPurchaseReturnedAmount(balance.returned());
            purchaseReturn.setPurchaseOutstandingAmount(balance.outstanding());
            purchaseReturn.setSupplierCreditAmount(balance.supplierCredit());
        }
    }


    private static Balance balance(BigDecimal netAmount, String paymentType, BigDecimal paid, BigDecimal returned) {

        BigDecimal net = netAmount != null ? netAmount : BigDecimal.ZERO;

        if (!PURCHASE_PAYMENT_TYPE_CREDIT.equalsIgnoreCase(paymentType)) {
            // Paid upfront: nothing owed, returns are owed back by the supplier.
            return new Balance(net, paid, returned, BigDecimal.ZERO, returned);
        }

        BigDecimal remaining = net.subtract(paid).subtract(returned);

        return remaining.signum() >= 0
                ? new Balance(net, paid, returned, remaining, BigDecimal.ZERO)
                : new Balance(net, paid, returned, BigDecimal.ZERO, remaining.negate());
    }


    private Map<Long, BigDecimal> paidByPurchase(Collection<Long> purchaseIds) {
        return purchaseIds.isEmpty()
                ? Map.of()
                : toAmountMap(supplierPaymentRepository.sumPaymentAmountPerPurchaseIds(purchaseIds));
    }


    private Map<Long, BigDecimal> returnedByPurchase(Collection<Long> purchaseIds) {
        return purchaseIds.isEmpty()
                ? Map.of()
                : toAmountMap(purchaseReturnRepository.sumReturnAmountPerPurchaseIds(
                        purchaseIds, PurchaseReturnStatus.CONFIRMED));
    }


    // [purchaseId, amount] rows -> lookup by purchase id. Payments are stored
    // as double, so amounts go through their string form and are rounded to
    // paise (a double sum can come back as 0.30000000000000004).
    private static Map<Long, BigDecimal> toAmountMap(List<Object[]> rows) {

        Map<Long, BigDecimal> amounts = new HashMap<>();

        for (Object[] row : rows) {
            BigDecimal amount = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            amounts.put((Long) row[0], amount.setScale(2, RoundingMode.HALF_UP));
        }

        return amounts;
    }


    private record Balance(
            BigDecimal netAmount,
            BigDecimal paid,
            BigDecimal returned,
            BigDecimal outstanding,
            BigDecimal supplierCredit) {
    }
}
