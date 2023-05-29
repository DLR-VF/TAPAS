package de.dlr.ivf.tapas.environment.gui.fx.view.factories;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class SimulationProgress<S> implements Callback<TableColumn<S,Double>, TableCell<S, Double>> {
    @Override
    public TableCell<S, Double> call(TableColumn<S, Double> param) {


        return new ProgressCell<>();
    }
}
