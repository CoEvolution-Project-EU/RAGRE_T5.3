package eu.coevolution.adapter.enums;

import lombok.Getter;

@Getter
public enum CategoryType {
    SBOM("SBOM");

    private final String description;

    CategoryType(String description) {
        this.description = description;
    }
}
