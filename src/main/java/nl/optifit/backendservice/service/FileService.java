package nl.optifit.backendservice.service;

import com.google.api.services.drive.model.File;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.FileDto;
import nl.optifit.backendservice.dto.SearchQueryDto;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class FileService {
    private final VectorStore filesVectorStore;
    private final AccountService accountService;
    private final KeycloakService keycloakService;
    private final DriveService driveService;

    public FileService(@Qualifier("filesVectorStore") VectorStore filesVectorStore, AccountService accountService, KeycloakService keycloakService, DriveService driveService) {
        this.filesVectorStore = filesVectorStore;
        this.accountService = accountService;
        this.keycloakService = keycloakService;
        this.driveService = driveService;
    }

    public void syncFiles() {
        accountService.findAllAccounts().stream()
                .map(account -> keycloakService.findUserById(account.getId()))
                .flatMap(Optional::stream)
                .map(UserResource::toRepresentation)
                .forEach(this::addUserFilesToCosmos);
    }

    public List<Document> search(SearchQueryDto searchQueryDto) {
        log.info("Searching for files");

        Filter.Expression accountFilter = new FilterExpressionBuilder()
                .eq("accountId", searchQueryDto.filterExpression())
                .build();

        SearchRequest searchRequest = SearchRequest.builder()
                .query(searchQueryDto.query())
                .topK(searchQueryDto.topK())
                .similarityThreshold(searchQueryDto.similarityThreshold())
                .filterExpression(accountFilter)
                .build();

        return filesVectorStore.similaritySearch(searchRequest);
    }

    public List<Document> search(int topK, double similarityThreshold, Filter.Expression filterExpression) {
        log.info("Searching for all files");

        SearchRequest searchRequest = SearchRequest.builder()
                .query("e")
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .filterExpression(filterExpression)
                .build();

        return filesVectorStore.similaritySearch(searchRequest);
    }

    private void addUserFilesToCosmos(UserRepresentation userRepresentation) {
        try {
            log.info("Syncing files for user '{}'", userRepresentation.getUsername());
            List<File> files = driveService.getFilesForUser(userRepresentation.getUsername());
            List<Document> documents = files.stream()
                    .map(file -> new Document(
                            file.getId(),
                            driveService.readContent(file),
                            Map.of(
                                    "accountId", userRepresentation.getId(),
                                    "version", String.valueOf(file.getVersion()
                                    )
                            )))
                    .toList();

            List<String> filesToDelete = files.stream().map(File::getId).toList();

            filesVectorStore.delete(filesToDelete); // first delete all existing documents for this user
            filesVectorStore.add(documents); // then add the new ones
            log.info("Files synced for user '{}'", userRepresentation.getId());
        } catch (IOException e) {
            log.error("Error while syncing files for user '{}': {}", userRepresentation.getUsername(), e.getMessage(), e);
        }
    }
}
