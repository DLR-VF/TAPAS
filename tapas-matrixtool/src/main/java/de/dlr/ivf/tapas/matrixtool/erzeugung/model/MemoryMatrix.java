package de.dlr.ivf.tapas.matrixtool.erzeugung.model;

import java.util.ArrayList;

import de.dlr.ivf.tapas.matrixtool.erzeugung.events.ModelEvent;

public class MemoryMatrix<T> extends AbstractMatrix<T>{

	private volatile ArrayList<ArrayList<T>> values;
	private volatile int yDimSizeShouldBe;
	private volatile int xDimSizeShouldBe;
	private T def;

	
	public MemoryMatrix(T def){
		
		values = new ArrayList<ArrayList<T>>();
		yDimSizeShouldBe = 0;
		xDimSizeShouldBe = 0;
		this.def = def;
	}
	
	public void clear(){
		values = new ArrayList<ArrayList<T>>();
		yDimSizeShouldBe = 0;
		xDimSizeShouldBe = 0;
	}
	
	
	/* (non-Javadoc)
	 * @see erzeugung_model_new.IMatrix#increaseXDim()
	 */
	public void increaseXDim(){
		
		xDimSizeShouldBe++;	
		checkForDimRanges();
	}

	/* (non-Javadoc)
	 * @see erzeugung_model_new.IMatrix#decreaseXDimAtIndex(int)
	 */
	public void decreaseXDimAtIndex(int i){

		xDimSizeShouldBe--;
		values.remove(i);
	}
	
	/* (non-Javadoc)
	 * @see erzeugung_model_new.IMatrix#increaseYDim()
	 */
	public void increaseYDim(){
		
		yDimSizeShouldBe++;		
		checkForDimRanges();
	}
	
	/* (non-Javadoc)
	 * @see erzeugung_model_new.IMatrix#decreaseYDimAtIndex(int)
	 */
	public void decreaseYDimAtIndex(int i){
		
		yDimSizeShouldBe--;		
		for (ArrayList<T> l : values){
			l.remove(i);
		}
	}
	
	/* (non-Javadoc)
	 * @see erzeugung_model_new.IMatrix#getXRange()
	 */
	public int getXRange(){
		return xDimSizeShouldBe;
	}
	
	/* (non-Javadoc)
	 * @see erzeugung_model_new.IMatrix#getYRange()
	 */
	public int getYRange(){
		return yDimSizeShouldBe;
	}
	
	/* (non-Javadoc)
	 * @see erzeugung_model_new.IMatrix#getActualXSize()
	 */
	public int getActualXSize(){
		
		return values.size();
	}
	
	/* (non-Javadoc)
	 * @see erzeugung_model_new.IMatrix#getActualYSize()
	 */
	public int getActualYSize(){
		
		if (values.size() > 0){
			return values.get(0).size();
		} else {
			return 0;
		}
	}

	/* (non-Javadoc)
	 * @see erzeugung_model_new.IMatrix#getValue(int, int)
	 */
	public T getValue(int x, int y){
		
		if (x >= values.size()){
			return null;
		} else {
			if (y >= values.get(x).size()){
				return null;
			} else {
				return values.get(x).get(y);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see erzeugung_model_new.IMatrix#setValue(int, int, T)
	 */
	public void setValue(int x, int y, T d){
		
		if (x < values.size()){
			if (y < values.get(0).size()){
				values.get(x).set(y, d);
				addToMessageSet(ModelEvent.Message.VAL_CHNGD);
			}
		}		
	}
	
	private void checkForDimRanges() {
		
		int missing = xDimSizeShouldBe - values.size();
		for (int i = 0; i < missing; i++){
			values.add(new ArrayList<T>());
		}
		
		for (ArrayList<T> l : values){
			missing = yDimSizeShouldBe - l.size();
			for (int i = 0; i < missing; i++){
				l.add(def);
			}
		}
	}

	public void increaseXDimAtIndex(int i) {
		
		values.add(i, new ArrayList<T>());
		xDimSizeShouldBe++;
		checkForDimRanges();
	}

	public void increaseYDimAtIndex(int i) {
		
		yDimSizeShouldBe++;
		
		for (ArrayList<T> l : values){
			l.add(i, def);
		}
	}
}
