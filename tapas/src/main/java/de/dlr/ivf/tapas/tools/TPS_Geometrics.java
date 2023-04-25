/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.loc.Locatable;
import de.dlr.ivf.tapas.loc.TPS_Coordinate;
import de.dlr.ivf.tapas.mode.TPS_Mode;
import de.dlr.ivf.tapas.parameter.ParamFlag;
import de.dlr.ivf.tapas.parameter.ParamMatrix;
import de.dlr.ivf.tapas.parameter.ParamValue;
import de.dlr.ivf.tapas.parameter.SimulationType;
import de.dlr.ivf.tapas.util.TPS_FastMath;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class for geometric operations. EG distance on earth surface etc.
 *
 * @author hein_mh
 */
public class TPS_Geometrics {

    /**
     * Mittlerer Erdradius in Metern
     */
    private static final double earthRadius = 6371000.785;

    /**
     * Function to calculate the TOP3-Strategy for diagonal elements. Elements=3 Damping factor: 0.8
     *
     * @param matrix
     */
    public static void calcTop3(double[][] matrix) {
        calcTopX(matrix, 3, 0.8);
    }

    /**
     * Function to calculate a generic TOPx-Strategy for diagonal elements with a given weight.
     *
     * @param matrix    the Input matrix. Must be square and larger than numOfElem+1
     * @param numOfElem Number of elems for average traveltime calculation
     * @param weight    the damping weight for the average.
     */
    public static void calcTopX(double[][] matrix, int numOfElem, double weight) {
        int i, j;
        if (matrix == null || matrix.length == 0 || matrix.length < (numOfElem + 1) ||
                matrix.length != matrix[0].length) return;

        for (i = 0; i < matrix.length; ++i) {
            List<Double> row = new ArrayList<>();
            for (j = 0; j < matrix[i].length; ++j) {
                if (i != j) row.add(matrix[i][j]);
            }
            Collections.sort(row);
            matrix[i][i] = 0;
            for (j = 0; j < numOfElem; ++j) {
                matrix[i][i] += row.get(j);
            }
            matrix[i][i] *= weight / (double) numOfElem;
        }
    }

    public static void convertToCartesian(TPS_Coordinate g, double[] p) {
        double phi, rho, z;
        rho = Math.toRadians(g.getValue(0));
        phi = Math.toRadians(g.getValue(1));
        z = Math.cos(phi); //verwende so wenig trigonometische Funktionen wie möglich!
        p[0] = earthRadius * z * Math.cos(rho);
        p[1] = earthRadius * z * Math.sin(rho);
        p[2] = earthRadius * Math.sin(phi);
    }

    /*
     * This method calculates the distance in meters between both coordinates using a sphere.
     *
     * @param l0
     * @param l1
     *
     * @return distance of the two coordinates depending an a sphere
     */
    public static double getDistance(Locatable l0, Locatable l1, double min_dist) {
        //if (l0 instanceof TPS_Location && l1 instanceof TPS_Location) {
        //	return TPS_Geometrics.getDistance((TPS_Location) l0, (TPS_Location) l1);
        //}
        return getDistance(l0.getCoordinate(), l1.getCoordinate(), min_dist);
    }

    /**
     * @param startLoc Starting location
     * @param endLoc   Destination location
     * @return distance in meters
     */
    public static double getDistance(Locatable startLoc, Locatable endLoc, TPS_Mode mode, SimulationType simType) {
        double dist = -1;

        /*
         * TODO RITA: check if this calculation method is valid and reasonable Determines the distance in meters between two
         * locations. Uses the intraTVZInfoMatrix if start and end traffic analysis zone are equal. Otherwise the distance is
         * calculated by the coordinates of the locations. Returns at least the in the configuration specified minimal
         * distance.
         */
        if (startLoc != null && endLoc != null) {
            int bezAId = startLoc.getTrafficAnalysisZone().getTAZId();
            int bezBId = endLoc.getTrafficAnalysisZone().getTAZId();

            if (bezAId == bezBId && mode.getParameters().isTrue(ParamFlag.FLAG_INTRA_INFOS_MATRIX) &&
                    mode.getParameters().isFalse(ParamFlag.FLAG_USE_BLOCK_LEVEL)) {

                // Rückkonvertierung erfolgt durch die Modifizierung der Intrazelleninfo per beelinefaktor:
                // beelineLoc/beelineTAZ

                double distanceNet = mode.getDistance(startLoc, endLoc, simType, null);

                double beelineTAZ = mode.getParameters().paramMatrixClass.getValue(ParamMatrix.DISTANCES_BL, bezAId,
                        bezBId);
                double beelineLoc = getDistance(startLoc.getCoordinate(), endLoc.getCoordinate(),
                        mode.getParameters().getDoubleValue(ParamValue.MIN_DIST));
                // Hier: Rückkonvertierung zu Luftliniendistanz Multiplizierung mit den "Luftlinienfaktor"
                dist = distanceNet * beelineLoc / beelineTAZ;
            } else {
                dist = getDistance(startLoc.getCoordinate(), endLoc.getCoordinate(),
                        mode.getParameters().getDoubleValue(ParamValue.MIN_DIST));
            }
        }
        return Math.max(mode.getParameters().getDoubleValue(ParamValue.MIN_DIST), dist);
    }

