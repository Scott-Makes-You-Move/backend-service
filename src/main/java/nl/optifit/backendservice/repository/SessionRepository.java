package nl.optifit.backendservice.repository;

import nl.optifit.backendservice.model.Biometrics;
import nl.optifit.backendservice.model.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {
    List<Session> findAllByAccountId(String accountId);
    Optional<Session> findFirstByAccountIdOrderBySessionStartDesc(String accountId);
}
