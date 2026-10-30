package eu.coevolution.adapter.service.ragre;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import eu.coevolution.adapter.dto.OrchestratorFileSearchResponseDTO;
import eu.coevolution.adapter.dto.OrchestratorFileSearchResultDTO;
import eu.coevolution.adapter.service.http.HttpClient;
import eu.coevolution.adapter.service.keycloak.AegisKeycloakProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RagreRiskReportUpdateService {

    private static final String AI_RISK_SCORE_PROPERTY_NAME = "ragre:ai-risk:score";

    private final ObjectMapper objectMapper;
    private final AegisKeycloakProviderService aegisKeycloakProviderService;
    private final HttpClient httpClient;

    @Value("${ragre.report-upload.base-url}")
    private String reportBaseUrl;

    @Value("${ragre.report-upload.endpoint}")
    private String reportUploadEndpoint;

    @Value("${ragre.report-search.endpoint}")
    private String reportSearchEndpoint;

    @Value("${ragre.report-download.endpoint}")
    private String reportDownloadEndpoint;

    public OrchestratorFileSearchResultDTO findLatestRiskReport(String modelUuid, String version) {
        String token = requireToken();
        String searchUrl = buildSearchUrl(modelUuid, version, "risk");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(searchUrl))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = java.net.http.HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Failed to search risk reports. HTTP status: " + response.statusCode());
            }

            OrchestratorFileSearchResponseDTO searchResponse = objectMapper.readValue(response.body(), OrchestratorFileSearchResponseDTO.class);
            if (searchResponse.getFiles() == null || searchResponse.getFiles().isEmpty()) {
                return null;
            }

            return searchResponse.getFiles()
                    .stream()
                    .max(Comparator.comparing(OrchestratorFileSearchResultDTO::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(null);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Failed to search risk reports for model " + modelUuid + " and version " + version, e);
        }
    }

    public byte[] downloadReportById(String fileId) {
        String token = requireToken();
        String downloadUrl = reportBaseUrl + reportDownloadEndpoint + "/" + urlEncode(fileId);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<byte[]> response = java.net.http.HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Failed to download risk report. HTTP status: " + response.statusCode());
            }

            return response.body();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Failed to download risk report with id " + fileId, e);
        }
    }

    public String updateAiRiskScore(byte[] bomContent, double aiRiskScore) {
        try {
            JsonNode root = objectMapper.readTree(bomContent);
            JsonNode propertiesNode = root.path("metadata").path("component").path("properties");
            if (!propertiesNode.isArray()) {
                throw new IllegalStateException("Risk report BOM does not contain metadata.component.properties array");
            }

            for (JsonNode propertyNode : propertiesNode) {
                if (AI_RISK_SCORE_PROPERTY_NAME.equals(propertyNode.path("name").asText()) && propertyNode.isObject()) {
                    ((com.fasterxml.jackson.databind.node.ObjectNode) propertyNode).put("value", Double.toString(aiRiskScore));
                    DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
                    printer.indentObjectsWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
                    printer.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
                    // the score change invalidates any existing signature; the signing engine signs the updated report
                    ((com.fasterxml.jackson.databind.node.ObjectNode) root).remove("signature");
                    return objectMapper.writer(printer).writeValueAsString(root);
                }
            }

            throw new IllegalStateException("Risk report BOM does not contain property " + AI_RISK_SCORE_PROPERTY_NAME);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to update AI risk score in BOM report", e);
        }
    }

    public String uploadUpdatedRiskReport(OrchestratorFileSearchResultDTO storedReport,
                                          String updatedBomContent,
                                          String modelUuid,
                                          String version) {
        if (storedReport == null) {
            throw new IllegalArgumentException("Stored risk report metadata is required");
        }

        String resolvedModelUuid = firstNonBlank(storedReport.getModelUuid(), modelUuid);
        String resolvedVersion = firstNonBlank(storedReport.getVersion(), version);
        if (isBlank(resolvedModelUuid) || isBlank(resolvedVersion)) {
            throw new IllegalStateException("Risk report upload requires model_uuid and version");
        }

        byte[] reportContent = updatedBomContent.getBytes(StandardCharsets.UTF_8);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", createReportResource(reportContent, storedReport.getFilename()));
        body.add("pool", storedReport.getPool());
        body.add("bucket", storedReport.getBucket());
        body.add("model_uuid", resolvedModelUuid);
        body.add("version", resolvedVersion);
        addTags(body, storedReport.getTags());

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + requireToken());

        return httpClient.postRequestWithFile(
                reportBaseUrl + reportUploadEndpoint,
                body,
                headers,
                Map.of()
        );
    }

    private String buildSearchUrl(String modelUuid, String version, String reportType) {
        return reportBaseUrl + reportSearchEndpoint
                + "?model_uuid=" + urlEncode(modelUuid)
                + "&version=" + urlEncode(version)
                + "&report_type=" + urlEncode(reportType);
    }

    private String requireToken() {
        String token = aegisKeycloakProviderService.getToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Failed to acquire AEGIS access token for risk report operations");
        }
        return token;
    }

    private String firstNonBlank(String first, String second) {
        return !isBlank(first) ? first : second;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private ByteArrayResource createReportResource(byte[] reportContent, String filename) {
        return new NamedByteArrayResource(reportContent, filename);
    }

    private void addTags(MultiValueMap<String, Object> body, java.util.List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }
        String joinedTags = tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .collect(java.util.stream.Collectors.joining(","));
        if (!joinedTags.isBlank()) {
            body.add("tags", joinedTags);
        }
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {

        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
