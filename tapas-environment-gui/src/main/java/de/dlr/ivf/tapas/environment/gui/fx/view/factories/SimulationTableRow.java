package de.dlr.ivf.tapas.environment.gui.fx.view.factories;

import de.dlr.ivf.tapas.environment.model.SimulationState;
import de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation.SimulationEntryViewModel;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.css.PseudoClass;
import javafx.scene.control.TableRow;

import java.util.EnumMap;

public class SimulationTableRow extends TableRow<SimulationEntryViewModel> {
    private final ChangeListener<SimulationState> simStateListener;
    private final WeakChangeListener<SimulationState> simStateWeakListener;
    private final EnumMap<SimulationState, PseudoClass> simStatePseudoClasses;

    public SimulationTableRow(EnumMap<SimulationState, PseudoClass> simStatePseudoClasses){
        this.simStatePseudoClasses = simStatePseudoClasses;
        this.simStateListener = ((observable, oldValue, newValue) -> updateSimStatePseudoClass(oldValue, newValue));
        this.simStateWeakListener = new WeakChangeListener<>(simStateListener);
    }

    private void updateSimStatePseudoClass(SimulationState oldState, SimulationState newState){

        if(oldState != null)
            updateSimStatePseudoClass(oldState, false);
        else
            updateSimStatePseudoClass(newState,true);

    }
    private void updateSimStatePseudoClass(SimulationState state, boolean activate) {

        PseudoClass pseudoClass = simStatePseudoClasses.get(state);
        pseudoClassStateChanged(pseudoClass, activate);
    }

    @Override
    protected void updateItem(SimulationEntryViewModel item, boolean empty) {

        SimulationEntryViewModel oldItem = getItem();

        if(oldItem != null){ //remove the pseudo class from the previous item
            ObjectProperty<SimulationState> stateProperty = oldItem.simulationStateProperty();
            stateProperty.removeListener(simStateWeakListener);
            pseudoClassStateChanged(simStatePseudoClasses.get(stateProperty.get()),false);
        }

        super.updateItem(item, empty);

        if(item != null){
            item.simulationStateProperty().addListener(simStateWeakListener);
            updateSimStatePseudoClass(item.simulationStateProperty().get(), true);
        }
    }
}
