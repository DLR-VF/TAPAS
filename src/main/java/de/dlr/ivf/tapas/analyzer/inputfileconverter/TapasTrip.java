package de.dlr.ivf.tapas.analyzer.inputfileconverter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Representation of a trip in a tapas style.
 */
public class TapasTrip {
	/**
	 * Sex defines an enumeration to realize type-safe constant declaration
	 */
	public enum Sex {
		FEMALE(2), MALE(1);
		/**
		 * Converts an integer to a value of Sex
		 * 
		 * @param i_sex
		 *            Code for sex (cf. declaration)
		 * @return Code converted to enumeration member
		 * @throws IllegalArgumentException
		 */
		public static Sex convert(int i_sex) throws IllegalArgumentException {
			for (Sex current : values()) {
				if (current.m_sex == i_sex)
					return current;
			}
			throw new IllegalArgumentException();
		}

		/** internal value of sex (needed to explicitly assign codes) */
		private final int m_sex;

		/** internal use only */
		Sex(int i_sex) {
			m_sex = i_sex;
		}
	}

	/**
	 * mandatory headers in trip table
	 */
	public static final Set<String> dbheaders_trip;
	/**
	 * mandatory headers in person table
	 */
	public static final Set<String> dbheaders_person;
	/**
	 * mandatory headers in household table
	 */
	public static final Set<String> dbheaders_household;
	static {
		String[] ps = { "p_id", "p_group", "p_hh_id", "p_key" };
		String[] tr = { "travel_time_sec", "mode", "distance_bl_m",
				"distance_real_m", "activity", "is_home", "p_id", "hh_id",
				"start_time_min" };
		String[] hs = { "hh_id", "hh_key" };

		dbheaders_trip = new HashSet<>(Arrays.asList(tr));
		dbheaders_person = new HashSet<>(Arrays.asList(ps));
		dbheaders_household = new HashSet<>(Arrays.asList(hs));

	}

	public enum header {
		TAZ_ID_START("bez92Dep", "taz_id_start"), //
		TAZ_ID_END("bez92Arr", "taz_id_end"), //
		LOC_ID_START("locIdStart", "loc_id_start"), //
		LOC_ID_END("locIdEnd", "loc_id_end"), //
		BBR_TYPE_START("bbrComing", "taz_bbr_type_start"), //
		BBR_TYPE_HOME("bbr_type_home"),
		DIST("dist", "distance_bl_m"), //
		DIST_NET("distNet", "distance_real_m"), //
		MODE_ID("id_mode", "mode"), //
		PERSON_ID("idPers", "p_id"), //
		HH_ID("idHh", "hh_id"), //
		JOB_ID("job", "p_group"), //
		START_TIME("startT", "start_time_min"), //
		TRAVEL_TIME("tt", "travel_time_sec"), //
		HOME("backHome", "is_home"), //
		ACTIVITY_CODE("actCode", "activity"),//
		ACTIVITY_START("actStart", "activity_start_min"),//
		ACTIVITY_DURATION("actDur", "activity_duration_min");//

		private final HashSet<String> headerSet;

		header(String... headers) {
			headerSet = new HashSet<>();
			headerSet.addAll(Arrays.asList(headers));
		}

		public static header fromString(String s) {

			for (header h : values()) {
				if (h.headerSet.contains(s))
					return h;
			}

			return null;

		}
	}

	private int activityCode;
	private int activityStart;
	private int activityDuration;
	private int bbrTypeStart;
	private int bbrTypeHome;
	private int tazIdStart;
	private int tazIdEnd;
	private int locIdStart;
	private int locIdEnd;
//	private double comingX;
//	private double comingY;
	private double distBL;
	private double dist;
	private double goingX;
	private double goingY;
	private int mode;
	private int pId;
	private int hhId;
	private int job;
	private int startTime;
	private double travelTime;
	private boolean isBackHome;

	private static String[] keyMapping;

	// final da diese Objekte nicht geändert werden können, keine Attribute
	// auslassen, um "Verschiebefehler" schnell zu finden

	/** Previous trip of the same person to get where trip starts */
	private TapasTrip m_prevTrip;

	/**
	 * Used with setValues. {@link Deprecated}
	 */
	public TapasTrip() {
	}

	public TapasTrip(int activityCode, int activityStart, int activityDuration, int bbrTypeStart, int bbrTypeHome,
			int tazIdStart, int tazIdEnd, int locIdStart, int locIdEnd, double distBL, double dist, int mode,
			int pId, int hhId, int job, int startTime, double travelTime,
			boolean isBackHome) {
		this.activityCode = activityCode;
		this.activityStart = activityStart;
		this.activityDuration = activityDuration;
		this.bbrTypeStart = bbrTypeStart;
		this.bbrTypeHome = bbrTypeHome;
		this.tazIdStart = tazIdStart;
		this.tazIdEnd = tazIdEnd;
		this.locIdStart = locIdStart;
		this.locIdEnd = locIdEnd;
		
		this.distBL = distBL;
		this.dist = dist;
		this.mode = mode;
		this.pId = pId;
		this.hhId = hhId;
		this.job = job;
		this.startTime = startTime;
		this.travelTime = travelTime;
		this.isBackHome = isBackHome;
	}

