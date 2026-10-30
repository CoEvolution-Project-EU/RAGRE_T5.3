package eu.coevolution.adapter.service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.coevolution.adapter.dto.OrchestratorFileReferenceDTO;
import eu.coevolution.adapter.dto.RagreModelEventDTO;
import eu.coevolution.adapter.service.http.HttpClient;
import eu.coevolution.adapter.service.keycloak.AegisKeycloakProviderService;
import eu.coevolution.adapter.service.ragre.InMemoryMultipartFile;
import eu.coevolution.adapter.service.ragre.OrchestratorFileDownloadService;
import eu.coevolution.adapter.service.ragre.RagreService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RagreModelRegistrationConsumer {

    private final ObjectMapper objectMapper;
    private final RagreService ragreService;
    private final HttpClient httpClient;
    private final AegisKeycloakProviderService aegisKeycloakProviderService;
    private final OrchestratorFileDownloadService orchestratorFileDownloadService;

    @Value("${ragre.report-upload.base-url}")
    private String reportUploadBaseUrl;

    @Value("${ragre.report-upload.endpoint}")
    private String reportUploadEndpoint;

    @KafkaListener(topics = "${ragre.kafka.model-registration.topic}", groupId = "${ragre.kafka.all-topics.group-id}")
    public void consume(String message) throws JsonProcessingException {
        RagreModelEventDTO event = objectMapper.readValue(message, RagreModelEventDTO.class);

        if (!"ragre_models".equals(event.getEvent())) {
            return;
        }

        OrchestratorFileReferenceDTO requirementsFileReference = resolveRequirementsFileReference(event);
        System.out.println("New requirements file: " + requirementsFileReference.getDownloadUrl());

        String asset = objectMapper.writeValueAsString(Map.of("name", event.getModelName()));
        byte[] content = orchestratorFileDownloadService.downloadFile(requirementsFileReference.getDownloadUrl());
        if (content == null || content.length == 0) {
            throw new IllegalStateException("Downloaded requirements file is empty: " + requirementsFileReference.getFilename());
        }

        MultipartFile file = new InMemoryMultipartFile(
                "file",
                requirementsFileReference.getFilename(),
                "application/zip",
                content
        );

        String report =  ragreService.generateUnifiedReportJson(asset, file);
        if (report == null || report.isBlank()) {
            throw new IllegalStateException("Failed to generate RAGRE unified report for model "
                    + event.getModelUuid() + " version " + event.getVersion()
                    + " from file " + requirementsFileReference.getFilename());
        }
        uploadReport(event, report);
    }

    private void uploadReport(RagreModelEventDTO event, String report) {
        byte[] reportContent = report.getBytes(StandardCharsets.UTF_8);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", createReportResource(reportContent, buildReportFilename(event)));
        body.add("pool", "RP");
        body.add("bucket", "risk-reports");
        body.add("model_uuid", event.getModelUuid());
        body.add("version", event.getVersion());
        addTags(body, event.getTags());

        String token = aegisKeycloakProviderService.getToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Failed to acquire AEGIS access token for report upload");
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);

        String response = httpClient.postRequestWithFile(
                reportUploadBaseUrl + reportUploadEndpoint,
                body,
                headers,
                Map.of()
        );
        System.out.println("Report upload response: " + response);
    }

    private String buildReportFilename(RagreModelEventDTO event) {
        return event.getModelUuid() + "-" + event.getVersion() + "-risk-report.json";
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

    private OrchestratorFileReferenceDTO resolveRequirementsFileReference(RagreModelEventDTO event) {
        if (event.getFiles() == null || event.getFiles().isEmpty()) {
            throw new IllegalStateException("Model event does not contain any file references");
        }

        OrchestratorFileReferenceDTO requirementsFileReference = event.getFiles().get(0);
        if (requirementsFileReference.getDownloadUrl() == null || requirementsFileReference.getDownloadUrl().isBlank()) {
            throw new IllegalStateException("Model event file reference does not contain a download URL");
        }
        if (requirementsFileReference.getFilename() == null || requirementsFileReference.getFilename().isBlank()) {
            throw new IllegalStateException("Model event file reference does not contain a filename");
        }
        return requirementsFileReference;
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
