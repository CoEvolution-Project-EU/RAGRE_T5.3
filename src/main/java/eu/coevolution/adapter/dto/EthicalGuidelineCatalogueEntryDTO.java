package eu.coevolution.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import eu.coevolution.adapter.enums.QuestionnaireAnswer;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EthicalGuidelineCatalogueEntryDTO {

    private String questionId;

    private String question;

    private List<QuestionnaireAnswer> answers;

    private Map<QuestionnaireAnswer, String> guidelines;

    private List<EthicalGuidelineReferenceDTO> references;

    public boolean allows(QuestionnaireAnswer answer) {
        return answers != null && answers.contains(answer);
    }

    public String guidelineFor(QuestionnaireAnswer answer) {
        String guideline = guidelines != null ? guidelines.get(answer) : null;
        return guideline != null && !guideline.isBlank() ? guideline : null;
    }
}
