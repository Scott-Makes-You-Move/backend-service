package nl.optifit.backendservice.repository;

import nl.optifit.backendservice.model.Session;
import nl.optifit.backendservice.model.SessionStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID>, JpaSpecificationExecutor<Session> {
    List<Session> findAllBySessionStatusEquals(SessionStatus sessionStatus);
}
