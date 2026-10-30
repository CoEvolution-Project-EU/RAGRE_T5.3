package eu.coevolution.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.coevolution.adapter.enums.BaseSeverity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ComponentDTO extends DateAuditNeo4jDTO {

    private String id;

    @Schema(title = "Component's Name")
    private String name;

    @Schema(title = "Component's Group")
    private String group;

    @Schema(title = "Component's version")
    private String version;

    @Schema(title = "Component's cpe")
    private String cpe;

    @Schema(title = "Component's purl")
    private String purl;

    @Schema(title = "Defines if it is custom")
    @JsonProperty("custom")
    private boolean isCustom;

    @Schema(title = "Component's purl coordinates")
    private String purlCoordinates;

    @Schema(title = "Component's Description")
    private String description;

    @Schema(title = "Component's connection with Purls")
    private List<VulnerableSoftwareDTO> purls;

    @Schema(title = "Component's organization")
    private OrganizationNodeDTO organizationNode;

    @Schema(title = "Component's organization")
    private String organizationNodeId;

    @Schema(title = "Asset's Risk score")
    private Double riskScore;

    @Schema(title = "Asset's Risk score percentage")
    private Double riskScorePercentage;

    @Enumerated(EnumType.STRING)
    private BaseSeverity riskScoreSeverity;

    @Schema(title = "Number of vulnerabilities related to Component")
    private Integer vulnerabilitiesCount;
}
