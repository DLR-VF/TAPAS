package de.dlr.ivf.tapas.environment.gui.fx.view.factories;

import de.dlr.ivf.tapas.environment.model.SimulationState;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;

public class SimulationActionButtonCell<S> extends TableCell<S,SimulationState> {

    private Button button;

    public SimulationActionButtonCell(){
        this.button = new Button();
    }
    @Override
    protected void updateItem(SimulationState item, boolean empty) {
        super.updateItem(item, empty);
        if(empty){
            setGraphic(null);
        }else{
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

            switch (item){
                case READY, PAUSED -> button.setText("Start");
                case RUNNING -> button.setText("Stop");
                default -> button.setText("");
            }
            setGraphic(button);

        }
    }
}
