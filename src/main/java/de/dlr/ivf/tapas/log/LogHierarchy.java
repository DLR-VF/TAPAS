package de.dlr.ivf.tapas.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.dlr.ivf.tapas.TPS_Main;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;

/**
 * With this annotation you can select a HierarchyLogLevel for a complete class. For usage see TPS_Main.
 * If this annotation is included you can call the short forms of the log method in TPS_Logger without the
 * explicit parameter of the HierarchyLogLevel.
 * 
 * @see TPS_Logger
 * @see TPS_Main
 * 
 * @author mark_ma
 * 
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
public @interface LogHierarchy {
	
	/**
	 * @return HierarchyLogLevel for this instance
	 */
	HierarchyLogLevel hierarchyLogLevel() default HierarchyLogLevel.OFF;
}