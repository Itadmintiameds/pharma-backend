package tiameds.pharmabackend.exception;

public class PharmacyNotSelectedException extends RuntimeException {

    public PharmacyNotSelectedException() {
        super("No pharmacy selected.");
    }
}