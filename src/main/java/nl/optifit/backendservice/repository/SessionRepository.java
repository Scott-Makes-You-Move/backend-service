package nl.optifit.backendservice.repository;

import nl.optifit.backendservice.model.Account;
import nl.optifit.backendservice.model.Biometrics;
import nl.optifit.backendservice.model.Session;
import nl.optifit.backendservice.model.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {
    List<Session> findAllByAccountId(String accountId);
    Optional<Session> findByAccountIdAndSessionStatus(String accountId, SessionStatus sessionStatus);
    List<Session> findAllBySessionStatusEquals(SessionStatus sessionStatus);
}
