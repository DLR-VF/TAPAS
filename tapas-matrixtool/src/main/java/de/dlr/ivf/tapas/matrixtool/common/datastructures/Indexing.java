package de.dlr.ivf.tapas.matrixtool.common.datastructures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

/*
 * a 1-to-1-mapping, where Objects are mapped to Integers. there is at most one
 * referenced value for a key and key for a value. 
 * the indexing doesnt reuse integers!
 */
public class Indexing<K> {

	private HashMap<K,Integer> map;
	private HashMap<Integer,K> revMap;
	private int counter;
	
	
	public Indexing(){
		map = new HashMap<K,Integer>();
		revMap = new HashMap<Integer, K>();
		counter = 0;
	}
	
	public Set<K> getKeySet(){
		return map.keySet();
	}
	
	public ArrayList<K> getKeySetInIndexedOrder(){
		
		ArrayList<K> res = new ArrayList<K>();
		for (Integer i : new TreeSet<Integer>(revMap.keySet())){
			res.add(revMap.get(i));
		}
		return res;
	}
	
	public Set<Integer> getIndexSet(){
		return revMap.keySet();
	}
	
	//falsch
//	public void put(K key){
//		
//		Integer prev = map.put(key, new Integer(counter));
//		revMap.put(new Integer(revMap.size()), key);
//		
//		if (prev == null)
//			counter++;
//	}
	public void put(K key){
		
		//evtl the tupel in revMap has to be altered, if already existent
		K prevRev = revMap.get(counter);
		
		Integer prev = map.put(key, counter);
		
		if (prev == null){
			revMap.put(counter, key);
			counter++;
		} else {
			revMap.remove(prevRev);
			revMap.put(counter, key);
		}
	}
	
	
	/*
	 * pay attention to evtl inconsistencies regarding referenced matrix-arrays!
	 */
	public void remove(K key){
		
		Integer prev = map.remove(key);
		if (prev != null)
			revMap.remove(prev);
	}
	
	public Integer getValueForKey(K key){
		return map.get(key);
	}
	
	public K getKeyForValue(Integer i){
		
		return revMap.get(i);
	}

	public int size() {
		return map.size();
	}

	public Set<Integer> getValueSet() {
		return revMap.keySet();
	}
}
