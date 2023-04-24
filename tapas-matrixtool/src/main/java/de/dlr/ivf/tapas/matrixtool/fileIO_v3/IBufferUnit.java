package de.dlr.ivf.tapas.matrixtool.fileIO_v3;

/*
 * a bunch of data/values that "belong" to an object, if not isEOF() 
 */
public interface IBufferUnit {

	public String getObject();

	public String getData();

	public boolean isEOF();

}