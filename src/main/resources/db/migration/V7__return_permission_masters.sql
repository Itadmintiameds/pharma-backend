-- Permission masters for the three return screens. The controllers check
-- MODULE/FEATURE/PERMISSION codes built from these rows:
--   SALES_RETURN/SALES_RETURN/{CREATE,VIEW}              (SalesReturnController)
--   PURCHASE_RETURN/PURCHASE_RETURN/{CREATE,VIEW,EDIT}   (PurchaseReturnController)
--   WAREHOUSE_RETURN/WAREHOUSE_RETURN/{CREATE,VIEW,EDIT} (WarehouseReturnController)
-- The CREATE/VIEW/EDIT rows in pharma_permission already exist; only modules,
-- features and feature permissions are added. Rows are looked up by name/code
-- rather than id, and every insert skips rows that are already there, so this
-- is a no-op on environments where it was applied by hand.
-- Flyway runs this inside its own transaction, so there is no BEGIN/COMMIT.
-- Granting these permissions to users is done from the admin screen.

-- The existing module/feature rows were inserted with explicit ids, which left
-- the identity sequences behind them (next id 1 while max id was 8). Move the
-- sequences past the existing rows so the inserts below get fresh ids.
SELECT setval(pg_get_serial_sequence('pharma_module', 'module_id'),
              (SELECT COALESCE(MAX(module_id), 0) + 1 FROM pharma_module), false);

SELECT setval(pg_get_serial_sequence('pharma_feature', 'feature_id'),
              (SELECT COALESCE(MAX(feature_id), 0) + 1 FROM pharma_feature), false);

-- Modules (Title Case like the existing ones; the code builds
-- 'Sales Return' -> SALES_RETURN)
INSERT INTO pharma_module (module_name)
VALUES ('Sales Return'),
       ('Purchase Return'),
       ('Warehouse Return')
ON CONFLICT (module_name) DO NOTHING;

-- Features, one per module; feature_code is used exactly as stored
INSERT INTO pharma_feature (module_id, feature_code, feature_name)
SELECT m.module_id, f.feature_code, f.feature_name
FROM (VALUES
        ('Sales Return',     'SALES_RETURN',     'Sales Return'),
        ('Purchase Return',  'PURCHASE_RETURN',  'Purchase Return'),
        ('Warehouse Return', 'WAREHOUSE_RETURN', 'Warehouse Return')
     ) AS f(module_name, feature_code, feature_name)
JOIN pharma_module m ON m.module_name = f.module_name
ON CONFLICT (feature_code) DO NOTHING;

-- Permissions each feature offers
INSERT INTO pharma_feature_permission (feature_id, permission_id)
SELECT f.feature_id, p.permission_id
FROM (VALUES
        ('SALES_RETURN',     'CREATE'),
        ('SALES_RETURN',     'VIEW'),
        ('PURCHASE_RETURN',  'CREATE'),
        ('PURCHASE_RETURN',  'VIEW'),
        ('PURCHASE_RETURN',  'EDIT'),
        ('WAREHOUSE_RETURN', 'CREATE'),
        ('WAREHOUSE_RETURN', 'VIEW'),
        ('WAREHOUSE_RETURN', 'EDIT')
     ) AS fp(feature_code, permission_name)
JOIN pharma_feature    f ON f.feature_code    = fp.feature_code
JOIN pharma_permission p ON p.permission_name = fp.permission_name
ON CONFLICT (feature_id, permission_id) DO NOTHING;
