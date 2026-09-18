-- pharma_supplier_master was missing the "status" column backing
-- SupplierMaster.status (org.hibernate.dialect...), causing
-- "column sm1_0.status does not exist" on every supplier read.
-- Flyway is disabled in this project (spring.flyway.enabled=false) and
-- schema is normally managed by spring.jpa.hibernate.ddl-auto=update, but
-- Hibernate's auto ALTER TABLE never succeeded here because the column was
-- declared NOT NULL with no default against a table that already had rows.
-- Run this manually against any environment that still has the column
-- missing (it's a no-op once applied).
ALTER TABLE pharma_supplier_master
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
