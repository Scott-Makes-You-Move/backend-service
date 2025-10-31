package nl.optifit.backendservice.exception.advice;

import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apiguardian.api.API;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;
import org.zalando.problem.Problem;
import org.zalando.problem.spring.common.HttpStatusAdapter;
import org.zalando.problem.spring.web.advice.ProblemHandling;

import java.io.IOException;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler implements ProblemHandling {

    @API(status = INTERNAL)
    @ExceptionHandler
    ResponseEntity<Problem> handleNotFoundException(
            final NotFoundException exception,
            final NativeWebRequest request) {
        return create(new HttpStatusAdapter(NOT_FOUND), exception, request);
    }

    @API(status = INTERNAL)
    @ExceptionHandler({Exception.class, IOException.class, IllegalStateException.class, NullPointerException.class})
    ResponseEntity<Problem> handleExceptions(
            final Exception exception,
            final NativeWebRequest request) {
        return create(new HttpStatusAdapter(INTERNAL_SERVER_ERROR), exception, request);
    }
}
