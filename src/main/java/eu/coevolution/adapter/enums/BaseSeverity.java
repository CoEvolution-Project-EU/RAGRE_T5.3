package eu.coevolution.adapter.enums;

import lombok.Getter;

@Getter
public enum BaseSeverity {
    NONE("None",0.0,0.0),
    LOW("Low",0.1,3.9),
    MEDIUM("Medium",4.0,6.9),
    HIGH("High",7.0,8.9),
    CRITICAL("Critical",9.0,10.0);

    private final String description;
    private final double startRange;
    private final double endRange;

    BaseSeverity(String description, double startRange, double endRange) {
        this.description = description;
        this.startRange = startRange;
        this.endRange = endRange;
    }

    public boolean containsIn(double baseScore, BaseSeverity baseSeverity) {

        return baseSeverity.startRange <= baseScore && baseSeverity.endRange >= baseScore;
    }

    public BaseSeverity getRate(double baseScore) {

        BaseSeverity baseSeverity = null;

        if (containsIn(baseScore, BaseSeverity.NONE))
            baseSeverity = BaseSeverity.NONE;
        else if (containsIn(baseScore, BaseSeverity.LOW))
            baseSeverity = BaseSeverity.LOW;
        else if (containsIn(baseScore, BaseSeverity.MEDIUM))
            baseSeverity = BaseSeverity.MEDIUM;
        else if (containsIn(baseScore, BaseSeverity.HIGH))
            baseSeverity = BaseSeverity.HIGH;
        else if (containsIn(baseScore, BaseSeverity.CRITICAL))
            baseSeverity = BaseSeverity.CRITICAL;

        return baseSeverity;
    }

}
