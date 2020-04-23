package de.dlr.ivf.tapas.runtime.client.Graphics.QualityChart;

import java.awt.*;

public enum Quality {
    NOT_SET(Color.GRAY), //
    BAD(Color.RED), //
    MEDIUM(Color.ORANGE), //
    GOOD(Color.GREEN);

    private final Color color;

    Quality(Color color) {
        this.color = color;
    }

    public static Quality getById(int id) {
        return values()[id];
    }

    public Color getColor() {
        return color;
    }
}
