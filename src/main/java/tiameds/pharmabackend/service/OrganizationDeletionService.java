package tiameds.pharmabackend.service;

import java.util.Map;

public interface OrganizationDeletionService {

    /**
     * Hard-deletes EVERYTHING that belongs to an organization: pharmacies, warehouses,
     * users (and their tokens/otps/permissions), products (and all product children,
     * batches, packaging, attributes) plus inventory, purchases and suppliers scoped to
     * the organization's pharmacies. Irreversible.
     *
     * @return a map of table name -> number of rows deleted (in deletion order).
     */
    Map<String, Integer> deleteOrganizationData(Long organizationId);
}
