package eu.coevolution.adapter.service.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.coevolution.adapter.dto.EthicalGuidelineCatalogueEntryDTO;
import eu.coevolution.adapter.dto.EthicalGuidelineReferenceDTO;
import eu.coevolution.adapter.dto.EthicalGuidelinesSubmissionEventDTO;
import eu.coevolution.adapter.dto.QuestionnaireResponseDTO;
import eu.coevolution.adapter.service.catalogue.EthicalGuidelinesCatalogueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class EthicalGuidelinesReportBuilder {

    private static final Logger logger = LoggerFactory.getLogger(EthicalGuidelinesReportBuilder.class);

    private static final String BUCKET = "ethical-guidelines-reports";
    private static final String BOM_FORMAT = "CycloneDX";
    private static final String SPEC_VERSION = "1.7";
    private static final int BOM_VERSION = 1;

    private final ObjectMapper objectMapper;
    private final EthicalGuidelinesCatalogueService catalogueService;

    public EthicalGuidelinesReportBuilder(ObjectMapper objectMapper,
                                          EthicalGuidelinesCatalogueService catalogueService) {
        this.objectMapper = objectMapper;
        this.catalogueService = catalogueService;
    }

    public GeneratedBomReport build(EthicalGuidelinesSubmissionEventDTO request) throws JsonProcessingException {
        List<AnsweredQuestion> answeredQuestions = resolveAnsweredQuestions(request);
        List<AnsweredQuestion> triggeringAnswers = answeredQuestions.stream()
                .filter(AnsweredQuestion::triggersGuideline)
                .toList();
        String modelRef = "model-" + request.getModelUuid();

        ObjectNode root = objectMapper.createObjectNode();
        root.put("bomFormat", BOM_FORMAT);
        root.put("specVersion", SPEC_VERSION);
        root.put("serialNumber", "urn:uuid:" + UUID.randomUUID());
        root.put("version", BOM_VERSION);
        root.set("metadata", buildMetadata(request, modelRef));
        if (!triggeringAnswers.isEmpty()) {
            root.set("definitions", buildDefinitions(triggeringAnswers));
            root.set("declarations", buildDeclarations(triggeringAnswers, modelRef));
        }
        root.set("properties", buildTopLevelProperties(request, answeredQuestions.size(), triggeringAnswers.size()));

        return new GeneratedBomReport(
                buildFilename(request),
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root),
                BUCKET
        );
    }

    private List<AnsweredQuestion> resolveAnsweredQuestions(EthicalGuidelinesSubmissionEventDTO request) {
        List<AnsweredQuestion> answered = new ArrayList<>();
        Set<String> answeredQuestionIds = new HashSet<>();
        if (request.getResponses() == null) {
            return answered;
        }

        for (QuestionnaireResponseDTO response : request.getResponses()) {
            if (response == null || response.getAnswer() == null) {
                logger.warn("Dropping ethical guidelines response without an answer for model {}", request.getModelUuid());
                continue;
            }

            EthicalGuidelineCatalogueEntryDTO catalogueEntry = catalogueService.findEntry(response.getQuestionId()).orElse(null);
            if (catalogueEntry == null) {
                logger.warn("Dropping ethical guidelines response for question {} of model {}: question not found in catalogue {} version {}",
                        response.getQuestionId(), request.getModelUuid(),
                        catalogueService.getCatalogueId(), catalogueService.getCatalogueVersion());
                continue;
            }

            if (!catalogueEntry.allows(response.getAnswer())) {
                logger.warn("Dropping ethical guidelines response for question {} of model {}: answer {} is not allowed, expected one of {}",
                        catalogueEntry.getQuestionId(), request.getModelUuid(), response.getAnswer(), catalogueEntry.getAnswers());
                continue;
            }

            if (!answeredQuestionIds.add(catalogueEntry.getQuestionId())) {
                logger.warn("Dropping duplicate ethical guidelines response for question {} of model {}",
                        catalogueEntry.getQuestionId(), request.getModelUuid());
                continue;
            }

            String refSegment = normalizeRefSegment(catalogueEntry.getQuestionId());
            answered.add(new AnsweredQuestion(response, catalogueEntry, "requirement-" + refSegment, "claim-" + refSegment));
        }

        return answered;
    }

    private ObjectNode buildMetadata(EthicalGuidelinesSubmissionEventDTO request, String modelRef) {
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS)));
        metadata.set("tools", buildTools());

        ObjectNode component = objectMapper.createObjectNode();
        component.put("type", "machine-learning-model");
        component.put("bom-ref", modelRef);
        component.put("name", request.getModelUuid());
        component.put("version", request.getVersion());
        metadata.set("component", component);
        return metadata;
    }

    private ObjectNode buildTools() {
        ObjectNode toolComponent = objectMapper.createObjectNode();
        toolComponent.put("type", ReportBuilderUtils.REPORT_TOOL_TYPE);
        toolComponent.put("bom-ref", UUID.randomUUID().toString());
        toolComponent.put("name", ReportBuilderUtils.REPORT_TOOL_NAME);
        toolComponent.put("version", ReportBuilderUtils.REPORT_TOOL_VERSION);

        ArrayNode properties = objectMapper.createArrayNode();
        addProperty(properties, "tool:vendor", ReportBuilderUtils.REPORT_TOOL_VENDOR);
        toolComponent.set("properties", properties);

        ArrayNode components = objectMapper.createArrayNode();
        components.add(toolComponent);
        ObjectNode tools = objectMapper.createObjectNode();
        tools.set("components", components);
        return tools;
    }

    private ObjectNode buildDefinitions(List<AnsweredQuestion> answeredQuestions) {
        ObjectNode standard = objectMapper.createObjectNode();
        standard.put("bom-ref", "standard-" + normalizeRefSegment(catalogueService.getCatalogueId()));
        standard.put("name", catalogueService.getCatalogueId());
        standard.put("version", catalogueService.getCatalogueVersion());
        standard.put("description", "RAGRE ethical guidelines catalogue");

        ArrayNode requirements = objectMapper.createArrayNode();
        for (AnsweredQuestion answeredQuestion : answeredQuestions) {
            requirements.add(buildRequirement(answeredQuestion));
        }
        standard.set("requirements", requirements);

        ArrayNode standards = objectMapper.createArrayNode();
        standards.add(standard);
        ObjectNode definitions = objectMapper.createObjectNode();
        definitions.set("standards", standards);
        return definitions;
    }

    private ObjectNode buildRequirement(AnsweredQuestion answeredQuestion) {
        EthicalGuidelineCatalogueEntryDTO catalogueEntry = answeredQuestion.catalogueEntry();

        ObjectNode requirement = objectMapper.createObjectNode();
        requirement.put("bom-ref", answeredQuestion.requirementRef());
        requirement.put("identifier", catalogueEntry.getQuestionId());
        requirement.put("text", answeredQuestion.guideline());

        ArrayNode externalReferences = buildExternalReferences(catalogueEntry.getReferences());
        if (!externalReferences.isEmpty()) {
            requirement.set("externalReferences", externalReferences);
        }
        return requirement;
    }

    private ArrayNode buildExternalReferences(List<EthicalGuidelineReferenceDTO> references) {
        ArrayNode externalReferences = objectMapper.createArrayNode();
        if (references == null) {
            return externalReferences;
        }

        for (EthicalGuidelineReferenceDTO reference : references) {
            if (reference == null || reference.getUrl() == null || reference.getUrl().isBlank()) {
                continue;
            }
            ObjectNode externalReference = objectMapper.createObjectNode();
            externalReference.put("type", "documentation");
            externalReference.put("url", reference.getUrl());
            if (reference.getLabel() != null) {
                externalReference.put("comment", reference.getLabel());
            }
            externalReferences.add(externalReference);
        }
        return externalReferences;
    }

    private ObjectNode buildDeclarations(List<AnsweredQuestion> triggeringAnswers, String modelRef) {
        ObjectNode declarations = objectMapper.createObjectNode();
        declarations.set("claims", buildClaims(triggeringAnswers, modelRef));
        declarations.set("attestations", buildAttestations(triggeringAnswers));
        return declarations;
    }

    private ArrayNode buildClaims(List<AnsweredQuestion> answeredQuestions, String modelRef) {
        ArrayNode claims = objectMapper.createArrayNode();
        for (AnsweredQuestion answeredQuestion : answeredQuestions) {
            ObjectNode claim = objectMapper.createObjectNode();
            claim.put("bom-ref", answeredQuestion.claimRef());
            claim.put("target", modelRef);
            claim.put("predicate", answeredQuestion.catalogueEntry().getQuestion());
            claim.put("reasoning", answeredQuestion.response().getAnswer().name());
            claims.add(claim);
        }
        return claims;
    }

    private ArrayNode buildAttestations(List<AnsweredQuestion> triggeringAnswers) {
        ArrayNode map = objectMapper.createArrayNode();
        for (AnsweredQuestion answeredQuestion : triggeringAnswers) {
            ObjectNode mapEntry = objectMapper.createObjectNode();
            mapEntry.put("requirement", answeredQuestion.requirementRef());
            ArrayNode claimRefs = objectMapper.createArrayNode();
            claimRefs.add(answeredQuestion.claimRef());
            mapEntry.set("claims", claimRefs);
            map.add(mapEntry);
        }

        ArrayNode attestations = objectMapper.createArrayNode();
        ObjectNode attestation = objectMapper.createObjectNode();
        attestation.put("summary", "Links each triggered guideline to the questionnaire answer that triggered it");
        attestation.set("map", map);
        attestations.add(attestation);
        return attestations;
    }

    private ArrayNode buildTopLevelProperties(EthicalGuidelinesSubmissionEventDTO request, int answeredQuestions, int triggeredGuidelines) {
        ArrayNode properties = objectMapper.createArrayNode();
        addProperty(properties, "ethics:model_uuid", request.getModelUuid());
        addProperty(properties, "ethics:model_version", request.getVersion());
        addProperty(properties, "ethics:catalogue_id", catalogueService.getCatalogueId());
        addProperty(properties, "ethics:catalogue_version", catalogueService.getCatalogueVersion());
        addProperty(properties, "ethics:answered_questions", Integer.toString(answeredQuestions));
        addProperty(properties, "ethics:triggered_guidelines", Integer.toString(triggeredGuidelines));
        return properties;
    }

    private void addProperty(ArrayNode properties, String name, String value) {
        if (value == null) {
            return;
        }
        ObjectNode property = objectMapper.createObjectNode();
        property.put("name", name);
        property.put("value", value);
        properties.add(property);
    }

    private String buildFilename(EthicalGuidelinesSubmissionEventDTO request) {
        return request.getModelUuid() + "-" + request.getVersion() + "-ethical-guidelines-report.json";
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

    private record AnsweredQuestion(QuestionnaireResponseDTO response,
                                    EthicalGuidelineCatalogueEntryDTO catalogueEntry,
                                    String requirementRef,
                                    String claimRef) {

        String guideline() {
            return catalogueEntry.guidelineFor(response.getAnswer());
        }

        boolean triggersGuideline() {
            return guideline() != null;
        }
    }
}
