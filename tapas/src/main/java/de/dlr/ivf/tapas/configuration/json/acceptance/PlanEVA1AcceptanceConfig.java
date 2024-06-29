package de.dlr.ivf.tapas.configuration.json.acceptance;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PlanEVA1AcceptanceConfig(
        @JsonProperty double eBottomFinance,
        @JsonProperty double fTopFinance,
        @JsonProperty double turningPointFinance,
        @JsonProperty double eBottomTime,
        @JsonProperty double fTopTime,
        @JsonProperty boolean checkBudgetConstraints,
        @JsonProperty double eBottomOverallTime,
        @JsonProperty double fTopOverallTime,
        @JsonProperty double turningPointMaxTime
) {}
