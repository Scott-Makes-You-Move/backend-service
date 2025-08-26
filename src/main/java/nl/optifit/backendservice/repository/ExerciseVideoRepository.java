package nl.optifit.backendservice.repository;

import nl.optifit.backendservice.model.ExerciseType;
import nl.optifit.backendservice.model.ExerciseVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ExerciseVideoRepository extends JpaRepository<ExerciseVideo, UUID> {
    ExerciseVideo findByExerciseTypeAndScoreEquals(ExerciseType exerciseTypeString, int score);
}
