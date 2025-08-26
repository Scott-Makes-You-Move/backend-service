package nl.optifit.backendservice.configuration;

import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakAdminConfiguration {

    @Value("${keycloak.auth-server-url}")
    private String serverUrl;

    @Value("${keycloak.realm.master}")
    private String realm;

    @Value("${keycloak.client.admin}")
    private String clientId;

    @Value("${keycloak.username}")
    private String username;

    @Value("${keycloak.password}")
    private String password;

    @Bean
    public Keycloak keycloak() {
        return Keycloak.getInstance(
                serverUrl,
                realm,
                username,
                password,
                clientId
        );
    }
}
