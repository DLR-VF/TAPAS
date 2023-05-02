/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.location;

import de.dlr.ivf.tapas.logger.LogHierarchy;
import de.dlr.ivf.tapas.logger.HierarchyLogLevel;

/**
 * This class implements a Coordinate by extending an array value distribution.
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.EPISODE)
public class TPS_Coordinate {

    double[] coord = new double[2];
    double[] euclidianCoord = new double[2];
    int Zone;
    char Letter;
    /**
     * Calls super constructor with size and sets both values for the x and y coordinate
     *
     * @param x x-coordinate value
     * @param y y-coordinate value
     */
    public TPS_Coordinate(double x, double y) {
        this.coord[0] = x;
        this.coord[1] = y;
        this.updateEuclidianCoordinates();
    }

    /**
     * Returns a copy of the coordinate
     *
     * @return copy the coordinate
     */
    public TPS_Coordinate copy() {
        return new TPS_Coordinate(this.getValue(0), this.getValue(1));
    }

    public double getEuclidianDistance(TPS_Coordinate b) {
        double dX = this.euclidianCoord[0] - b.euclidianCoord[0];
        double dY = this.euclidianCoord[1] - b.euclidianCoord[1];


        return Math.sqrt(dX * dX + dY * dY);
    }

    /**
     * Getter for the coordinates.
     *
     * @param index the index 0= x 1= y
     * @return the x or y value
     */
    public double getValue(int index) {
        return this.coord[index];
    }

    /**
     * Getter for the coordinate-array
     *
     * @return the 2 element array of the coordinate
     */
    public double[] getValues() {
        return this.coord;
    }

    /**
     * Setter for both variables a a 2-elem-array
     *
     * @param values
     */
    public void setValues(double[] values) {
        this.coord[0] = values[0];
        this.coord[1] = values[1];
        this.updateEuclidianCoordinates();
    }

    /**
     * Setter for a specific coordinate.
     * index 0= x = lon
     * index 1= y = lat
     *
     * @param index
     * @param value
     */
    public void setValue(int index, double value) {
        this.coord[index] = value;
        this.updateEuclidianCoordinates();
    }

    /**
     * Setter for both coordinates
     *
     * @param value1 x value
     * @param value2 y value
     */
    public void setValues(double value1, double value2) {
        this.coord[0] = value1;
        this.coord[1] = value2;
        this.updateEuclidianCoordinates();
    }

    private void updateEuclidianCoordinates() {
        //from https://stackoverflow.com/questions/176137/java-convert-lat-lon-to-utm
        Zone = (int) Math.floor(this.coord[0] / 6 + 31);
        if (this.coord[1] < -72) Letter = 'C';
        else if (this.coord[1] < -64) Letter = 'D';
        else if (this.coord[1] < -56) Letter = 'E';
        else if (this.coord[1] < -48) Letter = 'F';
        else if (this.coord[1] < -40) Letter = 'G';
        else if (this.coord[1] < -32) Letter = 'H';
        else if (this.coord[1] < -24) Letter = 'J';
        else if (this.coord[1] < -16) Letter = 'K';
        else if (this.coord[1] < -8) Letter = 'L';
        else if (this.coord[1] < 0) Letter = 'M';
        else if (this.coord[1] < 8) Letter = 'N';
        else if (this.coord[1] < 16) Letter = 'P';
        else if (this.coord[1] < 24) Letter = 'Q';
        else if (this.coord[1] < 32) Letter = 'R';
        else if (this.coord[1] < 40) Letter = 'S';
        else if (this.coord[1] < 48) Letter = 'T';
        else if (this.coord[1] < 56) Letter = 'U';
        else if (this.coord[1] < 64) Letter = 'V';
        else if (this.coord[1] < 72) Letter = 'W';
        else Letter = 'X';
        this.euclidianCoord[0] = 0.5 * Math.log((1 + Math.cos(this.coord[1] * Math.PI / 180) * Math.sin(
                this.coord[0] * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)) / (1 - Math.cos(
                this.coord[1] * Math.PI / 180) * Math.sin(
                this.coord[0] * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180))) * 0.9996 * 6399593.62 / Math.pow(
                (1 + Math.pow(0.0820944379, 2) * Math.pow(Math.cos(this.coord[1] * Math.PI / 180), 2)), 0.5) *
                (1 + Math.pow(0.0820944379, 2) / 2 * Math.pow((0.5 * Math.log((1 + Math.cos(
                        this.coord[1] * Math.PI / 180) * Math.sin(
                        this.coord[0] * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)) / (1 - Math.cos(
                        this.coord[1] * Math.PI / 180) * Math.sin(
                        this.coord[0] * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)))), 2) * Math.pow(
                        Math.cos(this.coord[1] * Math.PI / 180), 2) / 3) + 500000;
        this.euclidianCoord[0] = Math.round(this.euclidianCoord[0] * 100) * 0.01;
        this.euclidianCoord[1] = (Math.atan(Math.tan(this.coord[1] * Math.PI / 180) /
                Math.cos((this.coord[0] * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180))) -
                this.coord[1] * Math.PI / 180) * 0.9996 * 6399593.625 / Math.sqrt(
                1 + 0.006739496742 * Math.pow(Math.cos(this.coord[1] * Math.PI / 180), 2)) *
                (1 + 0.006739496742 / 2 * Math.pow(0.5 * Math.log((1 + Math.cos(this.coord[1] * Math.PI / 180) *
                        Math.sin((this.coord[0] * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180))) / (1 - Math.cos(
                        this.coord[1] * Math.PI / 180) * Math.sin(
                        (this.coord[0] * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)))), 2) * Math.pow(
                        Math.cos(this.coord[1] * Math.PI / 180), 2)) + 0.9996 * 6399593.625 *
                (this.coord[1] * Math.PI / 180 - 0.005054622556 * (this.coord[1] * Math.PI / 180 + Math.sin(
                        2 * this.coord[1] * Math.PI / 180) / 2) +
                        4.258201531e-05 * (3 * (this.coord[1] * Math.PI / 180 + Math.sin(
                                2 * this.coord[1] * Math.PI / 180) / 2) + Math.sin(2 * this.coord[1] * Math.PI / 180) *
                                Math.pow(Math.cos(this.coord[1] * Math.PI / 180), 2)) / 4 -
                        1.674057895e-07 * (5 * (3 * (this.coord[1] * Math.PI / 180 + Math.sin(
                                2 * this.coord[1] * Math.PI / 180) / 2) + Math.sin(2 * this.coord[1] * Math.PI / 180) *
                                Math.pow(Math.cos(this.coord[1] * Math.PI / 180), 2)) / 4 + Math.sin(
                                2 * this.coord[1] * Math.PI / 180) * Math.pow(Math.cos(this.coord[1] * Math.PI / 180),
                                2) * Math.pow(Math.cos(this.coord[1] * Math.PI / 180), 2)) / 3);
        if (Letter < 'M') this.euclidianCoord[1] = this.euclidianCoord[1] + 10000000;
        this.euclidianCoord[1] = Math.round(this.euclidianCoord[1] * 100) * 0.01;

    }
}
