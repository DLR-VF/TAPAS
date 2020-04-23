package de.dlr.ivf.tapas.tools.matrixMap;

import com.csvreader.CsvWriter;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import de.dlr.ivf.tapas.util.PropertyReader;
import de.dlr.ivf.tapas.util.TPS_Geometrics;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * @author Holger
 */
public class CruisingSpeedValidate extends TPS_BasicConnectionClass {
    final int BINS_WIDTH = 500;
    final int RECORDS_ALL = 3;
    final int RECORDS = RECORDS_ALL - 1;
    private final CruisingSpeedValData propData;
    private final CruisingSpeedCalcData calcData;
    private final DataRecord[] records;
    private final DataRecord[] differences;
    private final DataRecord[] factors;
    private int sizeOfClassDistance;
    private int length;
    private String directory = null;
    private ClassStatistics[][] statistics = null;
    private File file = null;
    private Properties properties = null;
    private String modus;
    private String terrain;
    //private boolean top3;
    //private boolean indirectWayFactor;
    private double[] calcFactors = null;

    public CruisingSpeedValidate(CruisingSpeedValData valData, CruisingSpeedCalcData calcData) {
        records = new DataRecord[RECORDS_ALL];        // record[0] - validation data
        // record[1] - reference data
        for (int i = 0; i < RECORDS_ALL; i++)         // record[2] - data withount correction factor
        {
            records[i] = new DataRecord();
            records[i].diagonalCount = 0;
        }

        differences = new DataRecord[RECORDS];

        for (int i = 0; i < RECORDS; i++)
            differences[i] = new DataRecord();      // differences[0] - reference data - validation data
        // differences[1] - base data - validation data
        factors = new DataRecord[RECORDS];

        for (int i = 0; i < RECORDS; i++)
            factors[i] = new DataRecord();


     /*   top3 = true;
        indirectWayFactor = true;*/

        this.propData = valData;
        this.calcData = calcData;
    }

    public static void main(String[] args) {
        CruisingSpeedValidate cv = new CruisingSpeedValidate(new CruisingSpeedValData(),
                new CruisingSpeedCalcData()); //TODO muss noch weg
        cv.start();
        System.exit(0);

    }

    /**
     * The method stores min and max values in the given array and returns the count of classes
     *
     * @param index index of the matrix 0 - validation data , 1 - reference data, 2 - base data
     * @return count of classes for the given matrix in relation to BINS_WIDTH
     */
    private int calulateMinMaxValues(int index) {
        int max;

        records[index].max = records[index].data[0][0];
        records[index].min = records[index].max;
        records[index].diagonalCount = 0;
        for (int i = 0; i < records[index].data.length; i++) {
            for (int j = 0; j < records[index].data[i].length; j++) {

                if (j >= i && (records[index].data[i][j] != 99999)) // value 99000 means, no way to this TAZ
                {
                    records[index].diagonalCount++;

                    if (records[index].max < records[index].data[i][j]) records[index].max = records[index].data[i][j];

                    if (records[index].min > records[index].data[i][j]) records[index].min = records[index].data[i][j];
                }
            }
        }
        max = (int) records[index].max;
        return (max + BINS_WIDTH - max % BINS_WIDTH) / BINS_WIDTH;
    }

    @SuppressWarnings("unused")
    private boolean checkFileName() {
        if (directory == null || directory.equals("")) return false;
        int index = directory.indexOf('.');
        if (index != -1) directory = directory.substring(0, index);

        System.out.println(directory);
        return true;
    }

