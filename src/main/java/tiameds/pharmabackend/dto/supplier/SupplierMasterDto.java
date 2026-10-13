package tiameds.pharmabackend.dto.supplier;

import lombok.Data;
import tiameds.pharmabackend.enums.SupplierStatus;

import java.time.LocalDateTime;

@Data
public class SupplierMasterDto {

    private Long supplierId;
    private String pharmacyId;
    private String warehouseId;
    private String supplierName;
    private String dlno;
    private String gstinNo;
    private String panNo;
    private LocalDateTime dlExpiryDate;
    private String issuingAuthority;
    private String fssaiNo;
    private String contactPersonName;
    private Long mobileNumber;
    private String supplierEmail;
    private String address;
    private String buildingNo;
    private Long pincode;
    private String city;
    private String district;
    private String state;
    private String bankName;
    private String accountHolderName;
    private Long accountNumber;
    private String ifscCode;
    private SupplierStatus status;
    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
}
