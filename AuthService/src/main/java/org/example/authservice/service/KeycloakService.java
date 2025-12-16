package org.example.authservice.service;

import org.example.authservice.dto.LoginRequest;
import org.example.authservice.dto.LoginResponse;
import org.example.authservice.dto.RegisterRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

@Service
public class KeycloakService {

    private final WebClient webClient;
    private final String realm;
    private final String clientId;
    private final String clientSecret;
    private final String adminUsername;
    private final String adminPassword;

    public KeycloakService(
            WebClient.Builder builder,
            @Value("${keycloak.base-url}") String baseUrl,
            @Value("${keycloak.realm}") String realm,
            @Value("${keycloak.client-id}") String clientId,
            @Value("${keycloak.client-secret}") String clientSecret,
            @Value("${keycloak.admin.username}") String adminUsername,
            @Value("${keycloak.admin.password}") String adminPassword
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    public Mono<Void> register(RegisterRequest request) {
        return getAdminToken()
                .flatMap(token -> createUser(token, request)
                        .flatMap(userId -> setPassword(token, userId, request.password())));
    }

    public Mono<LoginResponse> login(LoginRequest request) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/realms/{realm}/protocol/openid-connect/token")
                        .build(realm))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters.fromFormData("grant_type", "password")
                        .with("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("username", request.username())
                        .with("password", request.password()))
                .retrieve()
                .bodyToMono(Map.class)
                .map(body -> new LoginResponse(
                        (String) body.get("access_token"),
                        (String) body.get("refresh_token"),
                        (String) body.get("token_type"),
                        ((Number) body.getOrDefault("expires_in", 0)).longValue()
                ));
    }

    private Mono<String> getAdminToken() {
        return webClient.post()
                .uri("/realms/master/protocol/openid-connect/token")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters.fromFormData("grant_type", "password")
                        .with("client_id", "admin-cli")
                        .with("username", adminUsername)
                        .with("password", adminPassword))
                .retrieve()
                .bodyToMono(Map.class)
                .map(body -> (String) body.get("access_token"));
    }

    private Mono<String> createUser(String token, RegisterRequest request) {
        return webClient.post()
                .uri("/admin/realms/{realm}/users", realm)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "username", request.username(),
                        "email", request.email(),
                        // чтобы Keycloak не требовал ручного заполнения профиля
                        "firstName", defaultIfBlank(request.firstName(), request.username()),
                        "lastName", defaultIfBlank(request.lastName(), request.username()),
                        "enabled", true,
                        // чтобы не требовать ручное подтверждение в консоли
                        "emailVerified", true
                ))
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        String location = response.headers().asHttpHeaders().getFirst(HttpHeaders.LOCATION);
                        if (location != null) {
                            return Mono.just(URI.create(location).getPath().replaceAll(".*/", ""));
                        }
                    }
                    return response.createException().flatMap(Mono::error);
                });
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private Mono<Void> setPassword(String token, String userId, String password) {
        return webClient.put()
                .uri("/admin/realms/{realm}/users/{id}/reset-password", realm, userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "type", "password",
                        "value", password,
                        "temporary", false
                ))
                .retrieve()
                .toBodilessEntity()
                .then();
    }
}


