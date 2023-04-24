package de.dlr.ivf.tapas.matrixtool.fileIO_v3;

public class VisumVUnitProcessor extends AbstractFormatUnitProcessor {
	

	public VisumVUnitProcessor(String delim) {

		super(delim);
	}

	public void processUnit(DataBufferUnit unit, IFileDataContainer container) 
	throws UnitProcessingException, FormatLogicException {
		
		String[] tokens = unit.getData().split(delim);
		String obj = unit.getObject();
		
		/*
		 * tokens is a array with #zones values. the container will accept only
		 * [i,j,value]. so iterate pairwise over it.
		 */
		if (tokens.length != container.getObjects().size())
			throw new UnitProcessingException("number of values does not match number " +
					"of zones (for zone '"+unit.getObject()+"')");
		
		if (container.getObjects().getValueForKey(obj) == null)
			throw new FormatLogicException(obj);
			
		for (int i = 0; i < tokens.length; i++){
			container.putByIndex(Integer.parseInt(obj), new String[]{
					i + "",
					tokens[i]
				});
		}
	}
}
