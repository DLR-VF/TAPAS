package de.dlr.ivf.tapas.matrixtool.fileIO_v3;

import java.util.concurrent.BlockingQueue;

public class DataParseThread extends Thread{
	
	private IFormatDataProcessor proc;
	private BlockingQueue<IBufferUnit> buffer;
	private IFileDataContainer container;
	private IIOHandler handler;
	
	public DataParseThread(IFormatDataProcessor proc, IFileDataContainer container,
			BlockingQueue<IBufferUnit> buffer, IIOHandler handler){
		
		this.proc = proc;
		this.container = container;
		this.buffer = buffer;
		this.handler = handler;
	}
	
	public void run(){

		IBufferUnit unit = null; 
			
		while (true){

			try {
				
				unit = buffer.take();
				
				if (!unit.isEOF()){
					proc.processUnit((DataBufferUnit)unit, container);
				} else {
					break;
				}
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				handler.signalReadingException(e.getMessage());
			}			
		}
	}
}
