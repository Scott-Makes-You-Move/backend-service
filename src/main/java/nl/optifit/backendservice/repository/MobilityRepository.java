package nl.optifit.backendservice.repository;

import nl.optifit.backendservice.model.Mobility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MobilityRepository extends JpaRepository<Mobility, UUID> {
}
