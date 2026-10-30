package eu.coevolution.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RagreModelEventDTO {

    private String event;

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("model_uuid")
    private String modelUuid;

    private String version;
    private String uploadedBy;
    private Instant uploadedAt;
    private List<String> tags;
    private String pool;

    @JsonProperty("total_files")
    private String totalFiles;

    private List<OrchestratorFileReferenceDTO> files;
}
