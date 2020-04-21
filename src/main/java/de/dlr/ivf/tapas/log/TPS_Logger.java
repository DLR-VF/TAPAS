package de.dlr.ivf.tapas.log;

import java.util.LinkedList;
import java.util.List;

import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

/**
 * Basic class for the TAPAS loging functionality.
 * @author hein_mh
 *
 */
public class TPS_Logger {

	/**
	 * An item to log. Consists of calling class, HierarchyLogLevel, SeverenceLogLevel, a String and an Exception
	 * @author hein_mh
	 *
	 */
	private static class LogItem {
		Class<?> callerClass;
		HierarchyLogLevel hLog;
		SeverenceLogLevel sLog;
		String text;
		Throwable throwable;

		private LogItem(Class<?> callerClass, HierarchyLogLevel hLog, SeverenceLogLevel sLog, String text,
				Throwable throwable) {
			this.callerClass = callerClass;
			this.hLog = hLog;
			this.sLog = sLog;
			this.text = text;
			this.throwable = throwable;
		}

	}

	/**
	 * The list with the items to log
	 */
	private static List<LogItem> LOG_ITEMS = new LinkedList<>();

	private static HierarchyLogLevel HIERARCHY_LOG_LEVEL_MASK;

	private static SeverenceLogLevel SEVERENCE_LOG_LEVEL_MASK;

	private static TPS_LoggingInterface LOGGING;

