package nl.optifit.backendservice.cron;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.service.AccountService;
import nl.optifit.backendservice.service.DriveService;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class DriveCronScheduler {

    public static final String EUROPE_AMSTERDAM = "Europe/Amsterdam";

    private final DriveService driveService;
    private final AccountService accountService;
    @Qualifier("filesVectorStore")
    private final VectorStore filesVectorStore;

    @Scheduled(cron = "#{@cronProperties.drive.sync}", zone = EUROPE_AMSTERDAM)
    public void syncFiles() throws IOException {
        accountService.findAllAccounts().forEach(account -> {
//            try {
//                List<File> files = driveService.getDriveFilesForAccount(account);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
        });
    }
}
