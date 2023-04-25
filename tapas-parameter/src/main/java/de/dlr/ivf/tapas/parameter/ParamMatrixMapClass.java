/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.parameter;

import de.dlr.ivf.tapas.model.Matrix;
import de.dlr.ivf.tapas.model.MatrixMap;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.Arrays;
import java.util.EnumMap;

public class ParamMatrixMapClass {
    private final EnumMap<ParamMatrixMap, MutablePair<ParamType, MatrixMap[]>> paramMatrixMaps;

    ParamMatrixMapClass() {
        this.paramMatrixMaps = new EnumMap<>(ParamMatrixMap.class);
        this.paramMatrixMaps.put(ParamMatrixMap.ARRIVAL_PT, new MutablePair<>(ParamType.DEFAULT, new MatrixMap[2]));
        this.paramMatrixMaps.put(ParamMatrixMap.ARRIVAL_BIKE, new MutablePair<>(ParamType.DEFAULT, new MatrixMap[2]));
        this.paramMatrixMaps.put(ParamMatrixMap.ARRIVAL_WALK, new MutablePair<>(ParamType.DEFAULT, new MatrixMap[2]));
        this.paramMatrixMaps.put(ParamMatrixMap.ARRIVAL_MIT, new MutablePair<>(ParamType.DEFAULT, new MatrixMap[2]));
        this.paramMatrixMaps.put(ParamMatrixMap.AVERAGE_SPEED_SCHOOLBUS,
                new MutablePair<>(ParamType.DEFAULT, new MatrixMap[2]));
        this.paramMatrixMaps.put(ParamMatrixMap.EGRESS_PT, new MutablePair<>(ParamType.DEFAULT, new MatrixMap[2]));
        this.paramMatrixMaps.put(ParamMatrixMap.EGRESS_BIKE, new MutablePair<>(ParamType.DEFAULT, new MatrixMap[2]));
        this.paramMatrixMaps.put(ParamMatrixMap.EGRESS_WALK, new MutablePair<>(ParamType.DEFAULT, new MatrixMap[2]));
        this.paramMatrixMaps.put(ParamMatrixMap.EGRESS_MIT, new MutablePair<>(ParamType.DEFAULT, new MatrixMap[2]));
        this.paramMatrixMaps.put(ParamMatrixMap.INTERCHANGES_PT,
                new MutablePair<>(ParamType.DEFAULT, new MatrixMap[2]));
        this.paramMatrixMaps.put(ParamMatrixMap.PTBIKE_ACCESS_TAZ,
                new MutablePair<>(ParamType.DEFAULT, new MatrixMap[2]));
        this.paramMatrixMaps.put(ParamMatrixMap.PTBIKE_EGRESS_TAZ,
                new MutablePair<>(ParamType.DEFAULT, new MatrixMap[2]));
        this.paramMatrixMaps.put(ParamMatrixMap.PTCAR_ACCESS_TAZ,
                new MutablePair<>(ParamType.DEFAULT, new MatrixMap[2]));
        this.paramMatrixMaps.put(ParamMatrixMap.PTBIKE_INTERCHANGES,
                new MutablePair<>(ParamType.DEFAULT, new MatrixMap[2]));
        this.paramMatrixMaps.put(ParamMatrixMap.PTCAR_INTERCHANGES,
                new MutablePair<>(ParamType.DEFAULT, new MatrixMap[2]));
        this.paramMatrixMaps.put(ParamMatrixMap.TRAVEL_TIME_MIT,
                new MutablePair<>(ParamType.DEFAULT, new MatrixMap[2]));
        this.paramMatrixMaps.put(ParamMatrixMap.TRAVEL_TIME_PT, new MutablePair<>(ParamType.DEFAULT, new MatrixMap[2]));
        this.paramMatrixMaps.put(ParamMatrixMap.TRAVEL_TIME_WALK,
                new MutablePair<>(ParamType.DEFAULT, new MatrixMap[2]));
        this.paramMatrixMaps.put(ParamMatrixMap.TRAVEL_TIME_BIKE,
                new MutablePair<>(ParamType.DEFAULT, new MatrixMap[2]));
    }

    /**
     * This method clears all matrix maps.
     *
     * @param param matrix map parameter enum
     */
    public void clear(ParamMatrixMap param) {
        Arrays.fill(this.paramMatrixMaps.get(param).getRight(), null);
    }

    /**
     * If there exist only one matrixMap, the matrix for the given time of
     * this map is returned. If there exists two matrixMaps the matrix for
     * the given time is returned which matrixMap corresponds to the
     * scenario type.
     *
     * @param param matrix map parameter enum
     * @param time  the time slot for the requested matrix
     * @return one matrix of the enum constant
     * @throws RuntimeException if the constant was not defined
     */
    private Matrix getMatrixFromMap(ParamMatrixMap param, int time) {
        SimulationType type = SimulationType.BASE;
        if (this.paramMatrixMaps.get(param).getRight().length > 1) {
            type = SimulationType.SCENARIO;
        }
        return this.getMatrixFromMap(param, type, time);
    }

