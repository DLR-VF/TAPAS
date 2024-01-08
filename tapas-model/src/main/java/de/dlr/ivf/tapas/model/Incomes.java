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
    private final NavigableMap<Integer, TPS_Income> incomeMappings;

    public TPS_Income getIncomeClass(int income){
        return incomeMappings.ceilingEntry(income).getValue();
    }
}
