package eu.coevolution.adapter.service.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.coevolution.adapter.dto.AssessmentReportDTO;
import eu.coevolution.adapter.dto.AssessmentReportEntryDTO;
import eu.coevolution.adapter.service.catalogue.DefenceCatalogueEntry;
import eu.coevolution.adapter.service.catalogue.SecurityCatalogueService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DefenceBomReportBuilder {

    private static final String BUCKET = "defence-reports";
    private static final String BOM_FORMAT = "CycloneDX";
    private static final String SPEC_VERSION = "1.7";

    private final ObjectMapper objectMapper;
    private final SecurityCatalogueService securityCatalogueService;

    public DefenceBomReportBuilder(ObjectMapper objectMapper,
                                   SecurityCatalogueService securityCatalogueService) {
        this.objectMapper = objectMapper;
        this.securityCatalogueService = securityCatalogueService;
    }

    public GeneratedBomReport build(List<AssessmentReportDTO> reports) throws JsonProcessingException {
        if (!ReportBuilderUtils.hasReports(reports)) {
            return null;
        }

        AssessmentReportDTO firstReport = reports.get(0);
        ObjectNode root = objectMapper.createObjectNode();
        root.put("bomFormat", BOM_FORMAT);
        root.put("specVersion", SPEC_VERSION);
        root.put("serialNumber", "urn:uuid:" + UUID.randomUUID());
        root.set("metadata", buildMetadata());
        root.set("components", buildComponents(reports));

        return new GeneratedBomReport(
                buildFilename(firstReport),
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root),
                BUCKET
        );
    }

    private ArrayNode buildComponents(List<AssessmentReportDTO> reports) {
        ArrayNode components = objectMapper.createArrayNode();
        Map<String, ObjectNode> componentsByDefenceId = new LinkedHashMap<>();

        for (AssessmentReportDTO report : reports) {
            if (report == null || report.getReport() == null) {
                continue;
            }

            for (AssessmentReportEntryDTO entry : report.getReport()) {
                if (entry == null || entry.getDefenceIds() == null) {
                    continue;
                }

                for (String defenceId : entry.getDefenceIds()) {
                    DefenceCatalogueEntry catalogueEntry = securityCatalogueService.findDefence(defenceId).orElse(null);
                    if (catalogueEntry == null) {
                        continue;
                    }

                    componentsByDefenceId.computeIfAbsent(defenceId, ignored -> buildComponent(defenceId, catalogueEntry));
                }
            }
        }

        componentsByDefenceId.values().forEach(components::add);

        return components;
    }

    private ObjectNode buildMetadata() {
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("timestamp", Instant.now().toString());
        return metadata;
    }

    private ObjectNode buildComponent(String defenceId, DefenceCatalogueEntry defence) {
        ObjectNode component = objectMapper.createObjectNode();
        component.put("type", "library");
        component.put("bom-ref", UUID.randomUUID().toString());
        component.put("group", defenceId);
        component.put("name", defence.name());
        component.put("description", defence.description());
        return component;
    }

    private String buildFilename(AssessmentReportDTO report) {
        return report.getModelUuid() + "-" + report.getModelVersion() + "-defence-report.json";
    }
}
