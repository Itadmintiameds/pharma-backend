package tiameds.pharmabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.PharmaOtp;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.Optional;

@Repository
public interface PharmaOtpRepository extends JpaRepository<PharmaOtp, Long> {

    Optional<PharmaOtp> findTopByUserOrderByIssuedAtDesc(UserDetails user);
}