package de.dlr.ivf.tapas.runtime.client.Graphics.QualityChart;

import java.awt.Color;

public enum Quality {
	NOT_SET(Color.GRAY), //
	BAD(Color.RED), //
	MEDIUM(Color.ORANGE), //
	GOOD(Color.GREEN);

	private final Color color;

	Quality(Color color) {
		this.color = color;
	}

	public Color getColor() {
		return color;
	}

	public static Quality getById(int id) {
		return values()[id];
	}
}
