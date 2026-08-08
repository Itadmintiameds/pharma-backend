package tiameds.pharmabackend.repository.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.billing.DoctorDetails;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorDetailsRepository extends JpaRepository<DoctorDetails, Long> {

    List<DoctorDetails> findByPharmacyId(String pharmacyId);

    Optional<DoctorDetails> findByDoctorIdAndPharmacyId(
            Long doctorId,
            String pharmacyId);
}
