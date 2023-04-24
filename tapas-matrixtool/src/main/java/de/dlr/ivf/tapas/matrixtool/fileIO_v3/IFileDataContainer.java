package de.dlr.ivf.tapas.matrixtool.fileIO_v3;

import de.dlr.ivf.tapas.matrixtool.common.datastructures.Indexing;

/*
 * this container stores String-values for good interoperability. the fileIoManager 
 * knows nothing about the type or meaning of these values. it just writes or
 * reads them.
 * so the values have to be delivered in exactly the right format, how the 
 * fileIoMangaer should write them.
 */
public interface IFileDataContainer {

	public boolean isQuadratic();

	public Indexing<String> getObjects();

	/*
	 * if quadratic container, returns the same reference as getObjects()
	 */
	public Indexing<String> getAttributes();

	public String getValue(String object, String attribute);

	/*
	 * if quadratic, format of data is : j;val
	 * if not, format is : val_1;...;val_N for N attributes
	 * 
	 * the j;val-format when quadratic is chosen that way, because it is der kleinste
	 * gemeinsame nenner. but internally storage of values is always a "real" 2-dim-matrix 
	 */
	public void putByID(String object, String[] data);

	public void putByIndex(int idx, String[] data);
	
	/*
	 * format of return is an array with length getObjects().size(), if quadratic, 
	 * and N, if not, for N attributes.
	 */
	public String[] getLineByID(String object);

}