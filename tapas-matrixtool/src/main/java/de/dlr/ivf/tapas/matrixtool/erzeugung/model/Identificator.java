package de.dlr.ivf.tapas.matrixtool.erzeugung.model;

public class Identificator {
	
	private String[] id;
	public static String delim = "-";
	
	public Identificator(String... id){
		
		this.id = new String[id.length];
		
		for (int i = 0; i < id.length; i++){			
			this.id[i] = id[i]; 
		}
		
	}
	
	public boolean isEmpty(){
		
		/*
		 * das string[] hat immer mindestens die laenge 1 (mit evtl [0] = null) 
		 */
		
		for (int i = 0; i < id.length; i++){
			if (id[i] == null)
				return true;
		}
		
		return false;
	}
	
	public String toString(){
		
		String res = "";
		for (int i = 0; i < id.length; i++){
			res += id[i] + delim;
		}
		
		return res.substring(0, res.length() - delim.length());
	}

	public String getPart(int i) {
		return id[i];
	}

	public void setPart(int i, String s) {
		id[i] = s;
	}
	
	public int getNumberParts(){
		return id.length;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Identificator other = (Identificator) obj;
		if (other.getNumberParts() != getNumberParts())
			return false;
		
		boolean isEqual = true;
		for (int i = 0; i < getNumberParts(); i++){
			if (other.getPart(i) == null && getPart(i) != null){
				return false;
			} else if (other.getPart(i) != null && getPart(i) == null){				
				return false;
			} else if (other.getPart(i) == null && getPart(i) == null){
				;
			} else {
				isEqual &= other.getPart(i).equals(getPart(i));
			}
		}
				
		return isEqual;
	}	
}
