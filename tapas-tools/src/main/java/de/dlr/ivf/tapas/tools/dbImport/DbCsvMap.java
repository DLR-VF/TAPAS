/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.dbImport;

import java.util.HashMap;
import java.util.Map.Entry;

/**
 * This class is a map with that can be accessed by dbKey or csvKey. A
 * mapping between the two must be given at construction time.
 *
 * @author boec_pa
 */
public class DbCsvMap {
    public static final int DB2CSV = 0;
    public static final int CSV2DB = 1;

    private HashMap<String, String> dbMap;

    private final HashMap<String, String> db2csvMap;
    private final HashMap<String, String> csv2dbMap;

    /**
     * @param keyMapping a bijective mapping between dbKeys and csvKeys.
     * @param direction  either db2csv or vice versa.
     * @throws IllegalArgumentException if the given key mapping is not bijective.
     */
    public DbCsvMap(HashMap<String, String> keyMapping, int direction) {
        if (DB2CSV == direction) {
            db2csvMap = new HashMap<>(keyMapping);
            if (null == (csv2dbMap = invert(keyMapping))) {
                throw new IllegalArgumentException("The key mapping has to be bijective.");
            }
        } else if (CSV2DB == direction) {
            csv2dbMap = new HashMap<>(keyMapping);
            if (null == (db2csvMap = invert(keyMapping))) {
                throw new IllegalArgumentException("The key mapping has to be bijective.");
            }
        } else {
            throw new IllegalArgumentException("Specified direction unknown.");
        }
    }

    public String getByCSVKey(String csvKey) {
        if (!csv2dbMap.containsKey(csvKey)) throw new IllegalArgumentException("The given csv key is unknown.");
        return dbMap.get(csv2dbMap.get(csvKey));
    }

    public String getByDBKey(String dbKey) {
        if (!db2csvMap.containsKey(dbKey)) throw new IllegalArgumentException("The given database key is unknown.");
        return dbMap.get(dbKey);
    }

    public String getCSVKey(String dbKey) {
        return db2csvMap.get(dbKey);
    }

    public String getDBKey(String csvKey) {
        return csv2dbMap.get(csvKey);
    }

    private HashMap<String, String> invert(HashMap<String, String> map) {
        HashMap<String, String> invMap = new HashMap<>();
        for (Entry<String, String> e : map.entrySet()) {
            if (null != invMap.put(e.getValue(), e.getKey())) {
                return null;
            }
        }
        return map;
    }

    public String putByCSVKey(String csvKey, String value) {
        if (!csv2dbMap.containsKey(csvKey)) throw new IllegalArgumentException("The given csv key is unknown.");
        return dbMap.put(csv2dbMap.get(csvKey), value);
    }

    public String putByDBKey(String dbKey, String value) {
        if (!db2csvMap.containsKey(dbKey)) throw new IllegalArgumentException("The given database key is unknown.");
        return dbMap.put(dbKey, value);
    }

}