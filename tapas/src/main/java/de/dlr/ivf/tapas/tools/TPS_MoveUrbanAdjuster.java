package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import de.dlr.ivf.tapas.util.Matrix;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

//superfast hacked - no doku!
// Matthias Heinrichs
public class TPS_MoveUrbanAdjuster extends TPS_BasicConnectionClass {

    Matrix interchanges;
    Matrix ttTimes;

    Map<Integer, Integer> timeMod = new HashMap<>();
    Map<Integer, Integer> interchangeMod = new HashMap<>();

    public static void main(String[] args) {
        //repeat this for all time slices: 06_09 , 10_16, 16_19, 19_23
        String interchangeMatrix = "PT_VISUM_1223_2030_1_0_1_06_09_NTR";
        String ttMatrix = "PT_VISUM_1223_2030_1_0_1_06_09_SUM_TT";
        String outputSuffix = "_MU_PT_BOOST";
        String table = "core.berlin_matrices";

        TPS_MoveUrbanAdjuster worker = new TPS_MoveUrbanAdjuster();
        worker.loadInterchanges(interchangeMatrix,table);
        worker.loadTravelTimes(ttMatrix,table);
        worker.fillCellMaps();
        worker.adjustMatrices();
        worker.saveMatrixInDB(ttMatrix+outputSuffix,table,worker.ttTimes);
        worker.saveMatrixInDB(interchangeMatrix+outputSuffix,table,worker.interchanges);


    }

    public void fillCellMaps(){
        //these cell ids are the cells enroute of the new s-bahn line
        this.timeMod.put(467, -10*60);
        this.timeMod.put(511, -8*60);
        this.timeMod.put(514, -6*60);
        this.timeMod.put(520, -4*60);
        this.timeMod.put(518, -2*60);

        //these are the interchangereduchtioons for the cells enroute. note that the values are scaled by 100!
        this.interchangeMod.put(467, -80);
        this.interchangeMod.put(511, -80);
        this.interchangeMod.put(514, -80);
        this.interchangeMod.put(520, -80);
        this.interchangeMod.put(518, -80);
    }

    public void adjustMatrices(){
        boolean rowCellHit, colCellHit;
        for (int j = 0; j < ttTimes.vals.length; ++j) {
            rowCellHit = this.timeMod.containsKey(j+1);
            for (int k = 0; k < ttTimes.vals[j].length; ++k) {
                colCellHit = this.timeMod.containsKey(k+1);
                if(rowCellHit && colCellHit && j!=k){ //direct connection via s-bahn and not same cell!
                    // abs value makes it unneccessary to check which number is higher!
                    double oldTTValue = ttTimes.vals[j][k];
                    int adjustmentTT =  Math.abs(this.timeMod.get(j+1)-this.timeMod.get(k+1));
                    ttTimes.vals[j][k] = Math.min(oldTTValue,adjustmentTT); //only improve!
                    interchanges.vals[j][k] = 0; //direct connection!
                }
                else if(rowCellHit){ //use j as key for maps
                    double oldTTValue = ttTimes.vals[j][k];
                    double oldInterValue = interchanges.vals[j][k];
                    int adjustmentTT =  this.timeMod.get(j+1);
                    int adjustmentInterchange  = this.interchangeMod.get(j+1);
                    ttTimes.vals[j][k] = Math.max(0,oldTTValue+adjustmentTT);
                    interchanges.vals[j][k] = Math.max(0,oldInterValue+adjustmentInterchange);
                }
                else if(colCellHit){ //use k as key for maps
                    double oldTTValue = ttTimes.vals[j][k];
                    double oldInterValue = interchanges.vals[j][k];
                    int adjustmentTT =  this.timeMod.get(k+1);
                    int adjustmentInterchange  = this.interchangeMod.get(k+1);
                    ttTimes.vals[j][k] = Math.max(0,oldTTValue+adjustmentTT);
                    interchanges.vals[j][k] = Math.max(0,oldInterValue+adjustmentInterchange);
                }
            }
        }
    }

    public void loadInterchanges(String name, String tableName) {
        this.interchanges = this.readMatrix(name, tableName);
    }

    public void loadTravelTimes(String name, String tableName) {
        this.ttTimes = this.readMatrix(name, tableName);
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

    public void saveMatrixInDB(String matrixName, String tableName, Matrix saveObject) {
        //store into DB

        //delete old entry
        StringBuilder query = new StringBuilder(
                "DELETE FROM " + tableName + " WHERE matrix_name = '" + matrixName + "'");
        this.dbCon.execute(query.toString(), this);



        query = new StringBuilder("INSERT INTO " + tableName + " VALUES('" + matrixName + "','{");
        StringBuilder buffer;
        for (int j = 0; j < saveObject.vals.length; ++j) {
            buffer = new StringBuilder();
            for (int k = 0; k < saveObject.vals[j].length; ++k) {
                if (k == saveObject.vals[j].length - 1 && j == saveObject.vals.length - 1) buffer.append(
                        Math.round(saveObject.vals[j][k]));
                else buffer.append(Math.round(saveObject.vals[j][k])).append(",");
            }
            query.append(buffer);
        }
        query.append("}' )");
        this.dbCon.execute(query.toString(), this);
    }
}
