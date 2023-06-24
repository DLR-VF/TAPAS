/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.persistence.io;

import de.dlr.ivf.tapas.logger.legacy.LogHierarchy;
import de.dlr.ivf.tapas.logger.legacy.TPS_Logger;
import de.dlr.ivf.tapas.logger.legacy.HierarchyLogLevel;
import de.dlr.ivf.tapas.logger.legacy.SeverityLogLevel;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This class provides basic behaviour for reading a file. It can open the file, read the header and all lines and close the
 * file automatically. There are three main methods you can override. The init() and finish() methods are called at the
 * beginning respectively at the end of the reading process. The abstract method consume you have to implement. It is called
 * for each line read from the file.
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.CLIENT)
public abstract class TPS_FileConsumer {

    /**
     * Flag determines whether the file has a header or not
     */
    private final boolean hasHeader;

    /**
     * The internal file reader
     */
    private final TPS_FileReader reader;

    /**
     * The length one record in this file should have. If the record is too short an exception is thrown. If the record is
     * too long a warning is printed
     */
    private int recordLength;

    /**
     * Calls this(fileName, true);
     *
     * @param fileName name of the file to read
     */
    public TPS_FileConsumer(String fileName) {
        this(fileName, true);
    }

    /**
     * Calls this (fileName, hasHeader, -1);
     *
     * @param fileName  name of the file to read
     * @param hasHeader flag if the file has a header
     */
    public TPS_FileConsumer(String fileName, boolean hasHeader) {
        this(fileName, hasHeader, -1);
    }

    /**
     * The constructor builds a {@link TPS_FileReader} and initaliases the member variables.
     *
     * @param fileName     name of the file to read
     * @param hasHeader    flag if the file has a header
     * @param recordLength length of the record, values smaller than zero determines that the length is not specified
     */
    public TPS_FileConsumer(String fileName, boolean hasHeader, int recordLength) {
        try {
            this.reader = new TPS_FileReader(fileName);
            this.hasHeader = hasHeader;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.recordLength = recordLength;
    }

    /**
     * Calls this(fileName, true, recordLength);
     *
     * @param fileName     name of the file to read
     * @param recordLength length of the record, values smaller than zero determines that the length is not specified
     */
    public TPS_FileConsumer(String fileName, int recordLength) {
        this(fileName, true, recordLength);
    }

    /**
     * This method is called for each line of the file. It should consume the line and store all information in memory.
     *
     * @param record    the current line (record) of the file
     * @param lineCount the current line number
     */
    protected abstract void consume(String[] record, int lineCount);

    /**
     * This method consumes the complete file. At the beginning it calls the init() method. For each line the consume()
     * method is called. At least the finish() method is called. If there occur exceptions they are stored until an amuont of
     * ten and then they are thrown as one single exception.
     *
     * @param hasHeader flag if there exsits a header
     * @throws Exception This method can throw any Exception
     */
    private void consumeFile(boolean hasHeader) throws Exception {
        if (TPS_Logger.isLogging(SeverityLogLevel.INFO)) {
            TPS_Logger.log(SeverityLogLevel.INFO, "Opened file: " + this.getFileName());
        }

        if (hasHeader) this.init(this.reader.getHeaders());
        else this.init(null);

        String[] record;
        int lineCount = 0;
        SortedMap<Integer, Exception> exMap = new TreeMap<>();

        while ((record = reader.getRecord()) != null) {
            if (record.length == 0 || record[0].length() == 0) {
                if (TPS_Logger.isLogging(SeverityLogLevel.WARN)) {
                    TPS_Logger.log(SeverityLogLevel.WARN,
                            "Skipped empty line in file " + this.getShortFileName() + " in line " + (lineCount + 1) +
                                    ": " + Arrays.toString(record));
                }
            } else {
                try {
                    if (this.getRecordLength() > 0) {
                        if (record.length < this.getRecordLength()) {
                            throw new IOException(
                                    "Record  in file " + this.getShortFileName() + " in line " + (lineCount + 1) + " " +
                                            Arrays.toString(record) + " is too short: " + this.getRecordLength() +
                                            "(requested) > " + record.length);
                        } else if (record.length > this.getRecordLength()) {
                            if (TPS_Logger.isLogging(SeverityLogLevel.FINE)) {
                                TPS_Logger.log(SeverityLogLevel.FINE,
                                        "Record  in file " + this.getShortFileName() + " in line " + (lineCount + 1) +
                                                " " + Arrays.toString(record) + " is too long: " +
                                                this.getRecordLength() + "(requested) < " + record.length);
                            }
                        }
                    }
                    this.consume(record, lineCount);
                } catch (Exception e) {
                    // An exception is thrown when the whole file was read
                    exMap.put(lineCount, e);
                    if (exMap.size() > 10) {
                        throw new IOException("At least one exception occured while reading file: " +
                                new File(this.getFileName()).getName() + "\n" + createException(exMap));
                    }
                }
                lineCount++;
            }
        }

        if (exMap.size() > 0) {
            throw new IOException(
                    "At least one exception occured while reading file: " + new File(this.getFileName()).getName() +
                            "\n" + createException(exMap));
        }

        this.finish(lineCount);
        if (TPS_Logger.isLogging(SeverityLogLevel.INFO)) {
            TPS_Logger.log(SeverityLogLevel.INFO,
                    "Read " + lineCount + " entries of file: " + new File(this.getFileName()).getName());
        }
    }

    /**
     * All stored exceptions are combined to one single exception
     *
     * @param exMap all stored exceptions
     * @return one single exception
     */
    private String createException(SortedMap<Integer, Exception> exMap) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        for (Integer key : exMap.keySet()) {
            pw.write("Exception happened in line " + key + ":\n");
            Exception ex = exMap.get(key);
            ex.printStackTrace(pw);
        }
        pw.flush();
        pw.close();
        return sw.getBuffer().toString();
    }

    /**
     * This method is called after the last line of the file is consumed
     *
     * @param lineCount count of all read lines
     */
    protected void finish(int lineCount) {
        // if it is needed override it
    }

    /**
     * @return filename
     */
    protected String getFileName() {
        return this.reader.getFileName();
    }

    /**
     * @return length of a record, values smaller than zero determines that the length is not specified
     */
    public int getRecordLength() {
        return recordLength;
    }

    /**
     * Sets the record length.
     *
     * @param recordLength record length, values smaller than zero determines that the length is not specified
     */
    public void setRecordLength(int recordLength) {
        this.recordLength = recordLength;
    }

    /**
     * @return local filename without path
     */
    protected String getShortFileName() {
        return new File(this.reader.getFileName()).getName();
    }

    /**
     * This method is called before the first line of the file is read. When there exists a header it is the first argument
     * otherwise it is null.
     *
     * @param headers the header of the file
     */
    protected void init(String[] headers) {
        // if it is needed override it
    }

    /**
     * Starts the reading of the file. This method catches all exceptions and log them.
     */
    public void start() {
        try {
            this.consumeFile(this.hasHeader);
        } catch (Exception e) {
            TPS_Logger.log(SeverityLogLevel.ERROR, e);
            throw new RuntimeException("Fatal Error in TPS_FileConsumer.start() -> stop application", e);
        }
    }
}
