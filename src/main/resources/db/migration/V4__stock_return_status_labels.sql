-- WarehouseReturn.stockReturnStatus is now stored by its label
-- (Draft / Pending Receipt / Complete) through StockReturnStatusConverter,
-- instead of the enum constant names (DRAFT / PENDING_RECEIPT / COMPLETED).
-- Flyway is disabled in this project (spring.flyway.enabled=false) and schema is
-- managed by spring.jpa.hibernate.ddl-auto=update. When the column was still
-- @Enumerated(EnumType.STRING), Hibernate created a CHECK constraint listing the
-- old constant names; ddl-auto=update never rewrites it, so every insert of
-- 'Draft' fails with "violates check constraint
-- pharma_warehouse_return_stock_return_status_check".
-- Run this manually against any environment whose table was created before the
-- change (it's a no-op once applied).

-- Drop the old constraint first: it would reject the label values below.
ALTER TABLE pharma_warehouse_return
    DROP CONSTRAINT IF EXISTS pharma_warehouse_return_stock_return_status_check;

-- Bring rows written under the old names onto the labels.
UPDATE pharma_warehouse_return SET stock_return_status = 'Draft'
    WHERE stock_return_status = 'DRAFT';
UPDATE pharma_warehouse_return SET stock_return_status = 'Pending Receipt'
    WHERE stock_return_status = 'PENDING_RECEIPT';
UPDATE pharma_warehouse_return SET stock_return_status = 'Complete'
    WHERE stock_return_status IN ('COMPLETED', 'COMPLETE');

ALTER TABLE pharma_warehouse_return
    ADD CONSTRAINT pharma_warehouse_return_stock_return_status_check
    CHECK (stock_return_status IN ('Draft', 'Pending Receipt', 'Complete'));
