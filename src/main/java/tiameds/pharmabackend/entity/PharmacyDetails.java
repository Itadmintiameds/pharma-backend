package tiameds.pharmabackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "pharma_pharmacy_details")
public class PharmacyDetails {

    @Id
    @Column(name = "pharmacy_id")
    private String pharmacyId;

    @Column(name = "pharmacy_registration_id")
    private String pharmacyRegistrationId;

    @Column(name = "pharmacy_name")
    private String pharmacyName;

    @Column(name = "pharmacy_type")
    private String pharmacyType;

    @Column(name = "pharmacy_email")
    private String pharmacyEmail;

    @Column(name = "pharmacy_phone")
    private Long pharmacyPhone;

    @Column(name = "pharmacy_dlno")
    private String pharmacyDlno;

    @Column(name = "pharmacy_gstno")
    private String pharmacyGstno;

    @Column(name = "pharmacy_pan")
    private String pharmacyPan;

    @Column(name = "pharmacy_business_registration_no")
    private String pharmacyBusinessRegistrationNo;

    @Column(name = "pharmacy_dl_expiry_date")
    private LocalDateTime pharmacyDlExpiryDate;

    @Column(name = "pharmacy_address")
    private String pharmacyAddress;

    @Column(name = "pharmacy_logo")
    private String pharmacyLogo;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @OneToMany(
            mappedBy = "pharmacy",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    private List<UserDetails> users;
}
