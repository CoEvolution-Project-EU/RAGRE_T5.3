package eu.coevolution.adapter.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import eu.coevolution.adapter.enums.Library;
import eu.coevolution.adapter.enums.Type;
import eu.coevolution.adapter.enums.VulnerableSoftwareGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class VulnerableSoftwareDTO {

    private String id;

    @Schema(title = "CPE or PURL")
    private VulnerableSoftwareGroup group;

    @Schema(title = "The id of the Vulnerable Software")
    private String vulnerableSoftwareId;

    @Schema(title = "internal id for matching purposes")
    private String internalId;

    @Schema(title = "The cpe name id")
    private String cpeNameId;

    @Schema(title = "The cpe title")
    private String cpeTitle;

    @Schema(title = "Type of the cpe")
    @Enumerated(EnumType.STRING)
    private Type cpeType;

    @Schema(title = "ecosystem of vulnerable software")
    private String ecosystem;

    @Schema(title = "The purl name")
    private String purlName;

    @Schema(title = "Type of the purl")
    private String purlType;

    @Schema(title = "The purl namespace")
    private String purlNamespace;

    @Schema(title = "Greater than or equal to a specific version of a github vulnerability related to purl")
    private String versionStartIncluding;

    @Schema(title = "Greater than a specific version of github a vulnerability related to purl")
    private String versionEndIncluding;

    @Schema(title = "Less than or equal to a specific version of a github vulnerability related to purl")
    private String versionStartExcluding;

    @Schema(title = "Less than a specific version of a github vulnerability related to purl")
    private String versionEndExcluding;

    @Schema(title = "Vendor of the cpe")
    private String vendor;

    @Schema(title = "Product of cpe's Vendor")
    private String product;

    @Schema(title = "Version of cpe's Product")
    private String version;

    @Schema(title = "Update of the cpe")
    private String update;

    @Schema(title = "Edition of the cpe")
    private String edition;

    @Schema(title = "Language of the cpe")
    private String language;

    @Schema(title = "date of creation of the vulnerable software")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    @Schema(title = "date of update of the vulnerable software")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;

    @Schema(title = "Library of the cpe")
    @Enumerated(EnumType.STRING)
    private Library library;

    @Schema(title = "Whether or not cpe is deprecated")
    private Boolean deprecated;

    @Schema(title = "Deprecated By")
    private VulnerableSoftwareDTO deprecatedBy;

    @Schema(title = "Whether or not Vulnerable Software is vulnerable")
    private Boolean isVulnerable;

    @Schema(title = "Related Vulnerabilities to Cpe")
    private Set<BasicVulnearbilityDTO> vulnerabilities = new HashSet<>();
}
