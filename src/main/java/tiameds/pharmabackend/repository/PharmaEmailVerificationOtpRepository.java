package tiameds.pharmabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.verifictaion.PharmaEmailVerificationOtp;

import java.util.Optional;

@Repository
public interface PharmaEmailVerificationOtpRepository
        extends JpaRepository<PharmaEmailVerificationOtp, Long> {

    Optional<PharmaEmailVerificationOtp> findTopByEmailOrderByIssuedAtDesc(String email);
}
