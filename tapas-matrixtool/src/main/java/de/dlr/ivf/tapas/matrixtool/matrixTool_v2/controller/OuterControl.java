package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller;

import java.io.File;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeSet;

import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.GenericIdentifyable;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.MemoryModel;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view.OuterGui;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view.StatusBar;
import de.dlr.ivf.tapas.matrixtool.common.datastructures.Indexing;
import de.dlr.ivf.tapas.matrixtool.common.events.IOEvent;
import de.dlr.ivf.tapas.matrixtool.common.fileFormat.AbstractFileFilter;
import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;
import de.dlr.ivf.tapas.matrixtool.erzeugung.model.MemoryList;
import de.dlr.ivf.tapas.matrixtool.fileIO_v3.FileIOManager;
import de.dlr.ivf.tapas.matrixtool.fileIO_v3.HeaderData;
import de.dlr.ivf.tapas.matrixtool.fileIO_v3.IFileDataContainer;
import de.dlr.ivf.tapas.matrixtool.fileIO_v3.MatrixContainer;
import de.dlr.ivf.tapas.matrixtool.fileIO_v3.TableContainer;

public class OuterControl extends Observable implements Observer {

	private FileIOManager fiom;
	private OuterGui outerGui;
	private HashMap<Integer, MemoryModel> models;
	private HashMap<Integer, File> paths;
	private HashMap<Integer, AbstractFileFilter> filters;
	private int docCounter;

	public OuterControl(FileIOManager fiom, StatusBar statusBar) {

		this.fiom = fiom;
		models = new HashMap<Integer, MemoryModel>();
		paths = new HashMap<Integer, File>();
		filters = new HashMap<Integer, AbstractFileFilter>();
		docCounter = 0;

		addObserver(statusBar);
		fiom.addObserver(this);
	}

	public void setGui(OuterGui gui){
		outerGui = gui;
	}


	public void update(Observable o, Object arg) {

		if (o instanceof FileIOManager){
			if (arg instanceof IOEvent){

				if (((IOEvent) arg).getType() == IOEvent.Type.WRITING){
					setChanged();
					notifyObservers(arg);
				}

				if (((IOEvent) arg).getType() == IOEvent.Type.READING){
					setChanged();
					notifyObservers(arg);
				}

				if (((IOEvent) arg).getType() == IOEvent.Type.FINISHED_READING){

					TreeSet<Integer> sortedKeys = new TreeSet<Integer>(models.keySet());
					try {
						buildModel(fiom.getContainer(), models.get(sortedKeys.last()),sortedKeys.last());
					} catch (NumberFormatException nfe){
						arg = new IOEvent(this,IOEvent.Type.ERROR_READING,
								paths.get(sortedKeys.last()).getAbsolutePath(), 
								Localisation.getLocaleMessageTerm("EXC_UNIT_PROC"));
						cleanUpBuildingModel();
					}

					setChanged();
					notifyObservers(arg);
				}

				if (((IOEvent) arg).getType() == IOEvent.Type.FINISHED_WRITING){

					setChanged();
					notifyObservers(arg);
				}

				if (((IOEvent) arg).getType() == IOEvent.Type.ERROR_READING){

					if (!models.isEmpty()){
						TreeSet<Integer> sortedKeys = new TreeSet<Integer>(models.keySet());
						disposeDocument(sortedKeys.last());
					}

					setChanged();
					notifyObservers(arg);
				}

				if (((IOEvent) arg).getType() == IOEvent.Type.ERROR_WRITING){
					setChanged();
					notifyObservers(arg);
				}
			}
		}
	}

	private void buildModel(IFileDataContainer container, MemoryModel model, Integer docCounter) {

		Indexing<String> objs = container.getObjects();
		Indexing<String> atts = container.getAttributes();

		TreeSet<Integer> sortedObjIndices = new TreeSet<Integer>(objs.getValueSet());
		for (Integer i : sortedObjIndices){
			model.addRowID(objs.getKeyForValue(i));
		}

		TreeSet<Integer> sortedAttIndices = new TreeSet<Integer>(atts.getValueSet());
		for (Integer i : sortedAttIndices){
			model.addColumnID(atts.getKeyForValue(i));
		}

		String o = null;
		String a = null;
		for (Integer i : sortedObjIndices){
			o = objs.getKeyForValue(i);
			for (Integer j : sortedAttIndices){
				a = atts.getKeyForValue(j);
				//				System.out.println(o+","+a+","+container.getValue(o, a));
				//				model.putValue(o, a, container.getValue(o, a));
				model.setValue(i, j, Double.parseDouble(container.getValue(o, a)));
			}
		}

		//jetzt die gui benachrichtigen, dass das model nummer ? fertig ist
		outerGui.initDocument(model, docCounter);
		fiom.containerNotLongerNeeded();
		model.doNotify();
	}

	private void cleanUpBuildingModel() {

		TreeSet<Integer> sortedKeys = new TreeSet<Integer>(models.keySet());
		disposeDocument(sortedKeys.last());
	}

	public void initDocument(File file, AbstractFileFilter filter) {

		MemoryModel model = new MemoryModel();
		models.put(docCounter, model);
		paths.put(docCounter,file);
		filters.put(docCounter, filter);

		if (file != null){			
			fiom.readFile(file.getAbsolutePath(), filter.getFormat());
		} else {
			outerGui.initDocument(model, docCounter);
		}

		docCounter++;
	}

	public void writeDocument(File file, AbstractFileFilter fileFilter,
			Integer docId) {

		MemoryModel model = models.get(docId);

		paths.put(docId, file);
		filters.put(docId, fileFilter);

		MemoryList<GenericIdentifyable> columns = model.getColumnIDs();
		MemoryList<GenericIdentifyable> rows = model.getRowIDs();

		/*
		 * jetzt pruefen ob columns == rows wegen container-art
		 */
		IFileDataContainer container = null;
		HeaderData header = new HeaderData();
		header.setQuadratic(true);
		for (GenericIdentifyable i : rows){
			header.addObject(i.toString());
		}
		for (GenericIdentifyable i : columns)
			header.addAttribute(i.toString());

		if (columns.size() == rows.size()){
			if (haveSameElements(columns,rows)){

				header.setQuadratic(true);	
				container = new MatrixContainer(header);
			}
		} else {
			header.setQuadratic(false);
			container = new TableContainer(header);
		}

		for (int i = 0; i < rows.size(); i++){
			for (int j = 0; j < columns.size(); j++){
				container.putByID(rows.get(i), new String[]{
					columns.get(j),
					model.getMatrix().getValue(i, j) + ""
				});
			}
		}

		fiom.storeFile(file.getAbsolutePath(), fileFilter.getFormat(), container);
	}

	private boolean haveSameElements(MemoryList<GenericIdentifyable> columns,
			MemoryList<GenericIdentifyable> rows) {

		for (GenericIdentifyable i : columns)
			if (!rows.contains(i))
				return false;

		for (GenericIdentifyable i : rows)
			if (!columns.contains(i))
				return false;

		return true;
	}

	public void disposeDocument(Integer i) {

		models.remove(i);
		filters.remove(i);
		paths.remove(i);
	}

	public File getDocumentFile(int docId) {
		return paths.get(docId);
	}	

	public AbstractFileFilter getDocumentFilter(int docId){
		return filters.get(docId);
	}

	public void doNotify(IOEvent ioEvent) {

		setChanged();
		notifyObservers(ioEvent);
	}
}
