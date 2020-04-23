package de.dlr.ivf.scripts;

import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SQLBackupExtractor extends TPS_BasicConnectionClass {

	/**
	 * Internal class to process a textblock between a given pair of start/end-tokens
	 * @author hein_mh
	 *
	 */
	class BlockProcessor{
		String startToken ="";
		String endToken ="";
		String fileName="";
		/**
		 * optional filter value for every line in the output block. 
		 * If set, only lines containing this string will be written. 
		 * Leave it to null if no filter is needed.
		 */
		String filter =null;
		int blockNumber=0;

		public BlockProcessor(String start, String end, String fileName){
			this.startToken=start;
			this.endToken=end;
			this.fileName=fileName;
		}
		
		boolean startFound=false;
		/**
		 * Should the start token be included to the output?
		 */
		public boolean includeStart=true;
		/**
		 * Should the end token be included to the output?
		 */
		public boolean includeEnd=true;
		
		BufferedWriter fw =null;
		
		int lineCount=0;
		
		/**
		 * Method to generate a valid fileNAme
		 * @return the next Filename
		 */
		private String generateFilename(){
			String returnVal;
			
			if(fileName.contains("%")){ //we have a formated String
				returnVal = String.format(fileName, blockNumber); 
			}
			else{
				returnVal = fileName+blockNumber+".txt";
			}
			
			return returnVal;
		}
		
		/**
		 * Method to process a line:
		 * If Start token is found a new block and output file is created
		 * If end is found the actual block is closed
		 * All lines in between are written to the output, if the line matches the filter or no filter is set.
		 * @param lineToTest the line to test
		 */
		public void processLine(String lineToTest) {
				
			try {
				if(lineToTest==null){ // this marks eof!
					System.out.println("Found end of file!");
					if(fw!=null){
						System.out.println("Closing file: "+this.generateFilename()+" with "+this.lineCount+" lines written");
						fw.close();
						fw = null;
						startFound = false;
						blockNumber++;
					}					
				}
				if (startFound) { // we are in the block
					if (lineToTest.contains(endToken)) { // we reached the end of the block
						if (this.includeEnd) {
							fw.append(lineToTest+"\n");
							lineCount++;
						}
						System.out.println("Found end of block: <"+lineToTest +">\nClosing file: "+this.generateFilename()+" with "+this.lineCount+" lines written.");
						fw.close();
						fw = null;
						startFound = false;
						blockNumber++;
					} else {
						if(!(filter != null && !lineToTest.contains(filter))){
							fw.append(lineToTest+"\n");
							lineCount++;
						}
					}

				} else {
					if (lineToTest.contains(startToken)) { // we found a new starting block
						lineCount=0;
						startFound = true;
						System.out.println("Found start: <"+lineToTest +">\nWriting block to: "+this.generateFilename());
						fw = new BufferedWriter(new FileWriter(this.generateFilename()));
						if (this.includeStart) {
							fw.append(lineToTest+"\n");
							lineCount++;
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}				
		
		/**
		 * Close remaining files.
		 */
		public void finalize(){
			if(fw != null){
				try {
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				fw = null;
			}
		}
	}
	
	List<BlockProcessor> processors = new ArrayList<>();
	
	/**
	 * Method tpo process a given file. It opens the text-file and scans every line, 
	 * if it contains a start/end token of one or more block processors. 
	 * All lines between these start/end tokens are written to the block processor output file scheme.  
	 * A new start token creates a new file! 
	 * @param fileName the file to scan
	 */
	public void processFile(String fileName){
		try {
			BufferedReader fr = new BufferedReader(new FileReader(fileName));
			String line;
			do{
				line=fr.readLine();
				for(BlockProcessor e: processors){
					e.processLine(line);
				}
			}while(line!=null);
			fr.close();			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Method to create a new processor for filtering elements. This processor will be added to the internal list of BlockProcessors
	 * @param start Start token
	 * @param end End token
	 * @param fileName Filename pattern to write to
	 * @param includeStart boolean if start token should be included
	 * @param includeEnd boolean if end token should be included
	 * @return the created processor
	 */
	public BlockProcessor addProcessor(String start, String end, String fileName,boolean includeStart, boolean includeEnd){
		BlockProcessor b = new BlockProcessor (start,end,fileName);
		b.includeStart= includeStart;
		b.includeEnd = includeEnd;
		this.processors.add(b);
		return b;
	}
	
	public static void main(String[] args) {
		SQLBackupExtractor worker = new SQLBackupExtractor();
		BlockProcessor b;
		b = worker.addProcessor("COPY braunschweig_taz_fees_tolls","\\.","D:\\tmp\\bsw%d.sql",false,false);
		b.filter=null;
		//b.filter="MID2008_Y2010";
		worker.processFile("D:\\tmp\\backuptapas-perseus-2017-04-08.sql");
		
	}

}
