package de.dlr.ivf.tapas.tools.fileModifier;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Class for converting the former mode choice tree files resulting from the analysis of datasets with the spss package "answer tree"
 * and the subsequent transformation of the xml-files with the converter "CHAIDfromXML" into an sql-file to store in the db
 */
public class TPS_ModeChoiceTreeConverter {

	public static void main(String[] args){
		
		if(args.length !=3){
			System.out.println("Usage: TPS_ModeChoiceTreeConverter <csv-file> <sql_outputscript> <name>");
			return;
		}
		
		TPS_ModeChoiceTreeConverter worker = new TPS_ModeChoiceTreeConverter(args[0],args[1],args[2]);
		
		System.out.println("Loading input");
		worker.load();
		System.out.println("Saving output");
		worker.save();
		System.out.println("Done!");
	}

	/** map for the conversion of the initial split parameter names into the new constants */
	public Map<String, String> map;

	/** output for the new mode choice tree file */
	File outputFile;

	/** input of the original mode choice tree */
	File inputFile;

	ArrayList<String> treeEntries = new ArrayList<>();
	String identifier = "";
	
	/**
	 * This method imports the mode choice tree. The tree's root is in the first line after the header.
	 */
	public void load() {
		FileReader in = null;	BufferedReader input = null; String line = "";  
		try
		{
			in = new FileReader (inputFile);
			input = new BufferedReader (in);
			System.out.println("\t '--> File opened: "+this.inputFile.getAbsolutePath());
			int id=-1, parent=-1, count;
			String attVal="",splitValue="",dist="", token; 
			
			//read headers
			input.readLine();
			
			while((line = input.readLine()) != null)
			{
				if(line.startsWith("$")) // comment
					continue;
				StringTokenizer tok = new StringTokenizer (line,",");
				//check format
				if(tok.countTokens()!=7)
					continue;
				count =0;
				while(tok.hasMoreTokens()){
					token = tok.nextToken();
					token = token.replaceAll("\"", "");
					if(token.equals("")) //empty string to NULL String
						token = "\\N";
					
					switch(count){
					case 0: //id
						id = Integer.parseInt(token);
						break;
					case 1: //level
						break;
					case 2: //size
						break;
					case 3: //idParent
						parent = Integer.parseInt(token);
						break;
					case 4: //attval
						attVal = token.replaceAll(" ", ",");
						while(attVal.endsWith(","))
							attVal=attVal.substring(0, attVal.length()-1);
						attVal ="{"+attVal+"}";
						break;
					case 5: //splitval
						splitValue = map.get(token);
						if(splitValue == null || splitValue.equals(""))
							splitValue = "\\N";
						break;
					case 6: //distribution						
						dist =token.replaceAll(" ", ",");
						while(dist.endsWith(","))
							dist=dist.substring(0, dist.length()-1);
						dist ="{"+dist+"}";
						break;
					}
					count++;
				}
				this.treeEntries.add(identifier+"\t"+id+"\t"+parent+"\t"+attVal+"\t"+splitValue+"\t"+dist);
			}
			System.out.println("\t '--> Found: "+this.treeEntries.size()+" nodes.");
		}catch (Exception ex) {	
			System.out.println("\t '--> Error: "+ex.getMessage()); 	
			ex.printStackTrace(); 	
		}//catch 
		finally 	{	
			try	{	
				if(input != null)input.close();	
				if(in!= null)	in.close();	
				System.out.println("\t '--> Input file closed: "+this.inputFile.getAbsolutePath());
			}//try 
			catch (Exception ex) {
				System.out.println("\t '--> Could not close : "+this.inputFile.getAbsolutePath());
			}//catch
		}//finally
	}
	

	public TPS_ModeChoiceTreeConverter(String input, String output, String identifier) {

		inputFile = new File(input);

		outputFile = new File(output);

		this.identifier = identifier;

		this.map = new HashMap<>();
		map.put("cars_hh", "HOUSEHOLD_CARS");
		map.put("sex", "PERSON_SEX_CLASS_CODE");
		map.put("dist_cat", "CURRENT_DISTANCE_CLASS_CODE_MCT");
		map.put("age_stba", "PERSON_AGE_CLASS_CODE_STBA");
		map.put("income_hh", "HOUSEHOLD_INCOME_CLASS_CODE");
		map.put("purposeMC", "CURRENT_EPISODE_ACTIVITY_CODE_MCT");
		map.put("driveLic", "PERSON_DRIVING_LICENSE_CODE");
		map.put("bike", "PERSON_HAS_BIKE");
	}

	/**
	 * Save the tree to a sql-file, whch can be imported via psql
	 */
	private void save() {
		FileWriter out;
		try {
			System.out.println("\t '--> Output file opened: "+this.outputFile.getAbsolutePath());
			out = new FileWriter (outputFile);
			
			out.write("SET client_encoding = 'UTF8';\n");
			out.write("SET standard_conforming_strings = on;\n");
						
			out.write("COPY core.global_mode_choice_trees (name, node_id, parent_node_id, attribute_values, split_variable, distribution) FROM stdin;\n");
			for(String entryString : this.treeEntries){
				out.write(entryString+"\n");
			}
			out.write("\\.\n");
			out.close();
			System.out.println("\t '--> Output file closed: "+this.outputFile.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
