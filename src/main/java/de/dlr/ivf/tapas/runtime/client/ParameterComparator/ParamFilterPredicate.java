package de.dlr.ivf.tapas.runtime.client.ParameterComparator;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.function.Predicate;

public class ParamFilterPredicate {
	
	private String name;
	private ObjectProperty<Predicate<SimParamComparatorObject>> filter;
	
	public ParamFilterPredicate(String name, Predicate<SimParamComparatorObject> filter) {
		this.name = name;
		this.filter = new SimpleObjectProperty<>(filter);
	}

	public ObjectProperty<Predicate<SimParamComparatorObject>> predicateProperty() {
		return this.filter;
	}
	
	public Predicate<SimParamComparatorObject> getFilter(){
		return this.filter.get();
	}
	
	public String getName() {
		return this.name;
	}
	
	@Override
	public String toString() {
		return this.name;
	}
	
}
