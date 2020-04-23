package de.dlr.ivf.tapas.persistence.io;

import com.csvreader.CsvReader;
import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * This class provides basic methods to open and read from a comma separated value file via a CsvReader. You can
 * receive the headers and each record, i.e. a line in the file. A method to check the header is also provided
 * <p>
 * important The method for checking the headers is not implemented, therefore it is depreciated. If you implement it, remove
 * this comment and the deprecation statement.
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.CLIENT)
public class TPS_FileReader {

    /**
     * The filename of the file
     */
    private final String fileName;

    /**
     * The Reader of the file
     */
    private final CsvReader reader;

    /**
     * This constructor builds a CsvReader from the given file.
     *
     * @param fileName The filename of the file
     * @throws FileNotFoundException This exception is thrown if the file doesn't exist
     * @throws IOException           This exception is thrown if there is no permission to open the file or read from it
     */
    public TPS_FileReader(String fileName) throws FileNotFoundException, IOException {
        if (fileName == null) throw new NullPointerException("The fileName is null");

        this.fileName = fileName;
        FileReader fReader = new FileReader(this.getFileName());
        BufferedReader bReader = new BufferedReader(fReader, 1024 * 64);
        this.reader = new CsvReader(bReader);
    }


    /**
     * Returns the filename of this file
     *
     * @return the file name
     */
    public String getFileName() {
        return this.fileName;
    }

    /**
     * This method should be called only once, because it reads one record of the file. Usually this method can be called
     * after opening the file.
     *
     * @return header of the file
     * @throws IOException this exception is thrown when the file is already closed
     */
    String[] getHeaders() throws IOException {
        this.reader.readHeaders();
        return reader.getHeaders();
    }

    /**
     * Returns the next line of the file, separated in a String array
     *
     * @return the next line of the file
     * @throws IOException this exception is thrown if anything in the reading process of the record produces an exception
     */
    String[] getRecord() throws IOException {
        String[] array = null;
        if (this.reader.readRecord()) {
            array = reader.getValues();
        } else {
            this.reader.close();
        }
        return array;
    }
}
