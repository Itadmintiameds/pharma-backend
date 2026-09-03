package tiameds.pharmabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetails, String> {

    Optional<UserDetails> findByUserEmail(String email);

    boolean existsByUserEmail(String userEmail);

    @Query("""
            SELECT u
            FROM UserDetails u
            LEFT JOIN FETCH u.organization
            WHERE u.userId = :userId
            """)
    Optional<UserDetails> findByUserIdWithOrganization(@Param("userId") String userId);

    @Query("""
            SELECT DISTINCT u
            FROM UserDetails u
            LEFT JOIN FETCH u.pharmacies
            LEFT JOIN FETCH u.warehouses
            WHERE u.organization.organizationId = :organizationId
            """)
    List<UserDetails> findAllByOrganizationIdWithPharmacies(
            @Param("organizationId") Long organizationId);


    @Query("""
    SELECT COUNT(u) > 0
    FROM UserDetails u
    JOIN u.pharmacies p
    WHERE p.pharmacyId = :pharmacyId
      AND u.employeeId = :employeeId
""")
    boolean existsByEmployeeIdAndPharmacyId(
            @Param("employeeId") String employeeId,
            @Param("pharmacyId") String pharmacyId
    );

    // Employee ids are unique per organization (a user can access multiple
    // pharmacies under the same organization), so scope the check to the org.
    @Query("""
    SELECT COUNT(u) > 0
    FROM UserDetails u
    WHERE u.organization.organizationId = :organizationId
      AND u.employeeId = :employeeId
""")
    boolean existsByEmployeeIdAndOrganizationId(
            @Param("employeeId") String employeeId,
            @Param("organizationId") Long organizationId
    );

    // OLD: single warehouse per user.
    // @Query("""
    //         SELECT u.warehouse.warehouseId
    //         FROM UserDetails u
    //         WHERE u.userId = :userId
    //         """)
    // Optional<String> findWarehouseIdByUserId(@Param("userId") String userId);

    // All warehouse ids the user is mapped to (many-to-many).
    @Query("""
            SELECT w.warehouseId
            FROM UserDetails u
            JOIN u.warehouses w
            WHERE u.userId = :userId
            """)
    List<String> findWarehouseIdsByUserId(@Param("userId") String userId);

    // Whether the given user is mapped to the given warehouse.
    // Mirrors PharmacyDetailsRepository.existsUserPharmacy.
    @Query("""
            SELECT COUNT(w) > 0
            FROM UserDetails u
            JOIN u.warehouses w
            WHERE w.warehouseId = :warehouseId
              AND u.userId = :userId
            """)
    boolean existsUserWarehouse(
            @Param("warehouseId") String warehouseId,
            @Param("userId") String userId);
}
