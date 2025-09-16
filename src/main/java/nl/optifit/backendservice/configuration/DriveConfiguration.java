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
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Configuration
public class DriveConfiguration {

    @Value("${google.service-account.credentials:}")
    private String credentials;

    @Bean
    public Drive getDrive() throws IOException, GeneralSecurityException {
        try (InputStream in = new ByteArrayInputStream(credentials.getBytes(StandardCharsets.UTF_8))) {
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
