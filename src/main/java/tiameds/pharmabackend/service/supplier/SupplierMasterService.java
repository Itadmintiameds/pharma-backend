package tiameds.pharmabackend.service.supplier;

import tiameds.pharmabackend.dto.supplier.SupplierMasterDto;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.List;

public interface SupplierMasterService {

    SupplierMasterDto createSupplier( SupplierMasterDto supplierDto, UserDetails user);

    List<SupplierMasterDto> getAllSuppliers(UserDetails user);

}
