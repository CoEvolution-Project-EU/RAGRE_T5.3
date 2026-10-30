package eu.coevolution.adapter.enums;

import lombok.Getter;

@Getter
public enum Library {
    MITRE("Mitre"),
    NVD("NVD"),
    GITHUB("Github"),
    OSV("OSV"),
    CUSTOM("Custom");

    private final String description;

    Library(String description) {
        this.description = description;
    }
}
