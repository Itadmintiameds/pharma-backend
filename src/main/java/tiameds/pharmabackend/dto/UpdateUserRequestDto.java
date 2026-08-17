package tiameds.pharmabackend.dto;

import lombok.Data;

import java.util.List;

/**
 * Body for editing an existing user. Mirrors {@link CreateUserRequestDto}.
 *
 * Every field is optional: only what is present in the body is changed, so a
 * partial edit does not wipe the rest of the profile. Email and password are
 * never editable here and are ignored if sent.
 */
@Data
public class UpdateUserRequestDto {

    private UserDetailsDto user;

    /** Replaces the user's pharmacy assignment. Omit to leave it untouched. */
    private List<String> pharmacyIds;

    /** Replaces the user's permissions. Omit to leave them untouched. */
    private List<FeaturePermissionsDto> permissions;
}
