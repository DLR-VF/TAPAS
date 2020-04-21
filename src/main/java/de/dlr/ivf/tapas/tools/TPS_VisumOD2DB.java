package de.dlr.ivf.tapas.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import de.dlr.ivf.tapas.util.parameters.ParamString;
/**
 * Class to convert a visum file containing speeds, traveltimes, distances and beelines to TAPAS-Input.
 * 
 *  This Class provides all necessary functions to read the visum-file, process the intra-infos and write the output back to the db
 * @author hein_mh
 *
 */
public class TPS_VisumOD2DB  extends TPS_BasicConnectionClass{

 	/**
 	 * hashmap for id conversion
 	 */
	private HashMap<Integer, Integer> idToIndex = new HashMap<>();
 	/**
 	 * hashmap for id conversion
 	 */
 	private HashMap<Integer, Integer> indexToId = new HashMap<>();

	/**
	 * array for the values
	 */
	private double[][] matrix = null;
	
	/**
	 * @return the matrix
	 */
	public double[][] getMatrix() {
		return matrix;
	}

	
	public void readIDMap( String table)throws SQLException{
		String query ="";
		ResultSet rs = null;
		int taz, externalId;
		try{
			query = "SELECT taz_id, taz_num_id FROM "+table;
			rs = this.dbCon.executeQuery(query, this);
			while(rs.next()){
				taz = rs.getInt("taz_id")-1;
				externalId = rs.getInt("taz_num_id");
				if(externalId>0){
					if(idToIndex.get(externalId)==null){//new tvz
						idToIndex.put(externalId, taz);
						indexToId.put(taz, externalId);
					}					
				}
			}
			rs.close();
			if(TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
				TPS_Logger.log(SeverenceLogLevel.INFO, "Found "+this.idToIndex.size()+" TVZ-IDs");
			}
		}
		catch(SQLException e){
			TPS_Logger.log(SeverenceLogLevel.ERROR, "SQL error! Query: "+query, e);
			throw new SQLException("SQL error! Query: "+query, e);
		}
	}	
	
	public void readCSVFile(String fileName, Double factor) throws IOException{
		FileReader in = null;	BufferedReader input = null; String line = "";  
		try
		{
			int fromTVZ, toTVZ, tvzCounter = 0;
			tvzCounter = this.idToIndex.size();
			String delimiter =" ";
			if(tvzCounter==0){
				in = new FileReader (fileName);
				input = new BufferedReader (in);
				if(TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
					TPS_Logger.log(SeverenceLogLevel.INFO, "File opened: "+fileName);
				}
				//read tvzs
				if(TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
					TPS_Logger.log(SeverenceLogLevel.INFO, "Analyzing TVZ-IDs: ");
				}
				
				while((line = input.readLine()) != null) {
					if(line.startsWith("$") || line.startsWith("*") || !line.startsWith(" ")){ // comment or cell name
						continue;
					}
					String[] tok = line.replaceAll("  ", " ").trim().split(delimiter);
					//check format
					if( tok.length!=3)
						continue;
					
					//get from
					fromTVZ = Integer.parseInt(tok[0].trim());
					
					if(idToIndex.get(fromTVZ)==null){//new tvz
						idToIndex.put(fromTVZ, tvzCounter);
						indexToId.put(tvzCounter, fromTVZ);
						tvzCounter++;
					}
					
					//get to
					toTVZ = Integer.parseInt(tok[1].trim());
					if(idToIndex.get(toTVZ)==null){//new tvz
						idToIndex.put(toTVZ, tvzCounter);
						indexToId.put(tvzCounter, toTVZ);
						tvzCounter++;
					}
				}
				if(TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
					TPS_Logger.log(SeverenceLogLevel.INFO, "Found "+tvzCounter+" TVZ-IDs");
				}
				input.close();
				in.close();
			}
		

			//prepare arrays
			this.matrix = new double[tvzCounter][tvzCounter];
			
			//new local variables
			int indexFrom, indexTo;
			//open input
			in = new FileReader (fileName);
			input = new BufferedReader (in);
			if(TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
				TPS_Logger.log(SeverenceLogLevel.INFO, "File opened: "+fileName);
			}

			while((line = input.readLine()) != null)
			{
				if(line.startsWith("$") || line.startsWith("*") || !line.startsWith(" ")){ // comment or cell name
					continue;
				}
				String[] tok = line.replaceAll("  ", " ").trim().split(delimiter);
				//check format
				if( tok.length!=3)
					continue;
				
				//get from
				fromTVZ = Integer.parseInt(tok[0].trim());				
				indexFrom = idToIndex.get(fromTVZ);
				
				//get to
				toTVZ = Integer.parseInt(tok[1].trim());
				indexTo = idToIndex.get(toTVZ);
				this.matrix[indexFrom][indexTo] = Double.parseDouble(tok[2].trim())*factor;
			}
		} 
		finally 	{	
			try	{	
				if(input != null)input.close();	
				if(in!= null)	in.close();	
			}//try 
			catch (IOException ex) {
				TPS_Logger.log(SeverenceLogLevel.ERROR, " Could not close : "+fileName);
				throw new IOException(ex);
			}//catch
		}//finally
	}	
	
	/**
	 * Internal Method to check for multiple mins. 
	 * @param val the value to test
	 * @param array an array, which holds the array.length minimum values
	 * @return the position, where the value was inserted or -1 if "val" is bigger than every value in "array"
	 */

	private int checkMultiMin(double val, double[] array){
		for(int i=0; i<array.length; ++i){
			if(val<array[i]){
				insertMinAt(val, array, i);			
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Internal method to insert a value at a specific position in the array. All succedion values are shifted one to the left. In C I would use the shift command ;)
	 * @param val the new value
	 * @param array the array to insert into
	 * @param position the position
	 */
	private void insertMinAt(double val, double[] array, int position){
		//shift all other elements one back
		if (array.length - 1 - position >= 0) System.arraycopy(array, position, array, position + 1,
				array.length - 1 - position);
		//copy new value
		array[position]=val;
		
	}
	
	public void generateIntraCellInfos(double weight){
		
		int i,j;
		
		final int length = 3;

		double[] mins=new double[length];
		double intra;

		
		for(i=0; i<this.matrix.length;++i){
			//init the mins
			for(j=0; j<length; ++j){
				mins[j]=1e100;
			}
			//find the mins
			for(j=0; j<this.matrix[0].length; ++j){
				if(i!=j){ //diagonal is zero!
					checkMultiMin(this.matrix[i][j],mins);
				}
			}
			// init the avg
			intra=0;
			//build average
			for(j=0; j<length;++j){
				intra+=mins[j];
			}
			intra*=weight/length; //intraTT is half the average to the n closest cells
			this.matrix[i][i]=intra;

		}			
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException, SQLException {
		if(args.length != 4){
			System.out.println("Usage: TPS_VisumOD2DB <visum-file> <factor> <db-name> <entry-name>");
			return;
		}
			
		TPS_VisumOD2DB worker = new TPS_VisumOD2DB();
		worker.readIDMap("core.berlin_taz");		

		worker.readCSVFile(args[0], Double.parseDouble(args[1]));
		
		worker.generateIntraCellInfos(0.8); //top3 

		worker.getParameters().setString(ParamString.DB_TABLE_MATRICES, args[2]);
		worker.storeInDB(args[3], worker.getMatrix(), 0);
		
		
		System.out.println("Matrix successfully processed!");
			
	}
}
