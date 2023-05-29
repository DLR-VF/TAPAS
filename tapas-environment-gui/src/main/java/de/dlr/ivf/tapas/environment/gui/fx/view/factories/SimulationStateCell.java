package de.dlr.ivf.tapas.environment.gui.fx.view.factories;

import de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation.SimulationEntryViewModel;
import de.dlr.ivf.tapas.environment.model.SimulationState;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.css.PseudoClass;
import javafx.scene.control.TableCell;

import java.util.EnumMap;

public class SimulationStateCell extends TableCell<SimulationEntryViewModel, SimulationState> {
    private final WeakChangeListener<SimulationState> simStateWeakListener;
    private final EnumMap<SimulationState, PseudoClass> simStatePseudoClasses;

    public SimulationStateCell(EnumMap<SimulationState, PseudoClass> simStatePseudoClasses){
        this.simStatePseudoClasses = simStatePseudoClasses;
        ChangeListener<SimulationState> simStateListener = ((observable, oldValue, newValue) -> updateSimStatePseudoClass(oldValue, newValue));
        this.simStateWeakListener = new WeakChangeListener<>(simStateListener);
    }

    private void updateSimStatePseudoClass(SimulationState oldState, SimulationState newState){

        if(oldState != null)
            updateSimStatePseudoClass(oldState, false);
        if(newState != null)
            updateSimStatePseudoClass(newState,true);

    }
    private void updateSimStatePseudoClass(SimulationState state, boolean activate) {

        PseudoClass pseudoClass = simStatePseudoClasses.get(state);
        pseudoClassStateChanged(pseudoClass, activate);
    }
    @Override
    protected void updateItem(SimulationState item, boolean empty) {

        SimulationState oldItem = getItem();

        if(oldItem != null){ //remove the pseudo class from the previous item
            ObjectProperty<SimulationState> stateProperty = getTableRow().getItem().simulationStateProperty();
            stateProperty.removeListener(simStateWeakListener);
            pseudoClassStateChanged(simStatePseudoClasses.get(stateProperty.get()),false);
        }
        super.updateItem(item, empty);


        if(item != null){
            getTableRow().getItem().simulationStateProperty().addListener(simStateWeakListener);
            updateSimStatePseudoClass(getTableRow().getItem().simulationStateProperty().get(), true);
        }

    }
}
