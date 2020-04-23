package de.dlr.ivf.tapas.loc;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.constants.TPS_ActivityConstant.TPS_ActivityCodeType;
import de.dlr.ivf.tapas.constants.TPS_ActivityConstant.TPS_ActivityConstantAttribute;
import de.dlr.ivf.tapas.constants.TPS_LocationConstant;
import de.dlr.ivf.tapas.constants.TPS_PersonGroup;
import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.persistence.TPS_RegionResultSet;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.plan.TPS_LocatedStay;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.TPS_PlanningContext;
import de.dlr.ivf.tapas.util.ExtendedWritable;
import de.dlr.ivf.tapas.util.TPS_VariableMap;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.SimulationType;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * This class represents a complete region for a TAPAS simulation. A region can be Berlin. The region contains all
 * traffic analysis zones. Furthermore the values of time and the cfn values are connected to this class.
 * <p>
 * The class provides a basic method selectLocation in which for a specific stay a location from the region is selected.
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.EPISODE)
public class TPS_Region implements ExtendedWritable, Iterable<TPS_TrafficAnalysisZone> {
    /// The reference to the persistence manager
    private TPS_PersistenceManager PM = null;

    /// All CFN values for this region
    private TPS_CFN cfn;

    /// The map for all traffic analysis zones sorted by their id
    private final SortedMap<Integer, TPS_TrafficAnalysisZone> TAZ_Map;

    /// The map of the differentiated values of time
    private TPS_VariableMap valuesOfTime;

    // global locations storage
    private final HashMap<Integer, TPS_Location> locations = new HashMap<>();
    /**
     * Instance to the location select model
     */
    private TPS_LocationSelectModel LOCATION_SELECT_MODEL = null;
    /**
     * Instance to the location select model
     */
    private TPS_LocationChoiceSet LOCATION_CHOICE_SET = null;

    /**
     * Initialises the map for the traffic analysis zones
     */
    public TPS_Region(TPS_PersistenceManager pm) {
        this.TAZ_Map = new TreeMap<>();
        this.PM = pm;
    }

    /**
     * Adds the given location to this region;
     *
     * @param location The location to add
     */
    public void addLocation(TPS_Location location) {
        this.locations.put(location.getId(), location);
    }

    /**
     * This method adds the given traffic analysis zone when the id doesn't exist yet as a key in the traffic analysis
     * zone map
     *
     * @param taz the traffic analysis zone to add
     * @return true if added, false otherwise
     */
    private boolean addTrafficAnalysisZone(TPS_TrafficAnalysisZone taz) {
        if (!containsTrafficAnalysisZone(taz.getTAZId())) {
            TAZ_Map.put(taz.getTAZId(), taz);
            taz.setRegion(this);
            return true;
        }
        return false;
    }

    /**
     * Calculates the increase in travel time resulting from the transformation of the general costs into time usage;
     * amount of time being added depends on the value of time as a function of the costs specified and the appropriate
     * vot
     *
     * @param plan  the plan for holding the attributes
     * @param costs travel costs / general costs
     * @return travel time increase in seconds
     */
    private double calculateVOTadds(double costs, TPS_Plan plan) {
        // getting vots as a function of the activity type, mode, distance and income
        double vot = this.getValuesOfTime().getValue(plan.getAttributes());
        if (TPS_Logger.isLogging(this.getClass(), SeverenceLogLevel.FINE)) {
            TPS_Logger.log(this.getClass(), SeverenceLogLevel.FINE,
                    "Calculated vot: " + vot + " with request:" + this.getValuesOfTime().getLastRequest());
        }
        return 3600.0 * costs / vot;// travel time plus in seconds
    }

    /**
     * Flag determining if the traffic analysis zone, specified via the id, is a known element of the region
     *
     * @param id of the traffic analysis zone
     * @return true if the id is a key in the traffic analysis zone map
     */
    public boolean containsTrafficAnalysisZone(int id) {
        return TAZ_Map.containsKey(id);
    }

