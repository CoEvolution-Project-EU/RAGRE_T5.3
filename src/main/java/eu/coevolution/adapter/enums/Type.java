package eu.coevolution.adapter.enums;

import lombok.Getter;

@Getter
public enum Type {
    a("Application"),
    o("Operating System"),
    h("Hardware Platform");

    private final String description;

    Type(String description) {
        this.description = description;
    }
}
