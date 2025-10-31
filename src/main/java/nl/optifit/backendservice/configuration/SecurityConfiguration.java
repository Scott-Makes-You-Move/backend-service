package nl.optifit.backendservice.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import nl.optifit.backendservice.security.JwtConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

import java.net.URI;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    private final JwtConverter jwtConverter;
    private final ObjectMapper objectMapper;

    @Bean
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "false")
    public SecurityFilterChain permitAllSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "true")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(authz -> authz
                .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/v3/api-docs",
                        "/webjars/**",
                        "/h2-console/**"
                ).permitAll()
                .anyRequest().authenticated());
        http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));
        http.sessionManagement(sess -> sess.sessionCreationPolicy(
                SessionCreationPolicy.STATELESS));
        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(
                jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
        );

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint problemAuthenticationEntryPoint() {
        return (HttpServletRequest request, HttpServletResponse response,
                AuthenticationException authException) -> {

            Problem problem = Problem.builder()
                    .withType(URI.create("https://example.com/problems/unauthorized"))
                    .withTitle("Unauthorized")
                    .withStatus(Status.UNAUTHORIZED)
                    .withDetail(authException.getMessage())
                    .build();

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(problem));
        };
    }

    @Bean
    public AccessDeniedHandler problemAccessDeniedHandler() {
        return (HttpServletRequest request, HttpServletResponse response,
                AccessDeniedException accessDeniedException) -> {

            Problem problem = Problem.builder()
                    .withType(URI.create("https://example.com/problems/forbidden"))
                    .withTitle("Forbidden")
                    .withStatus(Status.FORBIDDEN)
                    .withDetail(accessDeniedException.getMessage())
                    .build();

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(problem));
        };
    }
}
