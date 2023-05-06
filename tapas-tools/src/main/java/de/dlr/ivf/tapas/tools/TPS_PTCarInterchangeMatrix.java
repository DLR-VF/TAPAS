/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.model.Matrix;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;


public class TPS_PTCarInterchangeMatrix  {
    double[][] interchanges;
    double[][] carTT;
    double[][] carAccess;
    double[][] carEgress;
    double[][] ptTT;
    double[][] ptAccess;
    double[][] ptEgress;
    int size;

    int[][] entryPoints = null;
    double[][] numInterchanges = null;
    int next = 0;

    public static void main(String[] args) {
        TPS_PTCarInterchangeMatrix worker = new TPS_PTCarInterchangeMatrix();
        worker.loadMatrices("PT_VISUM_1193_CALCULATED_AUG_2015_NTR", "MIT_ACCESS_VEU2_MID2008_Y2010_REF",
                "CAR_1193_2010_T0_TT_TOP3", "MIT_EGRESS_VEU2_MID2008_Y2010_REF",
                "PT_VISUM_1193_CALCULATED_AUG_2015_ACT", "PT_VISUM_1193_CALCULATED_AUG_2015_NTR",
                "PT_VISUM_1193_CALCULATED_AUG_2015_EGT", "core.berlin_matrices");
        worker.computeInterchanges();
        worker.saveInterchangeMatrixInDB("PTCAR_ACCESS_TAZ_1193", "PTCAR_INTERCHANGES_TAZ_1193",
                "core.berlin_matrices");
    }

    public void computeInterchanges() {
        int numThreads = 32;
        Vector<Thread> threads = new Vector<>();
        for (int i = 0; i < numThreads; ++i) {
            Thread t = new Thread(new ComputingThread(this));
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }

    public synchronized int getNextIndex() {
        if (next < size) {
            return next++;
        }
        return -1;
    }

    public void loadMatrices(String interchangesName, String carAccessName, String carTTName, String carEgressName, String ptAccessName, String ptTTName, String ptEgressName, String tableName) {
        Matrix Tinterchanges = this.readMatrix(interchangesName, tableName);
        Matrix TcarAccess = this.readMatrix(carAccessName, tableName);
        Matrix TcarTT = this.readMatrix(carTTName, tableName);
        Matrix TcarEgress = this.readMatrix(carEgressName, tableName);
        Matrix TptAccess = this.readMatrix(ptAccessName, tableName);
        Matrix TptTT = this.readMatrix(ptTTName, tableName);
        Matrix TptEgress = this.readMatrix(ptEgressName, tableName);
        size = TptTT.getNumberOfColums();
        interchanges = new double[size][size];
        carAccess = new double[size][size];
        carTT = new double[size][size];
        carEgress = new double[size][size];
        ptAccess = new double[size][size];
        ptTT = new double[size][size];
        ptEgress = new double[size][size];
        for (int i = 0; i < size; ++i) {
            for (int j = 0; j < size; ++j) {
                interchanges[i][j] = Tinterchanges.getValue(i, j);
                carAccess[i][j] = TcarAccess.getValue(i, j);
                carTT[i][j] = TcarTT.getValue(i, j);
                carEgress[i][j] = TcarEgress.getValue(i, j);
                ptAccess[i][j] = TptAccess.getValue(i, j);
                ptTT[i][j] = TptTT.getValue(i, j);
                ptEgress[i][j] = TptEgress.getValue(i, j);
            }
        }
        entryPoints = new int[size][size];
        numInterchanges = new double[size][size];
    }

    private Matrix readMatrix(String matrixName, String tableName) {
        Matrix m = null;
        String query = "SELECT matrix_values FROM " + tableName + " WHERE matrix_name='" + matrixName + "'";
//        try {
//            ResultSet rs = this.dbCon.executeQuery(query, this);
//            if (rs.next()) {
//                int[] iArray = TPS_DB_IO.extractIntArray(rs, "matrix_values");
//                int len = (int) Math.sqrt(iArray.length);
//                m = new Matrix(len, len);
//                for (int index = 0; index < iArray.length; index++) {
//                    m.setRawValue(index, iArray[index]);
//                }
//            }
//            rs.close();
//        } catch (SQLException e) {
//            System.err.println("Error during sql-statement: " + query);
//            e.printStackTrace();
//            e.getNextException().printStackTrace();
//        }
        return m;
    }

    public void saveInterchangeMatrixInDB(String entriesName, String interchangesName, String tableName) {
        //store into DB
        //delete old entry
        String query = "DELETE FROM " + tableName + " WHERE matrix_name='" + entriesName + "'";
//        this.dbCon.execute(query, this);
        query = "DELETE FROM " + tableName + " WHERE matrix_name='" + interchangesName + "'";
//        this.dbCon.execute(query, this);
        StringBuilder entriesBuffer = new StringBuilder();
        StringBuilder interchangesBuffer = new StringBuilder();
        for (int j = 0; j < this.entryPoints.length; ++j) {
            for (int k = 0; k < this.entryPoints[j].length; ++k) {
                if (k == this.entryPoints[j].length - 1 && j == this.entryPoints.length - 1) {
                    entriesBuffer.append(Math.round(this.entryPoints[j][k]));
                    interchangesBuffer.append(Math.round(this.numInterchanges[j][k]));
                } else {
                    entriesBuffer.append(Math.round(this.entryPoints[j][k])).append(",");
                    interchangesBuffer.append(Math.round(this.numInterchanges[j][k])).append(",");
                }
            }
        }
        query = "INSERT INTO " + tableName + " VALUES('" + entriesName + "', '{" + entriesBuffer.toString() + "}' )";
//        this.dbCon.execute(query, this);
        query = "INSERT INTO " + tableName + " VALUES('" + interchangesName + "', '{" + interchangesBuffer.toString() +
                "}' )";
//        this.dbCon.execute(query, this);
    }

    private static class ComputingThread implements Runnable {
        TPS_PTCarInterchangeMatrix parent;

        public ComputingThread(TPS_PTCarInterchangeMatrix _parent) {
            super();
            parent = _parent;
        }

        public void run() {
            int i = 0;
            do {
                i = parent.getNextIndex();
                if (i < 0) {
                    continue;
                }
                for (int j = 0; j < parent.size; ++j) {
                    int minEntry = -1;
                    double minDuration = -1;
                    for (int e1 = 0; e1 < parent.size; ++e1) {
                        if (parent.interchanges[e1][j] < 0) {
                            continue;
                        }
                        if (parent.carTT[i][e1] < 0) {
                            continue;
                        }
                        if (parent.ptTT[e1][j] < 0) {
                            continue;
                        }
                        double duration = 0;
                        duration += parent.carAccess[i][e1] + parent.carTT[i][e1] + parent.carEgress[i][e1] * 1.;
                        duration += parent.ptAccess[i][e1] * 1. + parent.ptTT[e1][j] + parent.ptEgress[i][e1];
                        if (minDuration < 0 || duration < minDuration) {
                            if (minEntry != i && minEntry != j) {
                                minDuration = duration;
                                minEntry = e1;
                            }
                        }
                    }
                    double cInterchanges;
                    if (minDuration < 0) {
                        minEntry = -1;
                        cInterchanges = -1;
                    } else {
                        cInterchanges = parent.interchanges[minEntry][j] + 100.;
                    }

                    parent.entryPoints[i][j] = minEntry;
                    parent.numInterchanges[i][j] = cInterchanges;
                    System.out.println("" + i + ";" + j + ";" + minEntry + ";" + cInterchanges);
                }


            } while (i >= 0);
        }
    }

}
