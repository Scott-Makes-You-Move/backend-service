package nl.optifit.backendservice.repository;

import nl.optifit.backendservice.model.Session;
import nl.optifit.backendservice.model.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID>, JpaSpecificationExecutor<Session> {
    List<Session> findAllBySessionStatusEquals(SessionStatus sessionStatus);

    Optional<Session> findByIdAndAccountId(UUID uuid, String accountId);
}
