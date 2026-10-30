package eu.coevolution.adapter.service.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.coevolution.adapter.dto.AssessmentReportDTO;
import eu.coevolution.adapter.dto.AssessmentReportEntryDTO;
import eu.coevolution.adapter.service.catalogue.SecurityCatalogueService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AssessmentBomReportBuilder {

    private static final String BUCKET = "assessment-reports";
    private static final String ATTACK_ONLY_REPORT_SUFFIX = "attack-only-assessment-report";
    private static final String DEFENDED_REPORT_SUFFIX = "defended-assessment-report";
    private static final String BOM_FORMAT = "CycloneDX";
    private static final String SPEC_VERSION = "1.7";
    private static final int BOM_VERSION = 1;

    private final ObjectMapper objectMapper;
    private final SecurityCatalogueService securityCatalogueService;

    public AssessmentBomReportBuilder(ObjectMapper objectMapper,
                                      SecurityCatalogueService securityCatalogueService) {
        this.objectMapper = objectMapper;
        this.securityCatalogueService = securityCatalogueService;
    }

    public GeneratedBomReport buildAttackOnlyReport(List<AssessmentReportDTO> reports) throws JsonProcessingException {
        return build(reports, ATTACK_ONLY_REPORT_SUFFIX);
    }

    public GeneratedBomReport buildDefendedReport(List<AssessmentReportDTO> reports) throws JsonProcessingException {
        return build(reports, DEFENDED_REPORT_SUFFIX);
    }

    private GeneratedBomReport build(List<AssessmentReportDTO> reports, String reportSuffix) throws JsonProcessingException {
        if (!ReportBuilderUtils.hasReports(reports)) {
            return null;
        }

        AssessmentReportDTO firstReport = reports.get(0);
        Map<String, String> toolRefsByToolId = buildToolRefsByToolId(reports);
        List<NormalizedAssessmentEntry> normalizedEntries = buildNormalizedEntries(reports);
        ObjectNode root = objectMapper.createObjectNode();
        root.put("bomFormat", BOM_FORMAT);
        root.put("specVersion", SPEC_VERSION);
        root.put("serialNumber", "urn:uuid:" + UUID.randomUUID());
        root.put("version", BOM_VERSION);
        root.set("metadata", buildMetadata(reports, toolRefsByToolId));
        root.set("components", buildComponents(normalizedEntries, toolRefsByToolId));
        root.set("properties", buildTopLevelProperties(reports));
        root.set("declarations", buildDeclarations(reports, normalizedEntries));

        return new GeneratedBomReport(
                buildFilename(firstReport, reportSuffix),
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root),
                BUCKET
        );
    }

    private ObjectNode buildMetadata(List<AssessmentReportDTO> reports, Map<String, String> toolRefsByToolId) {
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("timestamp", formatInstant(Instant.now()));

        ObjectNode tools = objectMapper.createObjectNode();
        ArrayNode toolComponents = objectMapper.createArrayNode();
        Map<String, ObjectNode> uniqueTools = new LinkedHashMap<>();
        for (AssessmentReportDTO report : reports) {
            if (report == null || report.getToolId() == null || uniqueTools.containsKey(report.getToolId())) {
                continue;
            }

            ObjectNode toolComponent = objectMapper.createObjectNode();
            toolComponent.put("type", "application");
            String toolRef = toolRefsByToolId.get(report.getToolId());
            toolComponent.put("bom-ref", toolRef);
            toolComponent.put("name", securityCatalogueService.findToolName(report.getToolId()).orElse(report.getToolId()));
            toolComponent.put("version", "unknown");

            ArrayNode properties = objectMapper.createArrayNode();
            addProperty(properties, "tool:type", "security-assessment");
            addProperty(properties, "tool:id", report.getToolId());
            toolComponent.set("properties", properties);
            uniqueTools.put(report.getToolId(), toolComponent);
        }

        uniqueTools.values().forEach(toolComponents::add);
        tools.set("components", toolComponents);
        metadata.set("tools", tools);
        return metadata;
    }

    private ArrayNode buildTopLevelProperties(List<AssessmentReportDTO> reports) {
        AssessmentReportDTO firstReport = reports.get(0);
        ArrayNode properties = objectMapper.createArrayNode();
        addProperty(properties, "assessment:model_uuid", firstReport.getModelUuid());
        addProperty(properties, "assessment:model_version", firstReport.getModelVersion());
        addProperty(properties, "assessment:report_count", Integer.toString(reports.size()));
        return properties;
    }

    private ObjectNode buildDeclarations(List<AssessmentReportDTO> reports,
                                         List<NormalizedAssessmentEntry> normalizedEntries) throws JsonProcessingException {
        ObjectNode declarations = objectMapper.createObjectNode();
        declarations.set("claims", buildClaims(normalizedEntries));
        declarations.set("evidence", buildEvidence(reports));
        return declarations;
    }

    private ArrayNode buildClaims(List<NormalizedAssessmentEntry> normalizedEntries) {
        ArrayNode claims = objectMapper.createArrayNode();
        for (NormalizedAssessmentEntry normalizedEntry : normalizedEntries) {
            ObjectNode claim = objectMapper.createObjectNode();
            claim.put("bom-ref", buildClaimRef());
            claim.put("target", normalizedEntry.componentRef());

            ArrayNode evidenceRefs = objectMapper.createArrayNode();
            evidenceRefs.add(buildEvidenceRef(normalizedEntry.report()));
            claim.set("evidence", evidenceRefs);
            claims.add(claim);
        }
        return claims;
    }

    private ArrayNode buildEvidence(List<AssessmentReportDTO> reports) throws JsonProcessingException {
        ArrayNode evidenceItems = objectMapper.createArrayNode();
        for (AssessmentReportDTO report : reports) {
            ObjectNode evidence = objectMapper.createObjectNode();
            evidence.put("bom-ref", buildEvidenceRef(report));
            evidence.put("description","Full assessment report - tool '" + report.getToolId() + "', model " + report.getModelUuid() + ", report " + report.getReportId()
            );

            ArrayNode dataArray = objectMapper.createArrayNode();
            ObjectNode data = objectMapper.createObjectNode();
            data.put("name", "report-" + report.getReportId());

            ObjectNode contents = objectMapper.createObjectNode();
            ObjectNode attachment = objectMapper.createObjectNode();
            attachment.put("encoding", "base64");
            attachment.put("content", Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(report)));
            contents.set("attachment", attachment);
            data.set("contents", contents);
            dataArray.add(data);

            evidence.set("data", dataArray);
            evidenceItems.add(evidence);
        }
        return evidenceItems;
    }

    private ArrayNode buildComponents(List<NormalizedAssessmentEntry> normalizedEntries,
                                      Map<String, String> toolRefsByToolId) {
        ArrayNode components = objectMapper.createArrayNode();
        for (NormalizedAssessmentEntry normalizedEntry : normalizedEntries) {
            AssessmentReportDTO report = normalizedEntry.report();
            AssessmentReportEntryDTO entry = normalizedEntry.entry();

            ObjectNode component = objectMapper.createObjectNode();
            component.put("type", "data");
            component.put("bom-ref", normalizedEntry.componentRef());
            component.put("name", resolveComponentName(report, entry));
            component.set("properties", buildComponentProperties(report, entry, toolRefsByToolId));
            components.add(component);
        }
        return components;
    }

    private ArrayNode buildComponentProperties(AssessmentReportDTO report,
                                              AssessmentReportEntryDTO entry,
                                              Map<String, String> toolRefsByToolId) {
        ArrayNode properties = objectMapper.createArrayNode();
        addProperty(properties, "ml:report_id", report.getReportId());
        addProperty(properties, "ml:report_type", report.getReportType());
        addProperty(properties, "ml:timestamp_utc", formatInstant(report.getTimestampUtc()));
        addProperty(properties, "ml:model_uuid", report.getModelUuid());
        addProperty(properties, "ml:model_version", report.getModelVersion());
        addProperty(properties, "ml:dataset", report.getDataset());
        addProperty(properties, "ml:tool_id", report.getToolId());
        addProperty(properties, "ml:tool_ref", report.getToolId() != null ? toolRefsByToolId.get(report.getToolId()) : null);
        addProperty(properties, "ml:has_defence", Boolean.toString(report.isHasDefence()));
        addProperty(properties, "ml:evidence_ref", buildEvidenceRef(report));
        addProperty(properties, "ml:attack_id", entry != null ? entry.getAttackId() : null);
        addProperty(properties, "ml:attack_name", entry != null ? securityCatalogueService.findAttackName(entry.getAttackId()).orElse(null) : null);
        addProperty(properties, "ml:category", entry != null && entry.getCategory() != null ? entry.getCategory().getValue() : null);
        addProperty(properties, "ml:confidence", entry != null && entry.getConfidence() != null ? entry.getConfidence().getValue() : null);
        addProperty(properties, "ml:occurrence", entry != null && entry.getOccurrence() != null ? entry.getOccurrence().getValue() : null);
        addProperty(properties, "ml:severity", entry != null && entry.getSeverity() != null ? entry.getSeverity().getValue() : null);
        addProperty(properties, "ml:analysis_state", "exploitable");
        addProperty(properties, "ml:analysis_detail", entry != null ? entry.getGeneralInfo() : null);
        return properties;
    }

    private void addProperty(ArrayNode properties, String name, String value) {
        if (value == null) {
            return;
        }
        properties.add(propertyNode(name, value));
    }

    private ObjectNode propertyNode(String name, String value) {
        ObjectNode property = objectMapper.createObjectNode();
        property.put("name", name);
        property.put("value", value);
        return property;
    }

    private String buildFilename(AssessmentReportDTO report, String reportSuffix) {
        return report.getModelUuid() + "-" + report.getModelVersion() + "-" + reportSuffix + ".json";
    }

    private String buildEvidenceRef(AssessmentReportDTO report) {
        return "evidence-report-" + normalizeRefSegment(report.getToolId()) + "-" + normalizeRefSegment(report.getReportId());
    }

    private String buildClaimRef() {
        return UUID.randomUUID().toString();
    }

    private Map<String, String> buildToolRefsByToolId(List<AssessmentReportDTO> reports) {
        Map<String, String> toolRefsByToolId = new LinkedHashMap<>();
        for (AssessmentReportDTO report : reports) {
            if (report == null || report.getToolId() == null || toolRefsByToolId.containsKey(report.getToolId())) {
                continue;
            }
            toolRefsByToolId.put(report.getToolId(), UUID.randomUUID().toString());
        }
        return toolRefsByToolId;
    }

    private List<NormalizedAssessmentEntry> buildNormalizedEntries(List<AssessmentReportDTO> reports) {
        if (!ReportBuilderUtils.hasReports(reports)) {
            return Collections.emptyList();
        }

        java.util.ArrayList<NormalizedAssessmentEntry> normalizedEntries = new java.util.ArrayList<>();
        for (AssessmentReportDTO report : reports) {
            if (report == null || report.getReport() == null) {
                continue;
            }
            for (AssessmentReportEntryDTO entry : report.getReport()) {
                normalizedEntries.add(new NormalizedAssessmentEntry(report, entry, UUID.randomUUID().toString()));
            }
        }
        return normalizedEntries;
    }

    private String resolveComponentName(AssessmentReportEntryDTO entry) {
        return "assessment-entry";
    }

    private String resolveComponentName(AssessmentReportDTO report, AssessmentReportEntryDTO entry) {
        StringBuilder name = new StringBuilder(resolveReportIdPrefix(report.getReportId()));
        name.append("_").append(entry != null && entry.getAttackId() != null ? entry.getAttackId() : "UNKNOWN_ATTACK");

        if (entry != null && entry.getDefenceIds() != null) {
            for (String defenceId : entry.getDefenceIds()) {
                if (defenceId != null && !defenceId.isBlank()) {
                    name.append("_").append(defenceId);
                }
            }
        }
        return name.toString();
    }

    private String resolveReportIdPrefix(String reportId) {
        if (reportId == null || reportId.isBlank()) {
            return "REP-UNKNOWN";
        }
        String[] parts = reportId.split("-");
        if (parts.length >= 3) {
            return parts[0] + "-" + parts[1] + "-" + parts[2];
        }
        return reportId;
    }

    private String formatInstant(Instant instant) {
        return instant != null ? DateTimeFormatter.ISO_INSTANT.format(instant) : null;
    }

    private String normalizeRefSegment(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
    }

    private record NormalizedAssessmentEntry(AssessmentReportDTO report,
                                             AssessmentReportEntryDTO entry,
                                             String componentRef) {
    }
}
