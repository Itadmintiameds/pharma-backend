package tiameds.pharmabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.Optional;

@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    Optional<UserDetails> findByUserEmail(String email);

    boolean existsByUserEmail(String userEmail);

    @Query("""
            SELECT u
            FROM UserDetails u
            LEFT JOIN FETCH u.organization
            WHERE u.userId = :userId
            """)
    Optional<UserDetails> findByUserIdWithOrganization(@Param("userId") Long userId);
}
