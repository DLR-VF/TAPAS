package de.dlr.ivf.tapas.matrixtool.fileIO_v3;

import java.util.StringTokenizer;

public class CSVUnitProcessor extends AbstractFormatUnitProcessor {
	
	public CSVUnitProcessor(String delim){
		
		super(delim);
	}

	public void processUnit(DataBufferUnit unit, IFileDataContainer container) 
	throws UnitProcessingException, FormatLogicException {
		
		StringTokenizer tokens = new StringTokenizer(unit.getData(),delim);
		String[] res = new String[tokens.countTokens()];
		String obj = unit.getObject();
		
		if (!container.isQuadratic()){
			if (res.length != container.getAttributes().size())
//				throw new UnitProcessingException("number of values does not match number " +
//						"of attributes (for zone '"+unit.getObject()+"')");
				throw new UnitProcessingException(unit.getObject());
		} else {
			if (res.length != 2)
//				throw new UnitProcessingException("number of values does not match number " +
//						"of zones (for zone '"+unit.getObject()+"')");
				throw new UnitProcessingException(unit.getObject());
		}
		
		if (container.getObjects().getValueForKey(obj) == null)
//			throw new FormatLogicException("object '"+obj+"' is not known from header");
			throw new UnitProcessingException(obj);
		
		for (int i = 0; i < res.length; i++){
			res[i] = tokens.nextToken();
		}
		
		container.putByID(obj, res);
	}
}
