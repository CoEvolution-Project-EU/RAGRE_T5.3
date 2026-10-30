package eu.coevolution.adapter.enums;

import lombok.Getter;

@Getter
public enum AssetStatus {
    Done("Done", true),
    InProgress("In Progress", true),
    ActionNeeded("Action Needed", true),
    Deleted("Deleted",false);

    private final String description;

    private boolean browsable;

    AssetStatus(String description, boolean browsable) {
        this.description = description;
        this.browsable = browsable;
    }
}
