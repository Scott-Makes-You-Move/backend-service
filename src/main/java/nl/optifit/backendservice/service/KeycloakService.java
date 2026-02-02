package nl.optifit.backendservice.service;

import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KeycloakService {

    @Value("${keycloak.realm.smym}")
    private String realm;

    private final Keycloak keycloak;

    public UsersResource findAllUsers() {
        return keycloak.realm(realm).users();
    }

    public Optional<UserResource> findUserById(String id) {
        return Optional.ofNullable(keycloak.realm(realm)
                .users()
                .get(id)
        );
    }
}
