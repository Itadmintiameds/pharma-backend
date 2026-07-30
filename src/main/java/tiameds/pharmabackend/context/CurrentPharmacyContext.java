package tiameds.pharmabackend.context;

import org.springframework.stereotype.Component;
import tiameds.pharmabackend.exception.PharmacyNotSelectedException;

@Component
public class CurrentPharmacyContext {

    private final ThreadLocal<String> currentPharmacy =
            new ThreadLocal<>();

    public void setCurrentPharmacy(String pharmacyId) {
        currentPharmacy.set(pharmacyId);
    }

    public String getCurrentPharmacy() {

        String pharmacyId = currentPharmacy.get();

        if (pharmacyId == null) {
            throw new PharmacyNotSelectedException();
        }

        return pharmacyId;
    }

    public void clear() {
        currentPharmacy.remove();
    }
}