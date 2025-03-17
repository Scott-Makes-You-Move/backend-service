package nl.optifit.backendservice.repository;

import nl.optifit.backendservice.model.Account;
import nl.optifit.backendservice.model.Leaderboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LeaderboardRepository extends JpaRepository<Leaderboard, UUID> {
    Leaderboard findByAccount(Account account);
}
