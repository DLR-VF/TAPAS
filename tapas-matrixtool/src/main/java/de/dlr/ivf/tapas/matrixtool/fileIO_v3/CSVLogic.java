package de.dlr.ivf.tapas.matrixtool.fileIO_v3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import de.dlr.ivf.tapas.matrixtool.common.datastructures.Indexing;

/*
 * the matrix doesnt have to be quadratic. it is defined in the QUAD-HeaderPart.
 * in the data-part a bufferunit is always i;v1;...;vN (non-quad) or i;j;vN (quad).
 * this logic just puts such a line in the container, which will treat it according
 * to if the container is quadratic or not. 
 */
public class CSVLogic extends AbstractFormatLogic {
	
	private static final String COMMENT = "#";
	private static final int CHARS_PER_UNIT = 100; 
	//!!!!!!! each value/name in a csv-file can have at most this much characters !!!!
	private enum HeaderPart{
		//must be in THIS order
		OBJ ("list of objects"),
		QUAD ("is datastructure quadratic"),
		ATT ("list of attributes");
		
		String comment;
		
		HeaderPart(String comment){
			this.comment = comment;
		}
	}	
	private String DELIM;

	
	public CSVLogic(int numberOfProcessors, String delim) {
		
		super(numberOfProcessors);
		this.DELIM = delim;
	}

	public LinkedList<IFormatDataProcessor> getDataProcessors(){
		
		LinkedList<IFormatDataProcessor> dataProcs = new LinkedList<IFormatDataProcessor>();
		for (int i = 0; i < numberOfProcessors; i++){
			dataProcs.add(new CSVUnitProcessor(DELIM));
		}
		return dataProcs;
	}

	public HeaderData gatherHeaderInfo(BufferedReader reader) throws IOException, 
	FormatLogicException {
		
		HeaderData header = new HeaderData();

		processHeader(header, HeaderPart.OBJ, CHARS_PER_UNIT, reader);
		processHeader(header, HeaderPart.QUAD, CHARS_PER_UNIT, reader);
		/*
		 * if header is not quadratic, then a attributes-part will follow.
		 * otherwise there is no attributes-part. the data-part will follow
		 * immediatly instead.
		 */
		if (!header.isQuadratic())
			//attributes
			processHeader(header, HeaderPart.ATT, CHARS_PER_UNIT * header.getAttributes().size(),
					reader);
		
		return header;
	}
	
	private void processHeader(HeaderData header, HeaderPart part, int read_ahead,
			BufferedReader reader) throws IOException, FormatLogicException {
		
		String line = null;
		boolean commentAlreadyFound = false;	//a headerpart starts with a comment
		boolean firstIter = true;

		while (true){

			reader.mark(read_ahead);
			line = reader.readLine();

			if (line == null){
//				throw new FormatLogicException("header ends unsuspectedly in part '" +
//						part.name() + "'");
				throw new FormatLogicException(part.name());
				/*
				 * that could mean, that the part after "part" could not be determined
				 * right.
				 */
			}
			
			if (line.trim().length() == 0)
//				throw new FormatLogicException("blank lines not allowed");
				throw new FormatLogicException(part.name());

			if (!line.startsWith(COMMENT) && firstIter){
//				throw new FormatLogicException("part does not begin with a '" +
//						COMMENT + "' (part '"+part+"')");
				throw new FormatLogicException(part.name());
			}
				
			if (line.startsWith(COMMENT)){
				if (commentAlreadyFound){
					reader.reset();
					break;	// to next headerpart
				} else {
					commentAlreadyFound = true; // beginning of a headerpart
				}

				firstIter = false;
				continue;
			}


			switch (part){
			case OBJ:
				header.addObject(line.trim());
				break;
			case QUAD:
				header.setQuadratic(Boolean.parseBoolean(line.trim()));
				break;
			case ATT:
				header.addAttribute(line.trim());
				break;
			}

		}
	}


	protected void fillBufferImplementation(BufferedReader reader, 
			LinkedBlockingQueue<IBufferUnit> buffer) throws IOException, 
			InterruptedException, FormatLogicException{

		String line = null;

		boolean commentAlreadyFound = false;
		while ((line = reader.readLine()) != null){

			if (line.startsWith(COMMENT)){
				if (commentAlreadyFound){
//					throw new FormatLogicException("no comment allowed in data part ('" + 
//							line + "')");
					throw new FormatLogicException();
				} else {
					commentAlreadyFound = true;
				}
				continue;
			}

			int idx = line.indexOf(DELIM);

			buffer.put(new DataBufferUnit(
					line.substring(0, idx), 
					line.substring(idx + 1, line.length())
			));
		}
	}

	public void writeHeader(IFileDataContainer container, BufferedWriter writer,
			String eol) throws IOException {


		writer.write(COMMENT + " " + HeaderPart.OBJ.comment + eol);
		Indexing<String> objects = container.getObjects();
		for (String o : objects.getKeySetInIndexedOrder()){
			writer.write(o + eol);
		}

		writer.write(COMMENT + " " + HeaderPart.QUAD.comment + eol);
		writer.write((container.isQuadratic() ? "true" : "false") + eol);

		if (!container.isQuadratic()){
			Indexing<String> atts = container.getAttributes();
			
			writer.write(COMMENT + " " + HeaderPart.ATT.comment + " : ");
			for (String a : atts.getKeySetInIndexedOrder()){
				writer.write(a + DELIM);
			}
			writer.write(eol);
			
			for (String a : atts.getKeySetInIndexedOrder()){
				writer.write(a + eol);
			}				
		}
	}
	
	public void writeData(IFileDataContainer container, BufferedWriter writer,
			String eol) throws IOException, FormatLogicException{
		
		String[] lineTokens = null;
		StringBuffer line = null;
		int readAhead;
		ArrayList<String> objects = container.getObjects().getKeySetInIndexedOrder();


		if (container.isQuadratic()){

			readAhead = 3 * container.getObjects().size() * CHARS_PER_UNIT;

			writer.write(COMMENT + " from" + DELIM + "to" + DELIM + "value" + eol);
			for (String o : objects){

				line = new StringBuffer(readAhead);

				for (String to : objects){
					line.append(o + DELIM + to + DELIM + container.getValue(o, to) + eol);
				}

				writer.write(line.toString());
			}

		} else {

			ArrayList<String> attributes = container.getAttributes().getKeySetInIndexedOrder();

			readAhead = container.getAttributes().size() * CHARS_PER_UNIT;

			line = new StringBuffer(COMMENT + " object" + DELIM);
			for (String a : attributes){
				line.append(a + DELIM);
			}

			writer.write(line.substring(0, line.length() - 1) + eol); // last DELIM

			for (String o : objects){

				line = new StringBuffer(readAhead);
				line.append(o + DELIM);

				lineTokens = container.getLineByID(o);
				for (int i = 0; i < lineTokens.length; i++){
					line.append(lineTokens[i] + DELIM);
				}

				writer.write(line.substring(0, line.length() - 1) + eol); // last DELIM
			}
		}
	}
}
