package de.dlr.ivf.tapas.matrixtool.erzeugung.model;

import java.util.ArrayList;
import java.util.Iterator;

public class MemoryList<T> implements IList<T>{

	private volatile ArrayList<T> list;
	
	public MemoryList(){
		list = new ArrayList<T>();
	}

	public MemoryList(ArrayList<T> list) {
		this.list = (ArrayList<T>) list.clone();
	}
	
	public boolean add(T e) {
		return list.add(e);
	}
	
	public void add(int index, T e){
		list.add(index, e);
	}

	public Iterator<T> iterator() {
		return list.iterator();
	}
	
	public boolean remove(T elem){
		return list.remove(elem);
	}
	
	public T remove(int index){
		return list.remove(index);
	}
	
	public int size(){
		return list.size();
	}
	
	public int indexOf(T elem){
		return list.indexOf(elem);
	}

	public String get(int index) {
		return list.get(index).toString();
	}

	public boolean contains(T t) {
		return list.contains(t);
	}
}
