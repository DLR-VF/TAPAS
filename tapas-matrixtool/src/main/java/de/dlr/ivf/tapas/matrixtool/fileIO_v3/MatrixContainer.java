package de.dlr.ivf.tapas.matrixtool.fileIO_v3;

import de.dlr.ivf.tapas.matrixtool.common.datastructures.Indexing;

public class MatrixContainer extends AbstractFileDataContainer {

	public MatrixContainer(HeaderData header) {
		super(header);
		values = new String[rows.size()][rows.size()];
	}

	public Indexing<String> getAttributes() {
		return rows;
	}

	public String getValue(String object, String attribute) {
		
		int objId = rows.getValueForKey(object);
		int attId = rows.getValueForKey(attribute);
		return values[objId][attId];
	}

	public boolean isQuadratic() {
		
		return true;
	}

	public synchronized void putByID(String object, String[] data) {
		
		// i ; j; ; value
		int objIdx = rows.getValueForKey(object);			
		putByIndex(objIdx, data);
	}

	public synchronized void putByIndex(int idx, String[] data) {
		
		// i ; j; ; value
		int to = rows.getValueForKey(data[0]);
		values[idx][to] = data[1];
	}
}
