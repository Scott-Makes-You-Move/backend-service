package nl.optifit.backendservice.repository.jpa;

import nl.optifit.backendservice.model.Leaderboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaderboardRepository extends JpaRepository<Leaderboard, UUID> {
    Optional<Leaderboard> findByAccountId(String accountId);
}
