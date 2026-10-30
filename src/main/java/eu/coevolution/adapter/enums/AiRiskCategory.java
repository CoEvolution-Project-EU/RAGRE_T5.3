package eu.coevolution.adapter.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AiRiskCategory {

    PROMPT_INJECTION("prompt_injection", 1.3),
    DATA_LEAKAGE("data_leakage", 1.4),
    TOOL_MISUSE("tool_misuse", 1.1),
    POLICY_BYPASS("policy_bypass", 1.2),
    UNSAFE_OUTPUT("unsafe_output", 1.0),
    MODEL_EXTRACTION("model_extraction", 1.3),
    PRIVACY_VIOLATION("privacy_violation", 1.5);

    private final String value;
    private final double weight;

    AiRiskCategory(String value, double weight) {
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
    public static AiRiskCategory fromValue(String value) {
        for (AiRiskCategory category : values()) {
            if (category.value.equalsIgnoreCase(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown AI risk category: " + value);
    }
}
