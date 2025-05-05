package nl.optifit.backendservice.repository;

import nl.optifit.backendservice.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findById(String accountId);
    void deleteById(String accountId);
}
