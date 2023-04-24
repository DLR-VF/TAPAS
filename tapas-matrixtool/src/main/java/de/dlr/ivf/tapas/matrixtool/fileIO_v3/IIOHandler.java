package de.dlr.ivf.tapas.matrixtool.fileIO_v3;

public interface IIOHandler {

	public void readFile(String path);
	
	public void signalFinishedReading();
	
	public void signalFinishedWriting();
	
	public void signalReadingException(String message);
	
	public IFileDataContainer getContainer();
	
	public void storeFile(String path, IFileDataContainer container);
	
}
