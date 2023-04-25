package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import de.dlr.ivf.tapas.model.Matrix;
import de.dlr.ivf.tapas.parameter.ParamString;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * This method tries to integrate the changes of a subnet matrix to the whole matrix.
 * Steps to do so:
 * Load subnet matrix and original outer matrix
 * Load mapping of the different cells
 * Define cordon points:
 *   For a given connection between A in the subnet and B in the outer net do:
 *   Search over all points in the sub net the cell C with a minimum value of: (AC+CB)-AB
 *   Update the Value AB with the difference of AC between the original net and the subnet
 * Update all connections in the original matrix, which lie within the subnet
 * Save the new matrix
 */

public class TPS_SubnetMatrixIntegrator extends TPS_BasicConnectionClass {


    Matrix subnet;
    Matrix outernet;

    int subOffset =Integer.MAX_VALUE;
    int outerOffset =Integer.MAX_VALUE;

    Map<Integer, Integer> mappingSubnet =new HashMap<>();
    Map<Integer, Integer> mappingOuternet =new HashMap<>();
    Map<Integer, Integer> mappingIndices =new HashMap<>();
    Set<Integer> outerTAZ = new TreeSet<>();

    int[][] cordonPoints;

    /**Reads the input. Needs DB_TABLE_MATRICES and DB_TABLE_TAZ to be set. The column taz_num_id from DB_TABLE_TAZ and
     * subnetTAZ is used for mapping between the subnet and the outer net.
     * First it reads all outer TAZ Ids from DB_TABLE_TAZ and fill the mappingOuternet-map.
     * Second read the same for the subnet
     * Third create a mapping between sdubnet and outernet
     * Fourth load the matrices
     * @param subnet The subnetmatrix to load
     * @param outernet the outer matrix to load
     * @param subnetTAZ the information about the subnetTAZ
     */
    public void loadMatricesAndMappings(String subnet, String outernet, String subnetTAZ){
        //load the mappings
        String query = "";
        ResultSet rs = null;
        try{
            int tazIndex,tazNum;
            String tazName=this.parameterClass.getString(
                    ParamString.DB_TABLE_TAZ);
            //do we have the taz table?
            if(tazName != null && tazName.length()>0) {
                query = "SELECT taz_id, taz_num_id FROM " + tazName;
                rs = this.dbCon.executeQuery(query, this);

                //clear old mapping in case we run this method twice
                this.mappingOuternet.clear();
                while (rs.next()) {
                    tazIndex = rs.getInt("taz_id");
                    tazNum = rs.getInt("taz_num_id");
                    this.outerOffset = Math.min(this.outerOffset, tazIndex);
                    this.mappingOuternet.put(tazNum, tazIndex);
                }
                rs.close();
            }
            //do we have the subnet taz table?
            if(subnetTAZ != null && subnetTAZ.length()>0) {
                query = "SELECT taz_id, taz_num_id FROM " + subnetTAZ;
                rs = this.dbCon.executeQuery(query, this);
                //clear old mapping in case we run this method twice
                mappingSubnet.clear();
                while (rs.next()) {
                    tazIndex = rs.getInt("taz_id");
                    tazNum = rs.getInt("taz_num_id");
                    this.subOffset = Math.min(this.subOffset, tazIndex);
                    this.mappingSubnet.put(tazNum, tazIndex);
                }
            }
            rs.close();
        } catch (SQLException throwables) {
            System.err.println("Error in sql-query: " + query);
            throwables.printStackTrace();
        }

        //prefill the outer tazes
        this.outerTAZ.clear();
        for( Map.Entry<Integer,Integer> e : this.mappingOuternet.entrySet()){
            int outerIndex = e.getValue();
            this.outerTAZ.add(outerIndex);
        }

        //calc the mapping
        this.mappingIndices.clear();
        for( Map.Entry<Integer,Integer> e : this.mappingSubnet.entrySet()){
            int innerIndex = e.getValue();
            int outerIndex = this.mappingOuternet.get(e.getKey());
            this.mappingIndices.put(innerIndex, outerIndex);
            //remove the found taz from the outer TAZ set
            this.outerTAZ.remove(outerIndex);
        }

        //load the matrices
        if(subnet != null && subnet.length()>0) {
            this.subnet = this.dbCon.readMatrix(subnet, this.subOffset, this);
        }
        if(outernet !=null && outernet.length()>0) {
            this.outernet = this.dbCon.readMatrix(outernet, this.outerOffset, this);
        }
    }

