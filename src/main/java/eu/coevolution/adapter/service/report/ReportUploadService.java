package eu.coevolution.adapter.service.report;

import eu.coevolution.adapter.service.http.HttpClient;
import eu.coevolution.adapter.service.keycloak.AegisKeycloakProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportUploadService {

    private final HttpClient httpClient;
    private final AegisKeycloakProviderService aegisKeycloakProviderService;

    @Value("${ragre.report-upload.base-url}")
    private String reportUploadBaseUrl;

    @Value("${ragre.report-upload.endpoint}")
    private String reportUploadEndpoint;

    public String uploadReport(String filename,
                               String reportContent,
                               String pool,
                               String bucket,
                               String modelUuid,
                               String version,
                               List<String> tags) {
        byte[] bytes = reportContent.getBytes(StandardCharsets.UTF_8);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", createReportResource(bytes, filename));
        body.add("pool", pool);
        body.add("bucket", bucket);
        body.add("model_uuid", modelUuid);
        body.add("version", version);
        addTags(body, tags);

        String token = aegisKeycloakProviderService.getToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Failed to acquire AEGIS access token for report upload");
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);

        return httpClient.postRequestWithFile(
                reportUploadBaseUrl + reportUploadEndpoint,
                body,
                headers,
                Map.of()
        );
    }

    private ByteArrayResource createReportResource(byte[] reportContent, String filename) {
        return new NamedByteArrayResource(reportContent, filename);
    }

    private void addTags(MultiValueMap<String, Object> body, List<String> tags) {
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
