package nl.optifit.backendservice.service;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class DriveService {

    @Value("${google.drive.root-folder-id}")
    private String rootFolderId;

    private static final String DOCS_MIME_TYPE = "application/vnd.google-apps.document";
    private static final String SPREADSHEET_MIME_TYPE = "application/vnd.google-apps.spreadsheet";
    private static final String TEXT_PLAIN = "text/plain";
    private static final String TEXT_CSV = "text/csv";

    private final Drive drive;

    public List<File> getFiles() throws IOException {
        var result = drive.files().list()
                .setFields("files(id, name, mimeType)")
                .execute();

        List<File> documents = result.getFiles().stream()
                .filter(file -> DOCS_MIME_TYPE.equals(file.getMimeType()) || SPREADSHEET_MIME_TYPE.equals(file.getMimeType()))
                .toList();

        documents.forEach(this::readContent);

        return documents;
    }

    public List<File> getFilesForUser(String username) throws IOException {
        List<File> folders = drive.files().list()
                .setQ("name='" + username + "' and '" + rootFolderId + "' in parents and mimeType='application/vnd.google-apps.folder'")
                .setFields("files(id, name, mimeType)")
                .execute()
                .getFiles();

        String userFolderId = folders.stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User folder not found"))
                .getId();

        List<File> files = drive.files().list()
                .setQ("'" + userFolderId + "' in parents and (mimeType='" + DOCS_MIME_TYPE + "' or mimeType='" + SPREADSHEET_MIME_TYPE + "')")
                .setFields("files(id, name, mimeType)")
                .execute()
                .getFiles();

        files.forEach(this::readContent);

        return files;
    }

    public void createDriveFolderInRoot(String folderName) throws IOException {
        log.info("Creating Google Drive folder '{}' in root folder", folderName);

        File folder = new File()
                .setName(folderName)
                .setMimeType("application/vnd.google-apps.folder")
                .setParents(List.of(rootFolderId));

        File createdFolder = drive.files()
                .create(folder)
                .setFields("id, name, parents")
                .execute();

        log.info("Created Google Drive folder '{}' with ID '{}' successfully", createdFolder.getName(), createdFolder.getId());
    }

    public void deleteDriveFolderInRoot(String folderName) throws IOException {
        List<File> folders = drive.files().list()
                .setQ("name='" + folderName + "' and '" + rootFolderId + "' in parents and mimeType='application/vnd.google-apps.folder'")
                .setFields("files(id, name)")
                .execute()
                .getFiles();

        String folderId = folders.stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Folder not found"))
                .getId();

        drive.files().delete(folderId).execute();
        log.info("Deleted Google Drive folder '{}' successfully", folderName);
    }

    public String readContent(File file) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (DOCS_MIME_TYPE.equals(file.getMimeType())) {
                drive.files().export(file.getId(), TEXT_PLAIN)
                        .executeMediaAndDownloadTo(outputStream);

            } else if (SPREADSHEET_MIME_TYPE.equals(file.getMimeType())) {
                drive.files().export(file.getId(), TEXT_CSV)
                        .executeMediaAndDownloadTo(outputStream);
            } else {
                log.warn("Skipping unsupported file type: {}", file.getMimeType());
                return null;
            }

            String contents = outputStream.toString(StandardCharsets.UTF_8);
            log.info("Contents of '{}':\n{}", file.getName(), contents);
            return contents;

        } catch (IOException e) {
            log.error("Failed to read file '{}': {}", file.getName(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
