package nl.optifit.backendservice.dto;

import org.keycloak.representations.idm.UserRepresentation;

public record UserDto(String userId, String firstName, String lastName) {
    public static UserDto fromUserRepresentation(UserRepresentation userRepresentation) {
        return new UserDto(userRepresentation.getId(), userRepresentation.getFirstName(), userRepresentation.getLastName());
    }
}
