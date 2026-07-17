package tiameds.pharmabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.PharmaFeature;

@Repository
public interface PharmaFeatureRepository extends JpaRepository<PharmaFeature, Long> {
}