    /**
     * This method calculates the distance in meters between both coordinates using a sphere.
     *
     * @param c0
     * @param c1
     * @param min_dist
     * @return distance of the two coordinates depending an a sphere
     */
    public static double getDistance(TPS_Coordinate c0, TPS_Coordinate c1, double min_dist) {
        // changes of mark_ma
        // MANTIS ENTRY 0001024
        // introduced beeline distance calculation for a ideal sphere with the same volume as the earth -> median radius
        // @see http://de.wikipedia.org/wiki/Entfernungsberechnung

        //return Math.max(min_dist, getDistance(c0.getValue(0), c0.getValue(1),c1.getValue(0),c1.getValue(1)));
        return Math.max(min_dist, c0.getEuclidianDistance(c1));

    }

    /**
     * This method calculates the distance in meters between both coordinates using a sphere.
     *
     * @return distance of the two coordinates depending an a sphere
     */
    public static double getDistance(double lat1, double lon1, double lat2, double lon2) {
        // changes of mark_ma
        // MANTIS ENTRY 0001024
        // introduced beeline distance calculation for a ideal sphere with the same volume as the earth -> median radius
        // @see http://de.wikipedia.org/wiki/Entfernungsberechnung

        double fs = Math.sin(Math.toRadians(lon1)) * Math.sin(Math.toRadians(lon2));
        double fc = Math.cos(Math.toRadians(lon1)) * Math.cos(Math.toRadians(lon2));
        double x = fs + fc * Math.cos(Math.toRadians(lat1 - lat2));
        x = Math.max(-1.0,
                Math.min(1.0, x)); //if c0 == c1 values slightly bigger than 1 occur. FIX: chop to valid acos-range
        return TPS_FastMath.acos(x) * earthRadius;
    }

