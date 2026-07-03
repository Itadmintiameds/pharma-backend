package tiameds.pharmabackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Column(name = "pharmacy_building_no")
    private String pharmacyBuildingNo;

    @Column(name = "pharmacy_street")
    private String pharmacyStreet;

    @Column(name = "pharmacy_city")
    private String pharmacyCity;

    @Column(name = "pharmacy_taluka")
    private String pharmacyTaluka;

    @Column(name = "pharmacy_districts")
    private String pharmacyDistricts;

    @Column(name = "pharmacy_pincode")
    private Long pharmacyPincode;

    @Column(name = "pharmacy_landmark")
    private String pharmacyLandmark;

    @Column(name = "pharmacy_state")
    private String pharmacyState;

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

    @ManyToMany(mappedBy = "pharmacies", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<UserDetails> users = new ArrayList<>();

    @OneToMany(
            mappedBy = "pharmacy",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    private List<PharmaDocuments> documents;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", referencedColumnName = "organization_id")
    private PharmacyOrganization organization;
}
