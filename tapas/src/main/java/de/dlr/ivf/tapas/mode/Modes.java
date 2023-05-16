package de.dlr.ivf.tapas.mode;

import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.model.mode.TPS_Mode.ModeType;
import lombok.Builder;
import lombok.Singular;

import java.util.Collection;
import java.util.Map;

@Builder
public class Modes {

    @Singular("addMode")
    private final Map<ModeType, TPS_Mode> modeMap;

    public TPS_Mode getMode(ModeType modeType){
        return modeMap.get(modeType);
    }

    public Collection<TPS_Mode> getModes(){
        return modeMap.values();
    }
}
