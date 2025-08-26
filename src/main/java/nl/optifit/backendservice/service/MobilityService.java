package nl.optifit.backendservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.MobilityDto;
import nl.optifit.backendservice.dto.PagedResponseDto;
import nl.optifit.backendservice.model.Account;
import nl.optifit.backendservice.model.Mobility;
import nl.optifit.backendservice.repository.AccountRepository;
import nl.optifit.backendservice.repository.MobilityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class MobilityService {
    private final AccountRepository accountRepository;
    private final MobilityRepository mobilityRepository;

    public PagedResponseDto<MobilityDto> getMobilitiesForAccount(String accountId, int page, int size, String direction, String sortBy) {
        log.debug("Retrieving mobilities with page '{}', size '{}', direction '{}', sortBy '{}'", page, size, direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));
        Page<MobilityDto> mobilityDtoPage = mobilityRepository.findAllByAccountId(pageable, accountId).map(MobilityDto::fromMobility);

        return PagedResponseDto.fromPage(mobilityDtoPage);
    }

    public MobilityDto saveMobilityForAccount(String accountId, MobilityDto mobilityDTO) {
        log.debug("Saving mobility for account '{}'", accountId);
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Could not find user"));
        Mobility mobility = MobilityDto.toMobility(account, mobilityDTO);
        Mobility savedMobility = mobilityRepository.save(mobility);

        return MobilityDto.fromMobility(savedMobility);
    }
}
