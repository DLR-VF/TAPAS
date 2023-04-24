package de.dlr.ivf.tapas.matrixtool.fileIO_v3;

public class DataBufferUnit implements IBufferUnit {

	private String object;
	private String data;
	
	public DataBufferUnit(){}

	public DataBufferUnit(String object, String data){
		this.object = object;
		this.data = data;
	}
	
	/* (non-Javadoc)
	 * @see common_io_file_v3.IBufferUnit#getObject()
	 */
	public String getObject(){
		return object;
	}
	
	/* (non-Javadoc)
	 * @see common_io_file_v3.IBufferUnit#getData()
	 */
	public String getData(){
		return data;
	}
	
	/* (non-Javadoc)
	 * @see common_io_file_v3.IBufferUnit#isEOF()
	 */
	public boolean isEOF(){
		return false;
	}
}
