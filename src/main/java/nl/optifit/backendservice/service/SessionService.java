package nl.optifit.backendservice.service;

import lombok.RequiredArgsConstructor;
import nl.optifit.backendservice.model.Session;
import nl.optifit.backendservice.model.SessionStatus;
import nl.optifit.backendservice.repository.SessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class SessionService {

    private final SessionRepository sessionRepository;

    public List<Session> getByStatus(SessionStatus sessionStatus) {
        return sessionRepository.findAllBySessionStatusEquals(sessionStatus);
    }
}
