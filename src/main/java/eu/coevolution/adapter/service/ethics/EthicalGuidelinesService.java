package eu.coevolution.adapter.service.ethics;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.coevolution.adapter.dto.EthicalGuidelinesSubmissionEventDTO;
import eu.coevolution.adapter.service.report.EthicalGuidelinesReportBuilder;
import eu.coevolution.adapter.service.report.GeneratedBomReport;
import eu.coevolution.adapter.service.report.ReportUploadService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EthicalGuidelinesService {

    private static final Logger logger = LoggerFactory.getLogger(EthicalGuidelinesService.class);

    private static final List<String> DEFAULT_TAGS = List.of("ethics", "guidelines", "ragre");

    private final EthicalGuidelinesReportBuilder reportBuilder;
    private final ReportUploadService reportUploadService;

    public String generateAndUploadReport(EthicalGuidelinesSubmissionEventDTO request) throws JsonProcessingException {
        GeneratedBomReport report = reportBuilder.build(request);

        List<String> tags = request.getTags() != null && !request.getTags().isEmpty()
                ? request.getTags()
                : DEFAULT_TAGS;

        String uploadResponse = reportUploadService.uploadReport(
                report.filename(),
                report.content(),
                request.getPool(),
                report.bucket(),
                request.getModelUuid(),
                request.getVersion(),
                tags
        );

        logger.info("Uploaded {} to bucket {}. Upload response: {}", report.filename(), report.bucket(), uploadResponse);

        return report.content();
    }
}
