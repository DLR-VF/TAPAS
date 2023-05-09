/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.locationallocation;

import de.dlr.ivf.tapas.tools.locationallocation.LocationProcessor.Location;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class Locator {

    private static final double searchRadius = 500;
    private final boolean exactOnly;

    /**
     * @param exactOnly
     */
    public Locator(boolean exactOnly) {
        this.exactOnly = exactOnly;
    }

    /**
     * Checks for valid PLZ (5 digits) or list of plz (comma-seperated
     *
     * @param input the inputstring
     * @return the input if valid else null
     */
    private String checkPLZ(String input) {
        if (input == null || input.length() == 0) {
            return null;
        } else {
            String[] plzs = input.split(",");
            for (String plz : plzs) {
                //correct length?
                if (plz.length() != 5) return null;
                // we are in Berlin!
                if (plz.charAt(0) != '1') return null;
                for (int i = 0; i < plz.length(); ++i) {
                    switch (plz.charAt(i)) {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            break;
                        default:
                            return null;
                    }
                }
            }
            return input;
        }
    }

    /**
     * Method to extract the housenumber from the gioven string
     *
     * @param houseNumber
     * @return the number only without "a" "/1" or whatever
     */

    private String extractHouseNumber(String houseNumber) {
        String result = "";
        for (int i = 0; i < houseNumber.length(); ++i) {
            if (this.isIntNumeric(houseNumber.charAt(i))) {
                result = result + houseNumber.charAt(i);
            } else {
                // in the database for berlin the housenumbers are only numeric!
                break;
            }
        }
        return result;
    }

    private String extractHouseNumberAddOn(String houseNumber) {
        String result = "", tmp;
        int i = 0;
        // get the start of the add on, if any
        while (i < houseNumber.length() && this.isIntNumeric(houseNumber.charAt(i))) {
            ++i;
        }
        if (i < houseNumber.length()) {
            tmp = houseNumber.substring(i).trim();
            //
            for (i = 0; i < tmp.length(); ++i) {
                if (this.isAddOddChar(tmp.charAt(i))) {
                    result = result + tmp.charAt(i);
                } else {
                    break;
                }
            }
        }

        return result;
    }

    private void generateQueriesForAdressToCoordinate(State bundesland, String streetValue, String houseNumber, String plz, String houseNumberAddOn, String additionalConstraints, String additionalColumns, String order, HashMap<Integer, String> queries, int startSqlIndex) {
        String query;
        if (streetValue == null) return;

        if (additionalConstraints == null) additionalConstraints = "";
        if (additionalColumns == null) additionalColumns = "";
        if (order == null) order = "";

        StreetnameProcessor processor = new StreetnameProcessor();
        boolean whereAdded;
        // filter spaces in street names
        String tmp = processor.replaceStreetTag(streetValue.replaceAll(" ", ""));
        if (Constants.ENABLE_CHANGES_OF_MARCO) {

            if (houseNumberAddOn != null && houseNumberAddOn.length() > 0) {
                // Query is generated with all possible whereClauses, is a variable
                // set to null the clause will be not added.
                query = "select * from (" +
                        "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y " +
                        "	from core.berlin_buildings ";
                if (streetValue != null || plz != null || houseNumberAddOn != null || bundesland != null ||
                        houseNumber != null) query += "	where ";
                whereAdded = false;
                if (streetValue != null) {
                    query += "lower(strasse)='" + streetValue + "' ";
                    whereAdded = true;
                }

                if (whereAdded) query += "AND ";
                query += "lower(hausnummer)='" + houseNumber + "' ";
                whereAdded = true;

                if (houseNumberAddOn != null && houseNumberAddOn.length() > 0) {
                    if (whereAdded) query += "AND ";
                    query += "lower(hausnummerzusatz)='" + houseNumberAddOn + "' ";
                    whereAdded = true;
                }
                if (bundesland != null) {
                    if (whereAdded) query += "AND ";
                    query += "bundesland LIKE '" + bundesland.getGemeindeSchluessel() + "' ";
                    whereAdded = true;
                }
                // Sort by houseNumber-Difference
                if (houseNumber != null && houseNumber.length() > 0)
                    query += " ORDER BY ABS(CAST(hausnummer AS INTEGER)-" + houseNumber + ")";
                query += ") as foo";
                queries.put(startSqlIndex + 1, query);
            } else {
                // Query is generated with all possible whereClauses, is a variable
                // set to null the clause will be not added.
                query = "select * from (" +
                        "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y " +
                        "	from core.berlin_buildings ";
                if (streetValue != null || plz != null || houseNumberAddOn != null || bundesland != null ||
                        houseNumber != null) query += "	where ";
                whereAdded = false;
                if (streetValue != null) {
                    query += "lower(strasse)='" + streetValue + "' ";
                    whereAdded = true;
                }

                if (houseNumber != null && houseNumber.length() > 0) {
                    if (whereAdded) query += "AND ";
                    query += "lower(hausnummer)='" + houseNumber + "' ";
                    whereAdded = true;
                }

                if (whereAdded) query += "AND ";
                query += "lower(hausnummerzusatz)='' ";
                whereAdded = true;

                if (bundesland != null) {
                    if (whereAdded) query += "AND ";
                    query += "bundesland LIKE '" + bundesland.getGemeindeSchluessel() + "' ";
                    whereAdded = true;
                }
                // Sort by houseNumber-Difference
                if (houseNumber != null && houseNumber.length() > 0)
                    query += " ORDER BY ABS(CAST(hausnummer AS INTEGER)-" + houseNumber + ")";
                query += ") as foo";
                queries.put(startSqlIndex + 1, query);
            }
            // Query is generated with all possible whereClauses, is a variable
            // set to null the clause will be not added.
            query = "select * from (" +
                    "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y " +
                    "	from core.berlin_buildings ";
            if (streetValue != null || plz != null || houseNumberAddOn != null || bundesland != null ||
                    houseNumber != null) query += "	where ";
            whereAdded = false;
            if (streetValue != null) {
                query += "lower(strasse)='" + streetValue + "' ";
                whereAdded = true;
            }
            if (houseNumber != null && houseNumber.length() > 0) {
                if (whereAdded) query += "AND ";
                query += "lower(hausnummer)='" + houseNumber + "' ";
                whereAdded = true;
            }

            if (bundesland != null) {
                if (whereAdded) query += "AND ";
                query += "bundesland LIKE '" + bundesland.getGemeindeSchluessel() + "' ";
                whereAdded = true;
            }
            // Sort by houseNumber-Difference
            if (houseNumber != null && houseNumber.length() > 0)
                query += " ORDER BY ABS(CAST(hausnummer AS INTEGER)-" + houseNumber + ")";
            query += ") as foo";
            queries.put(startSqlIndex + 2, query);


            if (houseNumberAddOn != null && houseNumberAddOn.length() > 0) {
                // Query is generated with all possible whereClauses, is a variable
                // set to null the clause will be not added.
                query = "select * from (" +
                        "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y " +
                        "	from core.berlin_buildings ";
                if (streetValue != null || plz != null || houseNumberAddOn != null || bundesland != null ||
                        houseNumber != null) query += "	where ";
                whereAdded = false;
                if (streetValue != null) {
                    query += "lower(strasse)='" + streetValue + "' ";
                    whereAdded = true;
                }

                if (plz != null && plz.length() > 0) {
                    if (whereAdded) query += "AND ";
                    query += "lower(plz) LIKE ANY(array[" + plz + "]) ";
                    whereAdded = true;
                }

                if (houseNumber != null && houseNumber.length() > 0) {
                    if (whereAdded) query += "AND ";
                    query += "lower(hausnummer)='" + houseNumber + "' ";
                    whereAdded = true;
                }


                if (houseNumberAddOn != null && houseNumberAddOn.length() > 0) {
                    if (whereAdded) query += "AND ";
                    query += "lower(hausnummerzusatz)='" + houseNumberAddOn + "' ";
                    whereAdded = true;
                }
                if (bundesland != null) {
                    if (whereAdded) query += "AND ";
                    query += "bundesland LIKE '" + bundesland.getGemeindeSchluessel() + "' ";
                    whereAdded = true;
                }
                // Sort by houseNumber-Difference
                if (houseNumber != null && houseNumber.length() > 0)
                    query += " ORDER BY ABS(CAST(hausnummer AS INTEGER)-" + houseNumber + ")";
                query += ") as foo";
                queries.put(startSqlIndex + 3, query);
            } else {
                // Query is generated with all possible whereClauses, is a variable
                // set to null the clause will be not added.
                query = "select * from (" +
                        "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y " +
                        "	from core.berlin_buildings ";
                if (streetValue != null || plz != null || houseNumberAddOn != null || bundesland != null ||
                        houseNumber != null) query += "	where ";
                whereAdded = false;
                if (streetValue != null) {
                    query += "lower(strasse)='" + streetValue + "' ";
                    whereAdded = true;
                }

                if (plz != null && plz.length() > 0) {
                    if (whereAdded) query += "AND ";
                    query += "lower(plz) LIKE ANY(array[" + plz + "]) ";
                    whereAdded = true;
                }

                if (houseNumber != null && houseNumber.length() > 0) {
                    if (whereAdded) query += "AND ";
                    query += "lower(hausnummer)='" + houseNumber + "' ";
                    whereAdded = true;
                }


                if (whereAdded) query += "AND ";
                query += "lower(hausnummerzusatz)='' ";
                whereAdded = true;
                if (bundesland != null) {
                    if (whereAdded) query += "AND ";
                    query += "bundesland LIKE '" + bundesland.getGemeindeSchluessel() + "' ";
                    whereAdded = true;
                }
                // Sort by houseNumber-Difference
                if (houseNumber != null && houseNumber.length() > 0)
                    query += " ORDER BY ABS(CAST(hausnummer AS INTEGER)-" + houseNumber + ")";
                query += ") as foo";
                queries.put(startSqlIndex + 3, query);
            }
            // Query is generated with all possible whereClauses, is a variable
            // set to null the clause will be not added.
            query = "select * from (" +
                    "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y " +
                    "	from core.berlin_buildings ";
            if (streetValue != null || plz != null || houseNumberAddOn != null || bundesland != null ||
                    houseNumber != null) query += "	where ";
            whereAdded = false;
            if (streetValue != null) {
                query += "lower(strasse)='" + streetValue + "' ";
                whereAdded = true;
            }

            if (plz != null && plz.length() > 0) {
                if (whereAdded) query += "AND ";
                query += "lower(plz) LIKE ANY(array[" + plz + "]) ";
                whereAdded = true;
            }

            if (houseNumber != null && houseNumber.length() > 0) {
                if (whereAdded) query += "AND ";
                query += "lower(hausnummer)='" + houseNumber + "' ";
                whereAdded = true;
            }

            if (bundesland != null) {
                if (whereAdded) query += "AND ";
                query += "bundesland LIKE '" + bundesland.getGemeindeSchluessel() + "' ";
                whereAdded = true;
            }
            // Sort by houseNumber-Difference
            if (houseNumber != null && houseNumber.length() > 0)
                query += " ORDER BY ABS(CAST(hausnummer AS INTEGER)-" + houseNumber + ")";
            query += ") as foo";
            queries.put(startSqlIndex + 4, query);

        } else {
            String region;
            if (bundesland != null) region = " AND bundesland LIKE '" + bundesland.getGemeindeSchluessel() + "' ";
            else region = "";
            if (plz != null) {
                if (houseNumber != null) {
                    if (houseNumberAddOn != null && houseNumberAddOn.length() > 0) {
                        query = "select * from (" +
                                "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y " +
                                additionalColumns + "	from core.berlin_buildings " + "	where lower(strasse)='" +
                                streetValue + "' " + "   AND lower(plz)='" + plz + "' " + "   AND lower(hausnummer)='" +
                                houseNumber + "' " + "   AND lower(hausnummerzusatz)='" + houseNumberAddOn + "'" +
                                region + additionalConstraints + ") as foo " + order;
                        queries.put(startSqlIndex + 2, query);
                        // filtered street name
                        query = "select * from (" +
                                "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y " +
                                additionalColumns + "	from core.berlin_buildings " + "	where lower(strasse)='" +
                                tmp + "' " + "   AND lower(plz)='" + plz + "' " + "   AND lower(hausnummer)='" +
                                houseNumber + "'" + "   AND lower(hausnummerzusatz)='" + houseNumberAddOn + "'" +
                                region + additionalConstraints + ") as foo " + order;
                        queries.put(startSqlIndex + 3, query);
                    }
                    query = "select * from (" +
                            "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y " +
                            additionalColumns + "	from core.berlin_buildings " + "	where lower(strasse)='" +
                            streetValue + "' " + "   AND lower(plz)='" + plz + "' " + "   AND lower(hausnummer)='" +
                            houseNumber + "'" + region + additionalConstraints + ") as foo " + order;
                    queries.put(startSqlIndex + 4, query);
                    // filtered street name
                    query = "select * from (" +
                            "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y " +
                            additionalColumns + "	from core.berlin_buildings " + "	where lower(strasse)='" + tmp +
                            "' " + "   AND lower(plz)='" + plz + "' " + "   AND lower(hausnummer)='" + houseNumber +
                            "'" + region + additionalConstraints + ") as foo " + order;
                    queries.put(startSqlIndex + 5, query);
                }
                // house number does not exist: take one
                query = "select * from (" +
                        "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y " +
                        additionalColumns + "	from core.berlin_buildings " + "	where lower(strasse)='" +
                        streetValue + "' " + "   AND lower(plz)='" + plz + "' " + region + additionalConstraints +
                        ") as foo " + order;
                queries.put(startSqlIndex + 6, query);
                // filtered street name
                query = "select * from (" +
                        "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y " +
                        additionalColumns + "	from core.berlin_buildings " + "	where lower(strasse)='" + tmp +
                        "' " + "   AND lower(plz)='" + plz + "' " + region + additionalConstraints + ") as foo " +
                        order;
                queries.put(startSqlIndex + 7, query);
            }
            if (houseNumber != null) {
                if (houseNumberAddOn != null && houseNumberAddOn.length() > 0) {
                    query = "select * from (" +
                            "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y " +
                            additionalColumns + "	from core.berlin_buildings " + "	where lower(strasse)='" +
                            streetValue + "' " + "   AND lower(hausnummer)='" + houseNumber + "'" +
                            "   AND lower(hausnummerzusatz)='" + houseNumberAddOn + "'" + region +
                            additionalConstraints + ") as foo " + order;
                    queries.put(startSqlIndex + 8, query);

                    query = "select * from (" +
                            "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y " +
                            additionalColumns + "	from core.berlin_buildings " + "	where lower(strasse)='" + tmp +
                            "' " + "   AND lower(hausnummer)='" + houseNumber + "'" +
                            "   AND lower(hausnummerzusatz)='" + houseNumberAddOn + "'" + region +
                            additionalConstraints + ") as foo " + order;
                    queries.put(startSqlIndex + 9, query);
                }
                query = "select * from (" +
                        "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y " +
                        additionalColumns + "	from core.berlin_buildings " + "	where lower(strasse)='" +
                        streetValue + "' " + "   AND lower(hausnummer)='" + houseNumber + "'" + region +
                        additionalConstraints + ") as foo " + order;
                queries.put(startSqlIndex + 10, query);
                query = "select * from (" +
                        "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y " +
                        additionalColumns + "	from core.berlin_buildings " + "	where lower(strasse)='" + tmp +
                        "' " + "   AND lower(hausnummer)='" + houseNumber + "'" + region + additionalConstraints +
                        ") as foo " + order;
                queries.put(startSqlIndex + 11, query);
            }
            query = "select * from (" +
                    "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y " +
                    additionalColumns + "	from core.berlin_buildings " + "	where lower(strasse)='" + streetValue +
                    "'" + region + additionalConstraints + ") as foo " + order;
            queries.put(startSqlIndex + 12, query);
            query = "select * from (" +
                    "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y " +
                    additionalColumns + "	from core.berlin_buildings " + "	where lower(strasse)='" + tmp + "'" +
                    region + additionalConstraints + ") as foo " + order;
            queries.put(startSqlIndex + 13, query);
        }
    }

    /**
     * Method to check if a character is an numeric integer
     *
     * @param character the character to check
     * @return true if character is 0-9 or + or -
     */
    private boolean isAddOddChar(char character) {
        boolean result;
        switch (character) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case '-':
            case '/':
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
            case 'g':
            case 'h':
            case 'i':
            case 'j':
            case 'k':
            case 'l':
            case 'm':
            case 'n':
            case 'o':
            case 'p':
            case 'q':
            case 'r':
            case 's':
            case 't':
            case 'u':
            case 'v':
            case 'w':
            case 'x':
            case 'y':
            case 'z':
                result = true;
                break;
            default:
                result = false;
                break;
        }

        return result;
    }

    public boolean isInRegion(Location loc) {
        boolean returnValue = false;
        String coord_x = stringBeautifier(loc.getValue("XKoord"));

        String coord_y = stringBeautifier(loc.getValue("YKoord"));

        // stupid comma to point conversion
        if (coord_x != null) coord_x = coord_x.replaceAll(",", ".");
        if (coord_y != null) coord_y = coord_y.replaceAll(",", ".");
        String query = "select Within (st_setsrid(st_makepoint(" + coord_x + "," + coord_y +
                "),4326),st_buffer((select the_geom from core.berlin_outline where id=1)," + searchRadius + "))";
        //todo revise this
//        try {
//            ResultSet rs = this.dbCon.executeQuery(query, this);
//            if (rs.next()) {
//                returnValue = rs.getBoolean(1);
//            }
//            rs.close();
//        } catch (SQLException e) {
//            System.out.println("SQL-Error!. Query: " + query);
//            e.printStackTrace();
//        }
        return returnValue;

    }

    /**
     * Method to check if a character is an numeric integer
     *
     * @param character the character to check
     * @return true if character is 0-9 or + or -
     */
    private boolean isIntNumeric(char character) {
        boolean result;
        switch (character) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                // case '-':
                // case '+':
                result = true;
                break;
            default:
                result = false;
                break;
        }

        return result;
    }

    public boolean localizeLocation(Location loc) {
        boolean returnValue = false;
        String queryIndex;
        String streetValue;
        String houseNumber;
        StringBuilder plz;
        String coord_x;
        String coord_y;
        String query = null;
        String tmp;
        String stateName;
        String geometricCheck;
        String regionalCheck;
        String geometricConstruct;
        HashMap<Integer, String> queries = new HashMap<>();
        StreetnameProcessor processor = new StreetnameProcessor();
        ResultSet rs;
        // read some data
        streetValue = stringBeautifier(loc.getValue("Straße"));
        String topQuery = "";

        houseNumber = stringBeautifier(loc.getValue("Hausnummer"));
        stateName = stringBeautifier(loc.getValue("Stadt_Kreis_Bezeichnung"));

        plz = new StringBuilder(stringBeautifier(loc.getValue("PLZ")));
        plz = new StringBuilder(checkPLZ(plz.toString()));
        if (plz != null) {
            String[] plzs = plz.toString().split(",");

            plz = new StringBuilder();

            for (int i = 0; i < plzs.length; ++i) {
                plz.append("'").append(plzs[i]).append("'");
                if (i + 1 < plzs.length) plz.append(",");
            }
        }

        coord_x = stringBeautifier(loc.getValue("XKoord"));

        coord_y = stringBeautifier(loc.getValue("YKoord"));

        // stupid comma to point conversion
        if (coord_x != null) coord_x = coord_x.replaceAll(",", ".");
        if (coord_y != null) coord_y = coord_y.replaceAll(",", ".");


        //set this if you have a reduced region
        regionalCheck = " AND bundesland='11'";

        boolean resultNotFound = true;
        for (int i = 0; i < 1 && resultNotFound; ++i) {
//todo revise this
//            try {
//                if (coord_x != null && coord_y != null) {
//                    geometricConstruct = "ST_Distance_Sphere(st_setsrid(st_makepoint(" + coord_x + "," + coord_y +
//                            "),4326),the_geom)";
//                    //geometricCheck = "   AND "+geometricConstruct+"<" + (searchRadius*(i+1));
//                    geometricCheck = "";
//                } else {
//                    geometricConstruct = "";
//                    geometricCheck = "";
//                }
//
//
//                // check what to do
//                if (streetValue == null) {
//                    // get an address from the coordinates
//                    if (coord_x != null && coord_y != null) {// LIKE
//                        // ANY(array['12625'])
//                        if (plz != null) {
//                            query = "select * from (" +
//                                    "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y, " +
//                                    geometricConstruct + " as distancetopoint " + "	from core.berlin_buildings " +
//                                    "	where lower(plz) = '" + plz + "'" + geometricCheck + regionalCheck +
//                                    ") as foo " + "order by distancetopoint";
//                            queries.put(1, query);
//                        }
//                        if (!exactOnly) {
//                            // alternative if plz is wrong
//                            query = "select * from (" +
//                                    "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y, " +
//                                    geometricConstruct + " as distancetopoint " + "	from core.berlin_buildings " +
//                                    "	where true " + geometricCheck + regionalCheck + ") as foo " +
//                                    "order by distancetopoint";
//                            queries.put(2, query);
//                        }
//                    }
//                } else if (houseNumber == null) {
//                    // get an address from the coordinates
//                    if (coord_x != null && coord_y != null) {
//                        if (plz != null) {
//                            query = "select * from (" +
//                                    "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y, " +
//                                    geometricConstruct + " as distancetopoint " + "	from core.berlin_buildings " +
//                                    "	where lower(strasse)='" + streetValue + "' " + "   AND lower(plz) = '" + plz +
//                                    "' " + geometricCheck + regionalCheck + ") as foo " + "order by distancetopoint";
//                            queries.put(3, query);
//
//                            // filter spaces in street names
//                            tmp = processor.replaceStreetTag(streetValue.replaceAll(" ", ""));
//                            query = "select * from (" +
//                                    "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y, " +
//                                    geometricConstruct + " as distancetopoint " + "	from core.berlin_buildings " +
//                                    "	where lower(strasse)='" + tmp + "' " + "   AND lower(plz) = '" + plz + "' " +
//                                    geometricCheck + regionalCheck + ") as foo " + "order by distancetopoint";
//                            queries.put(4, query);
//                            if (!exactOnly) {
//                                // alternative if streetname is wrong
//                                query = "select * from (" +
//                                        "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y, " +
//                                        geometricConstruct + " as distancetopoint " +
//                                        "	from core.berlin_buildings " + "	where lower(plz) = '" + plz + "' " +
//                                        geometricCheck + regionalCheck + ") as foo " + "order by distancetopoint";
//                                queries.put(5, query);
//                            }
//                        }
//                        // else{
//                        // no plz
//                        query = "select * from (" +
//                                "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y, " +
//                                geometricConstruct + " as distancetopoint " + "	from core.berlin_buildings " +
//                                "	where lower(strasse)='" + streetValue + "' " + geometricCheck + regionalCheck +
//                                ") as foo " + "order by distancetopoint";
//                        queries.put(6, query);
//
//                        // filter spaces in street names
//                        tmp = processor.replaceStreetTag(streetValue.replaceAll(" ", ""));
//                        query = "select * from (" +
//                                "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y, " +
//                                geometricConstruct + " as distancetopoint " + "	from core.berlin_buildings " +
//                                "	where lower(strasse)='" + tmp + "' " + geometricCheck + regionalCheck +
//                                ") as foo " + "order by distancetopoint";
//                        queries.put(7, query);
//                        if (!exactOnly) {
//                            // alternative if streetname is wrong
//                            query = "select * from (" +
//                                    "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y, " +
//                                    geometricConstruct + " as distancetopoint " + "	from core.berlin_buildings " +
//                                    " 	where true" + geometricCheck + regionalCheck + ") as foo " +
//                                    "order by distancetopoint";
//                            queries.put(8, query);
//                        }
//
//                        // }
//                    }
//                    this.generateQueriesForAdressToCoordinate(State.getByName(stateName), streetValue, null,
//                            plz.toString(), null, geometricCheck, ", " + geometricConstruct + " as distancetopoint ",
//                            "order by distancetopoint", queries, 8); // generates 13 more queries
//                    //this.generateQueriesForAdressToCoordinate(State.getByName(stateName), streetValue, null, plz, null, null,", "  + geometricConstruct+ " as distancetopoint ", "order by distancetopoint", queries,21);// generates 13 more queries
//                    // fallback if everything fails: get the closest building to
//                    // the given coordinate
//                    if (coord_x != null && coord_y != null) {
//                        query = "select * from (" +
//                                "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y, " +
//                                geometricConstruct + " as distancetopoint " + "	from core.berlin_buildings " +
//                                "	where true " + geometricCheck + regionalCheck + ") as foo " +
//                                "order by distancetopoint";
//                        queries.put(35, query);
//                    }
//                } else {
//                    String tmpHouseNumber = houseNumber;
//                    String houseNumberAddOn = "";
//                    houseNumber = this.extractHouseNumber(tmpHouseNumber);
//                    if (houseNumber.length() != tmpHouseNumber.length()) {
//                        houseNumberAddOn = extractHouseNumberAddOn(tmpHouseNumber);
//                    }
//
//                    // get coordinates from address
//                    this.generateQueriesForAdressToCoordinate(
//                            stateName == null ? null : State.getByName(stateName.toUpperCase()), streetValue,
//                            houseNumber, plz.toString(), houseNumberAddOn, geometricCheck,
//                            ", " + geometricConstruct + " as distancetopoint ", "order by distancetopoint", queries,
//                            35); // generates 13 more queries
//                    //this.generateQueriesForAdressToCoordinate(State.getByName("BERLIN"), streetValue, houseNumber, plz, houseNumberAddOn, geometricCheck,", "  + geometricConstruct+ " as distancetopoint ", "order by distancetopoint", queries,35); // generates 13 more queries
//                    //this.generateQueriesForAdressToCoordinate(State.getByName(stateName), streetValue, houseNumber, plz, houseNumberAddOn, null,", "  + geometricConstruct+ " as distancetopoint ", "order by distancetopoint", queries,48);// generates 13 more queries
//                    if (!exactOnly) {
//                        // fallback if everything fails: get the closest
//                        // building to
//                        // the given coordinate
//                        if (coord_x != null && coord_y != null) {
//                            query = "select * from (" +
//                                    "	select 	id, strasse, hausnummer,hausnummerzusatz,plz, st_X(the_geom) as x, st_Y(the_geom) as y, " +
//                                    geometricConstruct + " as distancetopoint " + "	from core.berlin_buildings " +
//                                    "	where true " + geometricCheck + regionalCheck + ") as foo " +
//                                    "order by distancetopoint";
//                            queries.put(62, query);
//                        }
//                    }
//                }
//                // something to do?
//                int numOfResults, minNum = Integer.MAX_VALUE;
//                for (int j = 0; j < 63 && resultNotFound; ++j) {
//                    if (queries.containsKey(j)) {
//                        // get dataset
//                        query = queries.get(j);//+ " LIMIT 1";
//
//
//                        rs = this.dbCon.executeQuery(query, this);
//                        numOfResults = 0;
//                        while (rs.next()) {
//                            numOfResults++;
//                        }
//
//                        rs.close();
//                        if (numOfResults < minNum && numOfResults > 0) {
//                            minNum = numOfResults;
//                            topQuery = query;
//                        }
//
//                        if (numOfResults == 1) {
//                            loc.setStatus(Location.STATUS_DISTINCT_IN_DATABASE);
//                            rs = this.dbCon.executeQuery(query, this);
//                            while (rs.next()) {
//                                queryIndex = "Query " + j;
//
//                                if (loc.getValue("Bemerkungen") != null) queryIndex += " " + loc.getValue(
//                                        "Bemerkungen");
//
//                                loc.updateOrAddValue("Bemerkungen", queryIndex);
//
//                                // update the data
//                                this.updateLocation(rs, loc);
//                                if (Constants.ENABLE_CHANGES_OF_MARCO) {
//                                    if (loc.getStatus() == Location.STATUS_NOT_DISTINCT_IN_DATABASE) {
//                                        returnValue = false;
//                                        break;
//                                    } else {
//                                        returnValue = true;
//                                    }
//                                } else {
//                                    // TODO soll hier nicht die gesamte äußere Schleife
//                                    // abgebrochen werden
//                                    returnValue = true;
//                                    resultNotFound = false;
//                                    break;
//                                }
//                            }
//                            rs.close();
//                            resultNotFound = false;
//                        } else if (numOfResults > 0) {
//                            loc.setStatus(Location.STATUS_NOT_DISTINCT_IN_DATABASE);
//                        }
//                    }
//                }
//            } catch (SQLException e) {
//                System.out.println("SQL-Error: " + query);
//                e.printStackTrace();
//            }
        }

        if (loc.getValue("Bemerkungen") != null) topQuery += loc.getValue("Bemerkungen") + " " + topQuery;

        loc.updateOrAddValue("Bemerkungen", topQuery);
        return returnValue;
    }

    private String stringBeautifier(String input) {
        String returnValue = null;
        if (input != null) {
            returnValue = input.trim().toLowerCase();
            if (returnValue.length() == 0) { // empty string
                returnValue = null;
            }
        }
        return returnValue;
    }

    private void updateLocation(ResultSet rs, Location loc) throws SQLException {
        // update the data
        String streetValue = rs.getString("strasse");
        String houseNumber = rs.getString("hausnummer");
        String plz = rs.getString("plz");

        String houseNumberAddOn = rs.getString("hausnummerzusatz");
        String coord_x = Double.toString(rs.getDouble("x"));
        String coord_y = Double.toString(rs.getDouble("y"));

        if (houseNumberAddOn != null) {
            houseNumberAddOn = houseNumberAddOn.trim();
        }

        if (houseNumberAddOn.length() > 0) {
            houseNumber = houseNumber + " " + houseNumberAddOn;
        }
        if (Constants.ENABLE_CHANGES_OF_MARCO && loc.isUpdatedByDB()) {
            // If the location was already updated, check if the database has
            // unique results only

            // Housenumber may change, if the streetname or postalcode changes
            // after
            // it was changed from the db, the db has no unique
            // address->coord-mapping for this query
            if (!loc.getValue("Straße").equals(streetValue)) {
                loc.setStatus(Location.STATUS_NOT_DISTINCT_IN_DATABASE);
                return;
            }
            if (!loc.getValue("PLZ").equals(plz)) {
                loc.setStatus(Location.STATUS_NOT_DISTINCT_IN_DATABASE);
                return;
            }
        } else {

            loc.updateOrAddValue("Straße", streetValue);

            loc.updateOrAddValue("Hausnummer", houseNumber);

            loc.updateOrAddValue("PLZ", plz);

            loc.updateOrAddValue("XKoord", coord_x);
            loc.updateOrAddValue("YKoord", coord_y);
            loc.setUpdatedByDB(true);
        }
    }
}
