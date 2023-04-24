package de.dlr.ivf.tapas.matrixtool.erzeugung.model;

import java.util.Iterator;
public interface IList<T> extends Iterable<T>{
	
	public boolean add(T t);

	public Iterator<T> iterator();
	
	public boolean remove(T t);
	
	public T remove(int index);
	
	public int size();
	
	public int indexOf(T t);
	
	public boolean contains(T t);
}