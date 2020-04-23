package de.dlr.ivf.scripts;

import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.PLAN)
public class SumoTripVisualization {
	private static final String table = "core.berlin_routes";

	
	class Route{
		String name;
		double[] xCoord;
		double[] yCoord;
		
		public Route(String name, int length){
			this.name = name;
			xCoord = new double[length];
			yCoord = new double[length];
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return the xCoord
		 */
		public double[] getxCoord() {
			return xCoord;
		}

		/**
		 * @return the yCoord
		 */
		public double[] getyCoord() {
			return yCoord;
		}		
	}
	
	private List<Route> myRoutes = new ArrayList<>();

	
	public static void main(String[] args) throws IOException{
		File configFile = new File("T:/Simulationen/runtime.csv");
		TPS_ParameterClass parameterClass = new TPS_ParameterClass();
		parameterClass.loadRuntimeParameters(configFile);

		SumoTripVisualization creator = new SumoTripVisualization();
		creator.readFile("T:\\Outputdateien\\route_sel_empty_net\\mh\\georoutes.txt");
		creator.writeSQLQuery("T:\\Outputdateien\\route_sel_empty_net\\mh\\georoutes.sql");
		
	}

	public void writeSQLQuery(String filename) throws IOException{
		
		try {
			FileWriter writer = new FileWriter(filename);
			
			for(Route s: this.myRoutes){
				StringBuilder command = new StringBuilder(
						"INSERT INTO " + SumoTripVisualization.table + " VALUES ('" + s.getName() +
								"', st_transform(GeomFromText('LINESTRING(");
				int size = s.getxCoord().length;
				for(int i=0; i< size; ++i){
					command.append(s.getxCoord()[i]).append(" ").append(s.getyCoord()[i]);
					if(i<size-1)
						command.append(",\n");
				}
				
				
				command.append(")', 4326),31467));\n");
				writer.append(command.toString());
			}
			
			writer.flush();
			writer.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void readFile(String fileName) {

		FileReader in;
		BufferedReader input;
		String line;
		try {
			// open input
			in = new FileReader(fileName);
			input = new BufferedReader(in);
			if(TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
				TPS_Logger.log(SeverenceLogLevel.INFO, "File opened: " + fileName);
			}

			String actToken;
			double xPos, yPos;
			int tokenCount;
			while ((line = input.readLine()) != null) {
				StringTokenizer tok = new StringTokenizer(line, " ");
				tokenCount = tok.countTokens();
				if(tokenCount>1){
					actToken = tok.nextToken(); //name										
					Route newRoute = new Route(actToken, tokenCount-1);
					//set the tokens
					int pos =0;
					while(tok.hasMoreElements()){
						actToken = tok.nextToken(); //act position
						StringTokenizer tok2 = new StringTokenizer(actToken, ",");
						if(tok2.countTokens()==2){
							xPos = Double.parseDouble(tok2.nextToken().trim());
							yPos = Double.parseDouble(tok2.nextToken().trim());
							newRoute.getxCoord()[pos]=xPos;
							newRoute.getyCoord()[pos]=yPos;
							pos++;
						}
						else{
							System.err.println("Unexpected format! Line: "+line+ " position: "+actToken);
							input.close();
							return;
						}
					}
					if(pos==tokenCount-1)
						this.myRoutes.add(newRoute);
				}
			}
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void finalize()
	 {
		//this.st.close();
		//this.con.close();
	 }
}
