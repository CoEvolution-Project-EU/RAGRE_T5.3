package eu.coevolution.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import eu.coevolution.adapter.enums.AbstractionCwe;
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
public class WeaknessDTO {

    private String id;

    private String weaknessId;

    private String description;

    @Enumerated(EnumType.STRING)
    private Library library;

    @Enumerated(EnumType.STRING)
    private Likelihood likelihoodOfExploit;

    private Set<BasicThreatDTO> threats = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private AbstractionCwe abstraction;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Set<BasicWeaknessDTO> childOf = new HashSet<>();

    private Set<BasicWeaknessDTO> parentOf = new HashSet<>();

    private Set<BasicWeaknessDTO> startsWith = new HashSet<>();

    private Set<BasicWeaknessDTO> canFollow = new HashSet<>();

    private Set<BasicWeaknessDTO> canPrecede = new HashSet<>();

    private Set<BasicWeaknessDTO> requiredBy = new HashSet<>();

    private Set<BasicWeaknessDTO> requires = new HashSet<>();

    private Set<BasicWeaknessDTO> canAlsoBe = new HashSet<>();

    private Set<BasicWeaknessDTO> peerOf = new HashSet<>();

}
