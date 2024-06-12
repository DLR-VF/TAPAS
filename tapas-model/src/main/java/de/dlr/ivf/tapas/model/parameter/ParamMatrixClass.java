/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.parameter;

import de.dlr.ivf.tapas.logger.legacy.SeverityLogLevel;
import de.dlr.ivf.tapas.logger.legacy.TPS_Logger;

import de.dlr.ivf.tapas.model.MatrixLegacy;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.Arrays;
import java.util.EnumMap;

public class ParamMatrixClass {
    private final EnumMap<ParamMatrix, MutablePair<ParamType, MatrixLegacy[]>> paramMatrix;


    ParamMatrixClass() {
        this.paramMatrix = new EnumMap<>(ParamMatrix.class);
        this.paramMatrix.put(ParamMatrix.DISTANCES_STREET, new MutablePair<>(ParamType.DEFAULT, new MatrixLegacy[1]));
        this.paramMatrix.put(ParamMatrix.DISTANCES_WALK, new MutablePair<>(ParamType.DEFAULT, new MatrixLegacy[1]));
        this.paramMatrix.put(ParamMatrix.DISTANCES_BIKE, new MutablePair<>(ParamType.DEFAULT, new MatrixLegacy[1]));
        this.paramMatrix.put(ParamMatrix.DISTANCES_PT, new MutablePair<>(ParamType.DEFAULT, new MatrixLegacy[1]));
        this.paramMatrix.put(ParamMatrix.DISTANCES_BL, new MutablePair<>(ParamType.DEFAULT, new MatrixLegacy[1]));
    }


    /**
     * Sets all matrices of a matrix parameter to null.
     *
     * @param param matrix parameter enum
     */
    public void clear(ParamMatrix param) {
        Arrays.fill(this.paramMatrix.get(param).getRight(), null);
    }

    /**
     * If there exist only one matrix, this matrix is returned. If there
     * exists two matrices this matrix is returned which corresponds to the
     * simulation type. The simulation type is retrieved by the method
     * getSimulationType().
     *
     * @param param matrix parameter enum
     * @return one matrix of the enum constant
     * @throws RuntimeException This exception is thrown if the constant
     *                          was not defined
     */
    public MatrixLegacy getMatrix(ParamMatrix param) {
        int index = 0;
        if (this.paramMatrix.get(param).getRight().length > 1) index = SimulationType.SCENARIO.getIndex();
        if (this.paramMatrix.get(param).getRight()[index] == null) throw new RuntimeException(
                "Enum-Value is not defined: " + this);
        return this.paramMatrix.get(param).getRight()[index];
    }

    /**
     * The matrix corresponding to the given simulation type is returned.
     *
     * @param param matrix parameter enum
     * @param type  simulation type
     * @return matrix corresponding to the given simulation type
     * @throws RuntimeException This exception is thrown if the constant
     *                          was not defined
     *                          or the constant is not simulation type
     *                          dependent
     */
    public MatrixLegacy getMatrix(ParamMatrix param, SimulationType type) {
        if (this.paramMatrix.get(param).getRight().length < 2) {
            throw new RuntimeException("Enum-Value is not simulation " + "type" + " dependent: " + this);
        }
        int index = type.getIndex();
        if (this.paramMatrix.get(param).getRight()[index] == null) throw new RuntimeException(
                "Enum-Value is not defined: " + this);
        return this.paramMatrix.get(param).getRight()[index];
    }

    /**
     * @param param parameter matrix enum
     * @return parameter type
     */
    public ParamType getType(ParamMatrix param) {
        return this.paramMatrix.get(param).getLeft();
    }

    /**
     * This method uses the getMatrix() method so it returns the value
     * corresponding to the current simulation type.
     *
     * @param param matrix parameter enum
     * @param i     line position
     * @param j     column position
     * @return value at position (i,j)
     * @throws RuntimeException if the constant was not defined
     */
    public double getValue(ParamMatrix param, int i, int j) {
        return this.getMatrix(param).getValue(i, j);
    }

    /**
     * This method uses the getMatrix(SimulationType type) method so it
     * returns the value corresponding to the given simulation type.
     *
     * @param param matrix parameter enum
     * @param i     line position
     * @param j     column position
     * @param type  type of the simulation
     * @return value at position (i,j)
     * @throws RuntimeException if the constant was not defined
     */
    public double getValue(ParamMatrix param, int i, int j, SimulationType type) {
        return this.getMatrix(param, type).getValue(i, j);
    }

    /**
     * Flag if matrix has been instantiated
     *
     * @param param matrix parameter enum
     * @return true, if matrix is defined, false else
     */
    public boolean isDefined(ParamMatrix param) {
        boolean bool = this.paramMatrix.get(param).getRight() != null;
        for (int i = 0; (i < this.paramMatrix.get(param).getRight().length) && bool; i++) {
            bool = (this.paramMatrix.get(param).getRight()[i] != null) && bool;
        }
        return bool;
    }

    /**
     * This method sets one matrix in the array. This method is for
     * constants which don't depend on the simulation type
     *
     * @param param  matrix parameter enum
     * @param matrix new matrix to set
     */
    public void setMatrix(ParamMatrix param, MatrixLegacy matrix) {
        if (this.paramMatrix.get(param).getRight().length > 1) throw new RuntimeException(
                "Specialise which matrix you set: " + "Choose one Simulation " + "type and call setMatrix" +
                        "(MatrixLegacy " + "matrix, SimulationType " + "type)");
        MatrixLegacy[] m = this.paramMatrix.get(param).getRight();
        if (m[0] != null) {
            TPS_Logger.log(SeverityLogLevel.DEBUG, "Overwriting existing matrix");
        }
        m[0] = matrix;
    }

    /**
     * This method sets one matrix in the array. This method is for
     * constants which do depend on the simulation type
     *
     * @param param  matrix parameter enum
     * @param matrix new matrix to set
     * @param type   type of the simulation
     */
    public void setMatrix(ParamMatrix param, MatrixLegacy matrix, SimulationType type) {
        if (this.paramMatrix.get(param).getRight().length == 1) throw new RuntimeException(
                "This parameter is independant " + "from the simulation type:" + " " + "call setMatrix(MatrixLegacy " +
                        "matrix)");
        MatrixLegacy[] m = this.paramMatrix.get(param).getRight();
        int index = type.getIndex();
        if (m[index] != null) {
            TPS_Logger.log(SeverityLogLevel.DEBUG, "Overwriting existing matrix");
        }
        m[index] = matrix;
    }

    /**
     * Creates a raw string for all matrices
     *
     * @param param matrix parameter enum
     * @return a String containing the raw values of all matrices
     */
    public String toRawString(ParamMatrix param) {
        StringBuilder sb = new StringBuilder();
        if (this.isDefined(param)) {
            for (int i = 0; i < this.paramMatrix.get(param).getRight().length; i++) {
                sb.append(paramMatrix.get(param).getRight()[i].toString());
            }
        }
        return sb.toString();
    }
}
