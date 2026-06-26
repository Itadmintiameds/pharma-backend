package tiameds.pharmabackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.UserDetailsDto;
import tiameds.pharmabackend.service.UserDetailsService;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserDetailsController {

    private final UserDetailsService userDetailsService;

    @PostMapping("/registration")
    public ResponseEntity<UserDetailsDto> registerUser(
            @RequestBody UserDetailsDto userDetailsDto) {
        System.out.println("CONTROLLER HIT");
        UserDetailsDto response =
                userDetailsService.registerUser(userDetailsDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}