    private void checkParameter(String text) throws IllegalArgumentException {
        if (records[Data.BASE_DATA.value].matricesTable == null ||
                records[Data.BASE_DATA.value].matricesTable.isEmpty()) throw new IllegalArgumentException(
                "missing mandatory parameter: \"tazTable\"" + text);

        if (records[Data.VALIDATION_DATA.value].matricesTable == null ||
                records[Data.VALIDATION_DATA.value].matricesTable.isEmpty() ||
                records[Data.REFERENCE_DATA.value].matricesTable == null ||
                records[Data.REFERENCE_DATA.value].matricesTable.isEmpty()) throw new IllegalArgumentException(
                "missing mandantory parameter: \"matricesTable\"" + text);

        if (records[Data.VALIDATION_DATA.value].dataName == null ||
                records[Data.VALIDATION_DATA.value].dataName.isEmpty()) throw new IllegalArgumentException(
                "missing mandantory parameter: \"validation\" in file: crusingspeed.properties" + text);

        if (records[Data.REFERENCE_DATA.value].dataName == null ||
                records[Data.REFERENCE_DATA.value].dataName.isEmpty()) throw new IllegalArgumentException(
                "missing mandantory parameter: \"reference\" in file: crusingspeed.properties" + text);

        if (directory == null || directory.isEmpty()) throw new IllegalArgumentException(
                "missing mandantory parameter: \"path\" in file: crusingspeed.properties" + text);

        if (modus == null || modus.isEmpty()) throw new IllegalArgumentException(
                "missing mandantory parameter: \"modus\" in file: crusingspeed.properties" + text);

        if (terrain == null || terrain.isEmpty()) throw new IllegalArgumentException(
                "missing mandantory parameter: \"terrain\"" + text);
    }

    CommandLine cliParser(String[] args) {
        CommandLine lvCmd = null;

        HelpFormatter lvFormater = new HelpFormatter();
        CommandLineParser lvParser = new DefaultParser();
        Options lvOptions = new Options();

        Option lvHilfe = new Option("h", "help", false, "show this help screen.");

        lvOptions.addOption(lvHilfe);

        lvOptions.addOption(Option.builder("ref").longOpt("reference").argName("REF_DATA_NAME").required(true)
                                  .valueSeparator(' ').hasArg().desc("reference distance data for validation.")
                                  .build());
        lvOptions.addOption(Option.builder("val").longOpt("validation").argName("NEW_DATA_NAME").required(true)
                                  .valueSeparator(' ').hasArg().desc("new distance data for validation.").build());
        lvOptions.addOption(Option.builder("mat").longOpt("matricesTable").argName("MATRICES_TABLE").required(true)
                                  .valueSeparator(' ').hasArg()
                                  .desc("tabel name of the matrices Table e.g \"core.berlin_matrices\".").build());
        lvOptions.addOption(Option.builder("taz").longOpt("tazTable").argName("TAZ_TABLE_NAME").required(true)
                                  .valueSeparator(' ').hasArg()
                                  .desc("tabel name the TAZ-table e.g. \"core.berlin_taz\".").build());
        lvOptions.addOption(Option.builder("p").longOpt("path").argName("OUTPUT_PATH").required(true).valueSeparator(
                ' ').hasArg().desc("Path for output cvs-file").build());
        lvOptions.addOption(Option.builder("m").longOpt("modus").argName("MODUS").required(true).valueSeparator(' ')
                                  .hasArg().desc("modus (PT, MIV, BIKE, WALK").build());
        lvOptions.addOption(Option.builder("t").longOpt("terrain").argName("TERRAIN").required(true).valueSeparator(' ')
                                  .hasArg().desc("terrain (PLAIN, DOWNS, MOUNTAINOUS, CITY, STOPS").build());

        try {
            lvCmd = lvParser.parse(lvOptions, args);

            if (lvCmd.hasOption('h')) {
                lvFormater.printHelp("CruisingSpeedValidate", lvOptions);
                return null;
            }

        } catch (ParseException pvException) {
            lvFormater.printHelp("CruisingSpeedValidate", lvOptions);
            System.out.println("Parse Error:" + pvException.getMessage());
        }
        return lvCmd;
    }

    private void fillStatistics(Evaluation type) {
        // initDistanceClasses();

        double value = 0;

        // for(int t = 0; t < RECORDS; t++)
        for (int t = type.value * 2; t < RECORDS + 2 * type.value; t++)  // 0...1 for type = FACTOR(0)
        {                                                         // 2...3 for type = DIFFERENCE(1)
            for (int i = 0; i < length; i++) {
                for (int j = 0; j < length; j++) {
                    if (j >= i && records[t % 2].data[i][j] != 99999) // right-diagonal-matrix-elements < 9900 only
                    {
                        int pos = (int) records[Data.BASE_DATA.value].data[i][j] / BINS_WIDTH;

                        pos = Math.max(0, Math.min(statistics[t].length - 1, pos));
                        if (type == Evaluation.FACTOR)
                            value = records[t].data[i][j] / records[Data.BASE_DATA.value].data[i][j];
                        if (type == Evaluation.DIFFERENCE) value = differences[t % 2].data[i][j];

                        statistics[t][pos].distanceClass.add(value);
                        statistics[t][pos].count++;
                        statistics[t][pos].sumOfValue += value;
                        statistics[t][pos].absoluteSumOfValue += Math.abs(value);
                    }
                }
            }
        }
    }

