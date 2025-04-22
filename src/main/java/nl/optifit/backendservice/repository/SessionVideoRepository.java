package nl.optifit.backendservice.repository;

import nl.optifit.backendservice.model.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.*;

import java.util.*;

@Repository
public interface SessionVideoRepository extends JpaRepository<SessionVideo, UUID> {
    SessionVideo findByExerciseTypeAndScoreEquals(ExerciseType exerciseTypeString, int score);
}
