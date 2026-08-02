package tiameds.pharmabackend.mapper.product.category;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import tiameds.pharmabackend.dto.product.ProductAttributeDrugDto;
import tiameds.pharmabackend.dto.product.ProductMoleculeDto;
import tiameds.pharmabackend.entity.product.ProductAttributeDrug;
import tiameds.pharmabackend.entity.product.ProductMolecule;

@Component
public class DrugMapper {

    public List<ProductAttributeDrugDto> toDrugDtoList(List<ProductAttributeDrug> entities) {
        if (entities == null || entities.isEmpty()) return new ArrayList<>();
        
        List<ProductAttributeDrugDto> dtos = new ArrayList<>();
        for (ProductAttributeDrug drugEntity : entities) {
            ProductAttributeDrugDto dto = new ProductAttributeDrugDto();
            dto.setDrugSchedule(drugEntity.getDrugSchedule());
            
            if (drugEntity.getProductMolecules() != null) {
                List<ProductMoleculeDto> moleculeDtos = new ArrayList<>();
                for (ProductMolecule molEntity : drugEntity.getProductMolecules()) {
                    ProductMoleculeDto molDto = new ProductMoleculeDto();
                    if (molEntity.getId() != null) {
                        molDto.setProductAttributeId(molEntity.getId().getProductAttributeId());
                    }
                    molDto.setMoleculeStrength(molEntity.getMoleculeStrength());
                    if (molEntity.getMolecule() != null) {
                        molDto.setMoleculeId(molEntity.getMolecule().getMoleculeId());
                    }
                    moleculeDtos.add(molDto);
                }
                dto.setProductMolecules(moleculeDtos);
            }
            dtos.add(dto);
        }
        return dtos;
    }

}