    /**
     * Creates a new traffic analysis zone specified by the id in the region ad returns it. If the taz exists it returns the existing taz.
     *
     * @param id the new id for this taz
     * @return the created taz
     */

    public TPS_TrafficAnalysisZone createTrafficAnalysisZone(int id) {
        if (!containsTrafficAnalysisZone(id)) {
            TPS_TrafficAnalysisZone taz = new TPS_TrafficAnalysisZone(id);
            this.addTrafficAnalysisZone(taz);
            return taz;
        } else return TAZ_Map.get(id);
    }

    /**
     * Retruns the block for the given id
     *
     * @param blockId the block id to look for
     * @return the block or null if nothing was found
     */
    public TPS_Block getBlock(int blockId) {
        for (TPS_TrafficAnalysisZone taz : this) {
            if (taz.containsBlock(blockId)) {
                return taz.getBlock(blockId);
            }
        }
        return null;
    }

    /**
     * @return the cfn
     */
    public TPS_CFN getCfn() {
        return cfn;
    }

    /**
     * Sets the reference of all cfn values
     *
     * @param cfn all cfn values for this region
     */
    public void setCfn(TPS_CFN cfn) {
        this.cfn = cfn;
    }

    /**
     * Gets the specified location from this region
     *
     * @param id the location id to retrieve
     * @return the location or null if no location was found
     */
    public TPS_Location getLocation(int id) {
        return this.locations.get(id);
    }

    public TPS_LocationChoiceSet getLocationChoiceSet() {
        if (LOCATION_CHOICE_SET == null) {
            initLocationChoiceSet();
        }
        return LOCATION_CHOICE_SET;
    }

    public TPS_LocationSelectModel getLocationSelectModel() {
        if (LOCATION_SELECT_MODEL == null) {
            initLocationSelectModel();
        }
        return LOCATION_SELECT_MODEL;
    }

    /**
     * Returns the smallest id of all traffic zones in the region
     *
     * @return the smallest id
     */
    public int getSmallestId() {
        return this.TAZ_Map.firstKey();
    }

    /**
     * Returns the traffic analysis zone specified by the id provides
     *
     * @param id id of the traffic analysis zone asked for
     * @return the traffic analysis zone or null if non existant
     */
    public TPS_TrafficAnalysisZone getTrafficAnalysisZone(int id) {
        return TAZ_Map.get(id);
    }

    /**
     * gets the key-set for the TAZ map
     *
     * @return
     */
    public Collection<Integer> getTrafficAnalysisZoneKeys() {
        return this.TAZ_Map.keySet();
    }

    /**
     * gets the value set for the TAZ map
     *
     * @return
     */
    public Collection<TPS_TrafficAnalysisZone> getTrafficAnalysisZones() {
        return this.TAZ_Map.values();
    }

    /**
     * Returns a map with all differentiated values of time
     *
     * @return the values of time
     */
    public TPS_VariableMap getValuesOfTime() {
        return valuesOfTime;
    }

    /**
     * Sets the reference of the values of time
     *
     * @param valuesOfTime
     */
    public void setValuesOfTime(TPS_VariableMap valuesOfTime) {
        this.valuesOfTime = valuesOfTime;
    }

