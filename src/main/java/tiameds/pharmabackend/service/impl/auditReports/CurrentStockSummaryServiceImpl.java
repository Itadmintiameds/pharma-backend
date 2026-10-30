package tiameds.pharmabackend.service.impl.auditReports;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.auditReports.CurrentStockSummaryFilter;
import tiameds.pharmabackend.dto.auditReports.CurrentStockSummaryPageDto;
import tiameds.pharmabackend.dto.auditReports.CurrentStockSummaryResponse;
import tiameds.pharmabackend.dto.auditReports.CurrentStockSummaryRowDto;
import tiameds.pharmabackend.dto.auditReports.CurrentStockSummaryTotalsDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.warehouse.Warehouse;
import tiameds.pharmabackend.enums.StockReturnStatus;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseRepository;
import tiameds.pharmabackend.service.auditReports.CurrentStockSummaryService;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurrentStockSummaryServiceImpl implements CurrentStockSummaryService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    // Stock Status dropdown -> condition on the computed row.
    private static final Map<String, String> STOCK_STATUS_CONDITIONS = Map.of(
            "ALL", "",
            "IN_STOCK", " AND available_stock > 0",
            "OUT_OF_STOCK", " AND available_stock = 0",
            "DAMAGED_STOCK_HELD", " AND damaged_stock > 0",
            "EXPIRED_STOCK_HELD", " AND expired_stock > 0");

    // Whitelist of sortable columns: the client sends the key, never SQL.
    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "locationName", "location_name",
            "productName", "product_name",
            "batchNo", "batch_number",
            "expiryDate", "expiry_date",
            "closingStock", "closing_stock",
            "damagedStock", "damaged_stock",
            "expiredStock", "expired_stock",
            "availableStock", "available_stock");

    @PersistenceContext
    private EntityManager entityManager;

    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationContextResolver locationContextResolver;


    @Override
    public CurrentStockSummaryResponse getCurrentStockSummary(
            CurrentStockSummaryFilter filter,
            UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate asOfDate = filter.getAsOfDate() != null ? filter.getAsOfDate() : LocalDate.now();

        if (filter.getExpiryFromDate() != null && filter.getExpiryToDate() != null
                && filter.getExpiryToDate().isBefore(filter.getExpiryFromDate())) {
            throw new IllegalArgumentException("expiryToDate cannot be before expiryFromDate.");
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
        String reportSql = buildReportSql(filter, pharmacyIds, warehouseIds, asOfDate, params);

        long totalRecords = fetchTotalRecords(reportSql, params);

        CurrentStockSummaryPageDto details = new CurrentStockSummaryPageDto();
        details.setData(fetchPage(reportSql, params, filter, size, offset));
        details.setPage(page);
        details.setSize(size);
        details.setTotalRecords(totalRecords);
        details.setTotalPages((int) ((totalRecords + size - 1) / size));

        CurrentStockSummaryResponse response = new CurrentStockSummaryResponse();
        response.setSummary(fetchSummary(reportSql, params));
        response.setStockDetails(details);
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
     *   inv     - stock on hand per location / product / batch at the end of
     *             asOfDate: the remaining_stock of the last audit row up to then
     *             for each inventory row (packagings of a batch are summed)
     *   damaged - damaged units reported when a distribution was received at the
     *             location on or before asOfDate, and not yet sent back on a
     *             warehouse return by then. These never enter inventory, so they
     *             are added on top of it.
     *   base    - one row per location / product / batch with the four columns:
     *               closing   = on hand + damaged
     *               expired   = on hand, when the batch expired before asOfDate
     *               available = on hand, when it had not
     *             so closing = damaged + expired + available.
     *   report  - base narrowed by the Stock Status dropdown
     */
    private String buildReportSql(
            CurrentStockSummaryFilter filter,
            List<String> pharmacyIds,
            List<String> warehouseIds,
            LocalDate asOfDate,
            Map<String, Object> params) {

        params.put("asOfDate", Date.valueOf(asOfDate));
        params.put("asOfEnd", asOfDate.plusDays(1).atStartOfDay());

        // An empty list in IN (...) is invalid SQL, so each location kind is
        // only included when it has ids.
        List<String> invParts = new ArrayList<>();
        List<String> damagedLocations = new ArrayList<>();

        if (!pharmacyIds.isEmpty()) {
            params.put("pharmacyIds", pharmacyIds);
            invParts.add("""
                    SELECT 'PHARMACY' AS location_type, i.pharmacy_id AS location_id,
                           i.product_id, i.batch_id, SUM(la.remaining_stock) AS qty
                    FROM pharma_inventory i
                    JOIN LATERAL (
                        SELECT a.remaining_stock
                        FROM pharma_inventory_audit a
                        WHERE a.inventory_id = i.inventory_id
                          AND a.changed_at < :asOfEnd
                        ORDER BY a.changed_at DESC, a.inventory_audit_id DESC
                        LIMIT 1
                    ) la ON TRUE
                    WHERE i.pharmacy_id IN (:pharmacyIds)
                    GROUP BY i.pharmacy_id, i.product_id, i.batch_id
                    """);
            damagedLocations.add("(w.destination_type = 'PHARMACY' AND w.destination_id IN (:pharmacyIds))");
        }

        if (!warehouseIds.isEmpty()) {
            params.put("warehouseIds", warehouseIds);
            invParts.add("""
                    SELECT 'WAREHOUSE' AS location_type, i.warehouse_id AS location_id,
                           i.product_id, i.batch_id, SUM(la.remaining_stock) AS qty
                    FROM pharma_warehouse_inventory i
                    JOIN LATERAL (
                        SELECT a.remaining_stock
                        FROM pharma_warehouse_inventory_audit a
                        WHERE a.warehouse_inventory_id = i.warehouse_inventory_id
                          AND a.changed_at < :asOfEnd
                        ORDER BY a.changed_at DESC, a.warehouse_inventory_audit_id DESC
                        LIMIT 1
                    ) la ON TRUE
                    WHERE i.warehouse_id IN (:warehouseIds)
                    GROUP BY i.warehouse_id, i.product_id, i.batch_id
                    """);
            damagedLocations.add("(w.destination_type = 'WAREHOUSE' AND w.destination_id IN (:warehouseIds))");
        }

        params.put("draftReturnStatus", StockReturnStatus.DRAFT.getLabel());

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

        if (!isEmpty(filter.getProductCategoryIds())) {
            productFilters.append(" AND p.product_category_id IN (:productCategoryIds)");
            params.put("productCategoryIds", filter.getProductCategoryIds());
        }

        if (!isEmpty(filter.getBrandNames())) {
            productFilters.append(" AND LOWER(TRIM(p.brand_name)) IN (:brandNames)");
            params.put("brandNames", lowerTrimmed(filter.getBrandNames()));
        }

        if (hasText(filter.getBatchNo())) {
            productFilters.append(" AND b.batch_number ILIKE :batchNo");
            params.put("batchNo", "%" + escapeLike(filter.getBatchNo().trim()) + "%");
        }

        if (filter.getExpiryFromDate() != null) {
            productFilters.append(" AND b.expiry_date >= :expiryFromDate");
            params.put("expiryFromDate", Date.valueOf(filter.getExpiryFromDate()));
        }

        if (filter.getExpiryToDate() != null) {
            productFilters.append(" AND b.expiry_date <= :expiryToDate");
            params.put("expiryToDate", Date.valueOf(filter.getExpiryToDate()));
        }

        String stockStatus = hasText(filter.getStockStatus())
                ? filter.getStockStatus().trim().toUpperCase().replace(' ', '_')
                : "ALL";
        String statusCondition = STOCK_STATUS_CONDITIONS.get(stockStatus);

        if (statusCondition == null) {
            throw new IllegalArgumentException("Invalid stockStatus: " + filter.getStockStatus()
                    + ". Allowed: " + String.join(", ", STOCK_STATUS_CONDITIONS.keySet()));
        }

        return """
                WITH inv AS (
                %1$s
                ),
                damaged AS (
                    SELECT w.destination_type AS location_type, w.destination_id AS location_id,
                           d.product_id, d.batch_id, SUM(d.damaged_quantity) AS qty
                    FROM pharma_warehouse_distribution_details d
                    JOIN pharma_warehouse_distribution w
                      ON w.warehouse_distribution_id = d.warehouse_distribution_id
                    WHERE d.damaged_quantity > 0
                      AND (%2$s)
                      AND EXISTS (
                          SELECT 1
                          FROM pharma_warehouse_distribution_status s
                          WHERE s.warehouse_distribution_id = w.warehouse_distribution_id
                            AND s.warehouse_distribution_status = 'STOCK_RECEIVED'
                            AND s.created_at < :asOfEnd
                      )
                      AND NOT EXISTS (
                          SELECT 1
                          FROM pharma_warehouse_return_details rd
                          JOIN pharma_warehouse_return r
                            ON r.warehouse_return_id = rd.warehouse_return_id
                          WHERE rd.warehouse_distribution_details_id = d.warehouse_distribution_details_id
                            AND COALESCE(r.is_delete, FALSE) = FALSE
                            AND r.stock_return_status <> :draftReturnStatus
                            AND COALESCE(r.stock_return_date, r.created_at) < :asOfEnd
                      )
                    GROUP BY w.destination_type, w.destination_id, d.product_id, d.batch_id
                ),
                stock AS (
                    SELECT k.location_type, k.location_id, k.product_id, k.batch_id,
                           COALESCE(inv.qty, 0) AS on_hand,
                           COALESCE(dm.qty, 0)  AS damaged_qty
                    FROM (SELECT location_type, location_id, product_id, batch_id FROM inv
                          UNION
                          SELECT location_type, location_id, product_id, batch_id FROM damaged) k
                    LEFT JOIN inv
                           ON inv.location_type = k.location_type AND inv.location_id = k.location_id
                          AND inv.product_id = k.product_id AND inv.batch_id IS NOT DISTINCT FROM k.batch_id
                    LEFT JOIN damaged dm
                           ON dm.location_type = k.location_type AND dm.location_id = k.location_id
                          AND dm.product_id = k.product_id AND dm.batch_id IS NOT DISTINCT FROM k.batch_id
                ),
                base AS (
                    SELECT s.location_type,
                           s.location_id,
                           COALESCE(ph.pharmacy_name, wh.warehouse_name) AS location_name,
                           p.product_id,
                           p.product_name,
                           s.batch_id,
                           b.batch_number,
                           b.expiry_date,
                           s.on_hand + s.damaged_qty AS closing_stock,
                           s.damaged_qty             AS damaged_stock,
                           CASE WHEN b.expiry_date < :asOfDate THEN s.on_hand ELSE 0 END AS expired_stock,
                           CASE WHEN b.expiry_date < :asOfDate THEN 0 ELSE s.on_hand END AS available_stock
                    FROM stock s
                    JOIN pharma_product_details p ON p.product_id = s.product_id
                    LEFT JOIN pharma_batch_details b ON b.batch_id = s.batch_id
                    LEFT JOIN pharma_pharmacy_details ph
                           ON s.location_type = 'PHARMACY' AND ph.pharmacy_id = s.location_id
                    LEFT JOIN pharma_warehouse wh
                           ON s.location_type = 'WAREHOUSE' AND wh.warehouse_id = s.location_id
                    WHERE 1 = 1
                      %3$s
                ),
                report AS (
                    SELECT * FROM base WHERE 1 = 1 %4$s
                )
                """.formatted(
                String.join("UNION ALL\n", invParts),
                String.join(" OR ", damagedLocations),
                productFilters,
                statusCondition);
    }


    private CurrentStockSummaryTotalsDto fetchSummary(String reportSql, Map<String, Object> params) {

        Object[] row = (Object[]) nativeQuery(reportSql + """
                SELECT COALESCE(SUM(closing_stock), 0),
                       COALESCE(SUM(damaged_stock), 0),
                       COALESCE(SUM(expired_stock), 0),
                       COALESCE(SUM(available_stock), 0)
                FROM report
                """, params).getSingleResult();

        CurrentStockSummaryTotalsDto summary = new CurrentStockSummaryTotalsDto();
        summary.setTotalClosingStock(toLong(row[0]));
        summary.setTotalDamagedStock(toLong(row[1]));
        summary.setTotalExpiredStock(toLong(row[2]));
        summary.setTotalAvailableStock(toLong(row[3]));

        return summary;
    }


    private long fetchTotalRecords(String reportSql, Map<String, Object> params) {

        return toLong(nativeQuery(reportSql + "SELECT COUNT(*) FROM report", params)
                .getSingleResult());
    }


    @SuppressWarnings("unchecked")
    private List<CurrentStockSummaryRowDto> fetchPage(
            String reportSql,
            Map<String, Object> params,
            CurrentStockSummaryFilter filter,
            int size,
            long offset) {

        String sortColumn = SORT_COLUMNS.getOrDefault(
                hasText(filter.getSortBy()) ? filter.getSortBy() : "availableStock", null);

        if (sortColumn == null) {
            throw new IllegalArgumentException("Invalid sortBy: " + filter.getSortBy()
                    + ". Allowed: " + String.join(", ", SORT_COLUMNS.keySet()));
        }

        String sortDir = "asc".equalsIgnoreCase(filter.getSortDir()) ? "ASC" : "DESC";

        // location, product and batch as tie-breakers keep page boundaries
        // stable when several rows share the same sort value.
        Query query = nativeQuery(reportSql + """
                SELECT location_type, location_name, product_name, batch_number, expiry_date,
                       closing_stock, damaged_stock, expired_stock, available_stock
                FROM report
                ORDER BY %s %s NULLS LAST, location_id, product_id, batch_id
                LIMIT :limit OFFSET :offset
                """.formatted(sortColumn, sortDir), params);

        query.setParameter("limit", size);
        query.setParameter("offset", offset);

        List<Object[]> results = query.getResultList();
        List<CurrentStockSummaryRowDto> rows = new ArrayList<>(results.size());

        for (Object[] r : results) {
            CurrentStockSummaryRowDto dto = new CurrentStockSummaryRowDto();
            dto.setLocationType((String) r[0]);
            dto.setLocationName((String) r[1]);
            dto.setProductName((String) r[2]);
            dto.setBatchNo((String) r[3]);
            dto.setExpiryDate(toLocalDate(r[4]));
            dto.setClosingStock(toLong(r[5]));
            dto.setDamagedStock(toLong(r[6]));
            dto.setExpiredStock(toLong(r[7]));
            dto.setAvailableStock(toLong(r[8]));
            rows.add(dto);
        }

        return rows;
    }


    private Query nativeQuery(String sql, Map<String, Object> params) {

        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);
        return query;
    }


    private static List<String> lowerTrimmed(List<String> values) {
        return values.stream().map(v -> v.trim().toLowerCase()).toList();
    }


    private static LocalDate toLocalDate(Object value) {

        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate date) {
            return date;
        }
        return ((Date) value).toLocalDate();
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
