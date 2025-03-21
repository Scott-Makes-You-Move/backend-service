package nl.optifit.backendservice.repository;

import nl.optifit.backendservice.model.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProgressRepository extends JpaRepository<Progress, UUID> {
}
