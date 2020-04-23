package de.dlr.ivf.tapas.constants;

/**
 * NOTE: This class is a stub. It is in an early stage. Development may go on in the future!
 * This class represents the constants for the location codes with a weight for the capacity.
 * This is necessary because at some locations more than one activity can be performed but the capacity is shared.
 *
 * @author hein_mh
 */
public class TPS_LocationConstantWeighted extends TPS_LocationConstant {
    private final double weight;

    protected TPS_LocationConstantWeighted(int id, String[] attributes) {
        super(id, attributes);
        weight = 1.0;
    }

    protected TPS_LocationConstantWeighted(int id, String[] attributes, double weight) {
        super(id, attributes);
        this.weight = weight;
    }

    public double getWeight() {
        return this.weight;
    }

}