    /**
     * This method calculates the distance in meters between a line form g0 to g1 and a point p
     *
     * @param g0           start point of the line
     * @param g1           end point of the line
     * @param point        point to measure the distance
     * @param min_distance minimum distance
     * @return minimum distance : zero if point is part of the line g0-g1, the length of the line through the point  perpendicular to g0-g1 otherwise.
     */
    public static double getDistance(TPS_Coordinate g0, TPS_Coordinate g1, TPS_Coordinate point, double min_distance) {
        if (isSameLocation(g0, g1)) { // give point dist
            return getDistance(g0, point, min_distance);
        } else {

            double phi, rho, z;
            double[] r = new double[3], p = new double[3], t = new double[3], s = new double[3], l = new double[3];
            double actMaxDist;

            //konvertierung geograph. kugel->karthesisch (sin und cos für phi vertauscht)
            // the point
            convertToCartesian(point, p);

            //the base of the line
            convertToCartesian(g0, t);

            //the 2nd point on the line
            convertToCartesian(g1, s);

            //the direction of the line
            for (int i = 0; i < 3; ++i) r[i] = s[i] - t[i];

            // die kürzeste Distanz zwischen einem Punkt und einer Linie ist der Abstand des Lotfußpunktes l auf der Linie zum Punkt.
            // zusätzlich muss in diesem Fall der Lotfußpunkt l zwischen g0=t und g1=s liegen!
            // die Gerade g ist deffiniert durch g = t + z*(s-t)
            // Der Lotfußpunkt l liegt zwischen t uns s, wenn z zwischen 0 und 1 liegt
            // Des weiteren ist das Skalarprodukt aus (s-t)(l-p)=0 (Senkrechtes Lot)
            // aus der Gleichung (s-t)*(t+z*(s-t)-p)=0 ergibt sich für den Parameter z:
            z = ((p[0] - t[0]) * r[0] + (p[1] - t[1]) * r[1] + (p[2] - t[2]) * r[2]) /
                    (r[0] * r[0] + r[1] * r[1] + r[2] * r[2]);

            if (z <= 0 || z >= 1) {  //lot nciht zwischen g0 und g1
                return Math.min(getDistance(g0, point, min_distance), getDistance(g1, point, min_distance));
            } else {  //lot zwischen g0 und g1

                // der Lotfußpunkt ist definiert durch:
                for (int i = 0; i < 3; ++i) l[i] = t[i] + z * r[i];

                //konvertierung karthesisch->geograph. kugel, radius wird vergessen und auf die erdoberfläche gesetzt!
                actMaxDist = Math.sqrt(l[0] * l[0] + l[1] * l[1] + l[2] * l[2]);
                phi = Math.toDegrees(Math.asin(l[2] / actMaxDist));
                rho = Math.toDegrees(Math.atan2(l[1], l[0]));

                //dieser punkt ist der Lotfußpunkt der direkten Linie von g0 nach g1 und wird dann auf dem Spheroiden Erde projiziert, aber das mach bei unseren Koodinaten nichts ;)
                TPS_Coordinate lot = new TPS_Coordinate(rho, phi);
                return getDistance(lot, point, min_distance);
            }
        }
    }

    /**
     * This method calculates the distance in meters between a line form g0 to g1 and a point point
     *
     * @param g0          start point of the line
     * @param distToStart radius around the start
     * @param g1          end point of the line
     * @param distToEnd   radius around the end
     * @param point       point, which distance should be measured
     * @param min_dist    minimum distance
     * @return distance in meters
     */
    public static boolean isBetweenLine(TPS_Coordinate g0, double distToStart, TPS_Coordinate g1, double distToEnd, TPS_Coordinate point, double min_dist) {
        if (distToEnd < 0 && distToStart < 0) return true;

        if (isSameLocation(g0, g1)) { // give point dist
            return isWithin(g0, point, min_dist, Math.max(distToStart, distToEnd));
        } else {

            double phi, rho, len, z;
            double[] r = new double[3], p = new double[3], t = new double[3], s = new double[3], l = new double[3];
            double actMaxDist;

            //konvertierung geograph. kugel->karthesisch (sin und cos für phi vertauscht)
            // the point
            convertToCartesian(point, p);

            //the base of the line
            convertToCartesian(g0, t);

            //the 2nd point on the line
            convertToCartesian(g1, s);

            //the direction of the line
            for (int i = 0; i < 3; ++i) r[i] = s[i] - t[i];


            // die kürzeste Distanz zwischen einem Punkt und einer Linie ist der Abstand des Lotfußpunktes l auf der Linie zum Punkt.
            // zusätzlich muss in diesem Fall der Lotfußpunkt l zwischen g0=t und g1=s liegen!
            // die Gerade g ist deffiniert durch g = t + z*(s-t)
            // Der Lotfußpunkt l liegt zwischen t uns s, wenn z zwischen 0 und 1 liegt
            // Des weiteren ist das Skalarprodukt aus (s-t)(l-p)=0 (Senkrechtes Lot)
            // aus der Gleichung (s-t)*(t+z*(s-t)-p)=0 ergibt sich für den Parameter z:
            z = ((p[0] - t[0]) * r[0] + (p[1] - t[1]) * r[1] + (p[2] - t[2]) * r[2]) /
                    (r[0] * r[0] + r[1] * r[1] + r[2] * r[2]);

            if (z <= 0 || z >= 1) {  //lot nciht zwischen g0 und g1
                return isWithin(g0, point, min_dist, distToStart) || isWithin(g1, point, min_dist, distToEnd);
            } else {  //lot zwischen g0 und g1

                // der Lotfußpunkt ist definiert durch:
                for (int i = 0; i < 3; ++i) l[i] = t[i] + z * r[i];

                //konvertierung karthesisch->geograph. kugel, radius wird vergessen und auf die erdoberfläche gesetzt!
                actMaxDist = Math.sqrt(l[0] * l[0] + l[1] * l[1] + l[2] * l[2]);
                phi = Math.toDegrees(Math.asin(l[2] / actMaxDist));
                rho = Math.toDegrees(Math.atan2(l[1], l[0]));

                //dieser punkt ist der Lotfußpunkt der direkten Linie von g0 nach g1 und wird dann auf dem Spheroiden Erde projiziert, aber das mach bei unseren Koodinaten nichts ;)
                TPS_Coordinate lot = new TPS_Coordinate(rho, phi);
                len = getDistance(lot, point, min_dist);

                //jetzt bestimmen wir die lineare gewichtung zwischen distToStart und distToEnd anhand von z
                actMaxDist = distToStart + z * (distToEnd - distToStart);

                return len <= actMaxDist;
            }
        }
    }

