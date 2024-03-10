package de.dlr.ivf.tapas.model.mode;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Collection;
import java.util.Map;

@Builder
@Getter
public class Modes {

    @Singular("addModeByName")
    private final Map<String, TPS_Mode> modesByName;

    @Singular("addModeById")
    private final Map<Integer, TPS_Mode> modesById;

    public TPS_Mode getModeByName(String modeType){
        return modesByName.get(modeType);
    }

    public TPS_Mode getModeById(int id){
        return modesById.get(id);
    }

    public Collection<TPS_Mode> getModes(){
        return modesByName.values();
    }
}
