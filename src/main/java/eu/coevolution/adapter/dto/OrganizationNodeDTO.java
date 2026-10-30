package eu.coevolution.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrganizationNodeDTO extends DateAuditNeo4jDTO{

    private String id;

    private String oidc;

    private String name;

    private String email;

    public OrganizationNodeDTO(){};

    public OrganizationNodeDTO(String organizationId) {
        this.oidc = organizationId;
    }
}
