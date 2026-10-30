package eu.coevolution.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EthicalGuidelinesCatalogueDTO {

    private String catalogueId;

    private String catalogueVersion;

    private List<EthicalGuidelineCatalogueEntryDTO> entries;
}
