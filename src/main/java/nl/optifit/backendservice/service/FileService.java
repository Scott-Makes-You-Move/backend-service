package nl.optifit.backendservice.service;

import com.google.api.services.drive.model.File;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
public class FileService {

    private static final String ACCOUNT_ID = "accountId";
    private static final String VERSION = "version";

    @Value("${chat.client.advisors.files.similarityThreshold}")
    private double filesSimilarityThreshold;
    @Value("${chat.client.advisors.files.topK}")
    private int filesTopK;

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
        log.info("Syncing files");
        long startSyncTime = System.nanoTime();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            accountService.findAllAccounts().stream()
                    .map(account -> keycloakService.findUserById(account.getId()))
                    .flatMap(Optional::stream)
                    .map(UserResource::toRepresentation)
                    .forEach(userRepresentation ->
                            executor.submit(() -> addUserFilesToCosmos(userRepresentation)));

        }

        log.info("Finished syncing files in {} ms", (System.nanoTime() - startSyncTime) / 1000000);
    }

    public List<Document> search(String accountId) {
        log.info("Searching for files of account '{}'", accountId);
        SearchRequest searchRequest = buildSearchRequestForAccount(accountId);
        return filesVectorStore.similaritySearch(searchRequest);
    }

    private void addUserFilesToCosmos(UserRepresentation userRepresentation) {
        try {
            log.info("Syncing files for user '{}'", userRepresentation.getUsername());
            List<File> filesInDrive = driveService.getFilesForUser(userRepresentation.getUsername());

            SearchRequest searchRequest = buildSearchRequestForAccount(userRepresentation.getId());

            filesVectorStore.similaritySearch(searchRequest).stream()
                    .map(Document::getId)
                    .forEach(filesVectorStore::delete);

            List<Document> documents = filesInDrive.stream()
                    .map(file -> buildDocumentFromFile(file, userRepresentation.getId()))
                    .toList();

            filesVectorStore.add(documents);

            log.info("Files synced for user '{}'", userRepresentation.getUsername());
        } catch (IOException e) {
            log.error("Error while syncing files for user '{}': {}", userRepresentation.getUsername(), e.getMessage(), e);
        }
    }

    private SearchRequest buildSearchRequestForAccount(String accountId) {
        Filter.Expression filterExpression = getFilterExpressionDocumentsForAccount(accountId);

        return SearchRequest.builder()
                .query("e") // find all documents
                .topK(filesTopK)
                .similarityThreshold(filesSimilarityThreshold)
                .filterExpression(filterExpression)
                .build();
    }

    private static Filter.Expression getFilterExpressionDocumentsForAccount(String accountId) {
        return new FilterExpressionBuilder()
                .eq(ACCOUNT_ID, accountId)
                .build();
    }

    private Document buildDocumentFromFile(File file, String accountId) {
        return new Document(
                file.getId(),
                driveService.readContent(file),
                Map.of(
                        ACCOUNT_ID, accountId,
                        VERSION, String.valueOf(file.getVersion())
                ));
    }
}
