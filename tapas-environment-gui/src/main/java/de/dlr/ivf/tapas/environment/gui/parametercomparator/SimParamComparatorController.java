/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.environment.gui.parametercomparator;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.runtime.util.MultilanguageSupport;
import de.dlr.ivf.tapas.util.parameters.ParamFlag;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class SimParamComparatorController implements Initializable {

    private final String simkey1, simkey2;
    private final TPS_DB_Connector conn;
    @FXML
    private TableView<SimParamComparatorObject> tvSimComparator;
    @FXML
    private TableColumn<SimParamComparatorObject, String> tcParameter, tcSim1, tcSim2;
    @FXML
    private AnchorPane root;
    @FXML
    private ComboBox<ParamFilterPredicate> cbFilter;
    @FXML
    private AnchorPane topBox;
    private final StringProperty para_header;
    private final StringProperty sim1_header;
    private final StringProperty sim2_header;
    private final boolean is_correct_input;
    private ListProperty<SimParamComparatorObject> tablecontent;
    private FilteredList<SimParamComparatorObject> lst;


    public SimParamComparatorController(String simkey1, String simkey2, TPS_DB_Connector conn) {
        this.simkey1 = simkey1;
        this.simkey2 = simkey2;
        this.para_header = new SimpleStringProperty("Parameters");
        this.sim1_header = new SimpleStringProperty(simkey1);
        this.sim2_header = new SimpleStringProperty(simkey2);
        this.conn = conn;
        is_correct_input = simkey1 != null && simkey2 != null && !simkey1.equals("") && !simkey2.equals("");
    }

    private void generateComparisonList() {

        if (is_correct_input) {
            Task<ObservableList<SimParamComparatorObject>> tsk = new Task<ObservableList<SimParamComparatorObject>>() {
                @Override
                protected ObservableList<SimParamComparatorObject> call() throws Exception {
                    List<SimParamComparatorObject> lst = new ArrayList<>();
                    ResultSet rs;

                    //
                    String query = "SELECT Z.param_key as key, A.param_value as sim1 , B.param_value as sim2\r\n" +
                            "FROM (SELECT DISTINCT param_key FROM simulation_parameters WHERE param_key IS NOT NULL AND param_key != '') AS Z " +
                            "FULL OUTER JOIN (SELECT * FROM simulation_parameters WHERE sim_key = '" + simkey1 +
                            "') AS A " + "ON Z.param_key = A.param_key " +
                            "FULL OUTER JOIN (SELECT * FROM simulation_parameters WHERE sim_key = '" + simkey2 +
                            "') AS B " + "ON Z.param_key = B.param_key " + "ORDER BY Z.param_key";
                    rs = conn.executeQuery(query, SimParamComparatorController.class);

                    Function<Object, String> handle_nulls = str -> str == null ? "" : str.toString();
                    Map<String, String> tps_parameters = new HashMap<>();

                    Arrays.stream(ParamString.values()).forEach(param -> tps_parameters.put(param.name(),
                            handle_nulls.apply((new TPS_ParameterClass()).paramStringClass.getPreset(param))));
                    Arrays.stream(ParamValue.values()).forEach(param -> tps_parameters.put(param.name(),
                            handle_nulls.apply((new TPS_ParameterClass()).paramValueClass.getPreset(param))));
                    Arrays.stream(ParamFlag.values()).forEach(param -> tps_parameters.put(param.name(),
                            handle_nulls.apply((new TPS_ParameterClass()).paramFlagClass.getPreset(param))));

                    while (rs.next()) {
                        lst.add(new SimParamComparatorObject(rs.getString("key"),
                                tps_parameters.get(rs.getString("key")), handle_nulls.apply(rs.getString("sim1")),
                                handle_nulls.apply(rs.getString("sim2"))));
                    }
                    rs.close();


                    return FXCollections.observableList(lst);
                }
            };

            tsk.run();

            tsk.setOnSucceeded(event -> {
                try {
                    tablecontent = new SimpleListProperty<>(tsk.get());
                    lst = new FilteredList<>(tablecontent);
                    tvSimComparator.setItems(lst);
                    cbFilter.valueProperty().addListener((obsv, oldv, newv) -> lst.setPredicate(newv.getFilter()));
                    cbFilter.getSelectionModel().selectFirst();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            });
        } else throw new IllegalArgumentException("At least one of the input arguments is either null or empty String");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        generateComparisonList();

        ResourceBundle rb = ResourceBundle.getBundle("SimParamComparatorControllerLabels",
                MultilanguageSupport.getLocale());
        this.para_header.setValue(rb.getString("PARAMETERS"));
        //init filter predicates
        cbFilter.getItems().add(new ParamFilterPredicate(rb.getString("SHOW_ALL"), param -> true));
        cbFilter.getItems().add(new ParamFilterPredicate(rb.getString("SHOW_NULLS"), SimParamComparatorObject::isNull));
        cbFilter.getItems().add(
                new ParamFilterPredicate(rb.getString("SHOW_DEFAULTS"), SimParamComparatorObject::isDefault));
        cbFilter.getItems().add(
                new ParamFilterPredicate(rb.getString("SHOW_EQUALS"), SimParamComparatorObject::isEqual));
        cbFilter.getItems().add(
                new ParamFilterPredicate(rb.getString("SHOW_UNEQUALS"), SimParamComparatorObject::isNotEqual));

        //bindings
        tcParameter.textProperty().bind(this.para_header);
        tcParameter.minWidthProperty().bind(tcParameter.textProperty().length().multiply(30));

        tcSim1.textProperty().bind(sim1_header);
        tcSim1.minWidthProperty().bind(tcSim1.textProperty().length().multiply(10));

        tcSim2.textProperty().bind(sim2_header);
        tcSim2.minWidthProperty().bind(tcSim2.textProperty().length().multiply(10));

        root.prefWidthProperty().bind(
                tcSim1.widthProperty().add(tcSim2.widthProperty().add(tcParameter.widthProperty().add(20))));

        tvSimComparator.prefWidthProperty().bind(root.widthProperty());
        tvSimComparator.prefHeightProperty().bind(root.heightProperty().subtract(topBox.heightProperty()));

        //cell and cell value factories
        tcParameter.setCellValueFactory(modal -> new SimpleStringProperty(modal.getValue().getParamKey()));
        tcSim1.setCellValueFactory(modal -> new SimpleStringProperty(modal.getValue().getValueOfFirstSim()));
        tcSim2.setCellValueFactory(modal -> new SimpleStringProperty(modal.getValue().getValueOfSecondSim()));

        tcSim1.setCellFactory(col -> new TableCell<SimParamComparatorObject, String>() {

            private final Label l = new Label();
            private final Circle c = new Circle(5);
            private final HBox container = new HBox(5, c, l);

            {
                container.setAlignment(Pos.CENTER_LEFT);
                c.getStyleClass().add("default-circle");
            }

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    l.setText(item);
                    if (item.length() > 0) {
                        setTooltip(new Tooltip(item));
                    }
                    SimParamComparatorObject o = getTableRow().getItem();
                    if (o != null) {
                        c.setVisible(o.getIsDefaultFirstSim());
                    }
                }
                setGraphic(empty ? null : container);
            }
        });

        tcSim2.setCellFactory(col -> new TableCell<SimParamComparatorObject, String>() {

            private final Label l = new Label();
            private final Circle c = new Circle(5);
            private final HBox container = new HBox(5, c, l);

            {
                container.setAlignment(Pos.CENTER_LEFT);
                c.getStyleClass().add("default-circle");
            }

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    l.setText(item);
                    if (item.length() > 0) {
                        if (getTooltip() == null) setTooltip(new Tooltip(item));
                        else getTooltip().setText(item);
                    }
                    SimParamComparatorObject o = getTableRow().getItem();
                    if (o != null) {
                        c.setVisible(o.getIsDefaultSecondSim());
                    }
                }
                setGraphic(empty ? null : container);
            }
        });

        //row factory
        tvSimComparator.setRowFactory(tv -> new TableRow<SimParamComparatorObject>() {
            @Override
            protected void updateItem(SimParamComparatorObject item, boolean empty) {
                super.updateItem(item, empty);

                getPseudoClassStates().stream().map(PseudoClass::getPseudoClassName).filter(
                        pseudoclass -> pseudoclass.equalsIgnoreCase("isnull") ||
                                pseudoclass.equalsIgnoreCase("inequal") || pseudoclass.equalsIgnoreCase("isequal"))
                                      .forEach(pseudoclazz -> pseudoClassStateChanged(
                                              PseudoClass.getPseudoClass(pseudoclazz), false));

                if (!empty && item != null) {
                    if (item.isNull()) {
                        pseudoClassStateChanged(PseudoClass.getPseudoClass("isnull"), true);
                    } else {
                        if (item.isEqual()) {
                            pseudoClassStateChanged(PseudoClass.getPseudoClass("isequal"), true);
                        } else {
                            pseudoClassStateChanged(PseudoClass.getPseudoClass("inequal"), true);
                        }
                    }
                }
            }
        });
    }

}
