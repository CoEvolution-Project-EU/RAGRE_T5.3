package eu.coevolution.adapter.service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.coevolution.adapter.dto.AssessmentReportDTO;
import eu.coevolution.adapter.dto.OrchestratorFileReferenceDTO;
import eu.coevolution.adapter.dto.OrchestratorFileSearchResultDTO;
import eu.coevolution.adapter.dto.RagreModelEventDTO;
import eu.coevolution.adapter.service.ragre.OrchestratorFileDownloadService;
import eu.coevolution.adapter.service.ragre.RagreRiskReportUpdateService;
import eu.coevolution.adapter.service.ragre.RagreService;
import eu.coevolution.adapter.service.report.AssessmentBomReportBuilder;
import eu.coevolution.adapter.service.report.DefenceBomReportBuilder;
import eu.coevolution.adapter.service.report.GeneratedBomReport;
import eu.coevolution.adapter.service.report.ReportUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SecurityAssessmentEngineConsumer {

    private final ObjectMapper objectMapper;
    private final RagreService ragreService;
    private final OrchestratorFileDownloadService orchestratorFileDownloadService;
    private final RagreRiskReportUpdateService ragreRiskReportUpdateService;
    private final AssessmentBomReportBuilder assessmentBomReportBuilder;
    private final DefenceBomReportBuilder defenceBomReportBuilder;
    private final ReportUploadService reportUploadService;

    @KafkaListener(topics = "${ragre.kafka.assessment-reports.topic}", groupId = "${ragre.kafka.all-topics.group-id}")
    public void consume(String message) throws IOException {
        RagreModelEventDTO event = objectMapper.readValue(message, RagreModelEventDTO.class);

        if (!"assessment_reports".equals(event.getEvent())) {
            return;
        }

        List<AssessmentReportDTO> assessmentReports = downloadAssessmentReports(event.getFiles());
        if (assessmentReports.isEmpty()) {
            System.out.println("No assessment reports found for model " + event.getModelUuid());
            return;
        }

        updateAiRiskScoreInRiskReport(event, assessmentReports);

        List<AssessmentReportDTO> assessmentAttackOnlyReports = assessmentReports.stream()
                .filter(report -> report != null && !report.isHasDefence())
                .collect(Collectors.toList());
        List<AssessmentReportDTO> defenceReports = assessmentReports.stream()
                .filter(report -> report != null && report.isHasDefence())
                .collect(Collectors.toList());

        if (!assessmentAttackOnlyReports.isEmpty()) {
            uploadGeneratedReport(
                    assessmentBomReportBuilder.buildAttackOnlyReport(assessmentAttackOnlyReports),
                    event
            );
        }

        if (!defenceReports.isEmpty()) {
            uploadGeneratedReport(
                    assessmentBomReportBuilder.buildDefendedReport(defenceReports),
                    event
            );
            uploadGeneratedReport(
                    defenceBomReportBuilder.build(defenceReports),
                    event
            );
        }
    }

    private void updateAiRiskScoreInRiskReport(RagreModelEventDTO event, List<AssessmentReportDTO> assessmentReports)
            throws IOException {
        double aiRiskScore = ragreService.calculateAggregatedAiRiskScore(assessmentReports);
        OrchestratorFileSearchResultDTO uploadedRiskReport = ragreRiskReportUpdateService.findLatestRiskReport(
                event.getModelUuid(),
                event.getVersion()
        );
        if (uploadedRiskReport == null) {
            System.out.println("No uploaded BOM risk report found for model " + event.getModelUuid() + " version " + event.getVersion());
            return;
        }

        byte[] bomContent = ragreRiskReportUpdateService.downloadReportById(uploadedRiskReport.getId());
        String updatedBom = ragreRiskReportUpdateService.updateAiRiskScore(bomContent, aiRiskScore);
        String uploadResponse = ragreRiskReportUpdateService.uploadUpdatedRiskReport(
                uploadedRiskReport,
                updatedBom,
                event.getModelUuid(),
                event.getVersion()
        );

        System.out.println("Calculated AI risk score for model " + event.getModelUuid() + ": " + aiRiskScore);
        System.out.println("Updated BOM AI risk score for file " + uploadedRiskReport.getId() + ". Upload response: " + uploadResponse);
    }

    private List<AssessmentReportDTO> downloadAssessmentReports(List<OrchestratorFileReferenceDTO> reportFileReferences)
            throws IOException {
        List<AssessmentReportDTO> assessmentReports = new ArrayList<>();
        if (reportFileReferences == null) {
            return assessmentReports;
        }

        for (OrchestratorFileReferenceDTO reportFileReference : reportFileReferences) {
            if (!isAssessmentReportReference(reportFileReference)) {
                continue;
            }

            byte[] content = orchestratorFileDownloadService.downloadFile(reportFileReference.getDownloadUrl());
            AssessmentReportDTO assessmentReport = objectMapper.readValue(content, AssessmentReportDTO.class);
            assessmentReports.add(assessmentReport);
        }

        return assessmentReports;
    }

    private boolean isAssessmentReportReference(OrchestratorFileReferenceDTO reportFileReference) {
        return reportFileReference != null
                && reportFileReference.getDownloadUrl() != null
                && !reportFileReference.getDownloadUrl().isBlank()
                && reportFileReference.getFilename() != null
                && reportFileReference.getFilename().toLowerCase().endsWith(".json");
    }

    private void uploadGeneratedReport(GeneratedBomReport generatedBomReport, RagreModelEventDTO event) {
        if (generatedBomReport == null) {
            return;
        }

        String uploadResponse = reportUploadService.uploadReport(
                generatedBomReport.filename(),
                generatedBomReport.content(),
                event.getPool(),
                generatedBomReport.bucket(),
                event.getModelUuid(),
                event.getVersion(),
                event.getTags()
        );

        System.out.println(
                "Uploaded " + generatedBomReport.filename()
                        + " to bucket " + generatedBomReport.bucket()
                        + ". Upload response: " + uploadResponse
        );
    }
}
