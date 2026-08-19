package tiameds.pharmabackend.dto;

import lombok.Data;
import tiameds.pharmabackend.dto.warehouse.WarehouseSummaryDto;

import java.util.List;

@Data
public class UserSummaryDto {

    private String userId;
    private String fullName;
    private String userEmail;
    private String employeeId;
    private Long roleId;
    private String roleName;
    private List<String> pharmacyCities;
    private String userStatus;
    // OLD: single warehouse per user.
    // private WarehouseSummaryDto warehouse;
    private List<WarehouseSummaryDto> warehouses;
}
