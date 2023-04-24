package de.dlr.ivf.tapas.matrixtool.fileIO_v3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class AbstractFormatLogic implements IFormatLogic {
	
	protected int numberOfProcessors;
	
	public AbstractFormatLogic(int numberOfProcessors){
		this.numberOfProcessors = numberOfProcessors;
	}

	public void fillBuffer(BufferedReader reader,
			LinkedBlockingQueue<IBufferUnit> buffer) throws FormatLogicException, 
			IOException, InterruptedException{
		
		fillBufferImplementation(reader,buffer);
		
		for (int i = 0; i < numberOfProcessors; i++){
			try {
				buffer.put(new EOFBufferUnit());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public abstract HeaderData gatherHeaderInfo(BufferedReader reader) 
	throws IOException, FormatLogicException;

	public abstract LinkedList<IFormatDataProcessor> getDataProcessors();

	protected abstract void fillBufferImplementation(BufferedReader reader,
			LinkedBlockingQueue<IBufferUnit> buffer) 
	throws IOException, InterruptedException, FormatLogicException;
	
	public abstract void writeHeader(IFileDataContainer container, BufferedWriter writer,
			String eol) throws IOException, FormatLogicException;
	
	public abstract void writeData(IFileDataContainer container, BufferedWriter writer,
			String eol) throws IOException, FormatLogicException;
}
