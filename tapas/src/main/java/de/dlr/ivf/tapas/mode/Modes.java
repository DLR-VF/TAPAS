package de.dlr.ivf.tapas.mode;

import de.dlr.ivf.tapas.legacy.TPS_UtilityFunction;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.model.mode.TPS_Mode.ModeType;

import java.util.Collection;
import java.util.Map;

public class Modes {

    private final Map<ModeType, TPS_Mode> modeMap;
    private final Map<ModeType, TPS_UtilityFunction> utilityFunctions;

    public Modes(Map<ModeType, TPS_Mode> modeMap, Map<ModeType, TPS_UtilityFunction> utilityFunctions){
        this.modeMap = modeMap;
        this.utilityFunctions = utilityFunctions;
    }


    public TPS_Mode getMode(ModeType modeType){
        return modeMap.get(modeType);
    }

    public TPS_UtilityFunction getUtilityFunction(ModeType modeType){
        return this.utilityFunctions.get(modeType);
    }

    public Collection<TPS_Mode> getModes(){
        return modeMap.values();
    }
}