    /**
     * The matrix for the given time of the matrixMap corresponding to the
     * given simulation type is returned.
     *
     * @param param matrix map parameter enum
     * @param type  simulation type
     * @param time  the time slot for the requested matrix
     * @return matrix corresponding to the given simulation type
     * @throws RuntimeException if the constant was not defined
     *                          or the constant is not simulation type
     *                          dependent
     */
    private Matrix getMatrixFromMap(ParamMatrixMap param, SimulationType type, int time) {
        if (this.paramMatrixMaps.get(param).getRight().length < 2) {
            throw new RuntimeException("Enum-Value is not simulation " + "type" + " dependent: " + this);
        }
        int index = type.getIndex();
        if (this.paramMatrixMaps.get(param).getRight()[index] == null) throw new RuntimeException(
                "MatrixMap for index " + index + " and param " + param + " is not defined: " + this);
        return this.paramMatrixMaps.get(param).getRight()[index].getMatrix(time);
    }

    /**
     * This method gets the whole MatrixMap for the given SimulationType
     *
     * @param param matrix map parameter enum
     * @param type  SimulationType for this request
     * @return the MatrixMap for the given SimulationType
     */
    public MatrixMap getMatrixMap(ParamMatrixMap param, SimulationType type) {
        if (this.paramMatrixMaps.get(param).getRight().length < 2) {
            throw new RuntimeException("MatrixMap is not simulation type " + "dependent: " + this);
        }
        int index = type.getIndex();
        if (this.paramMatrixMaps.get(param).getRight()[index] == null) throw new RuntimeException(
                "MatrixMap for index " + index + " is not defined: " + this);
        return this.paramMatrixMaps.get(param).getRight()[index];
    }

    /**
     * @param param matrix map parameter enum
     * @return parameter type
     */
    public ParamType getType(ParamMatrixMap param) {
        return paramMatrixMaps.get(param).getLeft();
    }

    /**
     * This method uses the getMatrixFromMap(time) method so it returns the
     * value corresponding to the current simulation type and to the given
     * time.
     *
     * @param param matrix map parameter enum
     * @param i     line position
     * @param j     column position
     * @param time  the time slot for the requested matrix
     * @return value at position (i,j)
     * @throws RuntimeException if the constant was not defined
     */
    public double getValue(ParamMatrixMap param, int i, int j, int time) {
        return this.getMatrixFromMap(param, time).getValue(i, j);
    }

    /**
     * This method uses the getMatrix(SimulationType type, double time)
     * method so it returns the value corresponding to the given simulation
     * type.
     *
     * @param param matrix map parameter enum
     * @param i     line position
     * @param j     column position
     * @param type  simulation type
     * @param time  the time slot for the requested matrix
     * @return value at position (i,j)
     * @throws RuntimeException if the constant was not defined
     */
    public double getValue(ParamMatrixMap param, int i, int j, SimulationType type, int time) {
        return this.getMatrixFromMap(param, type, time).getValue(i, j);
    }

    /**
     * Flag if matrix has been instantiated
     *
     * @param param matrix map parameter enum
     * @return true, if matrix is defined, false else
     */
    public boolean isDefined(ParamMatrixMap param) {
        boolean isDefined = false;
        for (int i = 0; i < this.paramMatrixMaps.get(param).getRight().length; ++i) {
            isDefined = this.paramMatrixMaps.get(param).getRight()[i] != null || isDefined;
        }
        return isDefined;
    }

    /**
     * This method sets one matrixMap in the array, which contains only one
     * matrix. This method is for constants which don't depend on the
     * simulation type
     *
     * @param param  matrix map parameter enum
     * @param matrix new matrix to set
     * @throws RuntimeException if matrix map is simulation dependent and
     *                          no simulation type is set
     */
    public void setMatrix(ParamMatrixMap param, Matrix matrix) {
        if (this.paramMatrixMaps.get(param).getRight().length > 1) throw new RuntimeException(
                "Specialise which matrix you set: " + "Choose one Simulation " + "type and call " +
                        "setMatrixMap(Matrix " + "matrix, SimulationType " + "type)");
        double[] distribution = new double[]{0};
        Matrix[] matrices = new Matrix[distribution.length];
        matrices[0] = matrix;
        this.paramMatrixMaps.get(param).getRight()[0] = new MatrixMap(distribution, matrices);
    }

