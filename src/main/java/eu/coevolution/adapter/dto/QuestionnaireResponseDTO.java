package eu.coevolution.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import eu.coevolution.adapter.enums.QuestionnaireAnswer;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionnaireResponseDTO {

    private String questionId;

    private QuestionnaireAnswer answer;
}