    /**
     * This method initializes the location select model.
     */
    public void initLocationChoiceSet() {
        try {
            String locationChoiceSetClass = this.PM.getParameters().getString(ParamString.LOCATION_CHOICE_SET_CLASS);
            locationChoiceSetClass = locationChoiceSetClass.substring(locationChoiceSetClass.lastIndexOf('.') + 1);
            Class<?> c = Class.forName(TPS_Region.class.getPackage().getName() + "." + locationChoiceSetClass);
            LOCATION_CHOICE_SET = (TPS_LocationChoiceSet) c.getDeclaredConstructor().newInstance();
            LOCATION_CHOICE_SET.setClassReferences(this, (TPS_DB_IOManager) this.PM);
        } catch (Exception e) {
            // this is bad style, but the above four lines produce way to many exceptions, which are all related to "ClassNotFound"
            TPS_Logger.log(HierarchyLogLevel.APPLICATION, SeverenceLogLevel.FATAL,
                    "Error in instantiating the " + "utility function: " + ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * This method initializes the location select model.
     */
    public void initLocationSelectModel() {
        try {
            String locationSelectModelClass = this.PM.getParameters().getString(
                    ParamString.LOCATION_SELECT_MODEL_CLASS);
            locationSelectModelClass = locationSelectModelClass.substring(
                    locationSelectModelClass.lastIndexOf('.') + 1);
            Class<?> c = Class.forName(TPS_Region.class.getPackage().getName() + "." + locationSelectModelClass);
            LOCATION_SELECT_MODEL = (TPS_LocationSelectModel) c.getDeclaredConstructor().newInstance();
            LOCATION_SELECT_MODEL.setClassReferences(this, (TPS_DB_IOManager) this.PM);

        } catch (ClassNotFoundException | InvocationTargetException | SecurityException | NoSuchMethodException | InstantiationException | IllegalArgumentException | IllegalAccessException e) {
            TPS_Logger.log(HierarchyLogLevel.APPLICATION, SeverenceLogLevel.FATAL,
                    "Error in instantiating the " + "utility function: " + ExceptionUtils.getStackTrace(e));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<TPS_TrafficAnalysisZone> iterator() {
        // return iterator over all traffic analysis zones
        return this.TAZ_Map.values().iterator();
    }

    /**
     * Manipulates the cfn4-values depending on the person group --> non used / stub
     *
     * @param confSel
     * @param personGroup
     * @param cfn4
     * @param actCode
     * @return modifizierten CFN4-Wert für die Personengruppe und den Wegezweck
     */
    @SuppressWarnings("unused")
    private double manipulateCNF4ByPersongroup(TPS_ActivityConstant confSel, TPS_PersonGroup personGroup, double cfn4, TPS_ActivityConstant actCode) {
        /*
         * TODO @Rita
         *
         * Wenn wir hier die Manipulation im Quellcode lassen ist dass unheimlich unflexibel! Aus diesem Grund wird die
         * spezielle Manipulation in eine Datei/Tabelle(DB) ausgelagert, von wo aus sie eingelsen bzw abgefragt werden
         * kann anhand der in der Methode übergebenen Parameter.
         */

        return cfn4;
    }

    /**
     * Calculates the travel time increase resulting from the entering of a toll zone or paying a parking fee; transfer
     * of generalized costs into time via the vots
     *
     * @param plan            the plan for this call
     * @param beelineDistance beeline distance
     * @param actCode         activity code
     * @param durationStay    duration of the stay
     * @param comingFromTVZ   traffic analysis zone of the prior stay
     * @param goingToTVZ      traffic analysis zone of the subsequent stay
     * @return travel time increase in seconds
     */
    double relocateLocationMIVArr(TPS_Plan plan, double beelineDistance, TPS_ActivityConstant actCode, double durationStay, TPS_TrafficAnalysisZone comingFromTVZ, TPS_TrafficAnalysisZone goingToTVZ) {
        double costs = 0.0;
        double resultingTravelTimePlus = 0.0;
        // determining if the destination traffic analysis zone requires paying a toll (not if trip is starting within
        // the
        // toll zone)
        if (goingToTVZ.getSimulationTypeValues(SimulationType.SCENARIO).hasToll() &&
                !comingFromTVZ.getSimulationTypeValues(SimulationType.SCENARIO).hasToll()) {// toll has to be payed
            // -->
            // cordon!
            costs += goingToTVZ.getSimulationTypeValues(SimulationType.SCENARIO).getFeeToll();
        }
        if (goingToTVZ.getSimulationTypeValues(SimulationType.SCENARIO).hasParkingFee()) {
            double parkingCosts = goingToTVZ.getSimulationTypeValues(SimulationType.SCENARIO).getFeeParking();
            if (parkingCosts > 0) {
                double stayingHours = durationStay * 2.7777777777e-4;// converting seconds into hours
                parkingCosts = parkingCosts * stayingHours;
            }
            costs += parkingCosts;
            if (TPS_Logger.isLogging(SeverenceLogLevel.FINE)) {
                TPS_Logger.log(SeverenceLogLevel.FINE, "GoingTo hast parkingfee of" +
                        goingToTVZ.getSimulationTypeValues(SimulationType.SCENARIO).getFeeParking() + "; duration is " +
                        durationStay + "; parkingcosts: " + parkingCosts + "; sum resulting costs " + costs);
            }
        }
        if (costs > 0.0) {
            resultingTravelTimePlus = calculateVOTadds(costs, plan);
            if (TPS_Logger.isLogging(SeverenceLogLevel.FINER)) {
                TPS_Logger.log(SeverenceLogLevel.FINER,
                        "taz (id=" + goingToTVZ.getTAZId() + ") with costs:" + costs + " and values: " +
                                goingToTVZ.getSimulationTypeValues(SimulationType.SCENARIO));
            }
        }

        return resultingTravelTimePlus;
    }

    /**
     * Calculates the travel time increase resulting from the leaving of a toll zone ; transfer of generalized costs
     * into time via the vots
     *
     * @param plan            the plan for this call
     * @param beelineDistance beeline distance
     * @param actCode         activity code
     * @param durationStay    duration of the stay
     * @param comingFromTVZ   traffic analysis zone of the prior stay
     * @param goingToTVZ      traffic analysis zone of the subsequent stay
     * @return travel time increase in seconds
     */
    double relocateLocationMIVDep(TPS_Plan plan, double beelineDistance, TPS_ActivityConstant actCode, double durationStay, TPS_TrafficAnalysisZone comingFromTVZ, TPS_TrafficAnalysisZone goingToTVZ) {
        double costs = 0.0;
        double resultingTravelTimePlus = 0.0;
        // determining if toll as to be paid because subsequent stay is outside of toll area
        if (comingFromTVZ.getSimulationTypeValues(SimulationType.SCENARIO).hasToll() &&
                !goingToTVZ.getSimulationTypeValues(SimulationType.SCENARIO).hasToll()) {// toll has to be payed -->
            // cordon!
            costs += comingFromTVZ.getSimulationTypeValues(SimulationType.SCENARIO).getFeeToll();
        }
        if (costs > 0.0) {
            resultingTravelTimePlus = calculateVOTadds(costs, plan);
            if (TPS_Logger.isLogging(SeverenceLogLevel.FINER)) {
                TPS_Logger.log(SeverenceLogLevel.FINER,
                        "taz (id=" + goingToTVZ.getTAZId() + ") with costs:" + costs + " and values:" +
                                goingToTVZ.getSimulationTypeValues(SimulationType.SCENARIO));
            }
        }

        return resultingTravelTimePlus;
    }

    /**
     * This method returns a location for the default activity on this location.
     * If no default location can be found, the person is forced to drive home. The original activity code is restored afterwards.
     *
     * @param plan        The plan to work on
     * @param pc          planning context
     * @param locatedStay the stay for this location
     * @return a default activity location
     */
    TPS_Location selectDefaultLocation(TPS_Plan plan, TPS_PlanningContext pc, TPS_LocatedStay locatedStay) {
        TPS_ActivityConstant actCode = locatedStay.getEpisode().getActCode();
        if (actCode.hasAttribute(TPS_ActivityConstantAttribute.DEFAULT)) {
            TPS_Logger.log(SeverenceLogLevel.ERROR,
                    "Can't continue without locations for at least the default activity " +
                            this.PM.getParameters().getIntValue(ParamValue.DEFAULT_ACT_CODE_ZBE) + ". Driving home!");
            // no location for the default activity: drive home!
            return plan.getPerson().getHousehold().getLocation();
        }
        // if no location for the act type can be found; the search is done again with a default act type
        locatedStay.getEpisode().setActCode(TPS_ActivityConstant.DEFAULT_ACTIVITY);
        TPS_Location loc = selectLocation(plan, pc, locatedStay);
        locatedStay.getEpisode().setActCode(actCode);
        return loc;
    }

    /**
     * TODO Rita translate
     * <p>
     * Wenn in der Zone eine Location zu dieser Aktivität gefunden wurde, werden im Folgenden pModeArr und pModeDep der
     * Map weightModes hinzugefügt, wenn sie noch nicht dort enthalten sind. Sind sie bereits enthalten, wird lediglich
     * der entsprechende weight-Wert zu diesem Mode in weightModes der aktuelle weight-Wert dieser Zone weightC addiert.
     * Am Ende beinhaltet die Map weightModes alle für diese act in allen Zonen angebotenen Modes und deren aufaddierten
     * Gewichte.
     * <p>
     * Nun werden die Gewichte im Vektor weightMode skaliert, in dem sie für alle Elemente der Map zu sumWeights
     * aufsummiert und anschließend durch sumWeights dividiert werden.
     * <p>
     * pLocC wird nun die oben gesetzte repr. Location für eine Zone, mit der als erstes die sog. Beeline-Entfernung vom
     * comingFrom und zum GoingTo ermittelt wird. Nun wird für alle Mode-Elemente der Map weightModes die Reisezeit vom
     * comingFrom und zum goingTo errechnet und mit dem entsprechenden Gewicht multipliziert. Abschließend wir das Paar
     * (isBez92C, ttArr + ttDep) dem Vektor seqZoneQ hinzugefügt.
     * <p>
     * Zuletzt wird mit draeGeometric idBez92C gesetzt. Die myPLocationRepr der idBez92C entsprechenden Zone wird
     * returned - oder null.
     *
     * @param plan        The plan to use
     * @param pc          planning context
     * @param locatedStay the located stay we are coming from
     * @return the selected location
     */
    public TPS_Location selectLocation(TPS_Plan plan, TPS_PlanningContext pc, TPS_LocatedStay locatedStay) {
        //if this is a working trip and the location is given select it!
        int primaryLocId = plan.getPerson().getWorkLocationID();
        TPS_ActivityConstant actCode = locatedStay.getStay().getActCode();
        //is it set?
        if (primaryLocId >= 0) {
            if (actCode.isFix()) {
                TPS_Location primaryLoc = this.getLocation(primaryLocId); //get location
                if (primaryLoc != null) { // have we found a location?
                    return primaryLoc;
                }
            }
        }

        // call occurs only in case of a tour part!
        // check for existent location types for a specific activity code
        Collection<TPS_LocationConstant> connectedLocCodesToActCode = TPS_TrafficAnalysisZone.ACTIVITY2LOCATIONS_MAP
                .get(actCode);
        //TPS_AbstractConstant.getConnectedConstants(actCode, TPS_LocationCode.class);
        if (connectedLocCodesToActCode == null || connectedLocCodesToActCode.isEmpty()) {
            TPS_Logger.log(SeverenceLogLevel.SEVERE,
                    "No location codes for activity code " + actCode.getId() + " " + "(ZBE:" +
                            actCode.getCode(TPS_ActivityCodeType.ZBE) + ") found");
            return this.selectDefaultLocation(plan, pc, locatedStay);
        }

        // get the location choice set
        TPS_RegionResultSet resultSet = this.getLocationChoiceSet().getLocationRepresentatives(plan, pc, locatedStay,
                this.PM.getParameters());
        //locComingFrom, td.getArrivalDuration(), locGoingTo, td.getDepartureDuration(), actCode,tourpart);
        //select a location from the choice set
        return this.getLocationSelectModel().selectLocationFromChoiceSet(resultSet, plan, pc, locatedStay);
    }

    /**
     * Returns the number of traffic analysis zones in this region
     *
     * @return number of traffic analysis zones
     */
    public int size() {
        return this.TAZ_Map.size();
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
        sb.append(prefix + "TAPAS Region\n");
        for (TPS_TrafficAnalysisZone taz : this.TAZ_Map.values()) {
            sb.append(" ").append(prefix).append(taz.toString()).append("\n");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}