    private void generateDifferencesMatrix() {
        for (int t = 0; t < RECORDS; t++) {
            differences[t].data = new double[length][length];

            for (int i = 0; i < length; i++)
                for (int j = 0; j < length; j++) {
                    if (records[t].data[i][j] > 99000) differences[t].data[i][j] = records[t].data[i][j];
                    else {
                        // t=0: records[VALIDATION_DATA] - records[BASE_DATA]
                        // t=1: records[REFERENCE_DATA] - records[BASE_DATA]
                        differences[t].data[i][j] = records[t].data[i][j] - records[Data.BASE_DATA.value].data[i][j];
                    }


                    differences[t].data[j][i] = differences[t].data[i][j];
                }
        }
    }

    private void generateFactorMatrix() {
        for (int t = 0; t < RECORDS; t++) {
            int count = 0;
            factors[t].data = new double[2][records[t].diagonalCount];
            for (int i = 0; i < length; i++) {
                for (int j = 0; j < length; j++) {
                    if (j >= i && records[t].data[i][j] != 99999) {
                        factors[t].data[0][count] = records[Data.BASE_DATA.value].data[i][j];
                        factors[t].data[1][count] = records[t].data[i][j] / records[Data.BASE_DATA.value].data[i][j];
                        count++;
                    }
                }
            }
        }
    }

    private void init() {
        records[Data.VALIDATION_DATA.value].dataName = propData
                .getRecordName();                            // CAR_1193TAZ_DIST_NEW
        records[Data.REFERENCE_DATA.value].dataName = properties.getProperty("reference")
                                                                .trim();           // CAR_1193TAZ_DIST
        records[Data.BASE_DATA.value].dataName = "XXX_1193TAZ_TAZ";                                         // XXX_1193TAZ_TAZ - table save name in a testversion
        records[Data.VALIDATION_DATA.value].matricesTable = properties.getProperty("matricesTable")
                                                                      .trim(); // core.berlin_matrices
        records[Data.REFERENCE_DATA.value].matricesTable = properties.getProperty("matricesTable")
                                                                     .trim();  // core.berlin_matrices
        // the matricesTable isnt needed for BASE_DATA, therefor the tazTable will stored in tazTable
        records[Data.BASE_DATA.value].matricesTable = properties.getProperty("tazTable")
                                                                .trim();            // core.berlin_taz
        directory = properties.getProperty("path")
                              .trim();                                                  // Directory name for save validate data
        modus = properties.getProperty("modus").trim();
        terrain = properties.getProperty("terrain").trim();
    }

    private void initDistanceClasses() {
        for (int t = 0; t < RECORDS * 2; t++) {
            for (int i = 0; i < sizeOfClassDistance; i++) {
                statistics[t][i] = new ClassStatistics();
                statistics[t][i].count = 0;
                statistics[t][i].sumOfValue = 0.0;
                statistics[t][i].arithmeticAverage = -1.0;
                statistics[t][i].absoluteSumOfValue = 0.0;
                statistics[t][i].distanceClass = new ArrayList<>();
            }
        }
    }

    private boolean readData() {
        String query = "";
        for (int i = 0; i < RECORDS; i++) {
            try {
                query = "SELECT matrix_values FROM " + records[i].matricesTable + " WHERE matrix_name = '" +
                        records[i].dataName + "';";

                ResultSet rs = dbCon.executeQuery(query, this);
                if (rs.next()) {
                    Object array = rs.getArray(1).getArray();
                    Integer[] matrixVal = (Integer[]) array;
                    records[i].data = TPS_BasicConnectionClass.array1Dto2D(matrixVal);
                } else {
                    System.err.println("Record " + records[i].dataName + " in table " + records[i].matricesTable +
                            " does not exist!");
                    return false;
                }
            } catch (SQLException ex) {
                System.err.println(
                        this.getClass().getCanonicalName() + " executeParameters: SQL-Error during statement: " +
                                query);
                ex.printStackTrace();
            }

            // generate bee-line-matrix to build factors of reference table
            CruisingSpeedCalculate csc = new CruisingSpeedCalculate(calcData);
            records[Data.BASE_DATA.value].data = csc.calcDistance(records[Data.BASE_DATA.value].matricesTable,
                    records[Data.REFERENCE_DATA.value].matricesTable, modus, terrain, false);
            calcFactors = csc.getFactors();

            TPS_Geometrics.calcTop3(records[2].data);
        }

        return true;

    }

