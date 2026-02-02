package nl.optifit.backendservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.model.ExerciseVideo;
import nl.optifit.backendservice.repository.ExerciseVideoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ExerciseVideoService {

    private final ExerciseVideoRepository exerciseVideoRepository;

    public void saveAll(List<ExerciseVideo> exerciseVideos) {
        log.debug("Saving {} exercise videos", exerciseVideos.size());
        exerciseVideoRepository.saveAll(exerciseVideos);
    }

    public void deleteAll() {
        log.debug("Deleting all exercise videos");
        exerciseVideoRepository.deleteAll();
    }
}
