package nl.optifit.backendservice.repository;

import nl.optifit.backendservice.model.Account;
import nl.optifit.backendservice.model.Biometrics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findById(String accountId);
    void deleteById(String accountId);
}
