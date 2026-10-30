package eu.coevolution.adapter.service.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.coevolution.adapter.dto.EthicalGuidelinesSubmissionEventDTO;
import eu.coevolution.adapter.dto.QuestionnaireResponseDTO;
import eu.coevolution.adapter.enums.QuestionnaireAnswer;
import eu.coevolution.adapter.service.catalogue.EthicalGuidelinesCatalogueService;
import org.cyclonedx.Version;
import org.cyclonedx.parsers.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EthicalGuidelinesReportBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final EthicalGuidelinesReportBuilder builder = new EthicalGuidelinesReportBuilder(
            objectMapper,
            new EthicalGuidelinesCatalogueService(objectMapper, "ethical-guidelines-test-catalogue.json")
    );

    @Test
    void yesAnswerTriggeringGuidelineIsRecordedAndMappedInAttestation() throws Exception {
        JsonNode report = build(response("1", QuestionnaireAnswer.YES));

        assertEquals("CycloneDX", report.path("bomFormat").asText());
        assertEquals("1.7", report.path("specVersion").asText());
        assertEquals("machine-learning-model", report.at("/metadata/component/type").asText());
        assertEquals("model-model-123", report.at("/metadata/component/bom-ref").asText());

        JsonNode standard = report.at("/definitions/standards/0");
        assertEquals("ragre-ethical-guidelines", standard.path("name").asText());
        assertEquals(1, standard.path("requirements").size());
        assertEquals("requirement-1", standard.at("/requirements/0/bom-ref").asText());
        assertEquals("1", standard.at("/requirements/0/identifier").asText());
        assertTrue(standard.at("/requirements/0/title").isMissingNode());
        assertEquals("If yes, you must identify and document your specific legal basis under Article 6(1) of the GDPR.",
                standard.at("/requirements/0/text").asText());
        assertEquals("https://gdpr-info.eu/art-6-gdpr/", standard.at("/requirements/0/externalReferences/0/url").asText());

        assertEquals("Do you process any Personal Data?", report.at("/declarations/claims/0/predicate").asText());
        assertEquals("YES", report.at("/declarations/claims/0/reasoning").asText());
        assertEquals("model-model-123", report.at("/declarations/claims/0/target").asText());
        assertEquals("Links each triggered guideline to the questionnaire answer that triggered it",
                report.at("/declarations/attestations/0/summary").asText());
        assertEquals("requirement-1", report.at("/declarations/attestations/0/map/0/requirement").asText());
        assertEquals("claim-1",report.at("/declarations/attestations/0/map/0/claims/0").asText());
        assertTrue(report.at("/declarations/attestations/0/map/0/conformance").isMissingNode());
        assertEquals("1", property(report, "ethics:triggered_guidelines"));
        assertTrue(report.path("signature").isMissingNode());
    }

    @Test
    void noAnswerTriggersItsGuideline() throws Exception {
        JsonNode report = build(response("2", QuestionnaireAnswer.NO));

        assertEquals("Ensure that consent meets the GDPR requirements and can be easily withdrawn.",
                report.at("/definitions/standards/0/requirements/0/text").asText());
        assertEquals("NO", report.at("/declarations/claims/0/reasoning").asText());
        assertEquals("requirement-2", report.at("/declarations/attestations/0/map/0/requirement").asText());
        assertEquals("1", property(report, "ethics:triggered_guidelines"));
    }

    @Test
    void answersNotTriggeringGuidelineAreOnlyCounted() throws Exception {
        QuestionnaireResponseDTO notApplicable = objectMapper.readValue(
                "{\"questionId\":\"2\",\"answer\":\"N_A\"}", QuestionnaireResponseDTO.class);
        assertEquals(QuestionnaireAnswer.N_A, notApplicable.getAnswer());

        JsonNode report = build(response("1", QuestionnaireAnswer.NO), notApplicable);

        assertTrue(report.path("definitions").isMissingNode());
        assertTrue(report.path("declarations").isMissingNode());
        assertEquals("2", property(report, "ethics:answered_questions"));
        assertEquals("0", property(report, "ethics:triggered_guidelines"));
    }

    @Test
    void onlyTriggeringAnswersAppearAmongMixedAnswers() throws Exception {
        JsonNode report = build(
                response("1", QuestionnaireAnswer.NO),
                response("2", QuestionnaireAnswer.NO)
        );

        assertEquals(1, report.at("/definitions/standards/0/requirements").size());
        assertEquals(1, report.at("/declarations/claims").size());
        assertEquals("claim-2", report.at("/declarations/claims/0/bom-ref").asText());
        assertEquals("2", property(report, "ethics:answered_questions"));
        assertEquals("1", property(report, "ethics:triggered_guidelines"));
    }

    @Test
    void answerNotAllowedByQuestionIsDropped() throws Exception {
        JsonNode report = build(
                response("1", QuestionnaireAnswer.N_A),
                response("2", QuestionnaireAnswer.NO)
        );

        assertEquals(1, report.at("/declarations/claims").size());
        assertEquals("claim-2", report.at("/declarations/claims/0/bom-ref").asText());
        assertEquals("1", property(report, "ethics:answered_questions"));
    }

    @Test
    void unknownAndDuplicateQuestionsAreDropped() throws Exception {
        JsonNode report = build(
                response("1", QuestionnaireAnswer.YES),
                response("999", QuestionnaireAnswer.YES),
                response("1", QuestionnaireAnswer.NO)
        );

        assertEquals(1, report.at("/definitions/standards/0/requirements").size());
        assertEquals(1, report.at("/declarations/claims").size());
        assertEquals("1", property(report, "ethics:answered_questions"));
    }

    @Test
    void reportIsValidAgainstCycloneDxSchema() throws Exception {
        assertValidCycloneDx(build(response("1", QuestionnaireAnswer.YES), response("2", QuestionnaireAnswer.NO)));
        assertValidCycloneDx(build(response("1", QuestionnaireAnswer.NO)));
    }

    private void assertValidCycloneDx(JsonNode built) throws Exception {
        ObjectNode report = (ObjectNode) built;
        // cyclonedx-core-java ships schemas up to 1.6; the 1.7 additions are not used by this report
        report.put("specVersion", "1.6");

        List<?> errors = new JsonParser().validate(objectMapper.writeValueAsBytes(report), Version.VERSION_16);
        assertTrue(errors.isEmpty(), () -> "Schema validation errors: " + errors);
    }

    private JsonNode build(QuestionnaireResponseDTO... responses) throws Exception {
        EthicalGuidelinesSubmissionEventDTO request = new EthicalGuidelinesSubmissionEventDTO();
        request.setModelUuid("model-123");
        request.setVersion("1.0");
        request.setResponses(List.of(responses));
        return objectMapper.readTree(builder.build(request).content());
    }

    private QuestionnaireResponseDTO response(String questionId, QuestionnaireAnswer answer) {
        QuestionnaireResponseDTO response = new QuestionnaireResponseDTO();
        response.setQuestionId(questionId);
        response.setAnswer(answer);
        return response;
    }

    private String property(JsonNode report, String name) {
        for (JsonNode property : report.path("properties")) {
            if (name.equals(property.path("name").asText())) {
                return property.path("value").asText();
            }
        }
        return null;
    }
}
