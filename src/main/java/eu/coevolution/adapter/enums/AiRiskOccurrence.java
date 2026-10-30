package eu.coevolution.adapter.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AiRiskOccurrence {

    RARE("rare", 0.8),
    INTERMITTENT("intermittent", 1.0),
    FREQUENT("frequent", 1.3),
    SYSTEMATIC("systematic", 1.6);

    private final String value;
    private final double weight;

    AiRiskOccurrence(String value, double weight) {
        this.value = value;
        this.weight = weight;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public double getWeight() {
        return weight;
    }

    @JsonCreator
    public static AiRiskOccurrence fromValue(String value) {
        for (AiRiskOccurrence occurrence : values()) {
            if (occurrence.value.equalsIgnoreCase(value)) {
                return occurrence;
            }
        }
        throw new IllegalArgumentException("Unknown AI risk occurrence: " + value);
    }
}
