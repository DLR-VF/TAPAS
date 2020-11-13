/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.inputfileconverter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum TripIntention {
    // Wegezweck
    TRIP_31(0, "Schule", generateSet(Activity.ACT_410, Activity.ACT_499)), //
    TRIP_38(1, "Studium", generateSet(Activity.ACT_411)), //
    TRIP_32(2, "Arbeit", generateSet(Activity.ACT_62, Activity.ACT_211)), //
    TRIP_33(3, "Private Erledigungen",
            generateSet(Activity.ACT_10, Activity.ACT_12, Activity.ACT_32, Activity.ACT_522, Activity.ACT_611,
                    Activity.ACT_631)), //
    TRIP_34(4, "Einkauf", generateSet(Activity.ACT_50)), //
    TRIP_35(5, "Freizeit",
            generateSet(Activity.ACT_231, Activity.ACT_300, Activity.ACT_511, Activity.ACT_512, Activity.ACT_531,
                    Activity.ACT_533, Activity.ACT_640, Activity.ACT_700, Activity.ACT_711, Activity.ACT_720,
                    Activity.ACT_721, Activity.ACT_722, Activity.ACT_723, Activity.ACT_724, Activity.ACT_800)), //
    TRIP_36(6, "Sonstige", generateSet(Activity.ACT_740, Activity.ACT_799)), //
    TRIP_37(7, "BackHome", generateSet()), //
    TRIP_MISC(8, "Nicht zugeordnet", generateSet());//

    private final int id;
    private final String caption;
    private final Set<Activity> actingCodes;

    TripIntention(int id, String caption, Set<Activity> actingCodes) {
        this.id = id;
        this.caption = caption;
        this.actingCodes = actingCodes;

    }

    private static Set<Activity> generateSet(Activity... activities) {
        return new HashSet<>(Arrays.asList(activities));
    }

    /**
     * @param act
     * @return den Wegezweck der diese Activity enthält, null wenn die activity
     * null ist oder die Activity keinem Wegezweck zugeordnet ist
     */
    public static TripIntention getByActivity(Activity act) {
        // if (act == null)
        // return null;
        for (TripIntention ti : TripIntention.values()) {
            if (ti.getActingCodes().contains(act)) {
                return ti;
            }
        }
        return null;
    }

    /**
     * @param id
     * @return den Wegezweck der dieser ID zugeordnet ist
     * @throws IllegalArgumentException geworfen wenn die ID keinem Wegezweck zugeordnet werden
     *                                  konnte
     */
    public static TripIntention getById(int id) throws IllegalArgumentException {
        for (TripIntention ti : TripIntention.values()) {
            if (ti.getId() == id) {
                return ti;
            }
        }

        throw new IllegalArgumentException();
    }

    public Set<Activity> getActingCodes() {
        return actingCodes;
    }

    public String getCaption() {
        return caption;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return caption;
    }
}
