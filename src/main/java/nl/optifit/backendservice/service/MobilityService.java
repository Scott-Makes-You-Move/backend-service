package nl.optifit.backendservice.service;

import lombok.*;
import lombok.extern.slf4j.*;
import nl.optifit.backendservice.dto.*;
import nl.optifit.backendservice.model.*;
import nl.optifit.backendservice.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.*;

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
