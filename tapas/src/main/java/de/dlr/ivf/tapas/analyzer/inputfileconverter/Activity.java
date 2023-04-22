/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.inputfileconverter;

public enum Activity {
    ACT_10(10), //HOUSEWORK_AT_HOME, code_tapas: 1
    ACT_12(12), //E_COMMERCE_OUT_OF_HOME, code_tapas: 0
    ACT_32(32), //HOUSEWORK_OUT_OF_HOME, code_tapas: 0
    ACT_50(50), //SHOPPING, code_tapas: 3
    ACT_51(51), //SHOPPING SHORT TERM, code_tapas: 3
    ACT_52(52), //SHOPPING MID TERM, code_tapas: 3
    ACT_53(53), //SHOPPING LONG TERM, code_tapas: 3
    ACT_54(54), //SHOPPING RITA, code_tapas: 3
    ACT_55(55), //SHOPPING RITA, code_tapas: 3
    ACT_62(62), //JOB_SEEKING, code_tapas: 1
    ACT_211(211), //WORKING, code_tapas: 1
    ACT_212(212), //WORKING FULL TIME
    ACT_213(213), //WORKING PART TIME
    ACT_231(231), //CONVERSATION_ABOUT_WORK, code_tapas: 0
    ACT_299(299), //FREETIME_ACTIVE_AT_HOME, code_tapas: 0
    ACT_300(300), //VOLUNTARY_WORK, code_tapas: 5
    ACT_410(410), //SCHOOL, code_tapas: 2
    ACT_411(411), //UNIVERSITY, code_tapas: 7
    ACT_412(412), //TRAINEE
    ACT_413(413), //KINDERGARDEN
    ACT_414(414), //QUALIFICATION
    ACT_499(499), //LUNCH_BREAK_PUPILS, code_tapas: 2
    ACT_511(511), //SLEEPING, code_tapas: 0
    ACT_512(512), //RELAXING, code_tapas: 0
    ACT_522(522), //PERSONAL_MATTERS, code_tapas: 4
    ACT_531(531), //EATING_AT_HOME, code_tapas: 0
    ACT_533(533), //LUNCH_CANTINE, code_tapas: 0
    ACT_611(611), //CONVERSATION_ABOUT_PERSONAL_MATTER, code_tapas: 4
    ACT_631(631), //VISITING, code_tapas: 4
    ACT_640(640), //EXCURSIONS, code_tapas: 5
    ACT_700(700), //FREETIME_AT_HOME, code_tapas: 0
    ACT_711(711), //TELEVIEWING, code_tapas: 0
    ACT_720(720), //DINING_OR_GOING_OUT, code_tapas: 5
    ACT_721(721), //SPORTS, code_tapas: 5
    ACT_722(722), //PROMENADING, code_tapas: 5
    ACT_723(723), //PLAYING, code_tapas: 5
    ACT_724(724), //BEING_AT_AN_EVENT, code_tapas: 5
    ACT_740(740), //FREETIME_ANY, code_tapas: 6
    ACT_799(799), //ACTIVITIES_ANY, code_tapas: 6
    ACT_800(800), //LEARNING_WITH_CHILDREN, code_tapas: 0
    ACT_880(880), //PLAYING WITH CHILDREN
    ACT_881(881); //PLAYING WITH CHILDREN AS A LOCATION

    private final int id;

    Activity(int id) {
        this.id = id;
    }

    /**
     * @param id
     * @return the activity corresponding to the id or null if there is none
     */
    public static Activity getById(int id) {
        for (Activity a : Activity.values()) {
            if (a.getId() == id) {
                return a;
            }
        }
        return null;
    }

    public int getId() {
        return id;
    }
}
