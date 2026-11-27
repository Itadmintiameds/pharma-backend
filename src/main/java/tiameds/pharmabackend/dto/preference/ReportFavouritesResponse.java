package tiameds.pharmabackend.dto.preference;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** The user's favourite report keys in the Reports hub, oldest first. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportFavouritesResponse {

    private List<String> reportKeys;
}
