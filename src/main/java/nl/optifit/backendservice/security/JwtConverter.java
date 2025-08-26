package nl.optifit.backendservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Component
public class JwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    private final JwtConverterProperties jwtConverterProperties;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> defaultAuthorities = jwtGrantedAuthoritiesConverter.convert(jwt);
        Collection<GrantedAuthority> resourceRoles = extractResourceRoles(jwt);

        Collection<GrantedAuthority> authorities = Stream.concat(
                        defaultAuthorities.stream(),
                        resourceRoles.stream())
                .collect(Collectors.toSet());

        String principal = getPrincipalClaim(jwt);
        return new JwtAuthenticationToken(jwt, authorities, principal);
    }

    public boolean isAccountCurrentUser(String accountId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth) || !authentication.isAuthenticated()) {
            return false;
        }
        Jwt jwt = jwtAuth.getToken();

        return jwt.getSubject().equals(accountId);
    }

    private String getPrincipalClaim(Jwt jwt) {
        String claimName = Optional.ofNullable(jwtConverterProperties.getPrincipalAttribute())
                .orElse(JwtClaimNames.SUB);
        return jwt.getClaimAsString(claimName);
    }

    private Collection<GrantedAuthority> extractResourceRoles(Jwt jwt) {
        Object resourceAccessObj = jwt.getClaim("resource_access");

        if (!(resourceAccessObj instanceof Map<?, ?> resourceAccess)) {
            return Set.of();
        }

        Object resourceObj = resourceAccess.get(jwtConverterProperties.getResourceId());
        if (!(resourceObj instanceof Map<?, ?> resource)) {
            return Set.of();
        }

        Object rolesObj = resource.get("roles");
        if (!(rolesObj instanceof Collection<?> roles)) {
            return Set.of();
        }

        return roles.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());
    }
}
