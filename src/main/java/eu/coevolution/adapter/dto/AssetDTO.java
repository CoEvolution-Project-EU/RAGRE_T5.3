package eu.coevolution.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import eu.coevolution.adapter.enums.*;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssetDTO extends DateAuditNeo4jDTO{

    private String id;

    private String sharedId;

    private String name;

    private String ipAddress;

    private String macAddress;

    private Long port;

    private String description;

    @Enumerated(EnumType.STRING)
    private Type type;

    @Enumerated(EnumType.STRING)
    private Criticality criticality;

    private AssetStatus status;

    private boolean importExcludedVulnerabilities;

    private List<ComponentDTO> components;

    private OrganizationNodeDTO organizationNode;

    private Double riskScore;

    private Double riskScorePercentage;

    @Enumerated(EnumType.STRING)
    private BaseSeverity riskScoreSeverity;

    private CategoryType categoryType;

    private String repository;

    private String agentId;

    private List<String> ips;

    private List<Integer> hostPorts;

    private List<Integer> containerPorts;

    private Boolean exposedToInternet;

    private String footprint;

}
