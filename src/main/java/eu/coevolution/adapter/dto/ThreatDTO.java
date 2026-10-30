package eu.coevolution.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import eu.coevolution.adapter.enums.Library;
import eu.coevolution.adapter.enums.Likelihood;
import eu.coevolution.adapter.enums.Status;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThreatDTO {

    private String id;

    private String threatId;

    private String description;

    @Enumerated(EnumType.STRING)
    private Library library;

    private String severity;

    private String name;

    @Enumerated(EnumType.STRING)
    private Likelihood likelihoodOfAttack;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Set<BasicWeaknessDTO> cwes = new HashSet<>();

    public String getSeverity() {
        if ("Very High".equals(severity)) {
            return "Very_High";
        }
        return severity;
    }
}
