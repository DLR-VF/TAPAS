package de.dlr.ivf.tapas.environment.gui.fx.view.factories;

import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

public class ProgressCell<S> extends TableCell<S, Double> {

    private ProgressBar progressBar;
    private Text cellText;

    private HBox horizontalWrapper;

    public ProgressCell(){
        this.progressBar = new ProgressBar();
        this.cellText = new Text();
        horizontalWrapper = new HBox();
        horizontalWrapper.getChildren().addAll(progressBar,cellText);
        horizontalWrapper.setAlignment(Pos.CENTER_LEFT);
        horizontalWrapper.setSpacing(5);
    }

    @Override
    protected void updateItem(Double item, boolean empty) {
        super.updateItem(item, empty);
        if(empty){
            setText("");
            setGraphic(null);
        }else{
            progressBar.setProgress(item);
            cellText.setText("value: "+item);
            setGraphic(horizontalWrapper);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }
    }
}
