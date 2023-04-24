package de.dlr.ivf.tapas.matrixtool.fileIO_v3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import de.dlr.ivf.tapas.matrixtool.common.constants.VisumHeaderConfigChar;
import de.dlr.ivf.tapas.matrixtool.common.constants.VisumHeaderValueType;

/*
 * the matrix is always quadratic in the Visum-V-format!!
 * in the data-part there is no object-identifier for a data-line. so we have to
 * work with zone-indices here instead of object-ids.
 */
public class VisumVLogic extends AbstractFormatLogic {
	
	private static final String COMMENT = "*";
	//use regExp only with String.split(),  NOT WITH StringTokenizer!!!! -> VisumVDataProcessor!!
	private static final String WRITE_DELIM = " ";
	private static final int ZONES_PER_LINE = 8;
	private static final int CHARS_PER_UNIT = 100; 
	//each value/name in a visum-v-file should have not much more than this much chars
	//for efficency reasons
	private enum HeaderPart{
		//must be in THIS order
		TSYS (false, "Verkehrsmittel-Nr"),
		TIME (true, "Von  Bis"),
		FACTOR (true, "Faktor"),
		ZONE_NR (true, "Anzahl Netzobjekte"),
		ZONE_NAMES (true, "Netzobjekt-Nummern");
		
		boolean isAvail;
		String comment;
		
		HeaderPart(boolean isAvail, String comment){
			this.isAvail = isAvail;
			this.comment = comment;
		}
	}	
	private String PARSE_DELIM;
	
	
	
	
	public VisumVLogic(int numberOfProcessors, String delim){
		super(numberOfProcessors);
		this.PARSE_DELIM = delim;
	}

	protected void fillBufferImplementation(BufferedReader reader, 
			LinkedBlockingQueue<IBufferUnit> buffer) throws IOException, InterruptedException {

		/*
		 * now the reader either points to one of the two comment-lines above 
		 * the first line with i-j-values.
		 * that is where gatherHeaderInfo() has left the reader.
		 */

		int zoneIdx = -1;
		StringBuffer completeLine = new StringBuffer();
		String line = null;


		while ((line = reader.readLine()) != null){

			if (line.startsWith(COMMENT)  &&  zoneIdx == -1){
				continue;

			} else if (!line.startsWith(COMMENT)  &&  zoneIdx == -1) {
				zoneIdx = 0;
				completeLine.append(line + WRITE_DELIM);

			} else if (line.startsWith(COMMENT)){
				buffer.put(new DataBufferUnit(zoneIdx + "", completeLine.toString()));
				completeLine = new StringBuffer();
				zoneIdx++;

			} else {
				completeLine.append(line + WRITE_DELIM);
			}
		}

		//dont forget last line! no comment-line after last line!
		buffer.put(new DataBufferUnit(zoneIdx + "", completeLine.toString()));
	}

	public HeaderData gatherHeaderInfo(BufferedReader reader) throws IOException, 
	FormatLogicException {
		
		HeaderData header = new HeaderData();
	
		header.setQuadratic(true);
		
		int numberOfPosAfterDecPoint = 0;
		int type = VisumHeaderValueType.INT;

		String line = reader.readLine();

		if (!line.startsWith("$V")){
//			throw new FormatLogicException("no $V format");
			throw new FormatLogicException();
		}

		for (int i = 2; i < line.length(); i++){
			switch (line.charAt(i)){
			case VisumHeaderConfigChar.DELIM:
				break;
			case VisumHeaderConfigChar.ROUND:
				break;
			case VisumHeaderConfigChar.INTERVAL:
				HeaderPart.TIME.isAvail = false;
				HeaderPart.FACTOR.isAvail = false;
				break;
			case VisumHeaderConfigChar.TSYS:
				HeaderPart.TSYS.isAvail = true;
				break;
			case VisumHeaderConfigChar.PREC:
				numberOfPosAfterDecPoint = Integer.parseInt(line.charAt(++i) + "");
				if (numberOfPosAfterDecPoint > 0  &&  type == VisumHeaderValueType.INT)
					type = VisumHeaderValueType.DOUBLE;
				break;
			case VisumHeaderConfigChar.TYPE:
				header.setVisumHeaderValueType(Integer.parseInt(line.charAt(++i) + ""));
				break;
			default:
//				throw new FormatLogicException("unknown format-config-character : '"+
//						line.charAt(i)+"'");
				throw new FormatLogicException(line.charAt(i)+"");
			}
		}

		for (HeaderPart p : HeaderPart.values()){

			if (!p.isAvail)
				continue;

			boolean partFinished = false;

			while (!partFinished){

				line = reader.readLine();
				if (line == null){
//					throw new FormatLogicException("header ends unsuspectedly in part '" +
//							p.name() + "'");
					throw new FormatLogicException(p.name());
				}
				if (line.trim().length() == 0)
					continue;

				if (line.startsWith(COMMENT)){
					continue;

				} else {
					switch (p){
					case TSYS:
						header.setTrafficSystem(line.trim());
						partFinished = true;
						break;
					case TIME:
						header.setTimeIntervall(line);
						partFinished = true;			
						break;
					case FACTOR:
						header.setFactor(line);
						partFinished = true;	
						break;
					case ZONE_NR:
						//no use for this
						partFinished = true;
						break;
					case ZONE_NAMES:
						partFinished = addZonesToHeader(line, header);
						break;
					}
				}
			}
		}
		
		return header;
	}

