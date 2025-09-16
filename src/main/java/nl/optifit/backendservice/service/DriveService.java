package nl.optifit.backendservice.service;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class DriveService {

    private static final String DOCS_MIME_TYPE = "application/vnd.google-apps.document";
    private static final String EXPORT_MIME_TYPE = "text/plain";

    private final Drive drive;
    private final KeycloakService keycloakService;

    public List<File> getDriveFiles() throws IOException {
        var result = drive.files().list()
                .setFields("files(id, name, mimeType)")
                .execute();

        List<File> googleDocs = result.getFiles().stream()
                .filter(file -> DOCS_MIME_TYPE.equals(file.getMimeType()))
                .toList();

        googleDocs.forEach(this::readFileContent);

        return googleDocs;
    }

    public List<File> getDriveFilesForAccount(String accountId) throws IOException {
        Optional<UserResource> optionalUser = keycloakService.findUserById(accountId);

        if (optionalUser.isEmpty()) {
            log.warn("User with ID '{}' not found", accountId);
            return List.of();
        }

        UserResource userResource = optionalUser.get();
        UserRepresentation representation = userResource.toRepresentation();
        String username = representation.getUsername();

        List<File> googleDocs = drive.files().list()
                .setFields("files(id, name, mimeType)")
                .setQ("mimeType = '" + DOCS_MIME_TYPE + "' and '" + username + "' in parents")
                .execute()
                .getFiles();

        googleDocs.forEach(this::readFileContent);

        return googleDocs;
    }

    public void createDriveFolder(String folderName) throws IOException {
        log.info("Creating Google Drive folder '{}'", folderName);
        File folder = new File()
                .setName(folderName)
                .setMimeType("application/vnd.google-apps.folder");

        File createdFolder = drive.files()
                .create(folder)
                .execute();

        log.info("Created Google Drive folder '{}' successfully", createdFolder.getName());
    }

    public void deleteDriveFolder(String folderName) throws IOException {
        log.info("Deleting Google Drive folder '{}'", folderName);
        drive.files()
                .delete(folderName)
                .execute();
        log.info("Deleted Google Drive folder '{}' successfully", folderName);
    }

    public String readFileContent(File doc) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            drive.files().export(doc.getId(), EXPORT_MIME_TYPE)
                    .executeMediaAndDownloadTo(outputStream);

            String contents = outputStream.toString(StandardCharsets.UTF_8);
            log.info("Contents of Google Doc '{}':\n{}", doc.getName(), contents);
            return contents;
        } catch (IOException e) {
            log.error("Failed to export Google Doc '{}': {}", doc.getName(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
