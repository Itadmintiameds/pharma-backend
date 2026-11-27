package tiameds.pharmabackend.controller.preference;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.preference.ReportFavouritesResponse;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.preference.ReportFavouriteService;

/**
 * The logged-in user's favourite reports in the Reports hub.
 * <p>
 * Scoped to the user only, so no X-Pharmacy-Id / X-Warehouse-Id is needed.
 * PUT and DELETE are idempotent and return 204.
 */
@RestController
@RequestMapping("/user-preferences/report-favourites")
@RequiredArgsConstructor
public class ReportFavouriteController {

    private final ReportFavouriteService reportFavouriteService;

    /** {@code { "reportKeys": ["BillSummary", ...] }}, oldest first. */
    @GetMapping
    public ResponseEntity<ReportFavouritesResponse> getFavourites(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(reportFavouriteService.getFavourites(currentUser.getUser()));
    }

    @PutMapping("/{reportKey}")
    public ResponseEntity<Void> addFavourite(
            @PathVariable String reportKey,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        reportFavouriteService.addFavourite(currentUser.getUser(), reportKey);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{reportKey}")
    public ResponseEntity<Void> removeFavourite(
            @PathVariable String reportKey,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        reportFavouriteService.removeFavourite(currentUser.getUser(), reportKey);
        return ResponseEntity.noContent().build();
    }
}
