package de.dlr.ivf.tapas.matrixtool.fileIO_v3;

import java.util.Observable;

import de.dlr.ivf.tapas.matrixtool.common.events.IOEvent;

public abstract class AbstractIOHandler extends Observable implements IIOHandler {
	
	protected volatile String file;

	public abstract IFileDataContainer getContainer();

	public abstract void readFile(String path);
	
	public String getCurrentFile(){
		return file;
	}

	public abstract void storeFile(String path, IFileDataContainer container);

	public void signalFinishedReading(){
		setChanged();
		notifyObservers(new IOEvent(this, IOEvent.Type.FINISHED_READING,file,""));
	}
	
	public void signalFinishedWriting(){
		setChanged();
		notifyObservers(new IOEvent(this, IOEvent.Type.FINISHED_WRITING,file,""));
	}
	
	public void signalReadingException(String message){
		setChanged();
		notifyObservers(new IOEvent(this, IOEvent.Type.ERROR_READING,file, message));
	}
	
	public void signalWritingException(String message){
		setChanged();
		notifyObservers(new IOEvent(this, IOEvent.Type.ERROR_WRITING,file, message));
	}
}
