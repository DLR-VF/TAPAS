package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model;

public class TAPASMemoryModel extends MemoryModel{
	public TAPASMemoryModel(double[] values){
		super();
		int dim =(int) Math.sqrt(values.length);
		
		//generate rows
		for(int j = 0; j< dim ; ++j){
			this.addRowID(j+"");
		}
		
		//generate colls
		for(int j = 0; j< dim ; ++j){
			this.addColumnID(j+"");
		}
		//fill values
		for(int j = 0, k = 0; j< dim ; ++j){		
			for(int i = 0; i< dim ; ++i, ++k)
			{
				this.setValue(i, j, values[k]);
			}			
		}
	}
	
	
	public TAPASMemoryModel(int[] values){
		super();
		int dim =(int) Math.sqrt(values.length);
		
		//generate rows
		for(int j = 0; j< dim ; ++j){
			this.addRowID(j+"");
		}
		
		//generate colls
		for(int j = 0; j< dim ; ++j){
			this.addColumnID(j+"");
		}
		//fill values
		for(int j = 0, k = 0; j< dim ; ++j){		
			for(int i = 0; i< dim ; ++i, ++k)
			{
				this.setValue(i, j, (double)values[k]);
			}			
		}
	}

}
