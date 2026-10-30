package eu.coevolution.adapter.service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.coevolution.adapter.dto.EthicalGuidelinesResponsesFileDTO;
import eu.coevolution.adapter.dto.EthicalGuidelinesSubmissionEventDTO;
import eu.coevolution.adapter.dto.OrchestratorFileReferenceDTO;
import eu.coevolution.adapter.dto.RagreModelEventDTO;
import eu.coevolution.adapter.service.ethics.EthicalGuidelinesService;
import eu.coevolution.adapter.service.ragre.OrchestratorFileDownloadService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class EthicalGuidelinesSubmissionConsumer {

    private static final String EVENT_NAME = "ethical_guidelines";

    private static final Logger logger = LoggerFactory.getLogger(EthicalGuidelinesSubmissionConsumer.class);

    private final ObjectMapper objectMapper;
    private final OrchestratorFileDownloadService orchestratorFileDownloadService;
    private final EthicalGuidelinesService ethicalGuidelinesService;

    @KafkaListener(topics = "${ragre.kafka.ethical-guidelines.topic}", groupId = "${ragre.kafka.all-topics.group-id}")
    public void consume(String message) throws IOException {
        RagreModelEventDTO envelope = objectMapper.readValue(message, RagreModelEventDTO.class);

        if (!EVENT_NAME.equals(envelope.getEvent())) {
            return;
        }

        OrchestratorFileReferenceDTO responsesFileReference = resolveResponsesFileReference(envelope);
        byte[] content = orchestratorFileDownloadService.downloadFile(responsesFileReference.getDownloadUrl());
        EthicalGuidelinesResponsesFileDTO responsesFile = objectMapper.readValue(content, EthicalGuidelinesResponsesFileDTO.class);

        if (responsesFile.getResponses() == null || responsesFile.getResponses().isEmpty()) {
            logger.warn("No questionnaire responses found for model {}", envelope.getModelUuid());
            return;
        }

        EthicalGuidelinesSubmissionEventDTO event = new EthicalGuidelinesSubmissionEventDTO();
        event.setModelUuid(envelope.getModelUuid());
        event.setVersion(envelope.getVersion());
        event.setPool(envelope.getPool());
        event.setTags(envelope.getTags());
        event.setResponses(responsesFile.getResponses());

        ethicalGuidelinesService.generateAndUploadReport(event);
    }

    private OrchestratorFileReferenceDTO resolveResponsesFileReference(RagreModelEventDTO envelope) {
        if (envelope.getFiles() == null || envelope.getFiles().isEmpty()) {
            throw new IllegalStateException("Ethical guidelines event does not contain any file references");
        }

        OrchestratorFileReferenceDTO responsesFileReference = envelope.getFiles().get(0);
        if (responsesFileReference.getDownloadUrl() == null || responsesFileReference.getDownloadUrl().isBlank()) {
            throw new IllegalStateException("Ethical guidelines file reference does not contain a download URL");
        }
        return responsesFileReference;
    }
}
