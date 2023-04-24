package de.dlr.ivf.tapas.matrixtool.fileIO_v3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

public interface IFormatLogic {

	public HeaderData gatherHeaderInfo(BufferedReader reader) throws IOException, 
	FormatLogicException;
	
	public void fillBuffer(BufferedReader reader, 
			LinkedBlockingQueue<IBufferUnit> buffer) throws  
			IOException, InterruptedException, FormatLogicException;

	public LinkedList<IFormatDataProcessor> getDataProcessors();

	public void writeHeader(IFileDataContainer container, BufferedWriter writer, 
			String eol) throws IOException, FormatLogicException;

	public void writeData(IFileDataContainer container, BufferedWriter writer,
			String eol) throws IOException, FormatLogicException;	

}
