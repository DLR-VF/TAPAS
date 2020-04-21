package de.dlr.ivf.tapas.analyzer.inputfileconverter;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.csvreader.CsvReader;

/**
 * This class provides basic methods to open and read from a comma separated value file via a {@link CsvReader}. You can receive the headers and each record, i.e. a line in the file. A method to check
 * the header is also provided
 * 
 * important The method for checking the headers is not implemented, therefore it is depreciated. If you implement it, remove this comment and the deprecation statement.
 * 
 * @author mark_ma
 */
public class TPS_FileReader {
	
	/**
	 * The filename of the file
	 */
	private String		fileName;
	
	/**
	 * The Reader of the file
	 */
	private CsvReader	reader;
	
	/**
	 * This constructor builds a {@link CsvReader} from the given file.
	 * 
	 * @param fileName
	 *            The filename of the file
	 * 
	 * @throws FileNotFoundException
	 *             This exception is thrown if the file doesn't exist
	 * @throws IOException
	 *             This exception is thrown if there is no permission to open the file or read from it
	 */
	public TPS_FileReader(String fileName, boolean hasHeader) throws FileNotFoundException, IOException {
		if (fileName == null)
			throw new NullPointerException("The fileName is null");
		
		this.fileName = fileName;
		// this.reader = new CsvReader(new BufferedReader(new FileReader(this.getFileName()), 1024*64));
		this.reader = new CsvReader(this.getFileName());
		
		if (hasHeader)
			this.reader.readHeaders();
	}
	
	/**
	 * Returns the name of the file
	 * 
	 * @return filename
	 */
	public String getFileName() {
		return this.fileName;
	}
	
	/**
	 * This method should be called only once, because it reads one record of the file. Usually this method can be called after opening the file.
	 * 
	 * @return header of the file
	 * 
	 * @throws IOException
	 *             this exception is thrown when the file is already closed
	 */
	public String[] getHeaders() throws IOException {
		return reader.getHeaders();
	}
	
	/**
	 * Returns the next line of the file, separated in a {@link String} array
	 * 
	 * @return the next line of the file
	 * @throws IOException
	 *             this exception is thrown if anything in the reading process of the record produces an exception
	 */
	public String[] getRecord() throws IOException {
		String[] array = null;
		if (this.reader.readRecord()) {
			array = reader.getValues();
		} else {
			this.reader.close();
		}
		return array;
	}
}
