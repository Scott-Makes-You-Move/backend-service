package nl.optifit.backendservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.*;
import nl.optifit.backendservice.model.*;
import nl.optifit.backendservice.repository.AccountRepository;
import nl.optifit.backendservice.security.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class AccountService {
    private final LeaderboardService leaderboardService;
    private final AccountRepository accountRepository;
    private final JwtConverter jwtConverter;

    @Transactional
    public AccountDto createAccount(String accountId) {
        log.info("Creating account '{}'", accountId);
        Account account = Account.builder().id(accountId).build();
        Leaderboard leaderboard = leaderboardService.createLeaderboardForAccount(account);
        account.setLeaderboard(leaderboard);
        Account savedAccount = accountRepository.save(account);

        return AccountDto.fromAccount(savedAccount);
    }

    @Transactional
    public void deleteAccount(String accountId) {
        log.info("Deleting account '{}'", accountId);
        accountRepository.deleteById(accountId);
    }

    public List<Account> findAllAccounts() {
        log.debug("Finding all accounts");
        return accountRepository.findAll();
    }
}
