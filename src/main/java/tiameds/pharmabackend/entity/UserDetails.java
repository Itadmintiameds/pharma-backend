package tiameds.pharmabackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.entity.warehouse.Warehouse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "pharma_user_details")
public class UserDetails {

    @Id
    @Column(name = "user_id", length = 20, nullable = false, updatable = false)
    private String userId;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "pharma_user_pharmacy",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "pharmacy_id")
    )
    @JsonIgnore
    private List<PharmacyDetails> pharmacies = new ArrayList<>();

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "password")
    private String password;

    @Column(name = "user_phone")
    private String userPhone;

    @Column(name = "employee_id")
    private String employeeId;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "gender")
    private String gender;

    @Column(name = "department")
    private String department;

    @Column(name = "image_url")
    private String imageUrl;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    @JsonIgnore
    private PharmaRoles role;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "is_rejected")
    private Boolean isRejected;

    @Column(name = "user_status")
    private String userStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @OneToMany(mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonIgnore
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    @OneToMany(mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonIgnore
    private List<PharmaOtp> pharmaOtps = new ArrayList<>();

    @OneToMany(mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonIgnore
    private List<UserFeaturePermission> featurePermissions = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", referencedColumnName = "organization_id")
    @JsonIgnore
    private PharmacyOrganization organization;

    // A user (e.g. a warehouse manager) may be bound to a single warehouse.
    // Nullable: pharmacy users have no warehouse, warehouse managers do.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", referencedColumnName = "warehouse_id")
    @JsonIgnore
    private Warehouse warehouse;

}