    /**
     * Method to check if two given locations are neglegtable close to eachother.
     *
     * @param g0 the 1st location
     * @param g1 the 2nd location
     * @return true if the location are VERY close to eachother
     */
    public static boolean isSameLocation(TPS_Coordinate g0, TPS_Coordinate g1) {
        double x = g0.getValue(0) - g1.getValue(0);
        double y = g0.getValue(1) - g1.getValue(1);

        return (x * x + y * y) < 1e-15;

    }

    /**
     * This method calculates if the distance between both coordinates is lower than the given maximum.
     *
     * @param c0
     * @param c1
     * @param minDistance the minimum distance
     * @param maxDistance the maximum distance
     * @return true if the distance is lower or equal maxDistance
     */
    public static boolean isWithin(TPS_Coordinate c0, TPS_Coordinate c1, double minDistance, double maxDistance) {
        double dist = getDistance(c0, c1, minDistance);
        return dist <= maxDistance;
    }

    //this is a debug function and thus sometimes unused
    @SuppressWarnings("unused")
    private static void writeKMLDebug(TPS_Coordinate c0, TPS_Coordinate c1, TPS_Coordinate c2, TPS_Coordinate c3) {
        try {
            FileOutputStream outStream = new FileOutputStream("C:\\temp\\Debug" + System.currentTimeMillis() + ".kml");
            PrintWriter outWrite = new PrintWriter(outStream);

            outWrite.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            outWrite.println("<kml xmlns=\"http://www.opengis.net/kml/2.2\">");
            outWrite.println("	<Document>");
            outWrite.println("		<Placemark>");
            outWrite.printf("			<name>%s</name>\n", "x");
            outWrite.printf("			<description>%s</description>\n", "Start");
            outWrite.printf("			<Point>\n");
            outWrite.printf("				<coordinates>%e,%e,0</coordinates>\n", c0.getValue(0), c0.getValue(1));
            outWrite.printf("			</Point>\n");
            outWrite.println("		</Placemark>");
            outWrite.println("		<Placemark>");
            outWrite.printf("			<name>%s</name>\n", "y");
            outWrite.printf("			<description>%s</description>\n", "Stop");
            outWrite.printf("			<Point>\n");
            outWrite.printf("				<coordinates>%e,%e,0</coordinates>\n", c1.getValue(0), c1.getValue(1));
            outWrite.printf("			</Point>\n");
            outWrite.println("		</Placemark>");
            outWrite.println("		<Placemark>");
            outWrite.printf("			<name>%s</name>\n", "p");
            outWrite.printf("			<description>%s</description>\n", "Location");
            outWrite.printf("			<Point>\n");
            outWrite.printf("				<coordinates>%e,%e,0</coordinates>\n", c2.getValue(0), c2.getValue(1));
            outWrite.printf("			</Point>\n");
            outWrite.println("		</Placemark>");
            outWrite.println("		<Placemark>");
            outWrite.printf("			<name>%s</name>\n", "l");
            outWrite.printf("			<description>%s</description>\n", "Lot");
            outWrite.printf("			<Point>\n");
            outWrite.printf("				<coordinates>%e,%e,0</coordinates>\n", c3.getValue(0), c3.getValue(1));
            outWrite.printf("			</Point>\n");
            outWrite.println("		</Placemark>");
            outWrite.println("	</Document>");
            outWrite.println("</kml>");


            outWrite.close();
        }//try
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
