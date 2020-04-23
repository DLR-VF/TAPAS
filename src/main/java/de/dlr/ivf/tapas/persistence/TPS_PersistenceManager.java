package de.dlr.ivf.tapas.persistence;

import de.dlr.ivf.tapas.loc.TPS_Region;
import de.dlr.ivf.tapas.mode.TPS_ModeSet;
import de.dlr.ivf.tapas.person.TPS_Household;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.scheme.TPS_SchemeSet;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

/**
 * This interface separates the software from the data structure. Behind this interface there can be implementations which
 * base on e.g. files or a database. The interface provides all methods which are used from the application.
 *
 * @author mark_ma
 */
public interface TPS_PersistenceManager {
    /**
     * Closes all open files, database connections, etc.
     */
    void close();

    /**
     * commit all pending data
     */
    boolean finish();

    /**
     * Returns a singleton instance of TPS_ModeSet.
     *
     * @return singleton of TPS_ModeSet
     */
    TPS_ModeSet getModeSet();

    /**
     * @return next household to process
     */
    TPS_Household getNextHousehold();

    /**
     * This method returns the parameterClass reference
     */
    TPS_ParameterClass getParameters();

    /**
     * Returns a singleton instance of TPS_Region.
     *
     * @return singleton of TPS_Region
     */
    TPS_Region getRegion();


    /**
     * Returns a singleton instance of TPS_SchemeSet.
     *
     * @return singleton of TPS_SchemeSet
     */
    TPS_SchemeSet getSchemesSet();


    /**
     * This method increments the occupancy and recalculates the weight of the location, the location set and the traffic
     * analysis zone.
     */
    boolean incrementOccupancy();

    /**
     * This method initialises the the persistence manager, i.e. for the database that a connection is established and the
     * main values are read or for files that the main path of the input files is set, etc.
     */
    void init();

    /**
     * This method checks if unfinished households are present in the database
     */
    void resetUnfinishedHouseholds();

    /**
     * This method returns the completely processed household back to the persistence manager. This method is used for
     * pooling the household and person objects. If you don't give the instances back you have to built always new instances
     * instead of using the old ones by initialising them only.
     *
     * @param hh completely processed household
     */
    void returnHousehold(TPS_Household hh);

    /**
     * This method stores the current plan on a device. This device can be e.g. a database or a file.
     *
     * @param plan plan to write
     * @throws Exception An exception is thrown if the plan wasn't write back to the storage medium
     */
    void writePlan(TPS_Plan plan);


}
