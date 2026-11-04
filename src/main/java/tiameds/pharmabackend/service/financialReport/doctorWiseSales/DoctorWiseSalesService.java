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
}
