package de.dlr.ivf.tapas.matrixtool.fileIO_v3;

public abstract class AbstractFormatUnitProcessor implements
		IFormatDataProcessor {
	
	protected String delim;
	
	public AbstractFormatUnitProcessor(String delim){
		this.delim = delim;
	}

	public abstract void processUnit(DataBufferUnit data, IFileDataContainer container) 
	throws UnitProcessingException, FormatLogicException;
}
