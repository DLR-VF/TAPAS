/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.model.MatrixLegacy;


public class TPS_FeederCellManipulator {
	double[][] busShare;
	double[][] ptTT;
	
	double[][] newPTTime = null;



	

		
	
	
	private MatrixLegacy readMatrix(String matrixName, String tableName) {
		MatrixLegacy m = null;
		String query= "SELECT matrix_values FROM " + tableName + " WHERE matrix_name='" + matrixName + "'";
//		try{
//			ResultSet rs = this.dbCon.executeQuery(query,this);
//			if (rs.next()) {
//				int[] iArray = TPS_DB_IO.extractIntArray(rs, "matrix_values");
//				int len = (int) Math.sqrt(iArray.length);
//				m = new MatrixLegacy(len, len);
//				for (int index = 0; index < iArray.length; index++) {
//					m.setRawValue(index, iArray[index]);
//				}
//			}
//			rs.close();
//		} catch(SQLException e){
//			System.err.println("Error during sql-statement: "+query);
//			e.printStackTrace();
//			e.getNextException().printStackTrace();
//		}
		return m;
	}
	
	
	public void loadAndInitMatrices(String busShareName, String ptTTName, String tableName){

		MatrixLegacy busShareM = this.readMatrix(busShareName, tableName);
		MatrixLegacy TptTT = this.readMatrix(ptTTName, tableName);
		busShare = new double[busShareM.getNumberOfColums()][busShareM.getNumberOfRows()];
		ptTT = new double[busShareM.getNumberOfColums()][busShareM.getNumberOfRows()];
		for(int i=0; i<busShareM.getNumberOfColums(); ++i) {
			for(int j=0; j<busShareM.getNumberOfRows(); ++j) {
				busShare[i][j] = busShareM.getValue(i, j);
				ptTT[i][j] = TptTT.getValue(i, j);
			}
		}

		newPTTime = new double[busShareM.getNumberOfColums()][busShareM.getNumberOfRows()];
	}
	
	


	
	public void saveMatrixInDB(String entriesName, double[][] vals, String tableName){
		//store into DB
		//delete old entry
		String query = "DELETE FROM "+tableName+" WHERE matrix_name='"+entriesName+"'";
//		this.dbCon.execute(query,this);

		StringBuilder entriesBuffer = new StringBuilder();

		for (int j = 0; j < vals.length; ++j) {
			for (int k = 0; k < vals[j].length; ++k) {
				if (k == vals[j].length - 1 && j ==vals.length - 1) {
					entriesBuffer.append(Math.round(vals[j][k]));

				} else {
					entriesBuffer.append(Math.round(vals[j][k])).append(",");

				}
			}
		}
		query = "INSERT INTO "+tableName+" VALUES('"+entriesName+"', '{"+entriesBuffer.toString()+"}' )";
//		this.dbCon.execute(query,this);
	}
	
	
	public static void main(String[] args) {
		TPS_FeederCellManipulator worker = new TPS_FeederCellManipulator();
		worker.loadAndInitMatrices("berlin_ramona_bus_percentage_factor1000_1223", "PT_VISUM_1223_2030_REF_10_16_SUM_TT", "core.berlin_matrices");

	}
	
}
