package de.dlr.ivf.tapas.util.parameters;

/**
 * This class provides all matrix enums which determine the name of
 * parameters available in the application
 *
 * This enum provides constants in matrix form for the application. It is
 * possible that on constant has two matrices depending on the type of the
 * constant. If it is simulation type dependent there exist one matrix for
 * the base an one for the scenario case.
 */
public enum ParamMatrix {

    /**
     * matrix containing the distances on the street net for each pair of
     * tvz
     */
    DISTANCES_STREET,
    /**
     * matrix containing the distances on the street and park net for each pair of
     * tvz
     */
    DISTANCES_WALK,
    /**
     * matrix containing the distances on the street and park net for each pair of
     * tvz
     */
    DISTANCES_BIKE,
    /**
     * matrix containing the distances on the train net for each pair of
     * tvz
     */
    DISTANCES_PT,
    /**
     * matrix containing the distances of the beelines
     * tvz
     */
    DISTANCES_BL
}


