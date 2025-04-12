package nl.optifit.backendservice.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.CreateAccountDto;
import nl.optifit.backendservice.dto.BiometricsMeasurementDto;
import nl.optifit.backendservice.dto.MobilityMeasurementDto;
import nl.optifit.backendservice.model.*;
import nl.optifit.backendservice.service.AccountService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/account")
@Tag(name = "Account", description = "Operations related to accounts")
@RestController
public class AccountController {
    private final AccountService accountService;

    @Tag(name = "getSessions", description = "Get sessions for account")
    @GetMapping("/{accountId}/sessions")
    public ResponseEntity<PagedResponse<Session>> getSessionsForAccount(@PathVariable String accountId,
                                                                        @RequestParam(required = false) String sessionStartDate,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "10") int size,
                                                                        @RequestParam(defaultValue = "DESC") String direction,
                                                                        @RequestParam(defaultValue = "sessionStart") String sortBy) {
        log.info("GET Account Sessions REST API called");
        Page<Session> sessionsForAccount = accountService.getSessionsForAccount(accountId, sessionStartDate, page, size, direction, sortBy);
        return ResponseEntity.ok(new PagedResponse<>(sessionsForAccount));
    }

    @Tag(name = "updateLatestSession", description = "Update session for account")
    @PutMapping("/{accountId}/sessions")
    public ResponseEntity<Session> updateSession(@PathVariable String accountId) {
        log.info("PUT Account Session REST API called");
        Session session = accountService.updateSessionForAccount(accountId);
        return Objects.nonNull(session)
                ? ResponseEntity.status(HttpStatus.NO_CONTENT).body(session)
                : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @Tag(name = "createAccount", description = "Create account")
    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody CreateAccountDto createAccountDTO) {
        log.info("POST Account REST API called");
        Account createdAccount = accountService.createAccount(createAccountDTO.getAccountId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAccount);
    }

    @Tag(name = "getMobilities", description = "Get mobilities for account")
    @GetMapping("/{accountId}/mobilities")
    public ResponseEntity<PagedResponse<Mobility>> getMobilitiesForAccount(@PathVariable String accountId,
                                                                           @RequestParam(defaultValue = "0") int page,
                                                                           @RequestParam(defaultValue = "10") int size,
                                                                           @RequestParam(defaultValue = "DESC") String direction,
                                                                           @RequestParam(defaultValue = "measuredOn") String sortBy) {
        log.info("GET Account Mobilities REST API called");
        Page<Mobility> mobilitiesForAccount = accountService.getMobilitiesForAccount(accountId, page, size, direction, sortBy);
        return ResponseEntity.ok(new PagedResponse<>(mobilitiesForAccount));
    }

    @Tag(name = "createMobility", description = "Create mobility for account")
    @PostMapping("/{accountId}/mobilities")
    public ResponseEntity<Mobility> createMobility(@PathVariable String accountId,
                                                   @RequestBody @Valid MobilityMeasurementDto mobilityMeasurementDTO) {
        log.info("POST Account Mobilities REST API called");
        Mobility mobility = accountService.saveMobilityForAccount(accountId, mobilityMeasurementDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(mobility);
    }
    @Tag(name = "getBiometrics", description = "Get biometrics for account")
    @GetMapping("/{accountId}/biometrics")
    public ResponseEntity<PagedResponse<Biometrics>> getBiometricsForAccount(@PathVariable String accountId,
                                                                             @RequestParam(defaultValue = "0") int page,
                                                                             @RequestParam(defaultValue = "10") int size,
                                                                             @RequestParam(defaultValue = "DESC") String direction,
                                                                             @RequestParam(defaultValue = "measuredOn") String sortBy) {
        log.info("GET Account Biometrics REST API called");
        Page<Biometrics> biometricsForAccount = accountService.getBiometricsForAccount(accountId, page, size, direction, sortBy);
        return ResponseEntity.ok(new PagedResponse<>(biometricsForAccount));
    }

    @Tag(name = "createBiometric", description = "Create biometric for account")
    @PostMapping("/{accountId}/biometrics")
    public ResponseEntity<Biometrics> createBiometric(@PathVariable String accountId,
                                                      @RequestBody @Valid BiometricsMeasurementDto biometricsMeasurementDTO) {
        log.info("POST Account Biometrics REST API called");
        Biometrics biometrics = accountService.saveBiometricForAccount(accountId, biometricsMeasurementDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(biometrics);
    }

    @Tag(name = "deleteAccount", description = "Delete account")
    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deleteAccount(@PathVariable String accountId) {
        log.info("DELETE Account REST API called");
        accountService.deleteAccount(accountId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
