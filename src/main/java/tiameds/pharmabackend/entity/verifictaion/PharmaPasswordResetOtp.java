package tiameds.pharmabackend.entity.verifictaion;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.entity.UserDetails;

import java.time.LocalDateTime;

/**
 * OTP issued for the forgot-password flow.
 *
 * Kept separate from pharma_otp on purpose: login picks the user's latest
 * pharma_otp row, so a reset OTP stored there would interfere with sign-in.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "pharma_password_reset_otps")
public class PharmaPasswordResetOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "otp_id")
    private Long otpId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "user_id", nullable = false)
    private UserDetails user;

    @Column(name = "otp", nullable = false, length = 6)
    private String otp;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "max_retry_limit")
    private Integer maxRetryLimit;

    @Column(name = "resend_count")
    private Integer resendCount;

    @Column(name = "is_locked")
    private Boolean isLocked;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "is_used")
    private Boolean isUsed;

    @Column(name = "used_at")
    private LocalDateTime usedAt;
}
