package de.dlr.ivf.tapas.matrixtool.fileIO_v3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;

public class AsciiIOHandler extends AbstractIOHandler{
	
	private static final String EOL = "\r\n";  //fuer windows
	private static final int BUFFERLIMIT = 10000;
	
	private AsciiFileReader reader;
	private AsciiFileWriter writer;
	private IFormatLogic logic;
	
	
	public AsciiIOHandler(IFormatLogic logic){
		this.logic = logic;
	}


	public void readFile(String path){
		
		file = path;
		
		BufferedReader r = null;
		if (!(new File(path)).exists()){
			signalReadingException(Localisation.getLocaleMessageTerm("IO_FILE_NOT_FOUND"));
		}
		try {
			r = new BufferedReader(new FileReader(path));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		reader = new AsciiFileReader(this, logic, new LinkedBlockingQueue<IBufferUnit>(BUFFERLIMIT), r);
		reader.start();
	}
	
	public IFileDataContainer getContainer(){
		return reader.getContainer();
	}	
	
	public void storeFile(String path, IFileDataContainer container) {
		
		file = path;
		
		BufferedWriter w = null;
		try {
			w = new BufferedWriter(new FileWriter(path));
		} catch (FileNotFoundException e) {
			signalWritingException(Localisation.getLocaleMessageTerm("IO_FILE_NOT_FOUND"));
			e.printStackTrace();
		} catch (IOException e) {
			signalWritingException(Localisation.getLocaleMessageTerm("IO_FILE_PROBLEM"));
			e.printStackTrace();
		}
		
		writer = new AsciiFileWriter(this, logic, container, w, EOL, path);
		writer.start();
	}
}
