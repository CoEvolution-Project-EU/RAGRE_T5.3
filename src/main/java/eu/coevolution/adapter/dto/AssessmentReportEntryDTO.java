package eu.coevolution.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import eu.coevolution.adapter.enums.AiRiskCategory;
import eu.coevolution.adapter.enums.AiRiskConfidence;
import eu.coevolution.adapter.enums.AiRiskOccurrence;
import eu.coevolution.adapter.enums.AiRiskSeverity;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssessmentReportEntryDTO {

    @JsonProperty("attack_id")
    private String attackId;

    @JsonProperty("defence_id")
    private List<String> defenceIds;

    private AiRiskCategory category;
    private AiRiskConfidence confidence;
    private AiRiskOccurrence occurrence;
    private AiRiskSeverity severity;

    private String generalInfo;

    @JsonProperty("general_info")
    public void setGeneralInfo(JsonNode generalInfo) {
        if (generalInfo == null || generalInfo.isNull()) {
            this.generalInfo = null;
        } else if (generalInfo.isTextual()) {
            this.generalInfo = generalInfo.asText();
        } else {
            this.generalInfo = generalInfo.toString();
        }
    }
}
