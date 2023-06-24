/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.logger.legacy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * With this annotation you can select a HierarchyLogLevel for a complete class. For usage see TPS_Main.
 * If this annotation is included you can call the short forms of the log method in TPS_Logger without the
 * explicit parameter of the HierarchyLogLevel.
 *
 * @author mark_ma
 * @see TPS_Logger
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
public @interface LogHierarchy {

    /**
     * @return HierarchyLogLevel for this instance
     */
    HierarchyLogLevel hierarchyLogLevel() default HierarchyLogLevel.OFF;
}