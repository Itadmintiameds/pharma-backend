package tiameds.pharmabackend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * One organization with the ids and names of its pharmacies and warehouses.
 */
@Data
@AllArgsConstructor
public class OrganizationDirectoryDto {

    private Long organizationId;
    private String organizationName;
    private List<LocationRefDto> pharmacies;
    private List<LocationRefDto> warehouses;
}
