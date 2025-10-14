package nl.optifit.backendservice.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.AccountDto;
import nl.optifit.backendservice.dto.BiometricsDto;
import nl.optifit.backendservice.dto.HealthIndexDto;
import nl.optifit.backendservice.dto.MobilityDto;
import nl.optifit.backendservice.dto.PagedResponseDto;
import nl.optifit.backendservice.dto.SessionDto;
import nl.optifit.backendservice.model.SessionStatus;
import nl.optifit.backendservice.service.AccountService;
import nl.optifit.backendservice.service.BiometricsService;
import nl.optifit.backendservice.service.MobilityService;
import nl.optifit.backendservice.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/account")
@Tag(name = "Account", description = "Operations related to accounts")
@RestController
public class AccountController {
    private final AccountService accountService;
    private final BiometricsService biometricsService;
    private final MobilityService mobilityService;
    private final SessionService sessionService;

    @PreAuthorize("@jwtConverter.isAccountCurrentUser(#accountId)")
    @GetMapping("{accountId}/healthindex")
    public ResponseEntity<HealthIndexDto> calculateHealthIndex(@PathVariable String accountId) {
        log.info("GET Health Index REST API called");
        HealthIndexDto healthIndex = accountService.calculateHealthIndex(accountId);
        return ResponseEntity.ok(healthIndex);
    }

    @PreAuthorize("@jwtConverter.isAccountCurrentUser(#accountId)")
    @GetMapping("/{accountId}/sessions")
    public ResponseEntity<PagedResponseDto<SessionDto>> getSessionsForAccount(@PathVariable String accountId,
                                                                              @RequestParam(required = false) String sessionStartDate,
                                                                              @RequestParam(required = false) SessionStatus sessionStatus,
                                                                              @RequestParam(defaultValue = "0") int page,
                                                                              @RequestParam(defaultValue = "10") int size,
                                                                              @RequestParam(defaultValue = "DESC") String direction,
                                                                              @RequestParam(defaultValue = "sessionStart") String sortBy) {
        log.info("GET Account Sessions REST API called");
        PagedResponseDto<SessionDto> accountSessions = sessionService.getSessionsForAccount(accountId, sessionStartDate, sessionStatus, page, size, direction, sortBy);
        return ResponseEntity.ok(accountSessions);
    }

    @PreAuthorize("@jwtConverter.isAccountCurrentUser(#accountId) and @sessionService.sessionBelongsToAccount(#sessionId, #accountId)")
    @GetMapping("/{accountId}/sessions/{sessionId}")
    public ResponseEntity<SessionDto> getSessionsForAccount(@PathVariable String accountId,
                                                            @PathVariable String sessionId) {
        log.info("GET Account Sessions REST API called");
        SessionDto accountSessions = sessionService.getSingleSessionForAccount(accountId, sessionId);
        return ResponseEntity.ok(accountSessions);
    }

    @PreAuthorize("@jwtConverter.isAccountCurrentUser(#accountId) and @sessionService.sessionBelongsToAccount(#sessionId, #accountId)")
    @PutMapping("/{accountId}/sessions/{sessionId}")
    public ResponseEntity<SessionDto> updateSession(@PathVariable String accountId,
                                                    @PathVariable String sessionId) {
        log.info("PUT Account Session REST API called");
        sessionService.updateSessionForAccount(sessionId);
        return ResponseEntity.noContent().build();
    }

    @Hidden // This endpoint is called when a user is created in Keycloak
    @PostMapping
    public ResponseEntity<AccountDto> createAccount(@RequestBody AccountDto accountDTO) throws IOException {
        log.info("POST Account REST API called");
        AccountDto createdAccount = accountService.createAccount(accountDTO.accountId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAccount);
    }

    @PreAuthorize("@jwtConverter.isAccountCurrentUser(#accountId)")
    @GetMapping("/{accountId}/mobilities")
    public ResponseEntity<PagedResponseDto<MobilityDto>> getMobilitiesForAccount(@PathVariable String accountId,
                                                                                 @RequestParam(defaultValue = "0") int page,
                                                                                 @RequestParam(defaultValue = "10") int size,
                                                                                 @RequestParam(defaultValue = "DESC") String direction,
                                                                                 @RequestParam(defaultValue = "measuredOn") String sortBy) {
        log.info("GET Account Mobilities REST API called");
        PagedResponseDto<MobilityDto> accountMobilities = mobilityService.getMobilitiesForAccount(accountId, page, size, direction, sortBy);
        return ResponseEntity.ok(accountMobilities);
    }

//    @PreAuthorize("@jwtConverter.isAccountCurrentUser(#accountId)") TODO: Check who will be able to POST mobilities
    @PostMapping("/{accountId}/mobilities")
    public ResponseEntity<MobilityDto> createMobility(@PathVariable String accountId,
                                                   @RequestBody @Valid MobilityDto mobilityDTO) {
        log.info("POST Account Mobilities REST API called");
        MobilityDto mobility = mobilityService.saveMobilityForAccount(accountId, mobilityDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(mobility);
    }

    @PreAuthorize("@jwtConverter.isAccountCurrentUser(#accountId)")
    @GetMapping("/{accountId}/biometrics")
    public ResponseEntity<PagedResponseDto<BiometricsDto>> getBiometricsForAccount(@PathVariable String accountId,
                                                                                   @RequestParam(defaultValue = "0") int page,
                                                                                   @RequestParam(defaultValue = "10") int size,
                                                                                   @RequestParam(defaultValue = "DESC") String direction,
                                                                                   @RequestParam(defaultValue = "measuredOn") String sortBy) {
        log.info("GET Account Biometrics REST API called");
        PagedResponseDto<BiometricsDto> accountBiometrics = biometricsService.getBiometricsForAccount(accountId, page, size, direction, sortBy);
        return ResponseEntity.ok(accountBiometrics);
    }

    //    @PreAuthorize("@jwtConverter.isAccountCurrentUser(#accountId)") TODO: Check who will be able to POST biometrics
    @PostMapping("/{accountId}/biometrics")
    public ResponseEntity<BiometricsDto> createBiometric(@PathVariable String accountId,
                                                      @RequestBody @Valid BiometricsDto biometricsDTO) {
        log.info("POST Account Biometrics REST API called");
        BiometricsDto biometrics = biometricsService.saveBiometricForAccount(accountId, biometricsDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(biometrics);
    }

    @Hidden // This endpoint is called when a user is deleted in Keycloak
    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deleteAccount(@PathVariable String accountId) throws Exception {
        log.info("DELETE Account REST API called");
        accountService.deleteAccount(accountId);
        return ResponseEntity.noContent().build();
    }
}
