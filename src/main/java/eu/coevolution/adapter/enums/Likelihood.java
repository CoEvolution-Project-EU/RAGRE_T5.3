package eu.coevolution.adapter.enums;

import lombok.Getter;

@Getter
public enum Likelihood {
    High("High"),
    Medium("Medium"),
    Low("Low"),
    Unknown("Unknown");

    private final String description;

    Likelihood(String description) {
        this.description = description;
    }
}
