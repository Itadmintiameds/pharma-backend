package tiameds.pharmabackend.service.billing;

import tiameds.pharmabackend.dto.billing.DoctorDetailsDto;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.List;

public interface DoctorDetailsService {

    DoctorDetailsDto createDoctor(DoctorDetailsDto doctorDto, UserDetails user);

    List<DoctorDetailsDto> getAllDoctors(UserDetails user);

    DoctorDetailsDto getDoctorById(Long doctorId, UserDetails user);
}
