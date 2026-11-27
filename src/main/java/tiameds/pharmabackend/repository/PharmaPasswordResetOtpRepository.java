package tiameds.pharmabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.verifictaion.PharmaPasswordResetOtp;

import java.util.Optional;

@Repository
public interface PharmaPasswordResetOtpRepository
        extends JpaRepository<PharmaPasswordResetOtp, Long> {

    Optional<PharmaPasswordResetOtp> findTopByUserOrderByIssuedAtDesc(UserDetails user);
}
