package eu.coevolution.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrchestratorFileReferenceDTO {

    @JsonProperty("_id")
    private String id;

    private String bucket;

    private String filename;

    @JsonProperty("download_url")
    private String downloadUrl;
}
