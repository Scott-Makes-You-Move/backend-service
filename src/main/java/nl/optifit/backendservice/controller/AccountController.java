package nl.optifit.backendservice.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.CreateAccountDTO;
import nl.optifit.backendservice.dto.BiometricsMeasurementDTO;
import nl.optifit.backendservice.dto.MobilityMeasurementDTO;
import nl.optifit.backendservice.model.Account;
import nl.optifit.backendservice.model.Biometrics;
import nl.optifit.backendservice.model.Mobility;
import nl.optifit.backendservice.service.AccountService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/account")
@RestController
public class AccountController {
    private final AccountService accountService;

    @GetMapping("/{accountId}/biometrics")
    public ResponseEntity<Page<Biometrics>> getBiometricsForAccount(@PathVariable UUID accountId,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size,
                                                                    @RequestParam(defaultValue = "DESC") String direction,
                                                                    @RequestParam(defaultValue = "measuredOn") String sortBy) {
        log.info("GET Account Biometrics REST API called");
        Page<Biometrics> biometricsForAccount = accountService.getBiometricsForAccount(accountId, page, size, direction, sortBy);
        return ResponseEntity.ok(biometricsForAccount);
    }

    @GetMapping("/{accountId}/mobilities")
    public ResponseEntity<Page<Mobility>> getMobilitiesForAccount(@PathVariable UUID accountId,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "10") int size,
                                                                  @RequestParam(defaultValue = "DESC") String direction,
                                                                  @RequestParam(defaultValue = "measuredOn") String sortBy) {
        log.info("GET Account Mobilities REST API called");
        Page<Mobility> mobilitiesForAccount = accountService.getMobilitiesForAccount(accountId, page, size, direction, sortBy);
        return ResponseEntity.ok(mobilitiesForAccount);
    }

    @PostMapping("/{accountId}/biometrics")
    public ResponseEntity<Biometrics> createBiometrics(@PathVariable String accountId,
                                                       @RequestBody BiometricsMeasurementDTO biometricsMeasurementDTO) {
        log.info("POST Account Biometrics REST API called");
        Biometrics biometrics = accountService.saveBiometricsForAccount(accountId, biometricsMeasurementDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(biometrics);
    }

    @PostMapping("/{accountId}/mobilities")
    public ResponseEntity<Mobility> createBiometrics(@PathVariable String accountId,
                                                     @RequestBody MobilityMeasurementDTO mobilityMeasurementDTO) {
        log.info("POST Account Mobilities REST API called");
        Mobility mobility = accountService.saveMobilityForAccount(accountId, mobilityMeasurementDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(mobility);
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody CreateAccountDTO createAccountDTO) {
        log.info("POST Account REST API called");
        Account createdAccount = accountService.createAccountForId(createAccountDTO.getAccountId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAccount);
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deleteAccount(@PathVariable String accountId) {
        log.info("DELETE Account REST API called");
        accountService.deleteAccount(accountId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
