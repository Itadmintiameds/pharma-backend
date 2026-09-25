-- Revision tracking for pharma_purchase_return_details (PurchaseReturnDetails
-- revisionNo / isActive / previousDetailId). Editing a return's quantities
-- deactivates the current line and inserts a new row with the next revision.
-- ddl-auto=update adds these columns as nullable, and the code reads a null as
-- revision 1 / active, so this script is optional: run it manually to backfill
-- existing rows (Flyway is disabled here). Safe to re-run.
ALTER TABLE pharma_purchase_return_details
    ADD COLUMN IF NOT EXISTS revision_no INTEGER,
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN,
    ADD COLUMN IF NOT EXISTS previous_detail_id BIGINT;

UPDATE pharma_purchase_return_details SET revision_no = 1 WHERE revision_no IS NULL;
UPDATE pharma_purchase_return_details SET is_active = TRUE WHERE is_active IS NULL;
