package tiameds.pharmabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.PharmaDocuments;

@Repository
public interface PharmaDocumentsRepository extends JpaRepository <PharmaDocuments, Long> {

    boolean existsByDocumentNo(String documentNo);


}
