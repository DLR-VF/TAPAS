package de.dlr.ivf.tapas.matrixtool.fileIO_v3;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

public class AsciiFileReader extends Thread{
	
	private LinkedList<DataParseThread> parseThreads;
	private IFormatLogic logic;
	private HeaderData header;
	private IFileDataContainer container;
	private LinkedBlockingQueue<IBufferUnit> buffer;
	private IIOHandler handler;
	private BufferedReader reader;


	public AsciiFileReader(IIOHandler handler, IFormatLogic logic, 
			LinkedBlockingQueue<IBufferUnit> buffer, BufferedReader reader) {
		
		this.logic = logic;
		this.buffer = buffer;
		this.handler = handler;
		this.reader = reader;
		parseThreads = new LinkedList<DataParseThread>();
	}	
	
	public IFileDataContainer getContainer(){
		return container;
	}

	public void run() {
		
		try {
			//is being executed in this thread, so no blocking of gui will occur
			header = logic.gatherHeaderInfo(reader);

			if (header.isQuadratic()){
				container = new MatrixContainer(header);
			} else {
				container = new TableContainer(header);
			}

			//jetzt mal threads starten. sie blockieren auch gleich
			for (IFormatDataProcessor proc : logic.getDataProcessors()){			
				DataParseThread t = new DataParseThread(proc, container, buffer, handler);
				t.start();
				parseThreads.add(t);
			}


			//is being executed in this thread, so no blocking of gui will occur
			logic.fillBuffer(reader, buffer);

			//threads laufen jetzt im hintergrund, weil parsen laenger dauert als einlesen
			//erst returnen, wenn alle threads fertig sind
			for (Thread t : parseThreads){
				t.join();
			}

		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
			handler.signalReadingException(e.getMessage());

		} finally {
			
			try {
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			handler.signalFinishedReading();
		}
	}
}