	public static void generateKeyMap(String[] headerArray) {
		keyMapping = headerArray;
	}

	/**
	 * @return the actCode
	 */
	public int getActCode() {
		return activityCode;
	}

	public int getActStart() {
		return activityStart;
	}
	
	public int getActDur() {
		return activityDuration;
	}

	public int getBbrGemTypeComing() {
		return bbrTypeStart;
	}

	public int getBbrTypeHome() {
		return bbrTypeHome;
	}

	public int getBez92Arr() {
		return tazIdEnd;
	}

	public int getBez92Dep() {
		return tazIdStart;
	}
	
	public int getLocIdStart() {
		return locIdStart;
	}

	public int getLocIdEnd() {
		return locIdEnd;
	}

//	public double getComingX() {
//		return comingX;
//	}
//	public double getComingY() {
//		return comingY;
//	}

	/**
	 * @return the dist
	 */
	public double getDistBL() {
		return distBL;
	}

	public double getDistNet() {
		return dist;
	}

	public double getGoingX() {
		return goingX;
	}
	
	public double getGoingY() {
		return goingY;
	}

	public int getIdMode() {
		return mode;
	}

	public int getIdPers() {
		return pId;
	}
	
	public int getIdHh() {
		return hhId;
	}

	public int getJob() {
		return job;
	}

	/**
	 * 
	 * @return the actcode of the previous trip
	 */
	public int getPreviousActCode() {
		return m_prevTrip.getActCode();

	}

	/**
	 * @return the prevTrip
	 */
	public TapasTrip getPrevTrip() {
		return m_prevTrip;
	}

	public double getStartTime() {
		return startTime;
	}

	/**
	 * @return the travel time
	 */
	public double getTT() {
		return travelTime;
	}

	/**
	 * @return the backHome
	 */
	public boolean isBackHome() {
		return isBackHome;
	}

	/**
	 * 
	 * @return true, when strip starts at home, false else
	 */
	public boolean isFromHome() {
		// falls kein Vorgänger existiert oder Vorgänger nach Hause führte,
		// starten wir von zuhause
		return (m_prevTrip == null) || m_prevTrip.isBackHome();
	}

	public void setPrevTrip(TapasTrip prevTrip) {
		this.m_prevTrip = prevTrip;
	}

	/**
	 * Cannot be used before {@link PersonFileElement#generateKeyMap(String[])}
	 * {@link Deprecated}
	 * 
	 * @param values
	 */
	public void setValues(String[] values) {
		if (keyMapping == null)
			throw new IllegalArgumentException(
					"KeyMapping must be generated first.");

		for (int i = 0; i < keyMapping.length; i++) {
			String s = keyMapping[i];
			header h = header.fromString(s);
			if (h == null) {
				continue; // unknown header ignored
			}

			switch (h) {
			case TAZ_ID_START:
				tazIdStart = Integer.parseInt(values[i]);
				break;
			case TAZ_ID_END:
				tazIdEnd = Integer.parseInt(values[i]);
				break;
			case LOC_ID_START:
				locIdStart = Integer.parseInt(values[i]);
				break;
			case LOC_ID_END:
				locIdEnd = Integer.parseInt(values[i]);
				break;
			case BBR_TYPE_START:
				bbrTypeStart = Integer.parseInt(values[i]);
				break;
			case BBR_TYPE_HOME:
				bbrTypeHome = Integer.parseInt(values[i]);
				break;
			case DIST:
				distBL = Double.parseDouble(values[i]);
				break;
			case DIST_NET:
				dist = Double.parseDouble(values[i]);
				break;
			case MODE_ID:
				mode = Integer.parseInt(values[i]);
				break;
			case PERSON_ID:
				pId = Integer.parseInt(values[i]);
				break;
			case HH_ID:
				hhId = Integer.parseInt(values[i]);
				break;
			case JOB_ID:
				job = Integer.parseInt(values[i]);
				break;
			case START_TIME:
				startTime = Integer.parseInt(values[i]);
				break;
			case TRAVEL_TIME:
				travelTime = Double.parseDouble(values[i]);
				break;
			case HOME:
				isBackHome = Boolean.parseBoolean(values[i]);
				break;
			case ACTIVITY_CODE:
				activityCode = Integer.parseInt(values[i]);
				break;
			case ACTIVITY_START:
				activityCode = Integer.parseInt(values[i]);
				break;
			case ACTIVITY_DURATION:
				activityCode = Integer.parseInt(values[i]);
				break;
			}
		}
	}

	/**
	 * @return
	 */
	public TapasTrip copy() {

		TapasTrip t = new TapasTrip(activityCode, activityStart, activityDuration, bbrTypeStart, bbrTypeHome,
				tazIdStart, tazIdEnd, locIdStart, locIdEnd, distBL, dist, mode, pId, hhId, job, startTime,
				travelTime, isBackHome);

		t.m_prevTrip = this.m_prevTrip;
		return t;
	}

	@Override
	public String toString() {
		return "(ID:" + getIdPers() + ", Job:" + getJob() + ", Act:"
				+ getActCode() + ", isBH:" + isBackHome() + ", isFH:"
				+ isFromHome() + ")";
	}
}
