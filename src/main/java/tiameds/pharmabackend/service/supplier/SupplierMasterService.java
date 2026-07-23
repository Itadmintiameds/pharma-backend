package tiameds.pharmabackend.service.supplier;

import tiameds.pharmabackend.dto.supplier.SupplierMasterDto;
import tiameds.pharmabackend.entity.UserDetails;

public interface SupplierMasterService {

    SupplierMasterDto createSupplier( SupplierMasterDto supplierDto, UserDetails user);
}
