package de.dlr.ivf.tapas.util;


/**
 * Class to read the variable Attributes for this plan. 
 * This class contains the enum singeltons TPS_Attribute, which holds all household, person and plan attributes needed for modelling.
 * Reading and writing this attributes is performed synchronised. 
 * @author mark_ma
 * 
 */
public class TPS_AttributeReader {

	/**
	 * Enum set of TPS_Attribute needed for modelling, including a Map of attribute values for each thread. 
	 * @author hein_mh
	 *
	 */
	public enum TPS_Attribute {
		CURRENT_DISTANCE_CLASS_CODE_MCT,
		CURRENT_DISTANCE_CLASS_CODE_VOT,
		CURRENT_EPISODE_ACTIVITY_CODE_MCT,
		CURRENT_EPISODE_ACTIVITY_CODE_TAPAS,
		CURRENT_EPISODE_ACTIVITY_CODE_VOT,
		CURRENT_MODE_CODE_VOT,
		CURRENT_TAZ_SETTLEMENT_CODE_TAPAS,
		HOUSEHOLD_CARS,
		HOUSEHOLD_INCOME_CLASS_CODE,
		PERSON_AGE,
		PERSON_AGE_CLASS_CODE_PERSON_GROUP,
		// change to FSO also in files
		PERSON_AGE_CLASS_CODE_STBA,
		PERSON_DRIVING_LICENSE_CODE,
		PERSON_SEX_CLASS_CODE,
		PERSON_HAS_BIKE

    }
}
