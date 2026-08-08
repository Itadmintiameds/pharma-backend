package tiameds.pharmabackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pharma_user_id_counter")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserIdCounter {

    @Id
    @Column(name = "year_value")
    private Integer year;

    @Column(name = "last_number", nullable = false)
    private Long lastNumber;
}