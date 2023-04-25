/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.environment.gui.parametercomparator;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.function.Predicate;

public class ParamFilterPredicate {

    private final String name;
    private final ObjectProperty<Predicate<SimParamComparatorObject>> filter;

    public ParamFilterPredicate(String name, Predicate<SimParamComparatorObject> filter) {
        this.name = name;
        this.filter = new SimpleObjectProperty<>(filter);
    }

    public Predicate<SimParamComparatorObject> getFilter() {
        return this.filter.get();
    }

    public String getName() {
        return this.name;
    }

    public ObjectProperty<Predicate<SimParamComparatorObject>> predicateProperty() {
        return this.filter;
    }

    @Override
    public String toString() {
        return this.name;
    }

}
