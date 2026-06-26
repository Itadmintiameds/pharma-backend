package tiameds.pharmabackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "pharma_otp")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PharmaOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "otp_id")
    private Long otpId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "user_id")
    private UserDetails user;

    @Column(name = "otp")
    private String otp;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "max_retry_limit")
    private Integer maxRetryLimit;

    @Column(name = "is_locked")
    private Boolean isLocked;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "is_used")
    private Boolean isUsed;

    @Column(name = "used_at")
    private LocalDateTime usedAt;
}
