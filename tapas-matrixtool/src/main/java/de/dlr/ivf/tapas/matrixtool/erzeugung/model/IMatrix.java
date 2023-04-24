package de.dlr.ivf.tapas.matrixtool.erzeugung.model;

public interface IMatrix<T> {

	public void increaseXDim();
	
	public void increaseXDimAtIndex(int i);

	public void decreaseXDimAtIndex(int i);

	public void increaseYDim();
	
	public void increaseYDimAtIndex(int i);

	public void decreaseYDimAtIndex(int i);

	public int getXRange();

	public int getYRange();

	public int getActualXSize();

	public int getActualYSize();

	public T getValue(int x, int y);

	public void setValue(int x, int y, T d);

	public void clear();
}