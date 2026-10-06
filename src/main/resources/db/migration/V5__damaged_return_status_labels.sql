-- WarehouseDistributionDetails.stockReturnStatus is now the DamagedReturnStatus
-- enum, stored by its label (Not Returned / Returned / Partially Returned)
-- through DamagedReturnStatusConverter. Lines were previously written with the
-- free-text value 'Not return', and lines created before the column existed are
-- NULL. The damaged-not-returned list filters on the new labels, so neither of
-- those shows up until they are brought onto 'Not Returned'.
-- Flyway is disabled in this project (spring.flyway.enabled=false); run this
-- manually against any environment with existing distribution lines (it's a
-- no-op once applied).

UPDATE pharma_warehouse_distribution_details SET stock_return_status = 'Not Returned'
    WHERE stock_return_status = 'Not return' OR stock_return_status IS NULL;
