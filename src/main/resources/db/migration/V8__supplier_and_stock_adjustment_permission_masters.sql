-- Permission masters for supplier master, supplier payment and stock
-- adjustment. The controllers check MODULE/FEATURE/PERMISSION codes built
-- from these rows:
--   SUPPLIER_MASTER/SUPPLIER_MASTER/{CREATE,VIEW,EDIT}   (SupplierMasterController)
--   SUPPLIER_PAYMENT/SUPPLIER_PAYMENT/{CREATE,VIEW}      (SupplierPaymentController)
--   STOCK_ADJUSTMENT/STOCK_ADJUSTMENT/{CREATE,VIEW}      (StockAdjustmentController)
-- The CREATE/VIEW/EDIT rows in pharma_permission already exist; only modules,
-- features and feature permissions are added. Rows are looked up by name/code
-- rather than id, and every insert skips rows that are already there, so this
-- is a no-op on environments where it was applied by hand.
-- Flyway runs this inside its own transaction, so there is no BEGIN/COMMIT.
-- Granting these permissions to users is done from the admin screen.

-- Keep the identity sequences past the existing rows in case anything was
-- inserted with explicit ids since V7.
SELECT setval(pg_get_serial_sequence('pharma_module', 'module_id'),
              (SELECT COALESCE(MAX(module_id), 0) + 1 FROM pharma_module), false);

SELECT setval(pg_get_serial_sequence('pharma_feature', 'feature_id'),
              (SELECT COALESCE(MAX(feature_id), 0) + 1 FROM pharma_feature), false);

-- Modules (Title Case like the existing ones; the code builds
-- 'Supplier Master' -> SUPPLIER_MASTER)
INSERT INTO pharma_module (module_name)
VALUES ('Supplier Master'),
       ('Supplier Payment'),
       ('Stock Adjustment')
ON CONFLICT (module_name) DO NOTHING;

-- Features, one per module; feature_code is used exactly as stored
INSERT INTO pharma_feature (module_id, feature_code, feature_name)
SELECT m.module_id, f.feature_code, f.feature_name
FROM (VALUES
        ('Supplier Master',  'SUPPLIER_MASTER',  'Supplier Master'),
        ('Supplier Payment', 'SUPPLIER_PAYMENT', 'Supplier Payment'),
        ('Stock Adjustment', 'STOCK_ADJUSTMENT', 'Stock Adjustment')
     ) AS f(module_name, feature_code, feature_name)
JOIN pharma_module m ON m.module_name = f.module_name
ON CONFLICT (feature_code) DO NOTHING;

-- Permissions each feature offers
INSERT INTO pharma_feature_permission (feature_id, permission_id)
SELECT f.feature_id, p.permission_id
FROM (VALUES
        ('SUPPLIER_MASTER',  'CREATE'),
        ('SUPPLIER_MASTER',  'VIEW'),
        ('SUPPLIER_MASTER',  'EDIT'),
        ('SUPPLIER_PAYMENT', 'CREATE'),
        ('SUPPLIER_PAYMENT', 'VIEW'),
        ('STOCK_ADJUSTMENT', 'CREATE'),
        ('STOCK_ADJUSTMENT', 'VIEW')
     ) AS fp(feature_code, permission_name)
JOIN pharma_feature    f ON f.feature_code    = fp.feature_code
JOIN pharma_permission p ON p.permission_name = fp.permission_name
ON CONFLICT (feature_id, permission_id) DO NOTHING;
