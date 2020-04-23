package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class TPS_MatrixComparison extends TPS_BasicConnectionClass {

    int[][] matrix1 = null, matrix2 = null, weightMatrix = null;

    Map<Integer, Integer> result = new TreeMap<>();

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        String table = "core.berlin_matrices";
        String trips = "berlin_trips_2018y_11m_12d_16h_49m_38s_210ms";
        int[] modes = {5};
        int binSize = 60;
        int binSizeTransfer = 10;
        String matrix1, matrix2, pattern1, pattern2;
        TPS_MatrixComparison worker = new TPS_MatrixComparison();
        String path = "T:\\Runs\\evoBus\\Basisstatistik\\Runde 2\\Iterationen\\1.0.1-2.0.1\\diff_trips_";
        pattern1 = "PT_VISUM_1223_2030_1_0_1_%02d_%02d_";
        pattern2 = "PT_VISUM_1223_2030_2_0_1_%02d_%02d_";

        int[] startMatrixArray = {6, 10, 16, 19};
        int[] endMatrixArray = {9, 16, 19, 23};

        for (int i = 0; i < startMatrixArray.length; i++) {
            matrix1 = String.format(pattern1, startMatrixArray[i], endMatrixArray[i]);
            matrix2 = String.format(pattern2, startMatrixArray[i], endMatrixArray[i]);
            worker.analyzeThis(table, matrix1 + "SUM_TT", matrix2 + "SUM_TT", path, binSize);
            //worker.analyzeThisTripWeighted(table, trips,matrix1+"SUM_TT", matrix2+"SUM_TT", modes,path,binSize);
            worker.analyzeThis(table, matrix1 + "ACT", matrix2 + "ACT", path, binSize);
            //worker.analyzeThisTripWeighted(table, trips,matrix1+"ACT", matrix2+"ACT", modes,path,binSize);
            worker.analyzeThis(table, matrix1 + "EGT", matrix2 + "EGT", path, binSize);
            //worker.analyzeThisTripWeighted(table, trips,matrix1+"EGT", matrix2+"EGT", modes,path,binSize);
            //the transfer has a different bin size!
            worker.analyzeThis(table, matrix1 + "NTR", matrix2 + "NTR", path, binSizeTransfer);
            //worker.analyzeThisTripWeighted(table, trips,matrix1+"NTR", matrix2+"NTR", modes,path,binSizeTransfer);
        }

//		matrix1= "PT_VISUM_1223__2030_HOCHFEIN60_6_9_IT0_ACT";
//		matrix2= "PT_VISUM_1223_2030_HF20_1_ACT";
//		worker.analyzeThis(table, matrix1, matrix2);
//
//		matrix1= "PT_VISUM_1223__2030_HOCHFEIN60_6_9_IT0_EGT";
//		matrix2= "PT_VISUM_1223_2030_HF20_1_EGT";
//		worker.analyzeThis(table, matrix1, matrix2);
//
//		matrix1= "PT_VISUM_1223__2030_HOCHFEIN60_6_9_IT0_SUM_TT";
//		matrix2= "PT_VISUM_1223_2030_HF20_1_SUM_TT";
//		worker.analyzeThis(table, matrix1, matrix2);

    }

    public void analyze(int radix) {
        //size check
        if (matrix1.length != matrix2.length) return;
        if (matrix1[0].length != matrix2[0].length) return;
        Integer count, bin;
        this.result.clear();
        int maxVal, minVal, maxCount, minCount;
        Map<Integer, Integer> oMax = new TreeMap<>();
        Map<Integer, Integer> oMin = new TreeMap<>();
        for (int i = 0; i < matrix1.length; ++i) {
            maxVal = 0;
            minVal = 0;
            maxCount = 0;
            minCount = 0;
            for (int j = 0; j < matrix1[i].length; ++j) {
                if (matrix1[i][j] < 0 || matrix2[i][j] < 0) continue;
                if (this.weightMatrix == null) count = 1;
                else count = this.weightMatrix[i][j];
                //calc val
                bin = matrix1[i][j] - matrix2[i][j];
                //count values with an bigger/smaller val
                if (bin > 0) {
                    maxVal += bin;
                    maxCount++;
                }
                if (bin < 0) {
                    minVal += bin;
                    minCount++;
                }

                //calc radix
                bin = (bin / radix) * radix;
                //fetch old value if existand
                if (this.result.containsKey(bin)) count += this.result.get(bin);
                //store value
                this.result.put(bin, count);
            }
            if (maxCount > 0) {
                oMax.put(-maxVal / maxCount, i);//minus to get biggest values first!
            } else {
                oMax.put(0, i);
            }
            if (minCount > 0) {
                oMin.put(minVal / minCount, i);
            } else {
                oMin.put(0, i);
            }
        }
        System.out.println("Max: ");
        int num = 0;
        for (Entry<Integer, Integer> entry : oMax.entrySet()) {
            System.out.println(entry.getKey() + "\t" + entry.getValue());
            num++;
            if (num > 10) break;
        }
        num = 0;
        System.out.println("Min: ");
        for (Entry<Integer, Integer> entry : oMin.entrySet()) {
            System.out.println(entry.getKey() + "\t" + entry.getValue());
            num++;
            if (num > 10) break;
        }
    }

    public void analyzeThis(String table, String matrix1, String matrix2, String path, int binSize) {
        this.loadMatrixces(table, matrix1, matrix2);
        //deklete the weightmatrix!
        this.weightMatrix = null;
        this.analyze(binSize);
        this.writeDiffData(path + matrix1 + "_" + matrix2 + ".csv");

    }

    public void analyzeThisTripWeighted(String table, String trips, String matrix1, String matrix2, int[] modes, String path, int binSize) {
        this.loadMatrixces(table, matrix1, matrix2);
        this.loadTrips(trips, modes);
        this.analyze(binSize);
        this.writeDiffData(path + "_trip_" + matrix1 + "_" + matrix2 + ".csv");

    }

    public int[][] loadMatrix(String table, String name) {
        String query = "";
        ResultSet rs;
        int[][] returnVal = new int[0][0];
        try {
            query = "SELECT matrix_values from " + table + " where matrix_name = '" + name + "'";
            rs = this.dbCon.executeQuery(query, this);
            if (rs.next()) {
                int[] val = TPS_DB_IO.extractIntArray(rs, "matrix_values");
                returnVal = intArray1Dto2D(val);
            } else {
                System.out.println("No result for query " + query);
                System.exit(0);
            }
            rs.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return returnVal;
    }

    public void loadMatrixces(String table, String matrix1, String matrix2) {
        this.matrix1 = this.loadMatrix(table, matrix1);
        this.matrix2 = this.loadMatrix(table, matrix2);
    }

    public void loadTrips(String trips, int[] modes) {
        if (this.matrix1 == null || this.matrix2 == null) {
            System.err.println("Load matrices first!");
            return;
        }
        if (this.matrix1.length == 0 || this.matrix2.length == 0) {
            System.err.println("Matrices are empty!");
            return;
        }
        if (modes.length == 0) {
            System.err.println("No modes given!");
            return;
        }


        this.weightMatrix = new int[matrix1.length][matrix1[0].length];

        try {
            ResultSet rs;
            StringBuilder query = new StringBuilder(
                    "SELECT taz_id_start,taz_id_end  from " + trips + " where mode =" + " ANY" + "(ARRAY[");
            for (int i = 0; i < modes.length - 1; i++) {
                query.append(modes[i]).append(",");
            }
            //last element
            query.append(modes[modes.length - 1]).append("])");

            rs = this.dbCon.executeQuery(query.toString(), this);
            while (rs.next()) {
                int from = rs.getInt("taz_id_start");
                int to = rs.getInt("taz_id_end");
                this.weightMatrix[from - 1][to - 1]++;
            }
            rs.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void writeDiffData(String filename) {
        FileWriter fw;
        try {
            fw = new FileWriter(new File(filename));

            fw.write("Val\tNum\n");
            for (Entry<Integer, Integer> e : this.result.entrySet()) {
                fw.write(e.getKey() + "\t" + e.getValue() + "\n");
            }


            fw.close();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

    }

}
