package de.dlr.ivf.tapas.model;

import de.dlr.ivf.tapas.model.constants.TPS_Income;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

@Builder
@Getter
public class Incomes {

    /**
     * INCOME_MAP maps id to income objects
     */
    @Singular
    private final Map<Integer, TPS_Income> incomeMappings;
    /**
     * MAX_VALUES maps max values of income to ids (=keys in INCOME_MAP)
     */
    @Singular
    private final NavigableMap<Integer, Integer> maxValuesMappings;
}
