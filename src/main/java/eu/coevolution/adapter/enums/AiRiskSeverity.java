package eu.coevolution.adapter.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AiRiskSeverity {

    LOW("low", 1.0),
    MEDIUM("medium", 2.0),
    HIGH("high", 4.0),
    CRITICAL("critical", 7.0);

    private final String value;
    private final double weight;

    AiRiskSeverity(String value, double weight) {
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
    public static AiRiskSeverity fromValue(String value) {
        for (AiRiskSeverity severity : values()) {
            if (severity.value.equalsIgnoreCase(value)) {
                return severity;
            }
        }
        throw new IllegalArgumentException("Unknown AI risk severity: " + value);
    }
}
