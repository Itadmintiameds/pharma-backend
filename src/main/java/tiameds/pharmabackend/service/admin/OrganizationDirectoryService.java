package tiameds.pharmabackend.service.admin;

import tiameds.pharmabackend.dto.admin.OrganizationDirectoryPageDto;

public interface OrganizationDirectoryService {

    /**
     * Pages through every organization with its pharmacies and warehouses (ids and names).
     * <p>
     * {@code search} (optional, case-insensitive, substring) matches the id or name of the
     * organization, a pharmacy or a warehouse. When the organization itself matches, all of
     * its locations are returned; when only some locations match, just those are returned.
     */
    OrganizationDirectoryPageDto search(String search, int page, int size);
}
