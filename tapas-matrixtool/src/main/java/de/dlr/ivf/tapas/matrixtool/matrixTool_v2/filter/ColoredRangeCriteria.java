package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter;

import java.awt.Color;

public class ColoredRangeCriteria<T extends Comparable<T>> extends RangeCriteria<T>{

	private Color c;

	public ColoredRangeCriteria(T min, T max, Color c){
		super(min,max);
		this.c = c;
	}
	
	public void setColor(Color c){
		this.c = c; 
	}
	
	public Color getColor(){
		return c;
	}
}
