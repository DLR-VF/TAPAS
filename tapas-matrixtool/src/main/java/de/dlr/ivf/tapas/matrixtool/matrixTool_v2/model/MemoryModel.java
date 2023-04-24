package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model;

import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.events.ModelEvent;
import de.dlr.ivf.tapas.matrixtool.erzeugung.model.AbstractMatrix;
import de.dlr.ivf.tapas.matrixtool.erzeugung.model.MemoryList;
import de.dlr.ivf.tapas.matrixtool.erzeugung.model.MemoryMatrix;

public class MemoryModel extends AbstractModel{

	private AbstractMatrix<Double> matrix;
	private MemoryList<GenericIdentifyable> rowIDs;
	private MemoryList<GenericIdentifyable> columnIDs;
	private volatile Double matrixMaxValue;
	private volatile Double matrixMinValue;
	
	public MemoryModel(){
		matrix = new MemoryMatrix<Double>(0.);
		rowIDs = new MemoryList<GenericIdentifyable>();
		columnIDs = new MemoryList<GenericIdentifyable>();
		matrixMaxValue = 0.;
		matrixMinValue = 0.;
	}
	
	public Double getMatrixMaxValue() {
		return matrixMaxValue == null ? 0. : matrixMaxValue;
	}
	
	public Double getMatrixMinValue() {
		return matrixMinValue == null ? 0. : matrixMinValue;
	}
	
	public void setValue(int x, int y, Double v){
		
		matrix.setValue(x, y, v);
		
		if (matrixMaxValue == null  &&  matrixMinValue == null){
			matrixMaxValue = v;
			matrixMinValue = v;
		} else {
			if (v > matrixMaxValue)
				matrixMaxValue = v;
			if (v < matrixMinValue)
				matrixMinValue = v;
		}
		
		addToMessageSet(ModelEvent.Type.VAL_CHNGD);
	}

	/*
	 * gedacht, wenn man nicht gleichzeitig row-ids, column-ids und values setzt,
	 * sondern nur values. man weiss ja nicht, ob sich die reihenfolge der rows 
	 * und columns im model von der unterscheidet, nach der man gerade daten
	 * einfuegt (z.B. von einer anderen datei aus)
	 */
	public void setValue(String x, String y, String v){
		
		Double currValue = Double.parseDouble(v);
		
		setValue(rowIDs.indexOf(new GenericIdentifyable(x)), 
				columnIDs.indexOf(new GenericIdentifyable(y)), 
				currValue);		
	}
	
	public void addRowID(String id){
		
		GenericIdentifyable gen = find(rowIDs, new GenericIdentifyable(id));
		if (gen == null){
			rowIDs.add(new GenericIdentifyable(id));
			matrix.increaseXDim();
		}
		
		addToMessageSet(ModelEvent.Type.RWS_CHNGD);
	}
	
	public void addRowIdAtIndex(String id, int index){

		GenericIdentifyable gen = find(rowIDs, new GenericIdentifyable(id));
		if (gen == null){
			rowIDs.add(index,new GenericIdentifyable(id));
			matrix.increaseXDimAtIndex(index);
		}
		
		addToMessageSet(ModelEvent.Type.RWS_CHNGD);
	}
	
	public void removeRowAtIndex(int index){
		
		if (index < rowIDs.size()){
			rowIDs.remove(index);
			matrix.decreaseXDimAtIndex(index);

			addToMessageSet(ModelEvent.Type.RWS_CHNGD);
		}
	}
	
	public void removeColumnAtIndex(int index){
		
		if (index < columnIDs.size()){
			columnIDs.remove(index);
			matrix.decreaseYDimAtIndex(index);

			addToMessageSet(ModelEvent.Type.CLS_CHNGD);
		}
	}
	
	public void addColumnIdAtIndex(String id, int index){

		GenericIdentifyable gen = find(columnIDs, new GenericIdentifyable(id));
		if (gen == null){
			columnIDs.add(index,new GenericIdentifyable(id));
			matrix.increaseYDimAtIndex(index);
		}
		
		addToMessageSet(ModelEvent.Type.CLS_CHNGD);
	}
	
	public void addColumnID(String id){
		
		GenericIdentifyable gen = find(columnIDs, new GenericIdentifyable(id));
		if (gen == null){
			columnIDs.add(new GenericIdentifyable(id));
			matrix.increaseYDim();
		}
		
		addToMessageSet(ModelEvent.Type.CLS_CHNGD);
	}	
	
	private GenericIdentifyable find(MemoryList<GenericIdentifyable> list, 
			GenericIdentifyable c) {
		 
		 for (GenericIdentifyable other : list){
		     if (other.equals(c))
		     		return other; 
		 }
		     
		 return null;		 
	}
	
	public AbstractMatrix<Double> getMatrix(){
		return matrix;
	}
	
	public MemoryList<GenericIdentifyable> getRowIDs(){
		return rowIDs;
	}
	
	public MemoryList<GenericIdentifyable> getColumnIDs(){
		return columnIDs;
	}
}
