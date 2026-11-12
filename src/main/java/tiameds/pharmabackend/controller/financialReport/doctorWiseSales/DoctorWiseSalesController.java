package tiameds.pharmabackend.controller.financialReport.doctorWiseSales;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.financialReport.doctorWiseSales.DoctorWiseSalesService;

@RestController
@RequestMapping("/financial-report")
@RequiredArgsConstructor
public class DoctorWiseSalesController {

    private final DoctorWiseSalesService doctorWiseSalesService;

    // Doctor-wise / referred-by sales: one row per pharmacy + doctor + product,
    // with sale and return quantity and value. Only bills with a referring
    // doctor are included. See FinancialReportFilter for the query parameters.
    @GetMapping("/doctor-wise-sales-report")
    public ResponseEntity<?> getDoctorWiseSales(
            @ModelAttribute FinancialReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(doctorWiseSalesService.getDoctorWiseSales(filter, currentUser.getUser()));
    }


    // Doctors added in the given pharmacies, for the doctor dropdown. No
    // X-Pharmacy-Id header needed: pharmacyIds=A&pharmacyIds=B (or
    // pharmacyIds=A,B); without it, every pharmacy the user is mapped to.
    @GetMapping("/get-all-doctors-by-pharmacies")
    public ResponseEntity<?> getAllDoctorsByPharmacies(
            @RequestParam(required = false) java.util.List<String> pharmacyIds,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(doctorWiseSalesService.getDoctorsByPharmacies(pharmacyIds, currentUser.getUser()));
    }
}
