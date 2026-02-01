package nl.optifit.backendservice.repository;

import nl.optifit.backendservice.model.Session;
import nl.optifit.backendservice.model.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID>, JpaSpecificationExecutor<Session> {
    List<Session> findAllBySessionStatusEquals(SessionStatus sessionStatus);

    Optional<Session> findByIdAndAccountId(UUID uuid, String accountId);

    @Modifying
    @Query("DELETE FROM Session s WHERE s.sessionStart < :cutoffDate")
    int deleteSessionsOlderThan(@Param("cutoffDate") ZonedDateTime cutoffDate);
}
