package tiameds.pharmabackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.UserDetailsDto;
import tiameds.pharmabackend.entity.PharmaRoles;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.mapper.UserDetailsMapper;
import tiameds.pharmabackend.repository.PharmaRolesRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.service.UserDetailsService;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserDetailsRepository userDetailsRepository;
    private final PharmaRolesRepository pharmaRolesRepository;
    private final UserDetailsMapper userDetailsMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetailsDto registerUser(UserDetailsDto userDetailsDto) {

        PharmaRoles role = pharmaRolesRepository
                .findById(userDetailsDto.getPharmaRolesDto().getRoleId())
                .orElseThrow(() ->
                        new RuntimeException("Role not found"));

        UserDetails user =
                userDetailsMapper.toEntity(userDetailsDto, role);

        user.setPassword(
                passwordEncoder.encode(userDetailsDto.getPassword())
        );

        user.setCreatedAt(LocalDateTime.now());
        user.setUserStatus(Boolean.TRUE);

        UserDetails savedUser =
                userDetailsRepository.save(user);

        return userDetailsMapper.toDto(savedUser);
    }


    @Override
    public void deleteUserByPharmacyRegistrationId(String pharmacyRegistrationId) {

        UserDetails user = userDetailsRepository
                .findByPharmacyRegistrationId(pharmacyRegistrationId)
                .orElseThrow(() ->
                        new RuntimeException("User not found with Pharmacy Registration Id : "
                                + pharmacyRegistrationId));

        userDetailsRepository.delete(user);
    }
}