package nl.optifit.backendservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.model.Account;
import nl.optifit.backendservice.repository.AccountRepository;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class AccountService {
    private final AccountRepository accountRepository;

    public Account createAccountForId(String accountId) {
        log.info("Creating account for id [{}]", accountId);
        return accountRepository.save(Account.builder().accountId(accountId).build());
    }
}
