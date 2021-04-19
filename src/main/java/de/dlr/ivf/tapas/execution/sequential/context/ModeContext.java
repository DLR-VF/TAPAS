package de.dlr.ivf.tapas.execution.sequential.context;

import de.dlr.ivf.tapas.mode.TPS_ExtMode;

public class ModeContext implements ContextUpdateable {

    private TPS_ExtMode current_mode;
    private TPS_ExtMode next_mode;

    public void setNextMode(TPS_ExtMode next_mode){
        this.next_mode = next_mode;
    }

    public TPS_ExtMode getNextMode(){
        return this.next_mode;
    }

    public TPS_ExtMode getCurrentMode(){
        return this.current_mode;
    }

    @Override
    public void updateContext() {
        this.current_mode = this.next_mode;
        this.next_mode = null;
    }
}
