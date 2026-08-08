package tiameds.pharmabackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.entity.UserIdCounter;
import tiameds.pharmabackend.repository.UserIdCounterRepository;

import java.time.Year;

@Service
@RequiredArgsConstructor
public class UserIdGeneratorService {

    private final UserIdCounterRepository userIdCounterRepository;

    @Transactional
    public String generateUserId() {

        int currentYear = Year.now().getValue();

        UserIdCounter counter = userIdCounterRepository
                .findById(currentYear)
                .orElseGet(() ->
                        new UserIdCounter(currentYear, 0L)
                );

        counter.setLastNumber(counter.getLastNumber() + 1);

        userIdCounterRepository.save(counter);

        return String.format(
                "USR-%d-%05d",
                currentYear,
                counter.getLastNumber()
        );
    }
}