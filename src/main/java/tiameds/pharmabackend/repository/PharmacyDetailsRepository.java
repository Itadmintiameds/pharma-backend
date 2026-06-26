package tiameds.pharmabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.PharmacyDetails;

import java.util.List;

@Repository
public interface PharmacyDetailsRepository extends JpaRepository<PharmacyDetails, String> {

    @Query("""
       SELECT p.pharmacyId
       FROM PharmacyDetails p
       ORDER BY p.createdAt DESC
       LIMIT 1
       """)
    String findLatestPharmacyId();
}
