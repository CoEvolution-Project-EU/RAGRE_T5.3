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
public class OrchestratorFileSearchResultDTO {

    @JsonProperty("_id")
    private String id;

    private String pool;
    private String bucket;

    @JsonProperty("model_uuid")
    private String modelUuid;

    private String version;
    private String objectKey;
    private String filename;
    private long size;
    private String contentType;
    private String uploadedBy;
    private List<String> tags;
    private Instant createdAt;
}