	/**
	 * Method to return the calling class for this function.
	 * @return The calling class.
	 */
	private static Class<?> getCallerClass() {
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		Class <?> cl = null;
		if(stackTraceElements!=null && stackTraceElements.length>=3){
			try {
				cl = Class.forName(stackTraceElements[2].getClassName());
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
				
		return cl;
	}

	/**
	 * Method to get the default HierarchyLogLevel for the provided class. 
	 * This is set by the "LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.EPISODE)" tag on top of the classes.
	 * If the tag is missing HierarchyLogLevel.ALL is returned to avoid a NullPointerException during logging.
	 * 
	 * @param callerClass The calling class
	 * @return The specified HierarchyLogLevel
	 */
	private static HierarchyLogLevel getHierarchyLogLevel(Class<?> callerClass) {
		LogHierarchy tmp = callerClass.getAnnotation(LogHierarchy.class);
		if(tmp!=null){
			return tmp.hierarchyLogLevel();
		}
		else{
			return HierarchyLogLevel.ALL;
		}
	}

	/**
	 * Method to check if logging is enabled for the given parameters.
	 * @param callerClass The calling Class
	 * @param hLog The HierarchyLogLevel
	 * @param sLog The SeverenceLogLevel
	 * @return true if logging is enabled for the given combination
	 */
	public static boolean isLogging(Class<?> callerClass, HierarchyLogLevel hLog, SeverenceLogLevel sLog) {
		if (LOGGING == null) {
			return true; // TODO: why true?
		} else if (shallBeLogged(hLog) && shallBeLogged(sLog)) {
			return LOGGING.isLogging(callerClass, sLog);
		}
		return false;
	}

	public static boolean isLogging(Class<?> callerClass, SeverenceLogLevel sLog) {
		if(!shallBeLogged(sLog)) {
			return false;
		}
		return isLogging(callerClass, getHierarchyLogLevel(callerClass), sLog);
	}

	/**
	 * Method to check if logging is enabled for the calling class with the given parameters.
	 * @param hLog The HierarchyLogLevel
	 * @param sLog The SeverenceLogLevel
	 * @return true if logging is enabled for the given combination
	 */
	public static boolean isLogging(HierarchyLogLevel hLog, SeverenceLogLevel sLog) {
		if(!shallBeLogged(hLog)||!shallBeLogged(sLog)) {
			return false;
		}
		return isLogging(getCallerClass(), hLog, sLog);
	}

	/**
	 * Method to check if logging is enabled for the calling class at the default HierarchyLogLevel with the given SeverenceLogLevel.
	 * @param sLog The SeverenceLogLevel
	 * @return true if logging is enabled for the given combination
	 */	
	public static boolean isLogging(SeverenceLogLevel sLog) {
		if(!shallBeLogged(sLog)) {
			return false;
		}
		return isLogging(getCallerClass(), sLog);
	}


	public static void log(Class<?> callerClass, HierarchyLogLevel hLog, SeverenceLogLevel sLog, String text) {
		if (LOGGING == null) {
			LOG_ITEMS.add(new LogItem(callerClass, hLog, sLog, text, null));
//			System.out.println("Stored log: "+text);
		} else if (shallBeLogged(hLog) && shallBeLogged(sLog)) {
			LOGGING.log(callerClass, sLog, text);
		}
	}


	public static void log(Class<?> callerClass, HierarchyLogLevel hLog, SeverenceLogLevel sLog, String text, Throwable throwable) {
		if(!shallBeLogged(sLog)) {
			return;
		}
		for(int i =0; i< throwable.getStackTrace().length; ++i){
			text = text.concat("\n"+throwable.getStackTrace()[i].toString());
		}
		if (LOGGING == null) {
			LOG_ITEMS.add(new LogItem(callerClass, hLog, sLog, text, throwable));
//			System.out.println("Stored log: "+text);
//			throwable.printStackTrace();
		} else if (shallBeLogged(hLog) && SEVERENCE_LOG_LEVEL_MASK.includes(sLog)) {
			LOGGING.log(callerClass, sLog, text, throwable);
		}
	}

	/**
	 * Method to log an info.
	 * @param callerClass The calling class
	 * @param sLog The SeverenceLogLevel
	 * @param text The text to log
	 */
	public static void log(Class<?> callerClass, SeverenceLogLevel sLog, String text) {
		if(!shallBeLogged(sLog)) {
			return;
		}
		log(callerClass, getHierarchyLogLevel(callerClass), sLog, text);
	}

	/**
	 * Method to log an info and parse an Exception.
	 * @param callerClass The calling class
	 * @param sLog The SeverenceLogLevel
	 * @param text The text to log
	 * @param throwable The exception to log
	 */
	public static void log(Class<?> callerClass, SeverenceLogLevel sLog, String text, Throwable throwable) {
		if(!shallBeLogged(sLog)) {
			return;
		}
		log(callerClass, getHierarchyLogLevel(callerClass), sLog, text, throwable);
	}

	/**
	 * Method to log an info.
	 * @param hLog The HierarchyLogLevel
	 * @param sLog The SeverenceLogLevel
	 * @param text The text to log
	 */
	public static void log(HierarchyLogLevel hLog, SeverenceLogLevel sLog, String text) {
		if(!shallBeLogged(sLog)) {
			return;
		}
		log(getCallerClass(), hLog, sLog, text);
	}

	/**
	 * Method to log an info and parse an Exception.
	 * @param hLog The HierarchyLogLevel
	 * @param sLog The SeverenceLogLevel
	 * @param text The text to log
	 * @param throwable The exception to log
	 */
	public static void log(HierarchyLogLevel hLog, SeverenceLogLevel sLog, String text, Throwable throwable) {
		if(!shallBeLogged(sLog)) {
			return;
		}
		if(text==null) {
			text = "NULL-Exception";
		}
		log(getCallerClass(), hLog, sLog, text, throwable);
	}

	/**
	 * Method to log an info.
	 * @param sLog The SeverenceLogLevel
	 * @param text The text to log
	 */
	public static void log(SeverenceLogLevel sLog, String text) {
		if(!shallBeLogged(sLog)) {
			return;
		}
		log(getCallerClass(), sLog, text);
	}

	/**
	 * Method to log an info and parse an Exception.
	 * @param sLog The SeverenceLogLevel
	 * @param e The exception to log
	 */
	public static void log(SeverenceLogLevel sLog, Exception e) {
		if(!shallBeLogged(sLog)) {
			return;
		}
		String text = "";
		log(getCallerClass(), sLog, text);
	}	
	
	/**
	 * Method to log an info and parse an Exception.
	 * @param sLog The SeverenceLogLevel
	 * @param text The text to log
	 * @param throwable The exception to log
	 */
	public static void log(SeverenceLogLevel sLog, String text, Throwable throwable) {
		if(!shallBeLogged(sLog)) {
			return;
		}
		log(getCallerClass(), sLog, text, throwable);
	}

	/**
	 * This method sets a new instance of the logging class.
	 * @param loggingClass The Class, which holds the logging functionality.
	 * @param parameterClass parameter class reference
	 */
	public static void setLoggingClass(String loggingClass, TPS_ParameterClass parameterClass) {
		if (LOGGING == null) {
			LOGGING = new TPS_Log4jLogger(parameterClass);
			HIERARCHY_LOG_LEVEL_MASK = HierarchyLogLevel.valueOf(parameterClass.getString(ParamString.HIERARCHY_LOG_LEVEL_MASK));
			SEVERENCE_LOG_LEVEL_MASK = SeverenceLogLevel.valueOf(parameterClass.getString(ParamString.SEVERENCE_LOG_LEVEL_MASK));

			for (LogItem logItem : LOG_ITEMS) {
				if (logItem.throwable == null) {
					log(logItem.callerClass, logItem.hLog, logItem.sLog, logItem.text);
				} else {
					log(logItem.callerClass, logItem.hLog, logItem.sLog, logItem.text, logItem.throwable);
				}
			}
			LOG_ITEMS.clear();
			LOG_ITEMS = null;
		} else {
			TPS_Logger.log(HierarchyLogLevel.CLIENT, SeverenceLogLevel.WARN, "Try to set the log level a second time");
		}
	}
	
	/** @brief Returns whether the given severence level messages shall be logged
	 */
	public static boolean shallBeLogged(SeverenceLogLevel sLog) {
		return SEVERENCE_LOG_LEVEL_MASK==null || SEVERENCE_LOG_LEVEL_MASK.includes(sLog);
	}
	
	/** @brief Returns whether the given hierarchy level messages shall be logged
	 */
	public static boolean shallBeLogged(HierarchyLogLevel hLog) {
		return HIERARCHY_LOG_LEVEL_MASK==null || HIERARCHY_LOG_LEVEL_MASK.includes(hLog);
	}
	
	public static void closeLoggers(){
		LOGGING.closeLogger();
	}
}

	 