package de.dlr.ivf.tapas.matrixtool.fileIO_v3;

import de.dlr.ivf.tapas.matrixtool.common.datastructures.Indexing;

public abstract class AbstractFileDataContainer implements IFileDataContainer {

	protected Indexing<String> rows;
	protected String[][] values;
	
	public AbstractFileDataContainer(HeaderData header){
		rows = header.getObjects();
	}
	
	public abstract Indexing<String> getAttributes();

	public Indexing<String> getObjects() {
		return rows;
	}

	public abstract String getValue(String object, String attribute);

	public abstract boolean isQuadratic();

	public abstract void putByID(String object, String[] data);

	public abstract void putByIndex(int idx, String[] data);
	
	public String[] getLineByID(String object) {
		
		int objIdx = rows.getValueForKey(object);		
		return values[objIdx];
	}
}
