package de.dlr.ivf.tapas.matrixtool.fileIO_v3;

import de.dlr.ivf.tapas.matrixtool.common.datastructures.Indexing;

public class TableContainer extends AbstractFileDataContainer {
	
	private Indexing<String> cols;
	
	public TableContainer(HeaderData header){
		
		super(header);		
		this.cols = header.getAttributes();
		values = new String[rows.size()][cols.size()];
	}
	
	/* (non-Javadoc)
	 * @see common_io_file_v3.IFileDataContainer#isQuadratic()
	 */
	public boolean isQuadratic(){
		return false;
	}
	
	
	/* (non-Javadoc)
	 * @see common_io_file_v3.IFileDataContainer#getAttributes()
	 */
	public Indexing<String> getAttributes(){
		return cols;
	}
	
	/* (non-Javadoc)
	 * @see common_io_file_v3.IFileDataContainer#getValue(java.lang.String, java.lang.String)
	 */
	public String getValue(String object, String attribute){
		
		int objId = rows.getValueForKey(object);
		int attId = cols.getValueForKey(attribute);
		return values[objId][attId];
	}
	
	/* (non-Javadoc)
	 * @see common_io_file_v3.IFileDataContainer#putByID(java.lang.String, java.lang.String[])
	 */
	public synchronized void putByID(String object, String[] data) {
		
		// i ; value1 ; ... ; valueN
		int objIdx = rows.getValueForKey(object);
		putByIndex(objIdx, data);
	}
	
	/* (non-Javadoc)
	 * @see common_io_file_v3.IFileDataContainer#putByIndex(java.lang.String, java.lang.String[])
	 */
	public synchronized void putByIndex(int idx, String[] data) {

		// i ; value1 ; ... ; valueN
		System.arraycopy(data, 0, values[idx], 0, data.length);		
	}
}
