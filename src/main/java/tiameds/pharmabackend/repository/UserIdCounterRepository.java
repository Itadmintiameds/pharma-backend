package tiameds.pharmabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.pharmabackend.entity.UserIdCounter;

public interface UserIdCounterRepository
        extends JpaRepository<UserIdCounter, Integer> {
}