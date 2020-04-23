package de.dlr.ivf.scripts;

import de.dlr.ivf.tapas.iteration.TPS_VisumConverter;
import de.dlr.ivf.tapas.mode.TPS_Mode;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import de.dlr.ivf.tapas.util.TPS_Geometrics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;

public class OTP2SQLMatrix extends TPS_BasicConnectionClass {
    double[][] matrixTT = null;
    double[][] matrixDist = null;
    double[][] matrixAcc = null;
    double[][] matrixEgr = null;
	
	
	public void readCSVFile(String fileName, boolean isPT){
		FileReader in;	BufferedReader input; String line;
		try
		{
			int minIndex=0x0fffffff;
			int maxIndex =0;
			int from=0, to=0, size;
			double time =0, acc=0, egr=0, dist=0;
			// read the file for min/max-values
			in = new FileReader (fileName);
			input = new BufferedReader (in);
			while((line = input.readLine()) != null)
			{
				StringTokenizer tok = new StringTokenizer (line);
				if(tok.countTokens()==7){
					from = Integer.parseInt(tok.nextToken());
					to = Integer.parseInt(tok.nextToken());
				}
				minIndex= Math.min(minIndex,  Math.min(from, to));
				maxIndex= Math.max(maxIndex,  Math.max(from, to));				
			}
			input.close();
			in.close();
			
			size = maxIndex-minIndex+1;
			
			this.matrixTT 	= new double[size][size];
			this.matrixAcc 	= new double[size][size];
			this.matrixEgr 	= new double[size][size];
			this.matrixDist = new double[size][size];
			//fill with no connection
			for(int i=0; i< size;++i){
				for( int j= 0; j< size; ++j){
					this.matrixTT[i][j] = this.matrixAcc[i][j] = this.matrixEgr[i][j] = this.matrixDist[i][j] = TPS_Mode.NO_CONNECTION;
				}
			}
			// read the file for time-values
			in = new FileReader (fileName);
			input = new BufferedReader (in);
			while((line = input.readLine()) != null)
			{
				StringTokenizer tok = new StringTokenizer (line);
				if(tok.countTokens()==7){
					from = Integer.parseInt(tok.nextToken())-minIndex;
					to = Integer.parseInt(tok.nextToken())-minIndex;
					time = Double.parseDouble(tok.nextToken());
					dist = Double.parseDouble(tok.nextToken());
					acc = Double.parseDouble(tok.nextToken());
					egr = Double.parseDouble(tok.nextToken());
				}
				
				if(isPT){ 
					this.matrixTT[from][to]= time;
					//this.matrixTT[from][to]= time-acc-egr; // in public transport the travel time includes access and egress times!
				}
				else{
					this.matrixTT[from][to]= time;
				}
				this.matrixAcc[from][to]= acc;
				this.matrixEgr[from][to]= egr;
				this.matrixDist[from][to]= dist;
				
			}						
			input.close();
			in.close();
			
			//check
			for(int i=0; i< this.matrixTT.length;++i){
				for( int j= 0; j< this.matrixTT[i].length; ++j){
					if(this.matrixTT[i][j]<1 && i!=j){
						System.out.println("No connection from "+i+" to "+j);
					}
				}
			}
			
			System.out.println("Input processed!");
		}
		catch (IOException e){
			e.printStackTrace();
		}
	}
	
	public void fixMissing(){
		int i,j,size;
		size= this.matrixTT.length;
		
		for(i=0; i<size; ++i){
			for(j=0; j<size; ++j){
				
				if(i!=j && this.matrixTT[i][j]==TPS_Mode.VISUM_NO_CONNECTION && this.matrixTT[j][i]!=TPS_Mode.VISUM_NO_CONNECTION){
					this.matrixTT[i][j]=this.matrixTT[j][i];
				}
				
				if(i!=j && this.matrixDist[i][j]==TPS_Mode.VISUM_NO_CONNECTION && this.matrixDist[j][i]!=TPS_Mode.VISUM_NO_CONNECTION){
					this.matrixDist[i][j]=this.matrixDist[j][i];
				}
				
				if(i!=j && this.matrixAcc[i][j]==TPS_Mode.VISUM_NO_CONNECTION && this.matrixEgr[j][i]!=TPS_Mode.VISUM_NO_CONNECTION){
					this.matrixAcc[i][j]=this.matrixEgr[j][i];
				}

				if(i!=j && this.matrixEgr[i][j]==TPS_Mode.VISUM_NO_CONNECTION && this.matrixAcc[j][i]!=TPS_Mode.VISUM_NO_CONNECTION){
					this.matrixEgr[i][j]=this.matrixAcc[j][i];
				}

				if(i!=j && this.matrixTT[i][j]==TPS_Mode.VISUM_NO_CONNECTION && this.matrixTT[j][i]==TPS_Mode.VISUM_NO_CONNECTION){
					System.out.println("Not fixable connection from "+i+" to "+j);
				}
			}
		}
	}
					
