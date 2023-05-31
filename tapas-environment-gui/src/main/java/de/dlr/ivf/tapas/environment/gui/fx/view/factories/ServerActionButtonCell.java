package de.dlr.ivf.tapas.environment.gui.fx.view.factories;

import de.dlr.ivf.tapas.environment.model.ServerState;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;

public class ServerActionButtonCell<S> extends TableCell<S, ServerState> {

    private Button button;
    @Override
    protected void updateItem(ServerState item, boolean empty) {
        super.updateItem(item, empty);
        if(empty){
            setText("");
            setGraphic(null);
        }else{
            setText(null);
            setGraphic(button);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }
    }
}
