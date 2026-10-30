package tiameds.pharmabackend.service.impl.auditReports;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.auditReports.StockMovementReportFilter;
import tiameds.pharmabackend.dto.auditReports.StockMovementReportPageDto;
import tiameds.pharmabackend.dto.auditReports.StockMovementReportResponse;
import tiameds.pharmabackend.dto.auditReports.StockMovementReportRowDto;
import tiameds.pharmabackend.dto.auditReports.StockMovementReportSummaryDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.warehouse.Warehouse;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseRepository;
import tiameds.pharmabackend.service.auditReports.StockMovementReportService;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockMovementReportServiceImpl implements StockMovementReportService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    // Movement Type dropdown: code -> label. The codes are produced by the
    // CASE in buildReportSql; insertion order is the order offered to users.
    private static final Map<String, String> MOVEMENT_TYPES = new LinkedHashMap<>();

    static {
        MOVEMENT_TYPES.put("PURCHASE_RECEIPT", "Purchase Receipt");
        MOVEMENT_TYPES.put("ALLOCATION_IN", "Allocation In");
        MOVEMENT_TYPES.put("ALLOCATION_OUT", "Allocation Out");
        MOVEMENT_TYPES.put("STOCK_TRANSFER_IN", "Stock Transfer In");
        MOVEMENT_TYPES.put("STOCK_TRANSFER_OUT", "Stock Transfer Out");
        MOVEMENT_TYPES.put("PHARMACY_SALE", "Pharmacy Sale");
        MOVEMENT_TYPES.put("SALES_RETURN", "Sales Return");
        MOVEMENT_TYPES.put("PURCHASE_RETURN", "Purchase Return");
        MOVEMENT_TYPES.put("DAMAGE_CAPTURE", "Damage Capture");
        MOVEMENT_TYPES.put("DAMAGE_RETURN", "Damage Return");
        MOVEMENT_TYPES.put("EXPIRED_STOCK_RETURN", "Expired Stock Return");
        MOVEMENT_TYPES.put("STOCK_ADJUSTMENT", "Stock Adjustment");
    }

    // Whitelist of sortable columns: the client sends the key, never SQL.
    private static final Map<String, String> SORT_COLUMNS = Map.ofEntries(
            Map.entry("transactionDate", "changed_at"),
            Map.entry("movementType", "movement_type"),
            Map.entry("referenceNo", "reference_no"),
            Map.entry("locationName", "location_name"),
            Map.entry("productName", "product_name"),
            Map.entry("batchNo", "batch_number"),
            Map.entry("source", "source_name"),
            Map.entry("destination", "destination_name"),
            Map.entry("stockIn", "stock_in"),
            Map.entry("stockOut", "stock_out"),
            Map.entry("balanceQuantity", "balance_quantity"));

    @PersistenceContext
    private EntityManager entityManager;

    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationContextResolver locationContextResolver;


    @Override
    public StockMovementReportResponse getStockMovementReport(
            StockMovementReportFilter filter,
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

        long totalRecords = fetchTotalRecords(reportSql, params);

        StockMovementReportPageDto details = new StockMovementReportPageDto();
        details.setData(fetchPage(reportSql, params, filter, size, offset));
        details.setPage(page);
        details.setSize(size);
        details.setTotalRecords(totalRecords);
        details.setTotalPages((int) ((totalRecords + size - 1) / size));

        StockMovementReportResponse response = new StockMovementReportResponse();
        response.setFromDate(fromDate);
        response.setToDate(toDate);
        response.setSummary(fetchSummary(reportSql, params));
        response.setMovementDetails(details);
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
     *   loc      - pharmacy / warehouse names, for the location, source and
     *              destination columns
     *   opening  - stock on hand at the start of fromDate: the remaining_stock
     *              of the last audit row before then for each inventory row
     *   mv       - one row per stock change in the period, read from the
     *              inventory audit ledgers (pharmacy and warehouse), plus a
     *              Damage Capture row per distribution line received with
     *              damaged units. Damaged units never enter inventory, so that
     *              row moves no stock.
     *   resolved - mv with its movement type, reference number, source and
     *              destination. Sales, purchases and transfers link straight
     *              to their document; purchase returns, stock adjustments and
     *              warehouse returns write no link, so the document is the one
     *              for the same location / product / batch stamped with the
     *              same timestamp (they share one "now"), else the latest
     *              one before the change.
     *   ledger   - resolved with a running balance from the opening stock
     *   report   - ledger narrowed by the movement type / reference / source /
     *              destination filters, applied after the balance so it stays
     *              the real stock level
     */
    private String buildReportSql(
            StockMovementReportFilter filter,
            List<String> pharmacyIds,
            List<String> warehouseIds,
            LocalDate fromDate,
            LocalDate toDate,
            Map<String, Object> params) {

        params.put("fromTs", Timestamp.valueOf(fromDate.atStartOfDay()));
        params.put("toTs", Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));

        // Each fragment starts with a space so it never glues onto the
        // previous one (a text block would drop that leading space).
        StringBuilder productFilters = new StringBuilder();

        if (!isEmpty(filter.getProductIds())) {
            productFilters.append(" AND p.product_id IN (:productIds)");
            params.put("productIds", filter.getProductIds());
        }

        if (!isEmpty(filter.getProductNames())) {
            productFilters.append(" AND LOWER(TRIM(p.product_name)) IN (:productNames)");
            params.put("productNames", lowerTrimmed(filter.getProductNames()));
        }

        if (hasText(filter.getBatchNo())) {
            productFilters.append(" AND bt.batch_number ILIKE :batchNo");
            params.put("batchNo", "%" + escapeLike(filter.getBatchNo().trim()) + "%");
        }

        StringBuilder movementFilters = new StringBuilder();

        if (!isEmpty(filter.getMovementTypes())) {
            movementFilters.append(" AND movement_type IN (:movementTypes)");
            params.put("movementTypes", parseMovementTypes(filter.getMovementTypes()));
        }

        if (hasText(filter.getReferenceNo())) {
            movementFilters.append(" AND reference_no ILIKE :referenceNo");
            params.put("referenceNo", "%" + escapeLike(filter.getReferenceNo().trim()) + "%");
        }

        if (!isEmpty(filter.getSourceLocationIds())) {
            movementFilters.append(" AND source_location_id IN (:sourceLocationIds)");
            params.put("sourceLocationIds", filter.getSourceLocationIds());
        }

        if (!isEmpty(filter.getDestinationLocationIds())) {
            movementFilters.append(" AND destination_location_id IN (:destinationLocationIds)");
            params.put("destinationLocationIds", filter.getDestinationLocationIds());
        }

        // An empty list in IN (...) is invalid SQL, so each location kind is
        // only included when it has ids.
        List<String> openingParts = new ArrayList<>();
        List<String> movementParts = new ArrayList<>();
        List<String> damagedLocations = new ArrayList<>();

        if (!pharmacyIds.isEmpty()) {
            params.put("pharmacyIds", pharmacyIds);
            openingParts.add("""
                    SELECT la.remaining_stock AS qty
                    FROM pharma_inventory i
                    JOIN pharma_product_details p ON p.product_id = i.product_id
                    LEFT JOIN pharma_batch_details bt ON bt.batch_id = i.batch_id
                    JOIN LATERAL (
                        SELECT a.remaining_stock
                        FROM pharma_inventory_audit a
                        WHERE a.inventory_id = i.inventory_id
                          AND a.changed_at < :fromTs
                        ORDER BY a.changed_at DESC, a.inventory_audit_id DESC
                        LIMIT 1
                    ) la ON TRUE
                    WHERE i.pharmacy_id IN (:pharmacyIds)
                    """ + productFilters);
            movementParts.add("""
                    SELECT 'PHARMACY' AS location_type, i.pharmacy_id AS location_id,
                           i.product_id, i.batch_id,
                           1 AS src_order, a.inventory_audit_id AS row_id, a.changed_at,
                           a.stock_movement, a.transaction_type, COALESCE(a.change_stock, 0) AS qty,
                           a.billing_id, a.purchase_details_id,
                           a.warehouse_distribution_details_id AS distribution_details_id
                    FROM pharma_inventory_audit a
                    JOIN pharma_inventory i ON i.inventory_id = a.inventory_id
                    WHERE i.pharmacy_id IN (:pharmacyIds)
                      AND a.changed_at >= :fromTs
                      AND a.changed_at <  :toTs
                    """);
            damagedLocations.add("(w.destination_type = 'PHARMACY' AND w.destination_id IN (:pharmacyIds))");
        }

        if (!warehouseIds.isEmpty()) {
            params.put("warehouseIds", warehouseIds);
            openingParts.add("""
                    SELECT la.remaining_stock AS qty
                    FROM pharma_warehouse_inventory i
                    JOIN pharma_product_details p ON p.product_id = i.product_id
                    LEFT JOIN pharma_batch_details bt ON bt.batch_id = i.batch_id
                    JOIN LATERAL (
                        SELECT a.remaining_stock
                        FROM pharma_warehouse_inventory_audit a
                        WHERE a.warehouse_inventory_id = i.warehouse_inventory_id
                          AND a.changed_at < :fromTs
                        ORDER BY a.changed_at DESC, a.warehouse_inventory_audit_id DESC
                        LIMIT 1
                    ) la ON TRUE
                    WHERE i.warehouse_id IN (:warehouseIds)
                    """ + productFilters);
            movementParts.add("""
                    SELECT 'WAREHOUSE' AS location_type, i.warehouse_id AS location_id,
                           i.product_id, i.batch_id,
                           2 AS src_order, a.warehouse_inventory_audit_id AS row_id, a.changed_at,
                           a.stock_movement, a.transaction_type, COALESCE(a.change_stock, 0) AS qty,
                           CAST(NULL AS BIGINT) AS billing_id, a.purchase_details_id,
                           a.warehouse_distribution_details_id AS distribution_details_id
                    FROM pharma_warehouse_inventory_audit a
                    JOIN pharma_warehouse_inventory i ON i.warehouse_inventory_id = a.warehouse_inventory_id
                    WHERE i.warehouse_id IN (:warehouseIds)
                      AND a.changed_at >= :fromTs
                      AND a.changed_at <  :toTs
                    """);
            damagedLocations.add("(w.destination_type = 'WAREHOUSE' AND w.destination_id IN (:warehouseIds))");
        }

        // Damage Capture: dated when the distribution was first marked received.
        movementParts.add("""
                SELECT w.destination_type AS location_type, w.destination_id AS location_id,
                       d.product_id, d.batch_id,
                       3 AS src_order, d.warehouse_distribution_details_id AS row_id, rcv.received_at,
                       'NONE', 'DAMAGE_CAPTURE', d.damaged_quantity,
                       CAST(NULL AS BIGINT), CAST(NULL AS BIGINT),
                       d.warehouse_distribution_details_id
                FROM pharma_warehouse_distribution_details d
                JOIN pharma_warehouse_distribution w
                  ON w.warehouse_distribution_id = d.warehouse_distribution_id
                JOIN LATERAL (
                    SELECT MIN(s.created_at) AS received_at
                    FROM pharma_warehouse_distribution_status s
                    WHERE s.warehouse_distribution_id = w.warehouse_distribution_id
                      AND s.warehouse_distribution_status = 'STOCK_RECEIVED'
                ) rcv ON TRUE
                WHERE d.damaged_quantity > 0
                  AND (%s)
                  AND rcv.received_at >= :fromTs
                  AND rcv.received_at <  :toTs
                """.formatted(String.join(" OR ", damagedLocations)));

        return """
                WITH loc AS (
                    SELECT 'PHARMACY' AS location_type, pharmacy_id AS location_id,
                           pharmacy_name AS location_name
                    FROM pharma_pharmacy_details
                    UNION ALL
                    SELECT 'WAREHOUSE', warehouse_id, warehouse_name
                    FROM pharma_warehouse
                ),
                opening AS (
                    SELECT COALESCE(SUM(qty), 0) AS qty
                    FROM (
                %1$s
                    ) o
                ),
                mv AS (
                %2$s
                ),
                resolved AS (
                    SELECT m.src_order, m.row_id, m.changed_at,
                           m.location_type, m.location_id, l.location_name,
                           p.product_id, p.product_name, bt.batch_number,
                           CASE
                               WHEN m.transaction_type = 'DAMAGE_CAPTURE' THEN 'DAMAGE_CAPTURE'
                               WHEN m.transaction_type = 'PURCHASE' THEN 'PURCHASE_RECEIPT'
                               -- stock put back when a bill is cancelled / edited
                               -- is the reverse of the sale, so it stays a sale
                               WHEN m.transaction_type = 'SALE' THEN 'PHARMACY_SALE'
                               WHEN m.transaction_type = 'STOCK_ADJUSTMENT' AND m.billing_id IS NOT NULL
                                   THEN 'PHARMACY_SALE'
                               WHEN m.transaction_type = 'SALES_RETURN' THEN 'SALES_RETURN'
                               WHEN m.transaction_type = 'PURCHASE_RETURN' THEN 'PURCHASE_RETURN'
                               WHEN m.transaction_type = 'STOCK_ADJUSTMENT' THEN 'STOCK_ADJUSTMENT'
                               WHEN m.transaction_type = 'STOCK_TRANSFER' AND wd.source_type = 'WAREHOUSE'
                                   THEN 'ALLOCATION_' || m.stock_movement
                               WHEN m.transaction_type = 'STOCK_TRANSFER'
                                   THEN 'STOCK_TRANSFER_' || m.stock_movement
                               -- stock_return_type is free text; returns are
                               -- damaged stock unless marked as expired
                               WHEN m.transaction_type = 'STOCK_RETURN' AND wr.stock_return_type ILIKE '%%expir%%'
                                   THEN 'EXPIRED_STOCK_RETURN'
                               WHEN m.transaction_type = 'STOCK_RETURN' THEN 'DAMAGE_RETURN'
                               ELSE m.transaction_type
                           END AS movement_type,
                           COALESCE(sr.sales_return_no, pr.return_no, sa.stock_adjustment_no,
                                    wr.stock_return_no, wd.allocation_no,
                                    COALESCE(pu.grn_no, pu.invoice_no), b.bill_no) AS reference_no,
                           CASE
                               WHEN m.transaction_type = 'PURCHASE' THEN sup.supplier_name
                               WHEN m.transaction_type = 'SALES_RETURN' THEN 'Patient Return'
                               WHEN m.transaction_type = 'PURCHASE_RETURN' THEN l.location_name
                               WHEN m.transaction_type = 'STOCK_TRANSFER' THEN wsrc.location_name
                               WHEN m.transaction_type = 'STOCK_RETURN' THEN wrsrc.location_name
                               WHEN m.stock_movement = 'IN' THEN NULL
                               ELSE l.location_name
                           END AS source_name,
                           CASE
                               WHEN m.transaction_type = 'PURCHASE_RETURN' THEN prsup.supplier_name
                               WHEN m.transaction_type = 'STOCK_TRANSFER' THEN wdst.location_name
                               WHEN m.transaction_type = 'STOCK_RETURN' THEN wrdst.location_name
                               WHEN m.stock_movement = 'IN' THEN l.location_name
                               ELSE NULL
                           END AS destination_name,
                           -- ids behind source_name / destination_name when that
                           -- side is a pharmacy or warehouse (not a supplier / patient)
                           CASE
                               WHEN m.transaction_type IN ('PURCHASE', 'SALES_RETURN') THEN NULL
                               WHEN m.transaction_type = 'PURCHASE_RETURN' THEN m.location_id
                               WHEN m.transaction_type = 'STOCK_TRANSFER' THEN wd.source_id
                               WHEN m.transaction_type = 'STOCK_RETURN' THEN wr.from_pharmacy_id
                               WHEN m.stock_movement = 'IN' THEN NULL
                               ELSE m.location_id
                           END AS source_location_id,
                           CASE
                               WHEN m.transaction_type = 'PURCHASE_RETURN' THEN NULL
                               WHEN m.transaction_type = 'STOCK_TRANSFER' THEN wd.destination_id
                               WHEN m.transaction_type = 'STOCK_RETURN' THEN wr.to_warehouse_id
                               WHEN m.stock_movement = 'IN' THEN m.location_id
                               ELSE NULL
                           END AS destination_location_id,
                           CASE WHEN m.stock_movement = 'IN'  THEN m.qty ELSE 0 END AS stock_in,
                           CASE WHEN m.stock_movement = 'OUT' THEN m.qty ELSE 0 END AS stock_out
                    FROM mv m
                    JOIN pharma_product_details p ON p.product_id = m.product_id
                    LEFT JOIN pharma_batch_details bt ON bt.batch_id = m.batch_id
                    LEFT JOIN loc l
                           ON l.location_type = m.location_type AND l.location_id = m.location_id
                    LEFT JOIN pharma_billing b ON b.billing_id = m.billing_id
                    LEFT JOIN pharma_purchase_details pd ON pd.purchase_details_id = m.purchase_details_id
                    LEFT JOIN pharma_purchase pu ON pu.purchase_id = pd.purchase_id
                    LEFT JOIN pharma_supplier_master sup ON sup.supplier_id = pu.supplier_id
                    LEFT JOIN pharma_warehouse_distribution_details wdd
                           ON wdd.warehouse_distribution_details_id = m.distribution_details_id
                    LEFT JOIN pharma_warehouse_distribution wd
                           ON wd.warehouse_distribution_id = wdd.warehouse_distribution_id
                    LEFT JOIN loc wsrc
                           ON wsrc.location_type = wd.source_type AND wsrc.location_id = wd.source_id
                    LEFT JOIN loc wdst
                           ON wdst.location_type = wd.destination_type AND wdst.location_id = wd.destination_id
                    LEFT JOIN LATERAL (
                        SELECT x.sales_return_no
                        FROM pharma_sales_return x
                        JOIN pharma_sales_return_details xd ON xd.sales_return_id = x.sales_return_id
                        WHERE m.transaction_type = 'SALES_RETURN'
                          AND x.billing_id = m.billing_id
                          AND xd.product_id = m.product_id
                          AND x.created_at <= m.changed_at
                        ORDER BY (x.created_at = m.changed_at OR x.modified_at = m.changed_at) DESC,
                                 x.created_at DESC
                        LIMIT 1
                    ) sr ON TRUE
                    LEFT JOIN LATERAL (
                        SELECT x.return_no, x.purchase_id
                        FROM pharma_purchase_return x
                        JOIN pharma_purchase_return_details xd ON xd.purchase_return_id = x.purchase_return_id
                        WHERE m.transaction_type = 'PURCHASE_RETURN'
                          AND xd.product_id = m.product_id
                          AND xd.batch_id IS NOT DISTINCT FROM m.batch_id
                          AND ((m.location_type = 'PHARMACY' AND x.pharmacy_id = m.location_id)
                            OR (m.location_type = 'WAREHOUSE' AND x.warehouse_id = m.location_id))
                          AND x.created_at <= m.changed_at
                        ORDER BY (x.created_at = m.changed_at OR x.modified_at = m.changed_at
                                  OR xd.created_at = m.changed_at) DESC,
                                 x.created_at DESC
                        LIMIT 1
                    ) pr ON TRUE
                    LEFT JOIN pharma_purchase prp ON prp.purchase_id = pr.purchase_id
                    LEFT JOIN pharma_supplier_master prsup ON prsup.supplier_id = prp.supplier_id
                    LEFT JOIN LATERAL (
                        SELECT x.stock_adjustment_no
                        FROM pharma_stock_adjustment x
                        WHERE m.transaction_type = 'STOCK_ADJUSTMENT'
                          AND m.billing_id IS NULL
                          AND x.product_id = m.product_id
                          AND x.batch_id IS NOT DISTINCT FROM m.batch_id
                          AND ((m.location_type = 'PHARMACY' AND x.pharmacy_id = m.location_id)
                            OR (m.location_type = 'WAREHOUSE' AND x.warehouse_id = m.location_id))
                          AND x.created_at <= m.changed_at
                        ORDER BY (x.created_at = m.changed_at) DESC, x.created_at DESC
                        LIMIT 1
                    ) sa ON TRUE
                    LEFT JOIN LATERAL (
                        SELECT x.stock_return_no, x.stock_return_type, x.from_pharmacy_id, x.to_warehouse_id
                        FROM pharma_warehouse_return x
                        JOIN pharma_warehouse_return_details xd ON xd.warehouse_return_id = x.warehouse_return_id
                        WHERE m.transaction_type = 'STOCK_RETURN'
                          AND COALESCE(x.is_delete, FALSE) = FALSE
                          AND xd.product_id = m.product_id
                          AND xd.batch_id IS NOT DISTINCT FROM m.batch_id
                          AND ((m.location_type = 'PHARMACY' AND x.from_pharmacy_id = m.location_id)
                            OR (m.location_type = 'WAREHOUSE' AND x.to_warehouse_id = m.location_id))
                          AND x.created_at <= m.changed_at
                        ORDER BY (x.created_at = m.changed_at OR x.modified_at = m.changed_at
                                  OR xd.modified_at = m.changed_at) DESC,
                                 x.created_at DESC
                        LIMIT 1
                    ) wr ON TRUE
                    LEFT JOIN loc wrsrc
                           ON wrsrc.location_type = 'PHARMACY' AND wrsrc.location_id = wr.from_pharmacy_id
                    LEFT JOIN loc wrdst
                           ON wrdst.location_type = 'WAREHOUSE' AND wrdst.location_id = wr.to_warehouse_id
                    WHERE 1 = 1
                      %3$s
                ),
                ledger AS (
                    SELECT r.*,
                           o.qty + SUM(r.stock_in - r.stock_out) OVER (
                               ORDER BY r.changed_at, r.src_order, r.row_id
                               ROWS UNBOUNDED PRECEDING) AS balance_quantity
                    FROM resolved r
                    CROSS JOIN opening o
                ),
                report AS (
                    SELECT * FROM ledger WHERE 1 = 1 %4$s
                )
                """.formatted(
                // Leading newline: an opening part ends with the product
                // filters, which would otherwise glue onto UNION
                // (e.g. ":batchNoUNION").
                String.join("\nUNION ALL\n", openingParts),
                String.join("\nUNION ALL\n", movementParts),
                productFilters,
                movementFilters);
    }


    // Opening / closing are the real stock level (every movement of the
    // selected locations and products); in / out follow the full filter.
    private StockMovementReportSummaryDto fetchSummary(String reportSql, Map<String, Object> params) {

        Object[] row = (Object[]) nativeQuery(reportSql + """
                SELECT (SELECT qty FROM opening),
                       (SELECT COALESCE(SUM(stock_in - stock_out), 0) FROM resolved),
                       COALESCE(SUM(stock_in), 0),
                       COALESCE(SUM(stock_out), 0)
                FROM report
                """, params).getSingleResult();

        long opening = toLong(row[0]);

        StockMovementReportSummaryDto summary = new StockMovementReportSummaryDto();
        summary.setOpeningStock(opening);
        summary.setTotalStockIn(toLong(row[2]));
        summary.setTotalStockOut(toLong(row[3]));
        summary.setClosingStock(opening + toLong(row[1]));

        return summary;
    }


    private long fetchTotalRecords(String reportSql, Map<String, Object> params) {

        return toLong(nativeQuery(reportSql + "SELECT COUNT(*) FROM report", params)
                .getSingleResult());
    }


    @SuppressWarnings("unchecked")
    private List<StockMovementReportRowDto> fetchPage(
            String reportSql,
            Map<String, Object> params,
            StockMovementReportFilter filter,
            int size,
            long offset) {

        String sortColumn = SORT_COLUMNS.getOrDefault(
                hasText(filter.getSortBy()) ? filter.getSortBy() : "transactionDate", null);

        if (sortColumn == null) {
            throw new IllegalArgumentException("Invalid sortBy: " + filter.getSortBy()
                    + ". Allowed: " + String.join(", ", SORT_COLUMNS.keySet()));
        }

        String sortDir = "desc".equalsIgnoreCase(filter.getSortDir()) ? "DESC" : "ASC";

        // Ledger order as tie-breaker keeps page boundaries stable and rows of
        // the same date in the order they happened.
        Query query = nativeQuery(reportSql + """
                SELECT changed_at, movement_type, reference_no, location_type, location_name,
                       source_name, destination_name,
                       stock_in, stock_out, balance_quantity
                FROM report
                ORDER BY %s %s NULLS LAST, changed_at, src_order, row_id
                LIMIT :limit OFFSET :offset
                """.formatted(sortColumn, sortDir), params);

        query.setParameter("limit", size);
        query.setParameter("offset", offset);

        List<Object[]> results = query.getResultList();
        List<StockMovementReportRowDto> rows = new ArrayList<>(results.size());

        for (Object[] r : results) {
            StockMovementReportRowDto dto = new StockMovementReportRowDto();
            dto.setTransactionDate(toLocalDateTime(r[0]));
            dto.setMovementType((String) r[1]);
            dto.setMovementTypeLabel(MOVEMENT_TYPES.getOrDefault((String) r[1], (String) r[1]));
            dto.setReferenceNo((String) r[2]);
            dto.setLocationType((String) r[3]);
            dto.setLocationName((String) r[4]);
            dto.setSource((String) r[5]);
            dto.setDestination((String) r[6]);
            dto.setStockIn(toLong(r[7]));
            dto.setStockOut(toLong(r[8]));
            dto.setBalanceQuantity(toLong(r[9]));
            rows.add(dto);
        }

        return rows;
    }


    private Query nativeQuery(String sql, Map<String, Object> params) {

        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);
        return query;
    }


    private List<String> parseMovementTypes(List<String> values) {

        List<String> types = new ArrayList<>();

        for (String value : values) {
            String code = value.trim().toUpperCase().replace(' ', '_').replace('-', '_');

            if (!MOVEMENT_TYPES.containsKey(code)) {
                throw new IllegalArgumentException("Invalid movementType: " + value
                        + ". Allowed: " + String.join(", ", MOVEMENT_TYPES.keySet()));
            }
            types.add(code);
        }

        return types;
    }


    private static List<String> lowerTrimmed(List<String> values) {
        return values.stream().map(v -> v.trim().toLowerCase()).toList();
    }


    private static LocalDateTime toLocalDateTime(Object value) {

        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        return ((Timestamp) value).toLocalDateTime();
    }


    private static long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }


    // Treats user input as literal text inside ILIKE (no % / _ wildcards).
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }


    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }


    private static boolean isEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }
}
