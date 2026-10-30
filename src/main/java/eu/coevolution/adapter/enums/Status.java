package eu.coevolution.adapter.enums;

import lombok.Getter;

@Getter
public enum Status {
    Deprecated("Deprecated"),
    Draft("Draft"),
    Incomplete("Incomplete"),
    Obsolete("Obsolete"),
    Stable("Stable"),
    Usable("Usable");

    private final String description;

    Status(String description) {
        this.description = description;
    }
}
