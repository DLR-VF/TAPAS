package de.dlr.ivf.tapas.environment.gui.fx.view.factories;

import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

public class ProgressCell<S> extends TableCell<S, Double> {

    private final ProgressBar progressBar;
    private final Text cellText;

    private final StackPane stackPane;

    public ProgressCell(){
        this.progressBar = new ProgressBar();
        progressBar.prefWidthProperty().bind(widthProperty());
        this.cellText = new Text();
        stackPane = new StackPane();
        stackPane.getChildren().addAll(progressBar,cellText);
        stackPane.setAlignment(Pos.CENTER);
    }

    @Override
    protected void updateItem(Double item, boolean empty) {
        super.updateItem(item, empty);
        if(empty){
            setText("");
            setGraphic(null);
        }else{
            progressBar.setProgress(item);
            setText(item * 100+"%");
            setGraphic(stackPane);
            setContentDisplay(ContentDisplay.CENTER);
        }
    }
}
