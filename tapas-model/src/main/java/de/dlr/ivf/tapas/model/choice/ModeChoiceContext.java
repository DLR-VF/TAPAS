package de.dlr.ivf.tapas.model.choice;

import de.dlr.ivf.tapas.model.location.Locatable;
import de.dlr.ivf.tapas.model.vehicle.TPS_Car;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Set;

@Builder
public class ModeChoiceContext<T> {

    @Singular("addAvailableMode")
    private final Set<T> availableModes;

    @Getter
    private final Locatable from;
    @Getter
    private final Locatable to;
    @Getter
    private final TPS_Car car;
    @Getter
    private final int startTime;

    public boolean modeIsAvailable(T mode){
        return availableModes.contains(mode);
    }
}
