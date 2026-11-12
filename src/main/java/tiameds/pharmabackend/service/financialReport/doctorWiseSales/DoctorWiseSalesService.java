package tiameds.pharmabackend.service.financialReport.doctorWiseSales;

import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportResponse;
import tiameds.pharmabackend.dto.financialReport.doctorWiseSales.DoctorWiseSalesKpiDto;
import tiameds.pharmabackend.dto.financialReport.doctorWiseSales.DoctorWiseSalesRowDto;
import tiameds.pharmabackend.entity.UserDetails;

public interface DoctorWiseSalesService {

    FinancialReportResponse<DoctorWiseSalesKpiDto, DoctorWiseSalesRowDto> getDoctorWiseSales(
            FinancialReportFilter filter,
            UserDetails user);

    // Doctors added in the given pharmacies (all of the user's pharmacies when
    // none are sent), for the report's doctor dropdown.
    java.util.List<tiameds.pharmabackend.dto.financialReport.doctorWiseSales.ReportDoctorDto> getDoctorsByPharmacies(
            java.util.List<String> pharmacyIds,
            UserDetails user);
}
