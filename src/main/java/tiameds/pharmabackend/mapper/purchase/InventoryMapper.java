package tiameds.pharmabackend.mapper.purchase;

import org.springframework.stereotype.Component;
import tiameds.pharmabackend.dto.purchase.InventoryDto;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.purchase.Inventory;

@Component
public class InventoryMapper {

    public InventoryDto toDto(Inventory inventory) {

        InventoryDto dto = new InventoryDto();

        dto.setInventoryId(inventory.getInventoryId());
        dto.setProductId(
                inventory.getProduct() != null
                        ? inventory.getProduct().getProductId()
                        : null
        );
        dto.setPackagingId(
                inventory.getPackaging() != null
                        ? inventory.getPackaging().getPackagingId()
                        : null
        );
        dto.setBatchId(
                inventory.getBatch() != null
                        ? inventory.getBatch().getBatchId()
                        : null
        );
        dto.setTotalStock(inventory.getTotalStock());
        dto.setCreatedBy(inventory.getCreatedBy());
        dto.setCreatedAt(inventory.getCreatedAt());
        dto.setModifiedBy(inventory.getModifiedBy());
        dto.setModifiedAt(inventory.getModifiedAt());

        return dto;
    }

    public Inventory toEntity(
            InventoryDto dto,
            ProductDetails product,
            PackagingDetails packaging,
            BatchDetails batch
    ) {

        Inventory inventory = new Inventory();

        inventory.setInventoryId(dto.getInventoryId());
        inventory.setProduct(product);
        inventory.setPackaging(packaging);
        inventory.setBatch(batch);
        inventory.setTotalStock(dto.getTotalStock());
        inventory.setCreatedBy(dto.getCreatedBy());
        inventory.setCreatedAt(dto.getCreatedAt());
        inventory.setModifiedBy(dto.getModifiedBy());
        inventory.setModifiedAt(dto.getModifiedAt());

        return inventory;
    }
}