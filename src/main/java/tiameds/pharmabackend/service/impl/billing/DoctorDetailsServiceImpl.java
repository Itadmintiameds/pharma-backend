package tiameds.pharmabackend.service.impl.billing;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tiameds.pharmabackend.context.CurrentPharmacyContext;
import tiameds.pharmabackend.dto.billing.DoctorDetailsDto;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.billing.DoctorDetails;
import tiameds.pharmabackend.mapper.billing.DoctorDetailsMapper;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.billing.DoctorDetailsRepository;
import tiameds.pharmabackend.service.billing.DoctorDetailsService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DoctorDetailsServiceImpl implements DoctorDetailsService {

    private final DoctorDetailsRepository doctorDetailsRepository;
    private final DoctorDetailsMapper doctorDetailsMapper;
    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final CurrentPharmacyContext pharmacyContext;


    @Override
    public DoctorDetailsDto createDoctor(DoctorDetailsDto doctorDto, UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String pharmacyId = requirePharmacy(persistentUser);

        if (doctorDto.getDoctorName() == null || doctorDto.getDoctorName().isBlank()) {
            throw new RuntimeException("Doctor name is required");
        }

        DoctorDetails doctor = doctorDetailsMapper.toEntity(doctorDto);

        doctor.setDoctorId(null);
        doctor.setPharmacyId(pharmacyId);
        doctor.setDoctorName(doctorDto.getDoctorName().trim());
        doctor.setCreatedBy(String.valueOf(persistentUser.getUserId()));
        doctor.setCreatedAt(LocalDateTime.now());
        doctor.setModifiedBy(null);
        doctor.setModifiedAt(null);

        DoctorDetails savedDoctor = doctorDetailsRepository.save(doctor);

        return doctorDetailsMapper.toDto(savedDoctor);
    }


    @Override
    public List<DoctorDetailsDto> getAllDoctors(UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String pharmacyId = requirePharmacy(persistentUser);

        return doctorDetailsRepository.findByPharmacyId(pharmacyId)
                .stream()
                .map(doctorDetailsMapper::toDto)
                .collect(Collectors.toList());
    }


    @Override
    public DoctorDetailsDto getDoctorById(Long doctorId, UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String pharmacyId = requirePharmacy(persistentUser);

        DoctorDetails doctor = doctorDetailsRepository
                .findByDoctorIdAndPharmacyId(doctorId, pharmacyId)
                .orElseThrow(() -> new RuntimeException(
                        "Doctor not found in this pharmacy with id : " + doctorId));

        return doctorDetailsMapper.toDto(doctor);
    }


    private String requirePharmacy(UserDetails persistentUser) {

        String pharmacyId = pharmacyContext.getCurrentPharmacy();

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        return pharmacyId;
    }
}
