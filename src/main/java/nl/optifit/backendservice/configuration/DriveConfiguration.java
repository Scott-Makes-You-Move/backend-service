package nl.optifit.backendservice.configuration;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Collections;

@Configuration
public class DriveConfiguration {

    @Value("${google.service-account.credentials:}")
    private String credentialsBase64;

    @Bean
    public Drive getDrive() throws IOException, GeneralSecurityException {
        if (credentialsBase64.isEmpty()) {
            throw new IllegalArgumentException("Google service account credentials are missing!");
        }

        byte[] decoded = Base64.getDecoder().decode(credentialsBase64);
        try (InputStream in = new ByteArrayInputStream(decoded)) {
            GoogleCredentials googleCredentials = GoogleCredentials.fromStream(in)
                    .createScoped(Collections.singleton(DriveScopes.DRIVE));

            return new Drive.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(googleCredentials)
            )
                    .setApplicationName("SMYM")
                    .build();
        }
    }
}
