package nl.optifit.backendservice.service;

import lombok.RequiredArgsConstructor;
import nl.optifit.backendservice.repository.ProgressRepository;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ProgressService {
    private final ProgressRepository progressRepository;
}