    public Matrix updateValues(){
        Matrix result = this.outernet.clone();

        /*
         * Define cordon points:
         *   For a given connection between A in the subnet and B in the outer net do:
         *   Search over all points in the sub net the cell C with a minimum value of: (AC+CB)
         *   Update the Value AB with the value of (AC+CB) between the original net and the subnet
         * Update all connections in the original matrix, which lie within the subnet
         */
        int a,b,c, minTime, ab, ac,acOrig, cb, cIndex, minTimeOrig;
        for( Map.Entry<Integer,Integer> inner : this.mappingSubnet.entrySet()) {
            a = inner.getValue();
            for (Integer outer : this.outerTAZ) {
                b = outer; // just to keep the names straight
                minTime = Integer.MAX_VALUE;
                minTimeOrig = ab = (int) this.outernet.getValue(this.mappingIndices.get(a), b);
                cIndex = -1;
                for (Map.Entry<Integer, Integer> innerC : this.mappingSubnet.entrySet()) {
                    c = innerC.getValue();
                    if (a != c) { //only update values, which need a third point
                        ac = (int) this.subnet.getValue(a, c);
                        acOrig = (int) this.outernet.getValue(this.mappingIndices.get(a), this.mappingIndices.get(c));
                        cb = (int) this.outernet.getValue(this.mappingIndices.get(c), b);
                        if ((ac + cb) < minTime) {
                            cIndex = c;
                            minTime = (ac + cb);
                            minTimeOrig = acOrig +cb; // save the deviation because of the detour to compensate the detourerror
                        }
                    }
                }
                //set the difference
                if (cIndex >= 0) {
                    result.setValue(a, b, minTime -(minTimeOrig-ab)); //compensate the detourerror
                }
            }
            //now set the subnetcells itself
            for (Map.Entry<Integer, Integer> innerB : this.mappingSubnet.entrySet()) {
                b = innerB.getValue();
                if (a != b) { //never touch the main diagonal!
                    result.setValue(a, b, this.subnet.getValue(a, b));
                }
            }
        }
        return result;
    }

    public void saveMatrix(String name, Matrix m){

        String query = "INSERT INTO "+parameterClass.getString(
                ParamString.DB_TABLE_MATRICES)+" VALUES ('" + name + "', "+ TPS_DB_IO.matrixToSQLArray(m, 0) + ")";
        dbCon.execute(query,this);
        System.out.println("Matrix "+name+" saved.");

    }

    /**
     * Method to extract a submatrix from the "outernet" for a given list if tazes.
     * @param submatrixName the name to store the Matrix in the db
     */

    public boolean extractSubmatrix(String submatrixName){

        if(submatrixName ==null | submatrixName.length()==0)
            return false;
        int a,b;
        a = this.mappingSubnet.size();
        Matrix result = new Matrix(a,a, this.subOffset);
        for( Map.Entry<Integer,Integer> inner : this.mappingIndices.entrySet()) {
            a = inner.getValue();
            for (Map.Entry<Integer, Integer> innerB : this.mappingIndices.entrySet()) {
                b = innerB.getValue();
                result.setValue(inner.getKey(), innerB.getKey(), this.outernet.getValue(a, b));
            }
        }
        this.saveMatrix(submatrixName,result);
        return true;
    }

    /**
     * The main entry point.
     *
     *
     * @param args
     */
    public static void main(String[] args) {
        TPS_SubnetMatrixIntegrator worker = new TPS_SubnetMatrixIntegrator();
        worker.parameterClass.setString(
                ParamString.DB_TABLE_MATRICES,"core.berlin_matrices");
        worker.parameterClass.setString(
                ParamString.DB_TABLE_TAZ,"core.berlin_taz_1223");
        String subNetTAZ= "core.berlin_move_urban_taz_1223", subNetName = null;
        subNetName="move_urban_bike_tt_hd";
        worker.loadMatricesAndMappings(subNetName, "BIKE_TT_HD", subNetTAZ);
        Matrix Result = worker.updateValues();
        worker.saveMatrix("BIKE_TT_HD_MOVE_URBAN",Result);


        //worker.extractSubmatrix(subNetName);
    }
}
