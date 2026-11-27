package tiameds.pharmabackend.entity.preference;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.entity.UserDetails;

import java.time.LocalDateTime;

/**
 * A report the user has marked as a favourite in the Reports hub.
 * <p>
 * A per-user preference, not per-pharmacy: the hub lists the same reports in
 * every pharmacy, so favourites follow the user across pharmacies.
 * <p>
 * {@code reportKey} is the report's route segment on the frontend
 * (e.g. "BillSummary"). Titles repeat across groups, route segments don't.
 * The backend keeps no catalogue of reports; it only validates the key's
 * shape, and the frontend ignores keys that no longer match a report.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "pharma_user_report_favourite",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_report_favourite_user_report",
                columnNames = {"user_id", "report_key"}
        )
)
public class UserReportFavourite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favourite_id")
    private Long favouriteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private UserDetails user;

    @Column(name = "report_key", length = 100, nullable = false)
    private String reportKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
