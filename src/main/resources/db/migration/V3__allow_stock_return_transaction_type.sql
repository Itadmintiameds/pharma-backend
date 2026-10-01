-- TransactionType gained STOCK_RETURN (pharmacy -> warehouse stock return,
-- written by WarehouseReturnServiceImpl.dispatchWarehouseReturn).
-- Flyway is disabled in this project (spring.flyway.enabled=false) and schema is
-- managed by spring.jpa.hibernate.ddl-auto=update. Hibernate adds a CHECK
-- constraint listing the enum values when it first creates an enum column, but
-- ddl-auto=update never rewrites that constraint, so inserting the new value
-- fails with "violates check constraint ..._transaction_type_check".
-- Run this manually against any environment created before STOCK_RETURN existed
-- (it's a no-op once applied).
ALTER TABLE pharma_inventory_audit
    DROP CONSTRAINT IF EXISTS pharma_inventory_audit_transaction_type_check;

ALTER TABLE pharma_inventory_audit
    ADD CONSTRAINT pharma_inventory_audit_transaction_type_check
    CHECK (transaction_type IN (
        'PURCHASE', 'SALE', 'PURCHASE_RETURN', 'SALES_RETURN', 'STOCK_ADJUSTMENT',
        'STOCK_TRANSFER', 'STOCK_RETURN', 'DAMAGE', 'EXPIRED'));

ALTER TABLE pharma_warehouse_inventory_audit
    DROP CONSTRAINT IF EXISTS pharma_warehouse_inventory_audit_transaction_type_check;

ALTER TABLE pharma_warehouse_inventory_audit
    ADD CONSTRAINT pharma_warehouse_inventory_audit_transaction_type_check
    CHECK (transaction_type IN (
        'PURCHASE', 'SALE', 'PURCHASE_RETURN', 'SALES_RETURN', 'STOCK_ADJUSTMENT',
        'STOCK_TRANSFER', 'STOCK_RETURN', 'DAMAGE', 'EXPIRED'));
