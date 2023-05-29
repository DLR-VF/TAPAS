package de.dlr.ivf.tapas.environment.gui.fx.view.factories;

import de.dlr.ivf.tapas.environment.model.SimulationState;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.EnumMap;

public class SimulationActionButtonCell<S> extends TableCell<S,SimulationState> {

    private final Button button;
    private final SVGPath triangle = new SVGPath();
    private final SVGPath rectangle = new SVGPath();

    private final HBox startSimWrapper;

    private final HBox stopSimWrapper;

    public SimulationActionButtonCell(EnumMap<SimulationState, PseudoClass> simStatePseudoClasses){

        this.button = new Button();
        triangle.setContent("M 0, 0 L0, 8 L8,4z");
        rectangle.setContent("M 0,0 L0,7 L7,7 L7,0z");
        button.setContentDisplay(ContentDisplay.LEFT);

        startSimWrapper = newButtonSkin(triangle,"Start");
        stopSimWrapper = newButtonSkin(rectangle,"Stop");

    }
    @Override
    protected void updateItem(SimulationState item, boolean empty) {
        super.updateItem(item, empty);
        if(empty){
            setGraphic(null);
        }else{
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            //button.setGraphic(startButtonSkin);
            button.setPickOnBounds(true);
            setGraphic(button);

            switch (item){
                case READY, PAUSED -> button.setGraphic(startSimWrapper);
                case RUNNING -> button.setGraphic(stopSimWrapper);
                default -> setGraphic(null); //for other states there is no need for a button
            }
        }
    }

    private HBox newButtonSkin(SVGPath svg, String buttonText){

        Circle circle = new Circle(8);
        circle.getStyleClass().add("no-fill");

        StackPane stackPane = new StackPane();

        svg.getStyleClass().clear();
        svg.getStyleClass().add("sim-action-button-graphic");
        stackPane.getChildren().addAll(circle,svg);
        StackPane.setAlignment(svg, Pos.CENTER);
        Text text = new Text(buttonText);
        text.setTextAlignment(TextAlignment.CENTER);
        text.getStyleClass().add("sim-action-button-text");

        HBox wrapper = new HBox(8, stackPane, text);
        button.getStyleClass().add("sim-action-button");
        wrapper.setAlignment(Pos.CENTER);

        return wrapper;
    }
}