	public void calcTop3(){
		TPS_Geometrics.calcTop3(this.matrixTT);
		TPS_Geometrics.calcTop3(this.matrixAcc);
		TPS_Geometrics.calcTop3(this.matrixEgr);
		TPS_Geometrics.calcTop3(this.matrixDist);
	}
	
	public void writeSQLScript(String output, String name, boolean isPT){
		
		try {
			FileWriter writer = new FileWriter(output);
			writer.append("DELETE FROM core.berlin_matrices WHERE matrix_name ='"+name+"_tt'; \n");
			writer.append("INSERT INTO core.berlin_matrices VALUES ('"+name+"_tt', \n");
			writer.append(TPS_VisumConverter.matrixToSQLArray(matrixTT, 0)+");\n");									
			writer.append("DELETE FROM core.berlin_matrices WHERE matrix_name ='"+name+"_dist'; \n");
			writer.append("INSERT INTO core.berlin_matrices VALUES ('"+name+"_dist', \n");
			writer.append(TPS_VisumConverter.matrixToSQLArray(matrixDist, 0)+");\n");
			if(isPT){
				writer.append("DELETE FROM core.berlin_matrices WHERE matrix_name ='"+name+"_acc'; \n");
				writer.append("INSERT INTO core.berlin_matrices VALUES ('"+name+"_acc', \n");
				writer.append(TPS_VisumConverter.matrixToSQLArray(matrixAcc, 0)+");\n");									
				writer.append("DELETE FROM core.berlin_matrices WHERE matrix_name ='"+name+"_egr'; \n");
				writer.append("INSERT INTO core.berlin_matrices VALUES ('"+name+"_egr', \n");
				writer.append(TPS_VisumConverter.matrixToSQLArray(matrixEgr, 0)+");\n");									
			}
			
			
			writer.close();
		}
		catch (IOException e){
			e.printStackTrace();
		}
		
		
	}
	
	public void checkDBMatrix(String matrixName){
		
		String query = "";
		try{
			query = "SELECT matrix_values FROM core.berlin_matrices WHERE matrix_name='" + matrixName + "'";
			ResultSet rs = dbCon.executeQuery(query, this);
			if (rs.next()) {
				int[] iArray = TPS_DB_IO.extractIntArray(rs, "matrix_values");
				rs.close();
				int len = (int) Math.sqrt(iArray.length);
				double[][] matrix 	= new double[len][len];
				int index =0;
				for(int i=0; i< len;++i){
					for( int j= 0; j< len; ++j){
						matrix[i][j]= iArray[index];
						index++;
					}
				}
				for(int i=0; i< len;++i){
					for( int j= 0; j< len; ++j){
						if(matrix[i][j]==TPS_Mode.VISUM_NO_CONNECTION || matrix[i][j]<=0.1){
							System.out.println("Not fixable connection from "+i+" to "+j);
							matrix[i][j] = TPS_Mode.NO_CONNECTION;
						}
					}
				}
				FileWriter writer = new FileWriter("C:\\temp\\car_tt.sql");
				writer.append("DELETE FROM core.berlin_matrices WHERE matrix_name ='"+matrixName+"'; \n");
				writer.append("INSERT INTO core.berlin_matrices VALUES ('"+matrixName+"', \n");
				writer.append(TPS_VisumConverter.matrixToSQLArray(matrix, 0)+");\n");		
				writer.close();
				
			}
		}catch(SQLException e){
			System.err.println(this.getClass().getCanonicalName()+" checkDBMatrix: SQL-Error during statement: "+query);
			e.printStackTrace();
		}
		catch (IOException e){
			e.printStackTrace();
		}
		
		
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length!=4){
			System.out.println("Usage: OTP2SQLMatrix <inputfile> <outputfile> <matrixName Pattern> <pt-flag: 1/0>");
			return;
		}
		String input = args[0];
		String output = args[1];
		String name = args[2];
		boolean isPT = args[3].equals("1");
		OTP2SQLMatrix worker = new OTP2SQLMatrix();		
		worker.checkDBMatrix("INITIAL_SUMO_MIV_TIMES_TAZ_1193");
		worker.readCSVFile(input,isPT);
		worker.fixMissing();
		worker.calcTop3();
		worker.writeSQLScript(output, name, isPT);

	}
}
