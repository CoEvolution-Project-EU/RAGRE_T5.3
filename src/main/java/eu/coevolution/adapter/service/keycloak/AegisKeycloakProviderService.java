package eu.coevolution.adapter.service.keycloak;

import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@NoArgsConstructor
public class AegisKeycloakProviderService {

    @Value("${aegis.auth.token-url}")
    private String tokenUrl;

    @Value("${aegis.auth.client-id}")
    private String clientId;

    @Value("${aegis.auth.username}")
    private String username;

    @Value("${aegis.auth.password}")
    private String password;

    private final Logger logger = (Logger) LoggerFactory.getLogger(AegisKeycloakProviderService.class);

    public String getToken() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(getRequestBody()))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                return jsonResponse.get("access_token").asText();
            }

            logger.error("Failed to get AEGIS token. HTTP status: {}, body: {}", response.statusCode(), response.body());
            return null;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.error("Error while getting token from AEGIS Keycloak", e);
            return null;
        }
    }

    private String getRequestBody() {
        Map<String, String> params = Map.of(
                "grant_type", "password",
                "client_id", clientId,
                "username", username,
                "password", password
        );
        return params.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }
}
