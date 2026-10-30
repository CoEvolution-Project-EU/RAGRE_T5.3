package eu.coevolution.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EthicalGuidelinesSubmissionEventDTO {

    private String modelUuid;

    private String version;

    private String pool;

    private List<QuestionnaireResponseDTO> responses;

    private List<String> tags;
}
