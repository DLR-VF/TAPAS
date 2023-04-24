package de.dlr.ivf.tapas.matrixtool.fileIO_v3;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

public class AsciiFileWriter extends Thread {
	
	private IIOHandler handler;
	private IFormatLogic logic;
	private IFileDataContainer container;
	private BufferedWriter writer;
	private String eol;
	private String path;

	public AsciiFileWriter(IIOHandler handler, IFormatLogic logic, 
			IFileDataContainer container, BufferedWriter writer, 
			String eol, String path){
		
		this.handler = handler;
		this.logic = logic;
		this.container = container;
		this.writer = writer;
		this.eol = eol;
		this.path = path;
	}

	public void run() {
		
		try {

			logic.writeHeader(container, writer, eol);

			writer.flush();

			logic.writeData(container, writer, eol);
			
		} catch (Exception e) {
			e.printStackTrace();
			(new File(path)).delete();
			handler.signalReadingException(e.getMessage());
			
		} finally {	

			try {
				writer.flush();
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			handler.signalFinishedWriting();
		}
	}
}
