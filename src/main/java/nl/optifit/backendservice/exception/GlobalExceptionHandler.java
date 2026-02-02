package nl.optifit.backendservice.exception;

import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apiguardian.api.API;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;
import org.zalando.problem.Problem;
import org.zalando.problem.spring.common.HttpStatusAdapter;
import org.zalando.problem.spring.web.advice.ProblemHandling;

import java.io.IOException;
import java.net.URI;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler implements ProblemHandling {

    @API(status = INTERNAL)
    @ExceptionHandler
    ResponseEntity<Problem> handleNotFoundException(
            final NotFoundException exception,
            final NativeWebRequest request) {
        log.debug("Handling NotFoundException: '{}'", exception.getMessage(), exception);
        return create(new HttpStatusAdapter(NOT_FOUND), exception, request);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Problem> handleAuthorizationDenied(
            AuthorizationDeniedException ex, NativeWebRequest request) {

        log.debug("Handling AuthorizationDeniedException: {}", ex.getMessage());

        Problem problem = Problem.builder()
                .withType(URI.create("https://example.com/problems/forbidden"))
                .withTitle("Forbidden")
                .withStatus(new HttpStatusAdapter(FORBIDDEN))
                .withDetail("Access Denied")
                .build();

        return ResponseEntity.status(FORBIDDEN)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @API(status = INTERNAL)
    @ExceptionHandler({Exception.class, IOException.class, IllegalStateException.class, NullPointerException.class})
    ResponseEntity<Problem> handleExceptions(
            final Exception exception,
            final NativeWebRequest request) {
        log.debug("Handling Exception: {}", exception.getMessage());
        return create(new HttpStatusAdapter(INTERNAL_SERVER_ERROR), exception, request);
    }

    @Override
    public boolean isCausalChainsEnabled() {
        return false;
    }
}
