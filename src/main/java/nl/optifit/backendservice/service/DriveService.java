package nl.optifit.backendservice.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class DriveService {

    private static final String DOCS_MIME_TYPE = "application/vnd.google-apps.document";
    private static final String EXPORT_MIME_TYPE = "text/plain";

    public List<File> getDriveFiles() throws IOException, GeneralSecurityException {
        Drive drive = getDrive();

        var result = drive.files().list()
                .setFields("files(id, name, mimeType)")
                .execute();

        List<File> googleDocs = result.getFiles().stream()
                .filter(file -> DOCS_MIME_TYPE.equals(file.getMimeType()))
                .toList();

        googleDocs.forEach(doc -> {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                drive.files().export(doc.getId(), EXPORT_MIME_TYPE)
                        .executeMediaAndDownloadTo(outputStream);

                String contents = outputStream.toString(StandardCharsets.UTF_8);
                log.info("Contents of Google Doc '{}':\n{}", doc.getName(), contents);
            } catch (IOException e) {
                log.error("Failed to export Google Doc '{}': {}", doc.getName(), e.getMessage(), e);
            }
        });

        return googleDocs;
    }

    private Drive getDrive() throws IOException, GeneralSecurityException {
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                .createScoped(Collections.singleton(DriveScopes.DRIVE));

        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        )
                .setApplicationName("MyApp")
                .build();
    }
}
