-- Billing gained sales_return_status (Not Returned / Partially Returned /
-- Returned), re-derived by SalesReturnServiceImpl after every sales return.
-- ddl-auto=update adds the column as nullable, so bills raised before it
-- existed are left null; the code reads a null as Not Returned, and this
-- backfills them so the column is consistent.
-- Flyway runs before Hibernate, so the column is added here first when it is
-- not there yet. Safe to run manually on environments with Flyway disabled
-- (it's a no-op once applied).
ALTER TABLE pharma_billing
    ADD COLUMN IF NOT EXISTS sales_return_status VARCHAR(20);

UPDATE pharma_billing
SET sales_return_status = 'Not Returned'
WHERE sales_return_status IS NULL;
