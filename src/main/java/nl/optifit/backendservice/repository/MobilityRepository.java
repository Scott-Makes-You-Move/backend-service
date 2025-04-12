package nl.optifit.backendservice.repository;

import nl.optifit.backendservice.model.Biometrics;
import nl.optifit.backendservice.model.Mobility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface MobilityRepository extends JpaRepository<Mobility, UUID> {
    Page<Mobility> findAllByAccountId(Pageable pageable, String accountId);
    Optional<Mobility> findTopByAccountIdOrderByMeasuredOnDesc(String accountId);
}
