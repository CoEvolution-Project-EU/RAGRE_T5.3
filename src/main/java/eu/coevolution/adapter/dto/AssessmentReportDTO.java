package eu.coevolution.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssessmentReportDTO {

    @JsonProperty("report_id")
    private String reportId;

    @JsonProperty("report_type")
    private String reportType;

    @JsonProperty("timestamp_utc")
    private Instant timestampUtc;

    @JsonProperty("model_uuid")
    private String modelUuid;

    @JsonProperty("model_version")
    private String modelVersion;

    private String dataset;

    @JsonProperty("tool_id")
    private String toolId;

    @JsonProperty("has_defence")
    private boolean hasDefence;

    private List<AssessmentReportEntryDTO> report;
}
