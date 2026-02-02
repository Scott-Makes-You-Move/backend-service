package nl.optifit.backendservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.BiometricsDto;
import nl.optifit.backendservice.dto.PagedResponseDto;
import nl.optifit.backendservice.model.Account;
import nl.optifit.backendservice.model.Biometrics;
import nl.optifit.backendservice.repository.AccountRepository;
import nl.optifit.backendservice.repository.BiometricsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class BiometricsService {
    private final AccountRepository accountRepository;
    private final BiometricsRepository biometricsRepository;

    public PagedResponseDto<BiometricsDto> getBiometricsForAccount(String accountId, int page, int size, String direction, String sortBy) {
        log.debug("Retrieving biometrics with page '{}', size '{}', direction '{}', sortBy '{}'", page, size, direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));
        Page<BiometricsDto> biometricsDtoPage = biometricsRepository.findAllByAccountId(pageable, accountId).map(BiometricsDto::fromBiometrics);

        return PagedResponseDto.fromPage(biometricsDtoPage);
    }

    public BiometricsDto saveBiometricForAccount(String accountId, BiometricsDto biometricsDTO) {
        log.debug("Saving biometric for account '{}'", accountId);
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Could not find user"));
        Biometrics biometrics = BiometricsDto.toBiometrics(account, biometricsDTO);
        Biometrics savedBiometrics = biometricsRepository.save(biometrics);

        return BiometricsDto.fromBiometrics(savedBiometrics);
    }

    public Biometrics findMostRecentBiometricsForAccount(String accountId) {
        return biometricsRepository.findFirstByAccountIdOrderByMeasuredOnDesc(accountId);
    }

    public void deleteAll() {
        log.debug("Deleting all biometrics");
        biometricsRepository.deleteAll();
    }
}
