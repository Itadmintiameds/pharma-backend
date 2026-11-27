package tiameds.pharmabackend.repository.preference;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.preference.UserReportFavourite;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserReportFavouriteRepository extends JpaRepository<UserReportFavourite, Long> {

    /** The user's favourite report keys, oldest first (the order they were added). */
    @Query("""
            SELECT f.reportKey
            FROM UserReportFavourite f
            WHERE f.user.userId = :userId
            ORDER BY f.createdAt ASC, f.favouriteId ASC
            """)
    List<String> findReportKeysByUserId(@Param("userId") String userId);

    boolean existsByUser_UserIdAndReportKey(String userId, String reportKey);

    long countByUser_UserId(String userId);

    /**
     * Adds the favourite, or does nothing if it is already there. ON CONFLICT
     * keeps a double click or two open tabs from failing on the unique
     * constraint. Returns the number of rows inserted (0 or 1).
     */
    @Modifying
    @Query(value = """
            INSERT INTO pharma_user_report_favourite (user_id, report_key, created_at)
            VALUES (:userId, :reportKey, :createdAt)
            ON CONFLICT (user_id, report_key) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") String userId,
                       @Param("reportKey") String reportKey,
                       @Param("createdAt") LocalDateTime createdAt);

    /** Removes the favourite. Returns the number of rows deleted (0 or 1). */
    @Modifying
    @Query("""
            DELETE FROM UserReportFavourite f
            WHERE f.user.userId = :userId AND f.reportKey = :reportKey
            """)
    int deleteByUserIdAndReportKey(@Param("userId") String userId,
                                   @Param("reportKey") String reportKey);
}