	private boolean addZonesToHeader(String line, HeaderData header) {

		if (line.startsWith(COMMENT))
			return true;

		String[] zones = line.trim().split(PARSE_DELIM);
		for (String zone : zones){
			header.addObject(zone);
		}

		/*
		 * if there are 8 zones in a line, then not finished, because maybe more 
		 * in next line.
		 *  -> check on COMMENT at the beginning
		 *  
		 * if there are less than 8 zones, then finished
		 */
		return !(zones.length == ZONES_PER_LINE);
	}

	public LinkedList<IFormatDataProcessor> getDataProcessors() {
		
		LinkedList<IFormatDataProcessor> dataProcs = new LinkedList<IFormatDataProcessor>();
		for (int i = 0; i < numberOfProcessors; i++){
			dataProcs.add(new VisumVUnitProcessor(PARSE_DELIM));
		}
		return dataProcs;
	}

	public void writeHeader(IFileDataContainer container, BufferedWriter writer,
			String eol) throws IOException, FormatLogicException {
		
		if (!container.isQuadratic())
//			throw new FormatLogicException("only quadratic structures allowed in $V");
			throw new FormatLogicException();

		/*
		 * no TSYS-, TIME-, FACTOR-Part. only ZONE-NR and ZONE-NAMES 
		 */
		
		ArrayList<String> objects = container.getObjects().getKeySetInIndexedOrder();
		int decPosAfterPoint = numberOfDecPosAfterPoint(container.getValue(objects.get(0), 
				objects.get(0)));

		String config = "$VN" + ((decPosAfterPoint > 0) ? 
				";Y" + VisumHeaderValueType.DOUBLE + ";D" + decPosAfterPoint : 
					";Y" + VisumHeaderValueType.INT);
		writer.write(config + eol);

		writer.write(COMMENT + " " + HeaderPart.ZONE_NR.comment + eol);
		writer.write(objects.size() + eol);

		writer.write(COMMENT + " " + HeaderPart.ZONE_NAMES.comment + eol);
		StringBuffer line = new StringBuffer(CHARS_PER_UNIT * objects.size());
		for (int i = 0; i < objects.size(); i++){
			line.append(objects.get(i) + WRITE_DELIM);
			if (i % ZONES_PER_LINE == ZONES_PER_LINE - 1)
				line.append(eol);
		}
		//if last line of objects has not 8 objects, then no eol would appear
		if (objects.size() % ZONES_PER_LINE != 0)
			line.append(eol);
		writer.write(line.toString());

		writer.write(COMMENT + eol);

	}
	
	private int numberOfDecPosAfterPoint(String test){

		int number = 0;

		int pointPos = test.indexOf(".");
		if (pointPos != -1){
			number = test.substring(pointPos + 1, test.length()).length();
		}

		return number;
	}

	public void writeData(IFileDataContainer container, BufferedWriter writer,
			String eol) throws IOException, FormatLogicException{

		ArrayList<String> objects = container.getObjects().getKeySetInIndexedOrder();

		for (String o : objects){

			writer.write(COMMENT + " " + o + eol);

			StringBuffer line = new StringBuffer(CHARS_PER_UNIT * objects.size());
			String[] values = container.getLineByID(o);
			for (int i = 0; i < values.length; i++){
				line.append(values[i] + WRITE_DELIM);
				if (i % ZONES_PER_LINE == ZONES_PER_LINE - 1)
					line.append(eol);
			}
			//if last line of objects has not 8 values, then no eol would appear
			if (objects.size() % ZONES_PER_LINE != 0)
				line.append(eol);

			writer.write(line.toString());


		}
	}

}
