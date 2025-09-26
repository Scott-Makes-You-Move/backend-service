package nl.optifit.backendservice.service;

import com.google.api.services.drive.model.File;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.FileDto;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class FileService {
    private final VectorStore vectorStore;
    private final AccountService accountService;
    private final KeycloakService keycloakService;
    private final DriveService driveService;

    public FileService(@Qualifier("filesVectorStore") VectorStore vectorStore, AccountService accountService, KeycloakService keycloakService, DriveService driveService) {
        this.vectorStore = vectorStore;
        this.accountService = accountService;
        this.keycloakService = keycloakService;
        this.driveService = driveService;
    }

    public Document storeFile(FileDto fileDto) {
        log.info("Storing file '{}'", fileDto.getId());
        Document document = fileDto.toDocument();
        vectorStore.add(List.of(document));
        log.info("File '{}' stored", fileDto.getId());
        return document;
    }

    public void syncFiles() {
        accountService.findAllAccounts().stream()
                .map(account -> keycloakService.findUserById(account.getId()))
                .flatMap(Optional::stream)
                .map(UserResource::toRepresentation)
                .forEach(this::addUserFilesToCosmos);
    }

    private void addUserFilesToCosmos(UserRepresentation userRepresentation) {
        try {
            log.info("Syncing files for user '{}'", userRepresentation.getUsername());
            List<File> files = driveService.getFilesForUser(userRepresentation.getUsername());
            List<Document> documents = files.stream()
                    .map(file ->
                            FileDto.builder()
                                    .id(file.getId())
                                    .accountId(userRepresentation.getId())
                                    .content(driveService.readContent(file))
                                    .build()
                    )
                    .map(FileDto::toDocument)
                    .toList();
            vectorStore.add(documents);
            log.info("Files synced for user '{}'", userRepresentation.getId());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
