package nl.optifit.backendservice.cron;

import com.google.api.services.drive.model.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.FileDto;
import nl.optifit.backendservice.service.AccountService;
import nl.optifit.backendservice.service.DriveService;
import nl.optifit.backendservice.service.FileService;
import nl.optifit.backendservice.service.KeycloakService;
import org.keycloak.admin.client.resource.UserResource;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class DriveCronScheduler {

    private final FileService fileService;

    @Scheduled(cron = "0 0 18 ? * 6")
    public void syncFiles() throws InterruptedException {
        fileService.syncFiles();
    }
}
