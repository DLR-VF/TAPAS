package de.dlr.ivf.scripts;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.model.Matrix;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class InterCellTrafficImporter {

    Map<Integer,Double> readIntraCellValues(String table){
        Map<Integer,Double> intraCellValues = new HashMap<>();
        String query = "";
//        try {
//            query = "SELECT median(avg_distance) as dist,taz FROM " + table + " group by taz";
//            ResultSet rs = dbCon.executeQuery(query, this);
//            while (rs.next()) {
//                int taz = rs.getInt("taz");
//                double dist = rs.getDouble("dist");
//                intraCellValues.put(taz,dist);
//            }
//            rs.close();
//
//        } catch (SQLException e) {
//            System.err.println(this.getClass().getCanonicalName() + " readIntraCellValues: SQL-Error during statement: " + query);
//            e.printStackTrace();
//        }

        System.out.println(intraCellValues.size()+" intra-cell values read.");

        return intraCellValues;
    }

    Matrix readMatrix(String name, String table){
        String query = "SELECT matrix_values FROM " + table +
                " WHERE matrix_name='" + name + "'";
//        ResultSet rs = dbCon.executeQuery(query,this);
//        Matrix m =null;
//        try{
//            if (rs.next()) {
//                int[] iArray = TPS_DB_IO.extractIntArray(rs, "matrix_values");
//                int len = (int) Math.sqrt(iArray.length);
//                m= new Matrix(len, len, 0);
//                for (int index = 0; index < iArray.length; index++) {
//                    m.setRawValue(index, iArray[index]);
//                }
//            }
//        } catch (SQLException e) {
//            System.err.println(this.getClass().getCanonicalName() + " readMatrix: SQL-Error during statement: " + query);
//            e.printStackTrace();
//        }
//
//        System.out.println("Matrix "+name+" read. Size: "+m.getNumberOfColums()+" x "+ m.getNumberOfRows());
//
//        return m;
        return null;
    }

    void setInterCellValues(Matrix target, Map<Integer,Double> diagonalValues, double factor){
        int minTAZ = Integer.MAX_VALUE;
        //calc the TAZ-offset
        for(Integer key : diagonalValues.keySet()){
            minTAZ = Math.min(minTAZ,key);
        }

        //now set the diagonals
        int count =0;
        for(Map.Entry<Integer,Double> e : diagonalValues.entrySet()){
            int index = e.getKey()-minTAZ;
            double value= e.getValue()*factor;
            target.setValue(index,index,Math.round(value));
            count++;
        }
        System.out.println(count+" values updated in the diagonal");

    }

    public void saveMatrix(String name, String table, Matrix m){

        String query = "INSERT INTO "+table+" VALUES ('" + name + "', "+ TPS_DB_IO.matrixToSQLArray(m, 0) + ")";
       // dbCon.execute(query,this);
        System.out.println("Matrix "+name+" saved.");

    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        String matrixTable="core.berlin_matrices";
        String intraTable = "intra_taz.berlin_out";
        Matrix m; //will be reused
        InterCellTrafficImporter worker = new InterCellTrafficImporter();
        Map<Integer,Double> intraCellValues = worker.readIntraCellValues(intraTable);
        //distances
        m = worker.readMatrix("WALK_IHK_DIST",matrixTable);
        worker.setInterCellValues(m,intraCellValues,1); //1m/s
        worker.saveMatrix("WALK_DIST_HD",matrixTable,m);
        //walk TT = speed 3,6km/h = 1m/s
        m = worker.readMatrix("WALK_IHK_TT",matrixTable);
        worker.setInterCellValues(m,intraCellValues,1); //1m/s
        worker.saveMatrix("WALK_TT_HD",matrixTable,m);
        //bike TT = speed 14 km/h
        m = worker.readMatrix("BIKE_IHK_TT",matrixTable);
        worker.setInterCellValues(m,intraCellValues,3.6/14.0); //14km/h in m/s
        worker.saveMatrix("BIKE_TT_HD",matrixTable,m);
        //CAR TT = speed 28 km/h
        m = worker.readMatrix("CAR_IHK_TT",matrixTable);
        worker.setInterCellValues(m,intraCellValues,3.6/28.0); //14km/h in m/s
        worker.saveMatrix("CAR_TT_HD",matrixTable,m);

    }
}
