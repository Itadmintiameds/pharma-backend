package tiameds.pharmabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.PharmaModule;

import java.util.List;

@Repository
public interface PharmaModuleRepository extends JpaRepository<PharmaModule, Long> {

    @Query("""
            SELECT DISTINCT m
            FROM PharmaModule m
            LEFT JOIN FETCH m.features
            ORDER BY m.moduleId
            """)
    List<PharmaModule> findAllWithFeatures();
}
