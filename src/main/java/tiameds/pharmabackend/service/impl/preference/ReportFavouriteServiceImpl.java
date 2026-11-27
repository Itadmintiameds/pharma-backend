package tiameds.pharmabackend.service.impl.preference;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.preference.ReportFavouritesResponse;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.repository.preference.UserReportFavouriteRepository;
import tiameds.pharmabackend.service.preference.ReportFavouriteService;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
@Transactional
@RequiredArgsConstructor
public class ReportFavouriteServiceImpl implements ReportFavouriteService {

    /**
     * Report keys are frontend route segments ("BillSummary",
     * "productwisePurchaseandSalesAnalysisReport"). The backend holds no list
     * of reports, so it only checks the shape; length matches the column.
     */
    private static final Pattern REPORT_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,100}$");

    /** Upper bound per user, so the table can't be filled by a misbehaving client. */
    private static final int MAX_FAVOURITES_PER_USER = 50;

    private final UserReportFavouriteRepository favouriteRepository;

    @Override
    @Transactional(readOnly = true)
    public ReportFavouritesResponse getFavourites(UserDetails user) {
        return new ReportFavouritesResponse(
                favouriteRepository.findReportKeysByUserId(user.getUserId()));
    }

    @Override
    public void addFavourite(UserDetails user, String reportKey) {
        validateReportKey(reportKey);
        String userId = user.getUserId();

        if (favouriteRepository.existsByUser_UserIdAndReportKey(userId, reportKey)) {
            return;
        }

        if (favouriteRepository.countByUser_UserId(userId) >= MAX_FAVOURITES_PER_USER) {
            throw new IllegalStateException(
                    "You can have at most " + MAX_FAVOURITES_PER_USER + " favourite reports.");
        }

        favouriteRepository.insertIfAbsent(userId, reportKey, LocalDateTime.now());
    }

    @Override
    public void removeFavourite(UserDetails user, String reportKey) {
        validateReportKey(reportKey);
        favouriteRepository.deleteByUserIdAndReportKey(user.getUserId(), reportKey);
    }

    private static void validateReportKey(String reportKey) {
        if (reportKey == null || !REPORT_KEY_PATTERN.matcher(reportKey).matches()) {
            throw new IllegalArgumentException("Invalid report key.");
        }
    }
}
