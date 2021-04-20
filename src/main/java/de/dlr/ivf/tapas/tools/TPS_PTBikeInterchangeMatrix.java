/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import de.dlr.ivf.tapas.util.Matrix;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;


public class TPS_PTBikeInterchangeMatrix extends TPS_BasicConnectionClass {
    double[][] interchanges;
    double[][] bikeTT;
    double[][] ptTT;
    int size;

    int[][] entryPoints = null;
    int[][] exitPoints = null;
    double[][] numInterchanges = null;
    int next = 0;

    public static void main(String[] args) {
        TPS_PTBikeInterchangeMatrix worker = new TPS_PTBikeInterchangeMatrix();
        worker.loadAndInitMatrices("PT_VISUM_1193_CALCULATED_AUG_2015_NTR", "BIKE_1193TAZ_TT_14KMH",
                "PT_VISUM_1193_CALCULATED_AUG_2015_SUM_TT", "core.berlin_matrices");
        worker.computeInterchanges();
        worker.saveInterchangeMatrixInDB("PTBIKE_ACCESS_TAZ_1193", "PTBIKE_EGRESS_TAZ_1193",
                "PTBIKE_INTERCHANGES_TAZ_1193", "core.berlin_matrices");
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

    public void loadAndInitMatrices(String interchangesName, String bikeTTName, String ptTTName, String tableName) {
        Matrix Tinterchanges = this.readMatrix(interchangesName, tableName);
        Matrix TbikeTT = this.readMatrix(bikeTTName, tableName);
        Matrix TptTT = this.readMatrix(ptTTName, tableName);
        size = TptTT.getNumberOfColums();
        interchanges = new double[size][size];
        bikeTT = new double[size][size];
        ptTT = new double[size][size];
        for (int i = 0; i < size; ++i) {
            for (int j = 0; j < size; ++j) {
                interchanges[i][j] = Tinterchanges.getValue(i, j);
                bikeTT[i][j] = TbikeTT.getValue(i, j);
                ptTT[i][j] = TptTT.getValue(i, j);
            }
        }
        entryPoints = new int[size][size];
        exitPoints = new int[size][size];
        numInterchanges = new double[size][size];
    }

    private Matrix readMatrix(String matrixName, String tableName) {
        Matrix m = null;
        String query = "SELECT matrix_values FROM " + tableName + " WHERE matrix_name='" + matrixName + "'";
        try {
            ResultSet rs = this.dbCon.executeQuery(query, this);
            if (rs.next()) {
                int[] iArray = TPS_DB_IO.extractIntArray(rs, "matrix_values");
                int len = (int) Math.sqrt(iArray.length);
                m = new Matrix(len, len);
                for (int index = 0; index < iArray.length; index++) {
                    m.setRawValue(index, iArray[index]);
                }
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("Error during sql-statement: " + query);
            e.printStackTrace();
            e.getNextException().printStackTrace();
        }
        return m;
    }

    public void saveInterchangeMatrixInDB(String entriesName, String exitsName, String interchangesName, String tableName) {
        //store into DB
        //delete old entry
        String query = "DELETE FROM " + tableName + " WHERE matrix_name='" + entriesName + "'";
        this.dbCon.execute(query, this);
        query = "DELETE FROM " + tableName + " WHERE matrix_name='" + exitsName + "'";
        this.dbCon.execute(query, this);
        query = "DELETE FROM " + tableName + " WHERE matrix_name='" + interchangesName + "'";
        this.dbCon.execute(query, this);
        StringBuilder entriesBuffer = new StringBuilder();
        StringBuilder exitsBuffer = new StringBuilder();
        StringBuilder interchangesBuffer = new StringBuilder();
        for (int j = 0; j < this.entryPoints.length; ++j) {
            for (int k = 0; k < this.entryPoints[j].length; ++k) {
                if (k == this.entryPoints[j].length - 1 && j == this.entryPoints.length - 1) {
                    entriesBuffer.append(Math.round(this.entryPoints[j][k]));
                    exitsBuffer.append(Math.round(this.exitPoints[j][k]));
                    interchangesBuffer.append(Math.round(this.numInterchanges[j][k]));
                } else {
                    entriesBuffer.append(Math.round(this.entryPoints[j][k])).append(",");
                    exitsBuffer.append(Math.round(this.exitPoints[j][k])).append(",");
                    interchangesBuffer.append(Math.round(this.numInterchanges[j][k])).append(",");
                }
            }
        }
        query = "INSERT INTO " + tableName + " VALUES('" + entriesName + "', '{" + entriesBuffer.toString() + "}' )";
        this.dbCon.execute(query, this);
        query = "INSERT INTO " + tableName + " VALUES('" + exitsName + "', '{" + exitsBuffer.toString() + "}' )";
        this.dbCon.execute(query, this);
        query = "INSERT INTO " + tableName + " VALUES('" + interchangesName + "', '{" + interchangesBuffer.toString() +
                "}' )";
        this.dbCon.execute(query, this);
    }

    private static class ComputingThread implements Runnable {
        TPS_PTBikeInterchangeMatrix parent;

        public ComputingThread(TPS_PTBikeInterchangeMatrix _parent) {
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
                    int minExit = -1;
                    double minDuration = -1;
                    for (int e1 = 0; e1 < parent.size; ++e1) {
                        for (int e2 = 0; e2 < parent.size; ++e2) {
                            if (parent.interchanges[e1][e2] < 0) {
                                continue;
                            }
                            if (parent.bikeTT[i][e1] < 0) {
                                continue;
                            }
                            if (parent.ptTT[e1][e2] < 0) {
                                continue;
                            }
                            if (parent.bikeTT[e2][j] < 0) {
                                continue;
                            }
                            double duration = parent.bikeTT[i][e1];
                            duration += parent.ptTT[e1][e2];
                            duration += parent.bikeTT[e2][j];
                            if (minDuration < 0 || duration < minDuration) {
                                if (minEntry != i && minExit != j) {
                                    minDuration = duration;
                                    minEntry = e1;
                                    minExit = e2;
                                }
                            }
                        }
                    }
                    double cInterchanges;
                    if (minDuration < 0) {
                        minEntry = -1;
                        minExit = -1;
                        cInterchanges = -1;
                    } else {
                        cInterchanges = parent.interchanges[minEntry][minExit];// + 200.;
                    }
                    parent.entryPoints[i][j] = minEntry;
                    parent.exitPoints[i][j] = minExit;
                    parent.numInterchanges[i][j] = cInterchanges;
                    System.out.println("" + i + ";" + j + ";" + minEntry + ";" + minExit + ";" + cInterchanges);
                }
            } while (i >= 0);
        }
    }

}
