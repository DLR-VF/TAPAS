package de.dlr.ivf.tapas.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import de.dlr.ivf.tapas.iteration.TPS_VisumConverter;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

/**
 * Class to convert a visum file containing speeds, traveltimes, distances and beelines to TAPAS-Input.
 * 
 *  This Class provides all necessary functions to read the visum-file, process the intra-infos and write the output back to the db
 * @author hein_mh
 *
 */
public class TPS_VisumToDB {

	
	public static void interactiveStoreInDB(String matrixName, double[][] matrix, TPS_VisumConverter worker ) {
		/**
		 * This method stores tables for traveltimes and distance in the db
		 * @param name the key for the matrix
		 * @return true if successful
		 */

		if (worker.checkMatrixName(matrixName)) {
			System.out.print("Matrix "+matrixName + " already exists. Overwrite? (y/n) ");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in) );
			String answer ="";
			try {
				answer = br.readLine();
			} catch (IOException e) {
				System.out.println("Input error!");
				e.printStackTrace();				
			}
			if(answer.toLowerCase().equals("y")){
				worker.deleteMatrix(matrixName);
			}
			else{
				System.out.println("Skipping matrix "+matrixName);
			}
		}
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws SQLException 
	 */
	public static void main(String[] args) throws IOException, SQLException {
		if(!(args.length == 4 || args.length == 6)){
			System.out.println("Usage: VisumToDB <csv-file> <name> <isPT true/false> <config file> (<compare matrices comma separated> <weight>)");
			return;
		}
		//TODO: Get this file from some property Value
		String file = "T:/Simulationen/runtime_perseus.csv";
		
		File loginFile = new File(file);
		boolean isPT = Boolean.parseBoolean(args[2]);

		TPS_ParameterClass parameterClass = new TPS_ParameterClass();
		parameterClass.loadRuntimeParameters(loginFile);
		TPS_VisumConverter worker = new TPS_VisumConverter(parameterClass, file, args[3]);
		parameterClass.loadSingleParameterFile(new File(args[3]));
		parameterClass.generateTemporaryParameters();

		if(args.length == 6){
			StringTokenizer tok = new StringTokenizer(args[4], ",;");
			if(tok.countTokens()>0){
                String[] array = new String[tok.countTokens()];
				int i=0;
				while(tok.hasMoreTokens()){
					array[i++]=tok.nextToken();
				}
				worker.setCompareMatrix(array);
				worker.setWeight(Double.parseDouble(args[5]));
			}
		}
		worker.setPT(isPT);		
		worker.readIDMap();		
		worker.readCSVFile(args[0]);
		if(args.length == 6)
			worker.mergeMatrices();
		worker.generateIntraCellInfos(0.8); //top3 for tt, dist, beeline factor and speed
						
		String name = args[1];
		
		HashMap <String, double[][]> values = new HashMap<>();
		
		values.put(name+"_TT", worker.getTravelTime());
		values.put(name+"_DIST", worker.getDistance());
		if(isPT){
			values.put(name+"_AT", worker.getAccessTime());
			values.put(name+"_ET", worker.getEgressTime());			
		}
		
		for(Entry<String, double[][]> entry : values.entrySet()){
			interactiveStoreInDB(entry.getKey(), entry.getValue(), worker);
		}
		
		
		System.out.println("All matrices successfully processed!");
			
		if(worker.checkIntraInfos(name)){

			System.out.print("Info name " +name + " already exists. Overwrite? (y/n) ");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in) );
			String answer;
			answer = br.readLine();
			if(answer.toLowerCase().equals("y")){
				worker.deleteIntraInfos(name);
			}
			else{
				System.out.println("Skipping info entries "+name);
			}
		}

		if(worker.storeIntraInfos(args[1])){
			System.out.println("All intra infos sucessfully processed!");
		}		
		System.out.println("End of import");		
	}
}
