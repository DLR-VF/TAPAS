package de.dlr.ivf.tapas.matrixtool.fileIO_v3;

public interface IFormatDataProcessor{
	
	public void processUnit(DataBufferUnit data, IFileDataContainer container) 
	throws UnitProcessingException, FormatLogicException;
}
