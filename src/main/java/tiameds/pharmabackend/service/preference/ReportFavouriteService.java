package tiameds.pharmabackend.service.preference;

import tiameds.pharmabackend.dto.preference.ReportFavouritesResponse;
import tiameds.pharmabackend.entity.UserDetails;

/** Per-user favourite reports in the Reports hub. */
public interface ReportFavouriteService {

    /** The user's favourite report keys, oldest first. */
    ReportFavouritesResponse getFavourites(UserDetails user);

    /**
     * Marks the report as a favourite. Idempotent: adding one that is already
     * a favourite does nothing.
     *
     * @throws IllegalArgumentException if the key is malformed (400)
     * @throws IllegalStateException    if the user is at the favourites limit (409)
     */
    void addFavourite(UserDetails user, String reportKey);

    /**
     * Unmarks the report. Idempotent: removing one that isn't a favourite
     * does nothing.
     *
     * @throws IllegalArgumentException if the key is malformed (400)
     */
    void removeFavourite(UserDetails user, String reportKey);
}
