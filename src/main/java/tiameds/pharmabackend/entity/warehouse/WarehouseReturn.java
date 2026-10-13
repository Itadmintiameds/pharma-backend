package tiameds.pharmabackend.entity.warehouse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.enums.StockReturnStatus;
import tiameds.pharmabackend.enums.StockReturnStatusConverter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "pharma_warehouse_return")
public class WarehouseReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_return_id")
    private Long warehouseReturnId;

    @Column(name = "from_pharmacy_id")
    private String fromPharmacyId;

    @Column(name = "to_warehouse_id")
    private String toWarehouseId;

    @Column(name = "stock_return_no")
    private String stockReturnNo;

    @Column(name = "stock_return_date")
    private LocalDateTime stockReturnDate;

    @Column(name = "stock_return_type")
    private String stockReturnType;

    // Persisted as its label (Draft / Pending Receipt / Complete).
    @Convert(converter = StockReturnStatusConverter.class)
    @Column(name = "stock_return_status", length = 20)
    private StockReturnStatus stockReturnStatus = StockReturnStatus.DRAFT;

    @Column(name = "total_return_products")
    private Long totalReturnProducts;

    @Column(name = "total_return_quantity")
    private Long totalReturnQuantity;

    @Column(name = "total_received_quantity")
    private Long totalReceivedQuantity;

    @Column(name = "total_not_received_quantity")
    private Long totalNotReceivedQuantity;

    @Column(name = "is_delete")
    private Boolean isDelete = false;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @OneToMany(
            mappedBy = "warehouseReturn",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    private List<WarehouseReturnDetails> warehouseReturnDetails= new ArrayList<>();
}
