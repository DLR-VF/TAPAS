package de.dlr.ivf.tapas.execution.sequential.context;

import de.dlr.ivf.tapas.mode.TPS_ExtMode;

import java.util.ArrayList;
import java.util.List;

public class ModeContext implements ContextUpdateable {

    private TPS_ExtMode previous_mode;
    private TPS_ExtMode next_mode;

    private List<TPS_ExtMode> modes = new ArrayList<>();

    public void setNextMode(TPS_ExtMode next_mode){

        this.next_mode = next_mode;
        modes.add(next_mode);
    }

    public TPS_ExtMode getNextMode(){

        return this.next_mode;
    }

    public TPS_ExtMode getPreviousMode(){

        return this.previous_mode;
    }

    @Override
    public void updateContext() {
        this.previous_mode = this.next_mode;
        this.next_mode = null;
    }
}
