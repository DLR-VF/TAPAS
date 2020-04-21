package de.dlr.ivf.tapas.runtime.client.ParameterComparator;

import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Arrays;

public class SimParamComparatorObject {
    private boolean isequal, isnull, sim1_default, sim2_default, isdefault;
    private String paramkey, sim1_value, sim2_value, default_value;

    public SimParamComparatorObject(String key, String defaultvalue, String simvalue1, String simvalue2) {
        this.paramkey = key;
        this.default_value = defaultvalue;
        this.sim1_value = simvalue1;
        this.sim2_value = simvalue2;

        if (NumberUtils.isCreatable(simvalue1) && NumberUtils.isCreatable(simvalue2)) {
            this.isequal = NumberUtils.toDouble(simvalue1) == NumberUtils.toDouble(simvalue2);
            this.sim1_default = NumberUtils.toDouble(simvalue1) == NumberUtils.toDouble(defaultvalue);
            this.sim2_default = NumberUtils.toDouble(simvalue2) == NumberUtils.toDouble(defaultvalue);
        } else {
            this.isequal = simvalue1.equalsIgnoreCase(simvalue2);
            this.sim1_default = simvalue1.equalsIgnoreCase(defaultvalue);
            this.sim2_default = simvalue2.equalsIgnoreCase(defaultvalue);
        }
        this.isdefault = sim1_default || sim2_default;
        this.isnull = simvalue1 == null || simvalue2 == null || simvalue1.equalsIgnoreCase("null") | simvalue1.equals(
                "") || simvalue2.equalsIgnoreCase("null") | simvalue2.equals("");
    }

    //testing purposes
    public static void main(String... args) {
        Arrays.stream(ParamString.values()).forEach(
                e -> System.out.println(e.name() + " | " + (new TPS_ParameterClass()).paramStringClass.getPreset(e)));
        Arrays.stream(ParamValue.values()).forEach(
                e -> System.out.println(e.name() + " | " + (new TPS_ParameterClass()).paramValueClass.getPreset(e)));
    }

    public String getParamKey() {
        return paramkey;
    }

    public void setParamKey(String paramkey) {
        this.paramkey = paramkey;
    }

    public boolean isEqual() {
        return isequal;
    }

    public boolean isNotEqual() {
        return !isequal;
    }

    public boolean isNull() {
        return isnull;
    }

    public boolean isDefault() {
        return isdefault;
    }

    public String getValueOfFirstSim() {
        return this.sim1_value;
    }

    public boolean getIsDefaultFirstSim() {
        return this.sim1_default;
    }

    public boolean getIsDefaultSecondSim() {
        return this.sim2_default;
    }

    public String getValueOfSecondSim() {
        return this.sim2_value;
    }

    public String getDefaultValue() {
        return this.default_value;
    }
}
