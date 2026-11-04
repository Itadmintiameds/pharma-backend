package tiameds.pharmabackend.service.impl.auditReports;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.auditReports.ProductWiseReportFilter;
import tiameds.pharmabackend.dto.auditReports.ProductWiseReportPageDto;
import tiameds.pharmabackend.dto.auditReports.ProductWiseReportResponse;
import tiameds.pharmabackend.dto.auditReports.ProductWiseReportRowDto;
import tiameds.pharmabackend.dto.auditReports.ProductWiseReportSummaryDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.warehouse.Warehouse;
import tiameds.pharmabackend.enums.PurchaseReturnStatus;
import tiameds.pharmabackend.enums.SalesReturnStatus;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseRepository;
import tiameds.pharmabackend.service.auditReports.ProductWiseReportService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductWiseReportServiceImpl implements ProductWiseReportService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    // Whitelist of sortable columns: the client sends the key, never SQL.
    private static final Map<String, String> SORT_COLUMNS = Map.ofEntries(
            Map.entry("productName", "product_name"),
            Map.entry("batchNo", "batch_number"),
            Map.entry("purchaseQuantity", "purchase_quantity"),
            Map.entry("supplierReturnQuantity", "supplier_return_quantity"),
            Map.entry("netPurchaseQuantity", "net_purchase_quantity"),
            Map.entry("saleQuantity", "sale_quantity"),
            Map.entry("salesReturnQuantity", "sales_return_quantity"),
            Map.entry("netSaleQuantity", "net_sale_quantity"),
            Map.entry("purchaseAmount", "purchase_amount"),
            Map.entry("supplierReturnAmount", "supplier_return_amount"),
            Map.entry("netPurchaseAmount", "net_purchase_amount"),
            Map.entry("saleAmount", "sale_amount"),
            Map.entry("salesReturnAmount", "sales_return_amount"),
            Map.entry("netSaleAmount", "net_sale_amount"));

    @PersistenceContext
    private EntityManager entityManager;

    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationContextResolver locationContextResolver;


    @Override
    public ProductWiseReportResponse getProductWiseReport(
            ProductWiseReportFilter filter,
            UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate fromDate = filter.getFromDate() != null ? filter.getFromDate() : LocalDate.now();
        LocalDate toDate = filter.getToDate() != null ? filter.getToDate() : fromDate;

        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate cannot be before fromDate.");
        }

        List<String> pharmacyIds;
        List<String> warehouseIds;

        boolean noLocationSent = isEmpty(filter.getPharmacyIds()) && isEmpty(filter.getWarehouseIds());

        if (noLocationSent) {
            pharmacyIds = accessiblePharmacyIds(persistentUser);
            warehouseIds = accessibleWarehouseIds(persistentUser);
        } else {
            pharmacyIds = isEmpty(filter.getPharmacyIds())
                    ? List.of()
                    : checkAccess(filter.getPharmacyIds(), accessiblePharmacyIds(persistentUser), "pharmacy");
            warehouseIds = isEmpty(filter.getWarehouseIds())
                    ? List.of()
                    : checkAccess(filter.getWarehouseIds(), accessibleWarehouseIds(persistentUser), "warehouse");
        }

        if (pharmacyIds.isEmpty() && warehouseIds.isEmpty()) {
            throw new AccessDeniedException("You do not have access to any pharmacy or warehouse.");
        }

        int size = filter.getSize() == null || filter.getSize() < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(filter.getSize(), MAX_PAGE_SIZE);
        int page = filter.getPage() == null || filter.getPage() < 0 ? 0 : filter.getPage();
        long offset = (long) page * size;

        Map<String, Object> params = new HashMap<>();
        String reportSql = buildReportSql(filter, pharmacyIds, warehouseIds, fromDate, toDate, params);

        ProductWiseReportSummaryDto summary = fetchSummary(reportSql, params);
        long totalRecords = summary.getLineItemCount();

        ProductWiseReportPageDto details = new ProductWiseReportPageDto();
        details.setData(totalRecords == 0
                ? List.of()
                : fetchPage(reportSql, params, filter, size, offset));
        details.setPage(page);
        details.setSize(size);
        details.setTotalRecords(totalRecords);
        details.setTotalPages((int) ((totalRecords + size - 1) / size));

        ProductWiseReportResponse response = new ProductWiseReportResponse();
        response.setSummary(summary);
        response.setProductDetails(details);
        response.setGeneratedBy(persistentUser.getFullName());
        response.setGeneratedOn(LocalDateTime.now());

        return response;
    }


    // Every pharmacy in the organization for a SUPER ADMIN, otherwise the
    // ones mapped to the user.
    private List<String> accessiblePharmacyIds(UserDetails user) {

        if (locationContextResolver.isSuperAdmin(user) && user.getOrganization() != null) {
            return pharmacyDetailsRepository
                    .findAllByOrganization_OrganizationIdOrderByPharmacyCity(
                            user.getOrganization().getOrganizationId())
                    .stream()
                    .map(PharmacyDetails::getPharmacyId)
                    .toList();
        }
        return pharmacyDetailsRepository.findPharmacyIdsByUserId(user.getUserId());
    }


    // Every warehouse in the organization for a SUPER ADMIN, otherwise the
    // ones mapped to the user (empty where warehouses are not in use).
    private List<String> accessibleWarehouseIds(UserDetails user) {

        if (locationContextResolver.isSuperAdmin(user) && user.getOrganization() != null) {
            return warehouseRepository
                    .findAllByOrganization_OrganizationIdOrderByWarehouseName(
                            user.getOrganization().getOrganizationId())
                    .stream()
                    .map(Warehouse::getWarehouseId)
                    .toList();
        }
        return userDetailsRepository.findWarehouseIdsByUserId(user.getUserId());
    }


    private List<String> checkAccess(List<String> requested, List<String> accessible, String kind) {

        List<String> denied = requested.stream()
                .filter(id -> !accessible.contains(id))
                .toList();

        if (!denied.isEmpty()) {
            throw new AccessDeniedException(
                    "You are not authorized to view " + kind + ": " + String.join(", ", denied));
        }

        return requested.stream().distinct().toList();
    }


    /*
     * Builds the report as a CTE chain; callers wrap it with SELECT ... FROM report.
     *
     *   purchases        - purchase invoice lines dated in the period
     *   supplier_returns - CONFIRMED supplier returns dated in the period
     *                      (deleted drafts and superseded revisions left out)
     *   sales            - bills raised in the period
     *   sales_returns    - Completed customer returns dated in the period
     *   report           - every product batch with any of the four, joined to
     *                      the product master, with product filters applied once
     *
     * Purchases and supplier returns are booked at a pharmacy or a warehouse,
     * so they are scoped to both; billing only happens at pharmacies. Each
     * half is grouped per product + batch across all in-scope locations.
     * Purchases and supplier returns are entered in purchase units (strip, box)
     * while billing is in stock units, so the purchase side is multiplied by
     * the batch packaging's purchase_unit_contains, free quantity included,
     * exactly as inventory is moved; both sides are then comparable.
     *
     * Supplier filter: purchase-side rows are narrowed by their own invoice's
     * supplier; a sale cannot be traced to an invoice, so the batch is kept
     * when it was ever bought from one of the suppliers.
     */
    private String buildReportSql(
            ProductWiseReportFilter filter,
            List<String> pharmacyIds,
            List<String> warehouseIds,
            LocalDate fromDate,
            LocalDate toDate,
            Map<String, Object> params) {

        // An empty list in IN (...) is invalid SQL, so each location kind is
        // only included when it has ids; with no pharmacy in scope the sales
        // side matches nothing.
        List<String> purchaseScope = new ArrayList<>();
        List<String> returnScope = new ArrayList<>();
        String billScope = "1 = 0";
        String salesReturnScope = "1 = 0";

        if (!pharmacyIds.isEmpty()) {
            params.put("pharmacyIds", pharmacyIds);
            purchaseScope.add("pu.pharmacy_id IN (:pharmacyIds)");
            returnScope.add("x.pharmacy_id IN (:pharmacyIds)");
            billScope = "b.pharmacy_id IN (:pharmacyIds)";
            salesReturnScope = "sr.pharmacy_id IN (:pharmacyIds)";
        }

        if (!warehouseIds.isEmpty()) {
            params.put("warehouseIds", warehouseIds);
            purchaseScope.add("pu.warehouse_id IN (:warehouseIds)");
            returnScope.add("x.warehouse_id IN (:warehouseIds)");
        }

        params.put("fromTs", Timestamp.valueOf(fromDate.atStartOfDay()));
        params.put("toTs", Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
        params.put("confirmedStatus", PurchaseReturnStatus.CONFIRMED.name());
        params.put("completedStatus", SalesReturnStatus.COMPLETED.getLabel());

        // Each fragment starts with a space so it never glues onto the
        // previous one (a text block would drop that leading space).
        StringBuilder supplierFilter = new StringBuilder();
        StringBuilder productFilters = new StringBuilder();

        if (!isEmpty(filter.getSupplierIds())) {
            supplierFilter.append(" AND pu.supplier_id IN (:supplierIds)");
            productFilters.append(" ").append("""
                     AND EXISTS (
                         SELECT 1
                         FROM pharma_purchase_details spd
                         JOIN pharma_purchase spu ON spu.purchase_id = spd.purchase_id
                         WHERE spd.batch_id = ids.batch_id
                           AND spu.supplier_id IN (:supplierIds)
                     )""");
            params.put("supplierIds", filter.getSupplierIds());
        }

        if (!isEmpty(filter.getProductIds())) {
            productFilters.append(" AND p.product_id IN (:productIds)");
            params.put("productIds", filter.getProductIds());
        }

        if (!isEmpty(filter.getProductNames())) {
            productFilters.append(" AND LOWER(TRIM(p.product_name)) IN (:productNames)");
            params.put("productNames", lowerTrimmed(filter.getProductNames()));
        }

        if (!isEmpty(filter.getProductCategoryIds())) {
            productFilters.append(" AND p.product_category_id IN (:productCategoryIds)");
            params.put("productCategoryIds", filter.getProductCategoryIds());
        }

        if (hasText(filter.getBatchNo())) {
            productFilters.append(" AND bt.batch_number ILIKE :batchNo");
            params.put("batchNo", "%" + escapeLike(filter.getBatchNo().trim()) + "%");
        }

        return """
                WITH purchases AS (
                    SELECT pd.product_id,
                           pd.batch_id,
                           SUM((COALESCE(pd.purchase_quantity, 0) + COALESCE(pd.free_quantity, 0))
                               * COALESCE(pk.purchase_unit_contains, 1)) AS qty,
                           SUM(COALESCE(pd.net_amount,
                                        COALESCE(pd.gross_amount, 0) - COALESCE(pd.discount, 0)
                                            + COALESCE(pd.gst, 0))) AS amount
                    FROM pharma_purchase_details pd
                    JOIN pharma_purchase pu ON pu.purchase_id = pd.purchase_id
                    LEFT JOIN pharma_batch_details pb ON pb.batch_id = pd.batch_id
                    LEFT JOIN pharma_packaging_details pk ON pk.packaging_id = pb.packaging_id
                    WHERE (%3$s)
                      AND COALESCE(pu.invoice_date, pu.created_at) >= :fromTs
                      AND COALESCE(pu.invoice_date, pu.created_at) <  :toTs
                      %1$s
                    GROUP BY pd.product_id, pd.batch_id
                ),
                supplier_returns AS (
                    SELECT d.product_id,
                           d.batch_id,
                           SUM((COALESCE(d.purchase_return_quantity, 0) + COALESCE(d.free_return_quantity, 0))
                               * COALESCE(pk.purchase_unit_contains, 1)) AS qty,
                           SUM(COALESCE(d.net_amount,
                                        COALESCE(d.gross_amount, 0) - COALESCE(d.discount_amount, 0)
                                            + COALESCE(d.gst_amount, 0))) AS amount
                    FROM pharma_purchase_return x
                    JOIN pharma_purchase_return_details d
                      ON d.purchase_return_id = x.purchase_return_id
                    LEFT JOIN pharma_purchase pu ON pu.purchase_id = x.purchase_id
                    LEFT JOIN pharma_batch_details pb ON pb.batch_id = d.batch_id
                    LEFT JOIN pharma_packaging_details pk ON pk.packaging_id = pb.packaging_id
                    WHERE (%4$s)
                      AND x.status = :confirmedStatus
                      AND COALESCE(x.is_draft_deleted, FALSE) = FALSE
                      AND COALESCE(d.is_active, TRUE) = TRUE
                      AND COALESCE(x.purchase_return_date, x.created_at) >= :fromTs
                      AND COALESCE(x.purchase_return_date, x.created_at) <  :toTs
                      %1$s
                    GROUP BY d.product_id, d.batch_id
                ),
                sales AS (
                    SELECT bd.product_id,
                           bd.batch_id,
                           SUM(COALESCE(bd.bill_quantity, 0)) AS qty,
                           SUM(COALESCE(bd.net_amount, 0))    AS amount
                    FROM pharma_billing_details bd
                    JOIN pharma_billing b ON b.billing_id = bd.billing_id
                    WHERE %5$s
                      AND b.created_at >= :fromTs
                      AND b.created_at <  :toTs
                    GROUP BY bd.product_id, bd.batch_id
                ),
                sales_returns AS (
                    SELECT srd.product_id,
                           srd.batch_id,
                           SUM(COALESCE(srd.sales_return_quantity, 0)) AS qty,
                           SUM(COALESCE(srd.net_amount, 0))            AS amount
                    FROM pharma_sales_return_details srd
                    JOIN pharma_sales_return sr ON sr.sales_return_id = srd.sales_return_id
                    WHERE %6$s
                      AND sr.sales_return_status = :completedStatus
                      AND COALESCE(sr.sales_return_date, sr.created_at) >= :fromTs
                      AND COALESCE(sr.sales_return_date, sr.created_at) <  :toTs
                    GROUP BY srd.product_id, srd.batch_id
                ),
                report AS (
                    SELECT p.product_id,
                           p.product_name,
                           ids.batch_id,
                           bt.batch_number,
                           COALESCE(pr.qty, 0)                              AS purchase_quantity,
                           COALESCE(spr.qty, 0)                             AS supplier_return_quantity,
                           COALESCE(pr.qty, 0) - COALESCE(spr.qty, 0)       AS net_purchase_quantity,
                           COALESCE(s.qty, 0)                               AS sale_quantity,
                           COALESCE(sr.qty, 0)                              AS sales_return_quantity,
                           COALESCE(s.qty, 0) - COALESCE(sr.qty, 0)         AS net_sale_quantity,
                           COALESCE(pr.amount, 0)                           AS purchase_amount,
                           COALESCE(spr.amount, 0)                          AS supplier_return_amount,
                           COALESCE(pr.amount, 0) - COALESCE(spr.amount, 0) AS net_purchase_amount,
                           COALESCE(s.amount, 0)                            AS sale_amount,
                           COALESCE(sr.amount, 0)                           AS sales_return_amount,
                           COALESCE(s.amount, 0) - COALESCE(sr.amount, 0)   AS net_sale_amount
                    FROM (SELECT product_id, batch_id FROM purchases
                          UNION
                          SELECT product_id, batch_id FROM supplier_returns
                          UNION
                          SELECT product_id, batch_id FROM sales
                          UNION
                          SELECT product_id, batch_id FROM sales_returns) ids
                    JOIN pharma_product_details p ON p.product_id = ids.product_id
                    LEFT JOIN pharma_batch_details bt ON bt.batch_id = ids.batch_id
                    LEFT JOIN purchases pr
                           ON pr.product_id = ids.product_id
                          AND pr.batch_id IS NOT DISTINCT FROM ids.batch_id
                    LEFT JOIN supplier_returns spr
                           ON spr.product_id = ids.product_id
                          AND spr.batch_id IS NOT DISTINCT FROM ids.batch_id
                    LEFT JOIN sales s
                           ON s.product_id = ids.product_id
                          AND s.batch_id IS NOT DISTINCT FROM ids.batch_id
                    LEFT JOIN sales_returns sr
                           ON sr.product_id = ids.product_id
                          AND sr.batch_id IS NOT DISTINCT FROM ids.batch_id
                    WHERE 1 = 1
                      %2$s
                )
                """.formatted(
                        supplierFilter,
                        productFilters,
                        String.join(" OR ", purchaseScope),
                        String.join(" OR ", returnScope),
                        billScope,
                        salesReturnScope);
    }


    private ProductWiseReportSummaryDto fetchSummary(String reportSql, Map<String, Object> params) {

        Object[] row = (Object[]) nativeQuery(reportSql + """
                SELECT COUNT(DISTINCT product_id),
                       COUNT(*),
                       COALESCE(SUM(purchase_quantity), 0),
                       COALESCE(SUM(supplier_return_quantity), 0),
                       COALESCE(SUM(net_purchase_quantity), 0),
                       COALESCE(SUM(sale_quantity), 0),
                       COALESCE(SUM(sales_return_quantity), 0),
                       COALESCE(SUM(net_sale_quantity), 0),
                       COALESCE(SUM(purchase_amount), 0),
                       COALESCE(SUM(supplier_return_amount), 0),
                       COALESCE(SUM(net_purchase_amount), 0),
                       COALESCE(SUM(sale_amount), 0),
                       COALESCE(SUM(sales_return_amount), 0),
                       COALESCE(SUM(net_sale_amount), 0)
                FROM report
                """, params).getSingleResult();

        ProductWiseReportSummaryDto summary = new ProductWiseReportSummaryDto();
        summary.setProductCount(toLong(row[0]));
        summary.setLineItemCount(toLong(row[1]));
        summary.setTotalPurchaseQuantity(toLong(row[2]));
        summary.setTotalSupplierReturnQuantity(toLong(row[3]));
        summary.setTotalNetPurchaseQuantity(toLong(row[4]));
        summary.setTotalSaleQuantity(toLong(row[5]));
        summary.setTotalSalesReturnQuantity(toLong(row[6]));
        summary.setTotalNetSaleQuantity(toLong(row[7]));
        summary.setTotalPurchaseAmount(toAmount(row[8]));
        summary.setTotalSupplierReturnAmount(toAmount(row[9]));
        summary.setTotalNetPurchaseAmount(toAmount(row[10]));
        summary.setTotalSaleAmount(toAmount(row[11]));
        summary.setTotalSalesReturnAmount(toAmount(row[12]));
        summary.setTotalNetSaleAmount(toAmount(row[13]));

        return summary;
    }


    @SuppressWarnings("unchecked")
    private List<ProductWiseReportRowDto> fetchPage(
            String reportSql,
            Map<String, Object> params,
            ProductWiseReportFilter filter,
            int size,
            long offset) {

        String sortColumn = SORT_COLUMNS.getOrDefault(
                hasText(filter.getSortBy()) ? filter.getSortBy() : "productName", null);

        if (sortColumn == null) {
            throw new IllegalArgumentException("Invalid sortBy: " + filter.getSortBy()
                    + ". Allowed: " + String.join(", ", SORT_COLUMNS.keySet()));
        }

        String sortDir = "desc".equalsIgnoreCase(filter.getSortDir()) ? "DESC" : "ASC";

        // product_id + batch_id as tie-breakers keep page boundaries stable
        // when several rows share the same sort value.
        Query query = nativeQuery(reportSql + """
                SELECT product_id, product_name, batch_number,
                       purchase_quantity, supplier_return_quantity, net_purchase_quantity,
                       sale_quantity, sales_return_quantity, net_sale_quantity,
                       purchase_amount, supplier_return_amount, net_purchase_amount,
                       sale_amount, sales_return_amount, net_sale_amount
                FROM report
                ORDER BY %s %s NULLS LAST, product_id, batch_id
                LIMIT :limit OFFSET :offset
                """.formatted(sortColumn, sortDir), params);

        query.setParameter("limit", size);
        query.setParameter("offset", offset);

        List<Object[]> results = query.getResultList();
        List<ProductWiseReportRowDto> rows = new ArrayList<>(results.size());

        for (Object[] r : results) {
            ProductWiseReportRowDto dto = new ProductWiseReportRowDto();
            dto.setProductId((String) r[0]);
            dto.setProductName((String) r[1]);
            dto.setBatchNo((String) r[2]);
            dto.setPurchaseQuantity(toLong(r[3]));
            dto.setSupplierReturnQuantity(toLong(r[4]));
            dto.setNetPurchaseQuantity(toLong(r[5]));
            dto.setSaleQuantity(toLong(r[6]));
            dto.setSalesReturnQuantity(toLong(r[7]));
            dto.setNetSaleQuantity(toLong(r[8]));
            dto.setPurchaseAmount(toAmount(r[9]));
            dto.setSupplierReturnAmount(toAmount(r[10]));
            dto.setNetPurchaseAmount(toAmount(r[11]));
            dto.setSaleAmount(toAmount(r[12]));
            dto.setSalesReturnAmount(toAmount(r[13]));
            dto.setNetSaleAmount(toAmount(r[14]));
            rows.add(dto);
        }

        return rows;
    }


    private Query nativeQuery(String sql, Map<String, Object> params) {

        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);
        return query;
    }


    private static long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }


    private static BigDecimal toAmount(Object value) {

        if (value == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal amount = value instanceof BigDecimal bd ? bd : new BigDecimal(value.toString());
        return amount.setScale(2, RoundingMode.HALF_UP);
    }


    // Treats user input as literal text inside ILIKE (no % / _ wildcards).
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }


    private static List<String> lowerTrimmed(List<String> values) {
        return values.stream().map(v -> v.trim().toLowerCase()).toList();
    }


    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }


    private static boolean isEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }
}
