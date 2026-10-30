package eu.coevolution.adapter.enums;

import lombok.Getter;

@Getter
public enum Criticality {
    Low("Low", 1),
    Medium("Medium", 2),
    High("High", 3);

    private final String description;
    private final int rank;

    Criticality(String description, int rank) {
        this.description = description;
        this.rank = rank;
    }
}
