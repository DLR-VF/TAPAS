package de.dlr.ivf.tapas.matrixtool.fileIO_v3;

import java.io.File;
import java.util.Observable;
import java.util.Observer;

import de.dlr.ivf.tapas.matrixtool.common.events.IOEvent;
import de.dlr.ivf.tapas.matrixtool.common.fileFormat.Format;

public class FileIOManager extends Observable implements Observer{	

	private AbstractIOHandler handler;
	private int numberOfProcessors;
	public enum Status{
		READING,
		WRITING,
		IDLE
	}
	private Status status;

	public FileIOManager(int numberOfProcessors){
		this.numberOfProcessors = numberOfProcessors;
		status = Status.IDLE;
	}
	
	public Status getStatus(){
		return status;
	}

	public void readFile(String path, Format format){

		setChanged();
		notifyObservers(new IOEvent(this,IOEvent.Type.READING,path,""));
		status = Status.READING;

		switch (format){
		case CSV_SEMC:
			handler = new AsciiIOHandler(new CSVLogic(numberOfProcessors, Format.CSV_SEMC.getDelim()));
			handler.addObserver(this);
			break;
		case CSV_CMA:
			handler = new AsciiIOHandler(new CSVLogic(numberOfProcessors, Format.CSV_CMA.getDelim()));
			handler.addObserver(this);
			break;
		case CSV_BLNK:
			handler = new AsciiIOHandler(new CSVLogic(numberOfProcessors, Format.CSV_BLNK.getDelim()));
			handler.addObserver(this);
			break;
		case VISUM:
			handler = new AsciiIOHandler(new VisumVLogic(numberOfProcessors, Format.VISUM.getDelim()));
			handler.addObserver(this);
			break;
		}	
		
		handler.readFile(path);
	}

	public void storeFile(String path, Format format, IFileDataContainer data){

		setChanged();
		notifyObservers(new IOEvent(this,IOEvent.Type.WRITING,path,""));
		status = Status.WRITING;

		switch (format){
		case CSV_SEMC:
			handler = new AsciiIOHandler(new CSVLogic(numberOfProcessors, Format.CSV_SEMC.getDelim()));
			handler.addObserver(this);
			break;
		case CSV_CMA:
			handler = new AsciiIOHandler(new CSVLogic(numberOfProcessors, Format.CSV_CMA.getDelim()));
			handler.addObserver(this);
			break;
		case CSV_BLNK:
			handler = new AsciiIOHandler(new CSVLogic(numberOfProcessors, Format.CSV_BLNK.getDelim()));
			handler.addObserver(this);
			break;
		case VISUM:
			handler = new AsciiIOHandler(new VisumVLogic(numberOfProcessors, Format.VISUM.getDelim()));
			handler.addObserver(this);
			break;
		}
		
		handler.storeFile(path, data);
	}
	
	public IFileDataContainer getContainer(){
		if (handler != null){
			return handler.getContainer();
		} else {
			return null;
		}
	}
	
	public void abortReading(){
		status = Status.IDLE;
		containerNotLongerNeeded();
	}
	
	public void abortWriting(){
		status = Status.IDLE;
		if (handler != null){
			String currFile = handler.getCurrentFile();
			containerNotLongerNeeded();
			(new File(currFile)).delete();
		}
	}
	
	public void containerNotLongerNeeded(){
		handler = null;
	}

	public void update(Observable o, Object arg) {

		if (arg instanceof IOEvent){

			if (((IOEvent) arg).getType() == IOEvent.Type.FINISHED_READING){
				if (status == Status.READING){
					status = Status.IDLE;
					setChanged();
					notifyObservers(arg);
				}
			}
			
			if (((IOEvent) arg).getType() == IOEvent.Type.FINISHED_WRITING){
				if (status == Status.WRITING){
					status = Status.IDLE;
					setChanged();
					setChanged();
					notifyObservers(arg);
				}
			}
			
			if (((IOEvent) arg).getType() == IOEvent.Type.ERROR_READING  ||
					((IOEvent) arg).getType() == IOEvent.Type.ERROR_WRITING){
				status = Status.IDLE;
//				System.out.println("FileIOManager : " + ((IOEvent)arg).getMessage());
				containerNotLongerNeeded();
				setChanged();
				notifyObservers(arg);
			}
		}
	}
}