    private void readProperties() throws IllegalArgumentException {
        try {
            properties = PropertyReader.getProperties(CruisingSpeed.propertyFileName);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        init(); // records[Data.VALIDATION_DATA.value].dataName is set to a different value in init()
        records[Data.VALIDATION_DATA.value].dataName = properties.getProperty("validation")
                                                                 .trim();  // CAR_1193TAZ_DIST_NEW

        checkParameter(" in command line.");
    }

    public void start() {
        try {
            this.readProperties();
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }

        System.out.println("TAZ-table name               :" + this.records[Data.BASE_DATA.value].matricesTable);
        System.out.println("database table name          :" + this.records[Data.VALIDATION_DATA.value].matricesTable);
        for (int i = 0; i < this.propData.getReferenceDist().length; ++i) {
            this.records[Data.VALIDATION_DATA.value].dataName = CruisingSpeed.generateRecordName(
                    Modus.values()[i].getName(), CruisingSpeed.DISTANCE);
            this.records[Data.REFERENCE_DATA.value].dataName = this.propData.getReferenceDist()[i];

            if (this.records[Data.VALIDATION_DATA.value].dataName != null &&
                    this.records[Data.VALIDATION_DATA.value].dataName.length() > 0 &&
                    this.records[Data.REFERENCE_DATA.value].dataName != null &&
                    this.records[Data.REFERENCE_DATA.value].dataName.length() > 0 &&
                    this.records[Data.BASE_DATA.value].matricesTable != null &&
                    this.records[Data.BASE_DATA.value].dataName.length() > 0 &&
                    this.records[Data.VALIDATION_DATA.value].matricesTable != null &&
                    this.records[Data.VALIDATION_DATA.value].dataName.length() > 0) {
                System.out.println(
                        "validation distance data     :" + this.records[Data.VALIDATION_DATA.value].dataName);
                System.out.println("reference distance data      :" + this.records[Data.REFERENCE_DATA.value].dataName);
                this.validate();
            }
            this.records[Data.VALIDATION_DATA.value].dataName = CruisingSpeed.generateRecordName(
                    Modus.values()[i].getName(), CruisingSpeed.TRAVELTIME);
            this.records[Data.REFERENCE_DATA.value].dataName = this.propData.getReferenceTT()[i];
            if (this.records[Data.VALIDATION_DATA.value].dataName != null &&
                    this.records[Data.VALIDATION_DATA.value].dataName.length() > 0 &&
                    this.records[Data.REFERENCE_DATA.value].dataName != null &&
                    this.records[Data.REFERENCE_DATA.value].dataName.length() > 0 &&
                    this.records[Data.BASE_DATA.value].matricesTable != null &&
                    this.records[Data.BASE_DATA.value].dataName.length() > 0 &&
                    this.records[Data.VALIDATION_DATA.value].matricesTable != null &&
                    this.records[Data.VALIDATION_DATA.value].dataName.length() > 0) {
                System.out.println(
                        "validation distance data     :" + this.records[Data.VALIDATION_DATA.value].dataName);
                System.out.println("reference distance data      :" + this.records[Data.REFERENCE_DATA.value].dataName);
                this.validate();
            }
        }
    }

    private void validate() {
        if (this.readData()) {
            this.length = this.records[Data.REFERENCE_DATA.value].data.length;
            this.calulateMinMaxValues(Data.REFERENCE_DATA.value); // call is needed to calculte sizeOfClassDistance
            this.calulateMinMaxValues(Data.VALIDATION_DATA.value);
            this.sizeOfClassDistance = this.calulateMinMaxValues(Data.BASE_DATA.value);
            System.out.println("min(REF) = " + this.records[Data.REFERENCE_DATA.value].min);
            System.out.println("max(REF) = " + this.records[Data.REFERENCE_DATA.value].max);
            System.out.println("min(VAL) = " + this.records[Data.VALIDATION_DATA.value].min);
            System.out.println("max(VAL) = " + this.records[Data.VALIDATION_DATA.value].max);
            System.out.println("sizeOfClassDistance = " + this.sizeOfClassDistance);

            this.statistics = new ClassStatistics[2 * this.RECORDS][this.sizeOfClassDistance];
            this.initDistanceClasses(); // Aufruf darf erst nach der Bestimmung von sizeOfClassDistance erfolgen

            this.generateDifferencesMatrix();
            this.generateFactorMatrix();

            //this.writeMatrixToDB(TPS_BasicConnectionClass.matrixToStringWriterSQL(this.differences[0].data,0), "CAR_1193TAZ_DIST_NEW_DIFF");

            this.fillStatistics(Evaluation.DIFFERENCE);
            this.fillStatistics(Evaluation.FACTOR);

            for (int t = 0; t < this.RECORDS * 2; t++) {
                for (int j = 0; j < this.sizeOfClassDistance; j++) {
                    if (this.statistics[t][j].count > 0) this.statistics[t][j].arithmeticAverage =
                            this.statistics[t][j].absoluteSumOfValue / (double) this.statistics[t][j].count;
                    else this.statistics[t][j].arithmeticAverage = -1;
                }
            }
            this.writeFactorsToCVS();
            this.writeStatisticsToCVS();
            this.file = null; // new dir for new statistics
        }
    }

    private void writeFactorsToCVS() {
        String output;
        SimpleDateFormat dateFormat = new SimpleDateFormat("__yyyy_MM_dd__HH_mm_ss");

        if (file == null) {
            output = directory + File.separator + records[Data.REFERENCE_DATA.value].dataName + "_Validation" +
                    dateFormat.format(new Date()).substring(0, 22);
            file = new File(output.toLowerCase());
            file.mkdirs();
        } else {
            output = file.getAbsolutePath();
        }

        for (int t = 0; t < RECORDS; t++) {
            String out = output + File.separator + "factors_" + Data.values()[t].toString() + ".csv";

            try {
                CsvWriter csvOutput = new CsvWriter(new FileWriter(out.toLowerCase(), false), ';');

                // write out the header line
                csvOutput.write("From TAZ");
                csvOutput.write("To TAZ");
                csvOutput.write("Distance");
                csvOutput.write("Factor");
                csvOutput.endRecord();

                // write out a few records
              /*  for(int i = 0; i < factors[t].data[0].length; i++)
                {
                    csvOutput.write(String.valueOf((int)factors[t].data[0][i]));
                    csvOutput.write(String.valueOf(factors[t].data[1][i]).replace('.', ','));
                    csvOutput.endRecord();
                }*/

                int count = 0;

                for (int i = 0; i < length; i++) {
                    for (int j = 0; j < length; j++) {
                        if (j >= i && records[t].data[i][j] != 99999) {
                            csvOutput.write(Integer.toString(i));
                            csvOutput.write(Integer.toString(j));
                         /*   if(count == records[t].diagonalCount)
                            {
                                System.out.println("i="+i);
                                System.out.println("j="+j);
                                System.out.println("count="+count);
                                System.out.println("diagonalCount="+records[t].diagonalCount);
                            }*/
                            csvOutput.write(String.valueOf((int) factors[t].data[0][count]));
                            csvOutput.write(String.valueOf(factors[t].data[1][count]).replace('.', ','));
                            csvOutput.endRecord();
                            count++;
                        }
                    }
                }

                csvOutput.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeStatisticsToCVS() {
        String output;
        SimpleDateFormat dateFormat = new SimpleDateFormat("__yyyy_MM_dd__HH_mm_ss");

        if (file == null) {
            output = directory + File.separator + records[Data.REFERENCE_DATA.value].dataName + "_Validation" +
                    dateFormat.format(new Date()).substring(0, 22);
            file = new File(output.toLowerCase());
            file.mkdirs();
            file = null;
        } else {
            output = file.getAbsolutePath();
        }

        String out = output + File.separator + "statistics.csv";

        try {
            CsvWriter csvOutput = new CsvWriter(new FileWriter(out, false), ';');

            // write out the header line
            for (int i = 0; i < statistics[0][i].count; i++)
                csvOutput.write(String.valueOf(i * BINS_WIDTH));
            csvOutput.endRecord();


            // write out records
            for (int t = 0; t < RECORDS * 2; t++) {
                for (int i = 0; i < statistics[0][i].count; i++)
                    csvOutput.write(String.valueOf(statistics[t][i].arithmeticAverage).replace('.', ','));

                csvOutput.endRecord();
            }

            // write bee-line distance
       /*     for( int i = 0; i < length; i++)
            {
                for(int j = 0; j < length; j++)
                {
                    if(j >= i && records[Data.BASE_DATA.value].data[i][j] < 99_000 )
                        csvOutput.write(String.valueOf(records[Data.BASE_DATA.value].data[i][j]).replace('.', ','));
                }
                csvOutput.endRecord();
            }*/

            // write formel and factors
            if (calcFactors != null) {
                csvOutput.endRecord();
                csvOutput.endRecord();
                csvOutput.write("(" + calcFactors[0] + "/log10(" + calcFactors[1] + "*(x^" + calcFactors[2] + ")+" +
                        calcFactors[3] + "))+" + calcFactors[4]);
                csvOutput.endRecord();
                csvOutput.write("modus = " + modus);
                csvOutput.endRecord();
                csvOutput.write("terrain = " + terrain);
            }
            csvOutput.endRecord();
            csvOutput.write("PT(" + Modus.PT.getCorrectionFactor() + ")");
            csvOutput.write("MIV(" + Modus.MIV.getCorrectionFactor() + ")");
            csvOutput.write("BIKE(" + Modus.BIKE.getCorrectionFactor() + ")");
            csvOutput.write("WALK(" + Modus.WALK.getCorrectionFactor() + ")");
            csvOutput.endRecord();

            csvOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    enum Data // Data types
    {
        VALIDATION_DATA(0), REFERENCE_DATA(1), BASE_DATA(2);        // distances without detour factor

        private final int value;

        Data(int value) {
            this.value = value;
        }

        public double getValue() {
            return this.value;
        }
    }

    enum Evaluation {
        FACTOR(0), DIFFERENCE(1);

        private final int value;

        Evaluation(int value) {
            this.value = value;
        }

        public double getValue() {
            return this.value;
        }
    }

    class DataRecord {
        String dataName;
        String matricesTable;
        double[][] data;
        double min;
        double max;
        int diagonalCount;
    }

    class ClassStatistics {
        List<Double> distanceClass;
        int count;
        double sumOfValue;
        double arithmeticAverage;
        double absoluteSumOfValue;
    }
    
  /*  private void writeStatisticsToCVS()
    {        
        String output;
        SimpleDateFormat dateFormat = new SimpleDateFormat("__yyyy_MM_dd__HH_mm_ss");      
         
         if( file == null)
         {
             output = directory+File.separator+records[Data.REFERENCE_DATA.value].dataName
                             +"_Validation"+dateFormat.format(new Date()).substring(0, 21);
             file = new File(output.toLowerCase());
             file.mkdirs();
         }
         else
         {
             output = file.getAbsolutePath();
         }
         
        for(int t = 0; t < RECORDS*2; t++)
        {
            String out = output + File.separator+"statistics_"+Data.values()[(t^(t%2)%2)].toString()+"_"+Evaluation.values()[t%2]+".csv";
  
            try // t^(t%2)%2) 0 and 1 -> 0   
            {   //            2 and 3 -> 1
                CsvWriter csvOutput = new CsvWriter(new FileWriter(out, false), ';');
    
                // write out the header line
                for(int i = 0; i < statistics[t][i].count; i++)
                    csvOutput.write(String.valueOf(i*BINS_WIDTH));
                csvOutput.endRecord();
                
                
    
                // write out records
                for(int i = 0; i < statistics[t][i].count; i++)
                {
                        csvOutput.write(String.valueOf(statistics[t][i].arithmeticAverage).replace('.', ','));
                       // csvOutput.write(String.valueOf(statistics[t][i].arithmeticAverage).replace('.', ',')); 
                   // csvOutput.endRecord();
                }
                
               
                csvOutput.endRecord();
               
                csvOutput.close();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }*/
}
