package tiameds.pharmabackend.service.impl.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.admin.LocationRefDto;
import tiameds.pharmabackend.dto.admin.OrganizationDirectoryDto;
import tiameds.pharmabackend.dto.admin.OrganizationDirectoryPageDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.PharmacyOrganization;
import tiameds.pharmabackend.entity.warehouse.Warehouse;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.PharmacyOrganizationRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseRepository;
import tiameds.pharmabackend.service.admin.OrganizationDirectoryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrganizationDirectoryServiceImpl implements OrganizationDirectoryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final PharmacyOrganizationRepository organizationRepository;

    private final PharmacyDetailsRepository pharmacyRepository;

    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional(readOnly = true)
    public OrganizationDirectoryPageDto search(String search, int page, int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        String term = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

        Page<PharmacyOrganization> organizations = organizationRepository.searchDirectory(
                "%" + term + "%",
                PageRequest.of(safePage, safeSize,
                        Sort.by(Sort.Order.asc("organizationName"), Sort.Order.asc("organizationId"))));

        List<Long> organizationIds = organizations.getContent().stream()
                .map(PharmacyOrganization::getOrganizationId)
                .toList();

        // Two queries for the whole page rather than two per organization.
        Map<Long, List<LocationRefDto>> pharmaciesByOrg = new HashMap<>();
        Map<Long, List<LocationRefDto>> warehousesByOrg = new HashMap<>();

        if (!organizationIds.isEmpty()) {

            for (PharmacyDetails p : pharmacyRepository
                    .findAllByOrganization_OrganizationIdInOrderByPharmacyName(organizationIds)) {
                pharmaciesByOrg
                        .computeIfAbsent(p.getOrganization().getOrganizationId(), k -> new ArrayList<>())
                        .add(new LocationRefDto(p.getPharmacyId(), p.getPharmacyName()));
            }

            for (Warehouse w : warehouseRepository
                    .findAllByOrganization_OrganizationIdInOrderByWarehouseName(organizationIds)) {
                warehousesByOrg
                        .computeIfAbsent(w.getOrganization().getOrganizationId(), k -> new ArrayList<>())
                        .add(new LocationRefDto(w.getWarehouseId(), w.getWarehouseName()));
            }
        }

        List<OrganizationDirectoryDto> data = organizations.getContent().stream()
                .map(o -> {
                    List<LocationRefDto> pharmacies =
                            pharmaciesByOrg.getOrDefault(o.getOrganizationId(), List.of());
                    List<LocationRefDto> warehouses =
                            warehousesByOrg.getOrDefault(o.getOrganizationId(), List.of());

                    // The organization was matched through some of its locations only:
                    // show just those, so the search narrows down to what was asked for.
                    if (!term.isEmpty()
                            && !matches(term, String.valueOf(o.getOrganizationId()), o.getOrganizationName())) {
                        pharmacies = pharmacies.stream().filter(l -> matches(term, l)).toList();
                        warehouses = warehouses.stream().filter(l -> matches(term, l)).toList();
                    }

                    return new OrganizationDirectoryDto(
                            o.getOrganizationId(), o.getOrganizationName(), pharmacies, warehouses);
                })
                .toList();

        return new OrganizationDirectoryPageDto(
                data,
                organizations.getNumber(),
                organizations.getSize(),
                organizations.getTotalElements(),
                organizations.getTotalPages(),
                organizations.hasNext());
    }

    private boolean matches(String term, LocationRefDto location) {
        return matches(term, location.getId(), location.getName());
    }

    // Mirrors the repository's LIKE '%term%' on the lower-cased id and name.
    private boolean matches(String term, String id, String name) {
        return (id != null && id.toLowerCase(Locale.ROOT).contains(term))
                || (name != null && name.toLowerCase(Locale.ROOT).contains(term));
    }
}
