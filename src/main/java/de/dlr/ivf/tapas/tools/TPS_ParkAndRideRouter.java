package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import de.dlr.ivf.tapas.util.Matrix;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class TPS_ParkAndRideRouter extends TPS_BasicConnectionClass {

    List<IntermodalRoute> connections = new ArrayList<>();
    boolean[] TAZInfo = null;
    Matrix miv = null, pt = null, walk = null;
    int[][] interchangeMatrix = null;

    public static void main(String[] args) {
        TPS_ParkAndRideRouter worker = new TPS_ParkAndRideRouter();
        worker.loadMatrices("CAR_1193TAZ_TT_MORNING", "PT_VISUM_1193_CALCULATED_AUG_2014_SUM_TT", "WALK_1193TAZ_TT",
                "core.berlin_matrices");
        worker.loadTAZRestriction("Berlin_Umweltzone", "core.berlin_taz_fees_tolls");
        worker.routePnR(300);
        Matrix pnr = worker.createPandRMatrix();
        worker.storeMatrixInDB(pnr, "Berlin_PNR_TT_1193", "core.berlin_matrices");
        worker.saveInterchangeMatrixInDB("Berlin_PNR_INTERCHANGE_1193", "core.berlin_matrices");
    }

    public Matrix createPandRMatrix() {
        if (this.TAZInfo == null || this.TAZInfo.length == 0) {
            //nothing to create
            return null;
        }
        int siz = this.TAZInfo.length, pnrRoutes = 0, classicRoutes = 0;
        Map<Integer, Integer> histogram = new HashMap<>();
        //build Matrix
        Matrix matrix = new Matrix(siz, siz);
        this.interchangeMatrix = new int[siz][siz];
        for (IntermodalRoute route : this.connections) {
            double val = 0;
            int change;
            for (IntermodalNode node : route.legs) {
                val += node.duration + node.durationTransfer;

            }
            if (route.legs.size() > 1) {
                change = route.legs.get(0).idEnd; // the interchange node
                pnrRoutes++;
                Integer difference = (int) ((val - this.miv.getValue(route.idStart, route.idEnd)) / 60);

                Integer num = histogram.get(difference);
                if (num == null) {
                    num = 0;
                }
                num++;
                histogram.put(difference, num);
            } else {
                classicRoutes++;
                change = -1;
            }

            //store in mem
            interchangeMatrix[route.idStart][route.idEnd] = change;
            matrix.setValue(route.idStart, route.idEnd, val);

        }

        System.out.println("Zeitdifferenz in Minuten\tAnzahl der Routen\tGesamtanzahl PnR-Routen:\t" + pnrRoutes +
                "\tGesamtanzahl Routen ohne PnR:\t" + classicRoutes + "\tVerkerszellen:" + this.TAZInfo.length);
        for (Entry<Integer, Integer> e : histogram.entrySet()) {
            System.out.println(e.getKey() + "\t" + e.getValue());
        }

        return matrix;

    }

    public void loadMatrices(String mivName, String ptName, String walkName, String tableName) {
        this.miv = this.readMatrix(mivName, tableName);
        this.pt = this.readMatrix(ptName, tableName);
        this.walk = this.readMatrix(walkName, tableName);
    }

    public void loadTAZRestriction(String tazInfo, String table) {
        String query = "select ft_taz_id, is_restricted from " + table + " where ft_name = '" + tazInfo + "'";
        if (this.miv == null || this.miv.getNumberOfColums() == 0) {
            System.err.println("Error in matrix sizes: Load matrices first!");
            return;
        }
        this.TAZInfo = new boolean[this.miv.getNumberOfColums()];
        try {
            ResultSet rs = this.dbCon.executeQuery(query, this);
            while (rs.next()) {
                this.TAZInfo[(rs.getInt("ft_taz_id") - 1)] = rs.getBoolean(
                        "is_restricted"); //the columns come ordered from the database!
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("Error during sql-statement: " + query);
            e.printStackTrace();
            e.getNextException().printStackTrace();
        }
    }

    /**
     * Bad copy&paste from TPS_DB_IO, but I do not want to set up a PM-Manager
     *
     * @param matrixName
     * @param tableName
     */
    protected Matrix readMatrix(String matrixName, String tableName) {
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

    public void routePnR(double transferTime) {
        int i, j, k, size;
        size = this.miv.getNumberOfColums();
        Map<Integer, Integer> changeSpots = new HashMap<>();

        for (i = 0; i < size; ++i) {
            for (j = 0; j < size; ++j) {
                IntermodalRoute newRoute = new IntermodalRoute();
                newRoute.idStart = i;
                newRoute.idEnd = j;
                //simple way= no restriction!
                if (!this.TAZInfo[i] && !this.TAZInfo[j]) {
                    IntermodalNode newNode = new IntermodalNode();
                    newNode.idStart = i;
                    newNode.idEnd = j;
                    newNode.duration = this.miv.getValue(i, j);
                    newNode.mode = ModeType.MIT;
                    newRoute.legs.add(newNode);
                }
                //simple way: both restricted!
                else if (this.TAZInfo[i] && this.TAZInfo[j]) {
                    IntermodalNode newNode = new IntermodalNode();
                    newNode.idStart = i;
                    newNode.idEnd = j;
                    newNode.duration = this.pt.getValue(i, j);
                    newNode.mode = ModeType.PT;
                    newRoute.legs.add(newNode);
                } else { //source or dest restricted
                    Matrix source = this.TAZInfo[i] ? this.pt : this.miv; //if restricted use PT
                    Matrix dest = this.TAZInfo[j] ? this.pt : this.miv; //if restricted use PT
                    ModeType sourceMode = this.TAZInfo[i] ? ModeType.PT : ModeType.MIT;
                    ModeType destMode = this.TAZInfo[j] ? ModeType.PT : ModeType.MIT;
                    /* ok folks: here we have to do something!
                     * 1st: search for the best connection between pt/miv, which is NOT restricted!
                     * 2nd store it in the leg-schema
                     */
                    double bestTime = Double.MAX_VALUE, actTime = 0;
                    int bestIndex = -1;
                    for (k = 0; k < size; ++k) {
                        if (!this.TAZInfo[k] && k != i && k != j) {
                            actTime = source.getValue(i, k) + dest.getValue(k, j);
                            if (actTime < bestTime) {
                                bestIndex = k;
                                bestTime = actTime;
                            }
                        }
                    }


                    Integer num = changeSpots.get(bestIndex);
                    if (num == null) {
                        num = 0;
                    }
                    num++;
                    changeSpots.put(bestIndex, num);

                    IntermodalNode startNode = new IntermodalNode();
                    startNode.idStart = i;
                    startNode.idEnd = bestIndex;
                    startNode.duration = source.getValue(i, bestIndex);
                    startNode.durationTransfer = transferTime;
                    startNode.mode = sourceMode;
                    IntermodalNode endNode = new IntermodalNode();
                    endNode.idStart = bestIndex;
                    endNode.idEnd = j;
                    endNode.duration = dest.getValue(bestIndex, j);
                    endNode.mode = destMode;

                    newRoute.legs.add(startNode);
                    newRoute.legs.add(endNode);
                }
                this.connections.add(newRoute);
            }
        }
        System.out.println("TAZ-ID\tUmstiege");
        for (Entry<Integer, Integer> e : changeSpots.entrySet()) {
            System.out.println((e.getKey() + 1) + "\t" + e.getValue());
        }
    }

    public void saveInterchangeMatrixInDB(String matrixName, String tableName) {
        //store into DB

        //delete old entry
        StringBuilder query = new StringBuilder(
                "DELETE FROM " + tableName + " WHERE matrix_name = '" + matrixName + "'");
        this.dbCon.execute(query.toString(), this);

        query = new StringBuilder("INSERT INTO " + tableName + " VALUES('" + matrixName + "','{");
        StringBuilder buffer;
        for (int j = 0; j < this.interchangeMatrix.length; ++j) {
            buffer = new StringBuilder();
            for (int k = 0; k < this.interchangeMatrix[j].length; ++k) {
                if (k == this.interchangeMatrix[j].length - 1 && j == this.interchangeMatrix.length - 1) buffer.append(
                        Math.round(this.interchangeMatrix[j][k]));
                else buffer.append(Math.round(this.interchangeMatrix[j][k])).append(",");
            }
            query.append(buffer);
        }
        query.append("}' )");
        this.dbCon.execute(query.toString(), this);
    }

    public void storeMatrixInDB(Matrix matrix, String matrixName, String tableName) {

        //store into DB
        //delete old entry
        StringBuilder query = new StringBuilder(
                "DELETE FROM " + tableName + " WHERE matrix_name = '" + matrixName + "'");
        this.dbCon.execute(query.toString(), this);

        query = new StringBuilder("INSERT INTO " + tableName + " VALUES('" + matrixName + "','{");
        StringBuilder buffer;
        for (int j = 0; j < matrix.getNumberOfRows(); ++j) {
            buffer = new StringBuilder();
            for (int k = 0; k < matrix.getNumberOfColums(); ++k) {
                if (k == matrix.getNumberOfColums() - 1 && j == matrix.getNumberOfRows() - 1) buffer.append(
                        Math.round(matrix.getValue(j, k)));
                else buffer.append(Math.round(matrix.getValue(j, k))).append(",");
            }
            query.append(buffer);
        }
        query.append("}' )");
        this.dbCon.execute(query.toString(), this);
    }

    class IntermodalNode {
        int idStart = -1;
        int idEnd = -1;
        ModeType mode = ModeType.WALK;
        double length = 0;
        double duration = 0;
        double cost = 0;
        double durationTransfer = 0;
        double parkingCostsPerHour = 0;

    }

    class IntermodalRoute {
        int idStart;
        int idEnd;
        List<IntermodalNode> legs = new ArrayList<>();
    }

}
