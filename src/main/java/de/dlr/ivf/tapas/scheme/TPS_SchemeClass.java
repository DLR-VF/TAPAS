package de.dlr.ivf.tapas.scheme;

import de.dlr.ivf.tapas.distribution.TPS_DiscreteDistribution;
import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.util.ExtendedWritable;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A scheme class collects similar schemes into one class. Furthermore it provide a time distribution for travel time acceptance
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.PLAN)
public class TPS_SchemeClass implements Iterable<TPS_Scheme>, ExtendedWritable {
    // The id of the scheme class
    private final int id;

    // Map of schemes indexed by their id
    private final SortedMap<Integer, TPS_Scheme> schemes;

    //probability distribution over the TPS_Scheme
    private TPS_DiscreteDistribution<TPS_Scheme> schemeDis;

    /**
     * Reference to the scheme set
     */
    private TPS_SchemeSet schemeSet;


    /**
     * mean or expected value of a Gaussian distribution of this scheme class
     */
    private double mean;

    /**
     * deviation of a gaussian distribution
     */
    private double deviation;

    /**
     * Constructor
     *
     * @param id The id of this scheme class
     */
    public TPS_SchemeClass(int id) {
        this.id = id;
        this.schemes = new TreeMap<>();
    }

    /**
     * Checks if a scheme with the given id exists in the scheme class
     *
     * @param schemeId corresponding to a scheme
     * @return true if there exists a scheme corresponding to the given id
     */
    public boolean containsScheme(int schemeId) {
        return this.schemes.containsKey(schemeId);
    }

    /**
     * When the scheme distribution is not created yet it gets created. Then a scheme is drawn out of the distribution.
     *
     * @return the drawn scheme
     */
    public TPS_Scheme draw() {
        if (schemes.isEmpty()) {
            return null;
        }
        if (this.schemeDis == null) {
            this.schemeDis = new TPS_DiscreteDistribution<>(schemes.values());
            this.schemeDis.normalize();
        }
        return this.schemeDis.drawKey();
    }

    double getDeviation() {
        return deviation;
    }

    private void setDeviation(double deviation) {
        this.deviation = deviation;
    }

    /**
     * Returns the id of the scheme class
     *
     * @return id
     */
    public int getId() {
        return id;
    }

    double getMean() {
        return mean;
    }

    private void setMean(double mean) {
        this.mean = mean;
    }

    /**
     * This method returns the scheme corresponding to the given scheme id. If the scheme doesn't exist it is created.
     *
     * @param schemeId       corresponding to the desired scheme
     * @param parameterClass parameter class reference
     * @return scheme with the given id
     */
    public TPS_Scheme getScheme(int schemeId, TPS_ParameterClass parameterClass) {
        TPS_Scheme scheme;
        if (!containsScheme(schemeId)) {
            scheme = new TPS_Scheme(schemeId, parameterClass);
            scheme.setSchemeClass(this);
            this.schemes.put(scheme.getId(), scheme);
        } else {
            scheme = this.schemes.get(schemeId);
        }
        return scheme;
    }

    /**
     * @return reference to the complete scheme set
     */
    public TPS_SchemeSet getSchemeSet() {
        return schemeSet;
    }

    /**
     * Sets reference to the schemeSet
     *
     * @param schemeSet schemeSet reference
     */
    void setSchemeSet(TPS_SchemeSet schemeSet) {
        this.schemeSet = schemeSet;
    }

    /**
     * Method to check if this TPS_SchemeClass is empty.
     *
     * @return true if no TPS_Scheme is attached to this object
     */
    public boolean isEmpty() {
        return this.schemes.isEmpty();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<TPS_Scheme> iterator() {
        return this.schemes.values().iterator();
    }

    /**
     * Sets the time distribution, i.e. mean and deviation
     *
     * @param mean mean
     * @param dev  deviation
     */
    public void setTimeDistribution(double mean, double dev) {
        setMean(mean);
        setDeviation(dev);
    }

    /**
     * Returns the size of the scheme class: e.g. the number of schemes contained
     *
     * @return number of schemes in the scheme class
     */
    public int size() {
        return this.schemes.size();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.toString("");
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.ivf.tapas.util.ExtendedWritable#toString(java.lang.String)
     */
    public String toString(String prefix) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix + "TAPAS SchemeClass [id=" + this.id + ", size=" + this.size() + "]\n");
        for (TPS_Scheme scheme : this.schemes.values()) {
            sb.append(scheme.toString(prefix + " ") + "\n");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

}