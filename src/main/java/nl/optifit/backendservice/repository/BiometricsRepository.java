package nl.optifit.backendservice.repository;

import nl.optifit.backendservice.model.Biometrics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BiometricsRepository extends JpaRepository<Biometrics, UUID> {
    Page<Biometrics> findAllByAccountId(Pageable pageable, String accountId);
}
