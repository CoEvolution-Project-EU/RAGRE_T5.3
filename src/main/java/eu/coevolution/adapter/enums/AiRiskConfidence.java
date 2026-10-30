package eu.coevolution.adapter.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AiRiskConfidence {

    LOW("low", 0.7),
    MEDIUM("medium", 1.0),
    HIGH("high", 1.2);

    private final String value;
    private final double weight;

    AiRiskConfidence(String value, double weight) {
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
    public static AiRiskConfidence fromValue(String value) {
        for (AiRiskConfidence confidence : values()) {
            if (confidence.value.equalsIgnoreCase(value)) {
                return confidence;
            }
        }
        throw new IllegalArgumentException("Unknown AI risk confidence: " + value);
    }
}
