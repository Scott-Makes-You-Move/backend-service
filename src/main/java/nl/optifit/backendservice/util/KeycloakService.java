package nl.optifit.backendservice.util;

import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KeycloakService {

    public static final String REALM = "smym-dev";

    private final Keycloak keycloak;

    public Optional<UserResource> findUserById(String id) {
        return Optional.ofNullable(keycloak.realm(REALM)
                .users()
                .get(id)
        );
    }

    public Optional<UserRepresentation> findUserByUsername(String username) {
        return keycloak.realm(REALM)
                .users()
                .search(username)
                .stream()
                .findFirst();
    }
}
