package tiameds.pharmabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.UserFeaturePermission;

import java.util.List;

@Repository
public interface UserFeaturePermissionRepository
        extends JpaRepository<UserFeaturePermission, Long> {

    @Query("""
            SELECT p
            FROM UserFeaturePermission p
            JOIN FETCH p.feature
            JOIN FETCH p.permission
            WHERE p.user.userId = :userId
            """)
    List<UserFeaturePermission> findAllByUserIdWithFeature(@Param("userId") String userId);

    void deleteByUser_UserId(String userId);

    boolean existsByUser_UserIdAndFeature_FeatureCodeAndPermission_PermissionName(
            String userId,
            String featureCode,
            String permissionName);
}