    /**
     * This method sets one matrix in the array, which contains only one
     * matrix. This method is for constants which do depend on the
     * simulation type
     *
     * @param param  matrix map parameter enum
     * @param matrix new matrix to set
     * @param type   simulation type
     * @throws RuntimeException if the parameter is independent from the
     *                          simulation type
     */
    public void setMatrix(ParamMatrixMap param, Matrix matrix, SimulationType type) {
        if (this.paramMatrixMaps.get(param).getRight().length == 1) throw new RuntimeException(
                "This parameter is independent " + "from the simulation type:" + " " + "call setMatrix(Matrix " +
                        "matrix)");
        double[] distribution = new double[]{0};
        Matrix[] matrices = new Matrix[distribution.length];
        matrices[0] = matrix;
        this.paramMatrixMaps.get(param).getRight()[type.getIndex()] = new MatrixMap(distribution, matrices);
    }

    /**
     * This method sets one matrixMap in the array. This method is for
     * constants which don't depend on the simulation type
     *
     * @param param     matrix map parameter enum
     * @param matrixMap new matrixMap to set
     * @throws RuntimeException if matrix map is simulation dependent and
     *                          no simulation type is set
     */
    public void setMatrixMap(ParamMatrixMap param, MatrixMap matrixMap) {
        if (this.paramMatrixMaps.get(param).getRight().length > 1) throw new RuntimeException(
                "Specialise which matrix you set: " + "Choose one Simulation " + "type and call " + "setMatrixMap" +
                        "(MatrixMap matrixMap, " + "SimulationType type)");
        //store local variables for debug tracing
        MatrixMap[] map = this.paramMatrixMaps.get(param).getRight();
        map[0] = matrixMap;
    }

    /**
     * This method sets one matrixMap in the array. This method is for
     * constants which do depend on the simulation type
     *
     * @param param     matrix map parameter enum
     * @param matrixMap new matrixMap to set
     * @param type      simulation type
     * @throws RuntimeException if the parameter is independent from the
     *                          simulation type
     */
    public void setMatrixMap(ParamMatrixMap param, MatrixMap matrixMap, SimulationType type) {
        if (this.paramMatrixMaps.get(param).getRight().length == 1) throw new RuntimeException(
                "This parameter is independent " + "from the simulation type:" + " " + "call setMatrixMap" +
                        "(MatrixMap " + "matrixMap)");
        //store local variables for debug tracing
        MatrixMap[] map = this.paramMatrixMaps.get(param).getRight();
        int index = type.getIndex();

        map[index] = matrixMap;
    }

    /**
     * This method sets one matrixMap in the array, which contains a set of
     * matrices with a given time distribution. This method is for constants
     * which don't depend on the simulation type
     *
     * @param param        matrix map parameter enum
     * @param distribution the time distribution for the given matrixset
     * @param matrices     new matrixset to set
     * @throws RuntimeException if matrix map is simulation dependent and
     *                          no simulation type is set
     *                          or distribution and matrices length does
     *                          not match
     */
    public void setMatrixMap(ParamMatrixMap param, double[] distribution, Matrix[] matrices) {
        if (this.paramMatrixMaps.get(param).getRight().length > 1) throw new RuntimeException(
                "Specialise which matrix you set: " + "Choose one Simulation " + "type and call " +
                        "setMatrixMap(Matrix " + "matrix, SimulationType " + "type)");
        if (distribution.length != matrices.length) throw new RuntimeException(
                "Inconsistent distribution and " + "matrices lengths! Length " + "dist: " + distribution.length + " " +
                        "length matrices: " + matrices.length);
        this.paramMatrixMaps.get(param).getRight()[0] = new MatrixMap(distribution, matrices);
    }

    /**
     * This method sets one matrix in the array, which contains only one
     * matrix. This method is for constants which do depend on the
     * simulation type
     *
     * @param param        matrix map parameter enum
     * @param distribution the time distribution for the given matrixset
     * @param matrices     new matrixset to set
     * @param type         simulation type
     * @throws RuntimeException if matrix map is simulation independent
     *                          or distribution and matrices length does
     *                          not match
     */
    public void setMatrixMap(ParamMatrixMap param, double[] distribution, Matrix[] matrices, SimulationType type) {
        if (this.paramMatrixMaps.get(param).getRight().length == 1) throw new RuntimeException(
                "This parameter is independant " + "from the simulation type:" + " " + "call setMatrix(Matrix " +
                        "matrix)");
        if (distribution.length != matrices.length) throw new RuntimeException(
                "Inconsistent distribution and " + "matrices lengths! Length " + "dist: " + distribution.length + " " +
                        "length matrices: " + matrices.length);

        this.paramMatrixMaps.get(param).getRight()[type.getIndex()] = new MatrixMap(distribution, matrices);
    }

    /**
     * Method to return the RAW values of all matrix maps
     *
     * @param param matrix map parameter enum
     * @return A String containing the RAW Values of the matrix map array.
     */
    public String toRawString(ParamMatrixMap param) {
        StringBuilder sb = new StringBuilder();
        if (this.isDefined(param)) {
            for (int i = 0; i < this.paramMatrixMaps.get(param).getRight().length; i++) {
                sb.append(paramMatrixMaps.get(param).getRight()[i].toString());
            }
        }
        return sb.toString();
    }

}
