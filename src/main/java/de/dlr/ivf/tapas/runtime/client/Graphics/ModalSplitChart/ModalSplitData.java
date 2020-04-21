package de.dlr.ivf.tapas.runtime.client.Graphics.ModalSplitChart;

import de.dlr.ivf.tapas.analyzer.tum.constants.CategoryCombination;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.Categories;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.Mode;
import de.dlr.ivf.tapas.runtime.client.Graphics.QualityChart.Quality;

public class ModalSplitData {

	private CategoryCombination categoryCombination;
	private Quality quality;
	private double model;
	private double reference;

	public ModalSplitData(CategoryCombination cc, Quality q, double model,
			double reference) {

		if (!cc.contains(Categories.Mode)) {
			throw new IllegalArgumentException(
					"The CategoryCombination must contain MODE");
		}

		categoryCombination = new CategoryCombination(cc.getCategories());

		quality = q;
		this.model = model;
		this.reference = reference;
	}

	public Quality getQuality() {
		return quality;
	}

	public double getModel() {
		return model;
	}

	public double getReference() {
		return reference;
	}

	public String getLabel() {
		return categoryCombination.toString();
	}

	@Override
	public String toString() {
		return "[" + getLabel() + ",ref=" + reference + ",mod=" + model + "]";
	}

	/**
	 * @return the {@link Mode} of this data point.
	 */
	@SuppressWarnings("rawtypes")
	public Mode getMode() {
		for (Enum e : categoryCombination.getCategories()) {
			if (e instanceof Mode)
				return (Mode) e;
		}
		return null;
	}

	public CategoryCombination getCategories() {
		return categoryCombination;
	}

}
