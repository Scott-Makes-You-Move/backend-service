package nl.optifit.backendservice.exception.advice;

import jakarta.ws.rs.*;
import lombok.extern.slf4j.*;
import nl.optifit.backendservice.exception.*;
import org.springframework.http.*;
import org.springframework.security.authorization.*;
import org.springframework.web.bind.annotation.*;

import java.io.*;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = { BootstrapException.class })
    public ResponseEntity<ErrorResponse> handleBootstrapFailure(BootstrapException ex) {
        log.error("Handling bootstrap failure", ex);
        return new ResponseEntity<>(ErrorResponse.of(INTERNAL_SERVER_ERROR.value(), ex), INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = { IllegalStateException.class })
    public ResponseEntity<ErrorResponse> handleConflicts(IllegalStateException ex) {
        log.error("Handling conflict error", ex);
        return new ResponseEntity<>(ErrorResponse.of(CONFLICT.value(), ex), CONFLICT);
    }

    @ExceptionHandler(value = { NotFoundException.class })
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        log.error("Handling not found error", ex);
        return new ResponseEntity<>(ErrorResponse.of(NOT_FOUND.value(), ex), NOT_FOUND);
    }

    @ExceptionHandler(value = {AuthorizationDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        log.error("Handling authorization denied error", ex);
        return new ResponseEntity<>(ErrorResponse.of(FORBIDDEN.value(), ex), FORBIDDEN);
    }

    @ExceptionHandler(value = { IOException.class })
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(IOException ex) {
        log.error("Handling IO error", ex);
        return new ResponseEntity<>(ErrorResponse.of(SERVICE_UNAVAILABLE.value(), ex), SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(value = { Exception.class })
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        log.error("Handling fallback error", ex);
        return new ResponseEntity<>(ErrorResponse.of(INTERNAL_SERVER_ERROR.value(), ex), INTERNAL_SERVER_ERROR);
    }
}
