package nl.optifit.backendservice.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.model.Account;
import nl.optifit.backendservice.model.Leaderboard;
import nl.optifit.backendservice.repository.AccountRepository;
import nl.optifit.backendservice.repository.LeaderboardRepository;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final LeaderboardRepository leaderboardRepository;

    @Transactional
    public Account createAccountForId(String accountId) {
        log.info("Creating account for id [{}]", accountId);

        Account account = Account.builder().accountId(accountId).build();
        Leaderboard leaderboard = Leaderboard.builder().account(account).completionRate(0.0).currentStreak(0).longestStreak(0).build();
        account.setLeaderboard(leaderboard);

        Account savedAccount = accountRepository.save(account);

        log.info("Created account [{}]", savedAccount.getAccountId());

        return savedAccount;
    }

    @Transactional
    public void deleteAccount(String accountId) {
        log.info("Deleting account [{}]", accountId);
        accountRepository.deleteByAccountId(accountId);
        log.info("Deleted account [{}]", accountId);
        leaderboardRepository.deleteByAccount_AccountId(accountId);
        log.info("Deleted leaderboard for account [{}]", accountId);
    }
}
