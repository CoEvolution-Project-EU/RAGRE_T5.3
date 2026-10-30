package eu.coevolution.adapter.enums;

import lombok.Getter;

@Getter
public enum AbstractionCwe {
    Pillar("Pillar"),
    Class("Class"),
    Base("Base"),
    Variant("Variant"),
    Compound("Compound");

    private final String description;

    AbstractionCwe(String description) {
        this.description = description;
    }
}
