package de.dlr.ivf.tapas.tools.matrixMap;

import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import de.dlr.ivf.tapas.util.PropertyReader;
import de.dlr.ivf.tapas.util.TPS_Geometrics;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;


public class CruisingSpeed extends TPS_BasicConnectionClass {
    public static final String propertyFileName = "crusingspeed.properties";

    public static final int DISTANCE = 0;
    public static final int TRAVELTIME = 1;

    private static Properties properties = null;

    static {
        try {
            properties = PropertyReader.getProperties(CruisingSpeed.propertyFileName);
        } catch (IOException ex) {
            System.err.println("Warning: No Property-file found.");
        }
    }

    private CruisingSpeedCalcData calcData = null;
    private CruisingSpeedValData valData = null;
    private boolean[] val;
    private boolean validation = false;
    @SuppressWarnings("unused")
    private boolean indirectWayFactor = true;


    public CruisingSpeed() {
        val = new boolean[8];
    }

    public static String generateRecordName(String modus, int type) {
        return "" + modus + "_" + properties.getProperty("record") + "_" + (type == DISTANCE ? "DIST" : "TT");
    }

    public static Properties getProperties() {
        return properties;
    }

    public static void setProperties(Properties properties) {
        CruisingSpeed.properties = properties;
    }
    
  /*  private void checkParameter(String text) throws IllegalArgumentException
    {
        if(tazTable == null || tazTable.isEmpty())
            throw new IllegalArgumentException("missing mandantory parameter: \"tazTable\""+ text);  
        if(matricesTable == null || matricesTable.isEmpty())
            throw new IllegalArgumentException("missing mandantory parameter: \"matricesTable\""+ text);
        if(recordName == null || recordName.isEmpty())
            throw new IllegalArgumentException("missing mandantory parameter: \"record\""+ text);
        if(modus == null || modus.isEmpty())
            throw new IllegalArgumentException("missing mandantory parameter: \"modus\""+ text);
        if(terrain == null || terrain.isEmpty())
            throw new IllegalArgumentException("missing mandantory parameter: \"terrain\""+ text);
    }*/

    public static void main(String[] args) {
        CruisingSpeed speed = new CruisingSpeed();
        speed.start(args);
    }

    void StringWriter2File(StringWriter str, String fileName) {
        try {
            FileWriter fw = new FileWriter(fileName);
            fw.write(str.toString());
            fw.close();
        } catch (IOException ex) {
            System.out.println("Error to Write file " + fileName);
        }
    }

    CommandLine cliParser(String[] args) {
        CommandLine lvCmd = null;

        HelpFormatter lvFormater = new HelpFormatter();
        CommandLineParser lvParser = new DefaultParser();
        Options lvOptions = new Options();

        Option lvHilfe = new Option("h", "help", false, "show this help screen.");

        lvOptions.addOption(lvHilfe);

        lvOptions.addOption(Option.builder("newp").longOpt("newProperty").desc("generate a new empty property-file")
                                  .build());
        lvOptions.addOption(Option.builder("prop").longOpt("property")
                                  .desc("force to read the property-file even other commandline parameter exists")
                                  .build());
        lvOptions.addOption(Option.builder("mat").longOpt("matricesTable").argName("MATRICES_TABLE").desc(
                "table name of the matrices Table e.g \"core.berlin_matrices\"").required(true).valueSeparator(' ')
                                  .hasArg().build());
        lvOptions.addOption(Option.builder("taz").longOpt("tazTable").argName("TAZ_TABLE_NAME").desc(
                "table name the TAZ-table e.g. \"core.berlin_taz\"").required(true).valueSeparator(' ').hasArg()
                                  .build());
        lvOptions.addOption(Option.builder("rec").longOpt("record").argName("NEW_RECORD_NAME").desc(
                "name of the new database record").required(true).valueSeparator(' ').hasArg().build());
        lvOptions.addOption(Option.builder("m").longOpt("modus").argName("MODUS").desc("modus (PT, MIV, BIKE, WALK)")
                                  .required(true).valueSeparator(' ').hasArg().build());
        lvOptions.addOption(Option.builder("t").longOpt("terrain").argName("TERRAIN").desc(
                "terrain (PLAIN, DOWNS, MOUNTAINOUS, CITY, STOPS)").required(true).valueSeparator(' ').hasArg()
                                  .build());
        lvOptions.addOption(Option.builder("top3").valueSeparator(' ').hasArg()
                                  .desc("generate top3 values in matrix diagonal").build());
        lvOptions.addOption(Option.builder("ind").longOpt("indirectWayFactor").valueSeparator(' ').hasArg()
                                  .desc("true/false - multiplies bee-line with detour factor\n(default:true)").build());
        lvOptions.addOption(Option.builder("z").longOpt("zValues").valueSeparator(' ').hasArg()
                                  .desc("include z-Values.\n(default:false)").build());


        /* *****************************************************************************
         *                             Matrix-Calculation                              *
         *******************************************************************************/

        lvOptions.addOption(Option.builder("twc").longOpt("timeWalkCalc").valueSeparator(' ').hasArg()
                                  .desc("true/false - calculte traveltime for WALK\n(default:false)").build());
        lvOptions.addOption(Option.builder("tbc").longOpt("timeBikeCalc").valueSeparator(' ').hasArg()
                                  .desc("true/false - calculte traveltime for BIKE\n(default:false)").build());
        lvOptions.addOption(Option.builder("tpc").longOpt("timePTCalc").valueSeparator(' ').hasArg()
                                  .desc("true/false - calculte traveltime for PT\n(default:false)").build());
        lvOptions.addOption(Option.builder("tmc").longOpt("timeMIVCalc").valueSeparator(' ').hasArg()
                                  .desc("true/false - calculte traveltime for MIV\n(default:false)").build());
        lvOptions.addOption(Option.builder("dwc").longOpt("distWalkCalc").valueSeparator(' ').hasArg()
                                  .desc("true/false - calculte distance for WALK\n(default:false)").build());
        lvOptions.addOption(Option.builder("dbc").longOpt("distBikeCalc").valueSeparator(' ').hasArg()
                                  .desc("true/false - calculte distance for BIKE\n(default:false)").build());
        lvOptions.addOption(Option.builder("dpc").longOpt("distPTCalc").valueSeparator(' ').hasArg()
                                  .desc("true/false - calculte distance for PT\n(default:false)").build());
        lvOptions.addOption(Option.builder("dmc").longOpt("distMIVCalc").valueSeparator(' ').hasArg()
                                  .desc("true/false - calculte distance for MIV\n(default:false)").build());

        /* *****************************************************************************
         *                             Velocity-Parameter                            *
         *******************************************************************************/

        lvOptions.addOption(Option.builder("sw").longOpt("speedWalk").valueSeparator(' ').hasArg()
                                  .desc("velocity for WALK\n(default:4,2)").build());
        lvOptions.addOption(Option.builder("sb").longOpt("speedBike").valueSeparator(' ').hasArg()
                                  .desc("velocity for BIKE\n(default:12)").build());
        lvOptions.addOption(Option.builder("sp").longOpt("speedPT").valueSeparator(' ').hasArg()
                                  .desc("velocity for PT\n(default:18)").build());
        lvOptions.addOption(Option.builder("sm").longOpt("speedMIV").valueSeparator(' ').hasArg()
                                  .desc("velocity for MIV\n(default:30)").build());

        /* ******************************************************************************
         *                             Travel-Time-Parameter                                  *
         *******************************************************************************/

        lvOptions.addOption(Option.builder("twa").longOpt("timeWalkApproach").valueSeparator(' ').hasArg()
                                  .desc("additional approach time for WALK\n(default:4,2)").build());
        lvOptions.addOption(Option.builder("twd").longOpt("timeWalkDeparture").valueSeparator(' ').hasArg()
                                  .desc("additional departure time for WALK\n(default:4,2)").build());
        lvOptions.addOption(Option.builder("tba").longOpt("timeBikeApproach").valueSeparator(' ').hasArg()
                                  .desc("additional approach time for BIKE\n(default:12)").build());
        lvOptions.addOption(Option.builder("tbd").longOpt("timeBikeDeparture").valueSeparator(' ').hasArg()
                                  .desc("additional departure time for WALK\n(default:4,2)").build());
        lvOptions.addOption(Option.builder("tpa").longOpt("timePTApproach").valueSeparator(' ').hasArg()
                                  .desc("additional approach time for PT\n(default:18)").build());
        lvOptions.addOption(Option.builder("tpb").longOpt("timePTDeparture").valueSeparator(' ').hasArg()
                                  .desc("additional departure time for PT\n(default:18)").build());
        lvOptions.addOption(Option.builder("tma").longOpt("timeMIVApproach").valueSeparator(' ').hasArg()
                                  .desc("additional approach time for MIV\n(default:30)").build());
        lvOptions.addOption(Option.builder("tmb").longOpt("timeMIVDeparture").valueSeparator(' ').hasArg()
                                  .desc("additional departure time for MIV\n(default:30)").build());


        /* *****************************************************************************
         *                                  VALIDATION                                 *
         *******************************************************************************/

        lvOptions.addOption(Option.builder("val").longOpt("validation").valueSeparator(' ').hasArg()
                                  .desc("generate any validation.\n(default:false)").build());

        /*     distance     */
        lvOptions.addOption(Option.builder("vwd").longOpt("validationWalkDist").valueSeparator(' ').hasArg()
                                  .desc("true/false - calculate traveltime for WALK\n(default:false)").build());
        lvOptions.addOption(Option.builder("rwd").longOpt("referenceWalkDist").argName("REF_RECORD__WALK_DIST")
                                  .valueSeparator(' ').hasArg()
                                  .desc("name of the reference data record for WALK and distance").build());
        lvOptions.addOption(Option.builder("vbd").longOpt("validationBikeDist").valueSeparator(' ').hasArg()
                                  .desc("true/false - calculte traveltime for BIKE\n(default:false)").build());
        lvOptions.addOption(Option.builder("rbd").longOpt("referenceBikeDist").argName("REF_RECORD_BIKE_DIST")
                                  .valueSeparator(' ').hasArg()
                                  .desc("name of the reference data record for BIKE and distance").build());
        lvOptions.addOption(Option.builder("vpd").longOpt("validationPTDist").valueSeparator(' ').hasArg()
                                  .desc("true/false - calculte traveltime for PT\n(default:false)").build());
        lvOptions.addOption(Option.builder("rpd").longOpt("referencePTDist").argName("REF_RECORD_PT_DIST")
                                  .valueSeparator(' ').hasArg()
                                  .desc("name of the reference data record for PT and distance").build());
        lvOptions.addOption(Option.builder("vmd").longOpt("validationMIVDist").valueSeparator(' ').hasArg()
                                  .desc("true/false - calculte traveltime for MIV\n(default:false)").build());
        lvOptions.addOption(Option.builder("rmd").longOpt("referenceMIVDist").argName("REF_RECORD_MIV_DIST")
                                  .valueSeparator(' ').hasArg()
                                  .desc("name of the reference data record for MIV and distance").build());

        /*     travel time     */
        lvOptions.addOption(Option.builder("vwt").longOpt("validationWalkTT").valueSeparator(' ').hasArg()
                                  .desc("true/false - calculte distance for WALK\n(default:false)").build());
        lvOptions.addOption(Option.builder("rwt").longOpt("referenceWalkTT").argName("REF_RECORD_WALK_TT")
                                  .valueSeparator(' ').hasArg()
                                  .desc("name of the reference data record for WALK and travel time").build());
        lvOptions.addOption(Option.builder("vbt").longOpt("validationBikeTT").valueSeparator(' ').hasArg()
                                  .desc("true/false - calculte distance for BIKE\n(default:false)").build());
        lvOptions.addOption(Option.builder("rbt").longOpt("referenceBikeTT").argName("REF_RECORD_Bike_TT")
                                  .valueSeparator(' ').hasArg()
                                  .desc("name of the reference data record for WALK and travel time").build());
        lvOptions.addOption(Option.builder("vpt").longOpt("validationPTTT").valueSeparator(' ').hasArg()
                                  .desc("true/false - calculte distance for PT\n(default:false)").build());
        lvOptions.addOption(Option.builder("rpt").longOpt("referencePTTT").argName("REF_RECORD_PT_TT").valueSeparator(
                ' ').hasArg().desc("name of the reference data record for PT and travel time").build());
        lvOptions.addOption(Option.builder("vmt").longOpt("validationMIVTT").valueSeparator(' ').hasArg()
                                  .desc("true/false - calculte distance for MIV\n(default:false)").build());
        lvOptions.addOption(Option.builder("rmt").longOpt("referenceMIVTT").argName("REF_RECORD_MIV_TT").valueSeparator(
                ' ').hasArg().desc("name of the reference data record for MIV and travel time").build());
        lvOptions.addOption(Option.builder("p").longOpt("path").argName("PATH").valueSeparator(' ').hasArg()
                                  .desc("path for saving validation data").build());

        try {
            lvCmd = lvParser.parse(lvOptions, args);

            if (lvCmd.hasOption('h')) {
                lvFormater.printHelp("CruisingSpeed", lvOptions);
                return null;
            }

        } catch (ParseException pvException) {
            lvFormater.printHelp("CruisingSpeed", lvOptions);
            System.out.println("Parse Error:" + pvException.getMessage());
        }
        return lvCmd;
    }

    public CruisingSpeedCalcData getCalcData() {
        return calcData;
    }

    public CruisingSpeedValData getValData() {
        return valData;
    }

    private void readProperties() throws IllegalArgumentException {
        System.out.println("readProperties");
    	/*#############################################
        #               basic properties              #
        #############################################*/
        getCalcData().setTazTable(properties.getProperty("tazTable").trim());
        getValData().setTazTable(properties.getProperty("tazTable").trim());
        getCalcData().setMatricesTable(properties.getProperty("matricesTable").trim());
        getValData().setMatricesTable(properties.getProperty("matricesTable").trim());
        getValData().setRecordName(properties.getProperty("record").trim());
        getCalcData().setTerrain(properties.getProperty("terrain").trim());
        getValData().setTerrain(properties.getProperty("terrain").trim());

        if (properties.containsKey("top3")) getCalcData().setTop3(
                Boolean.parseBoolean(properties.getProperty("top3").trim()));
        if (properties.containsKey("indirectWayFactor")) getCalcData().setIndirectWayFactor(
                Boolean.parseBoolean(properties.getProperty("indirectWayFactor").trim()));
        if (properties.containsKey("zValues")) getCalcData().setzValues(
                Boolean.parseBoolean((properties.getProperty("zValues").trim())));

      /*#############################################
        #       properties for VALIDATION           #
        #############################################*/
        if (properties.containsKey("validation")) validation = Boolean.parseBoolean(
                properties.getProperty("validation").trim());

        if (properties.containsKey("validationWalkDist")) val[0] = Boolean.parseBoolean(
                properties.getProperty("validationWalkDist").trim());
        if (properties.containsKey("referenceWalkDist"))
            getValData().getReferenceDist()[Modus.WALK.getValue()] = properties.getProperty("referenceWalkDist").trim();
        if (properties.containsKey("validationBikeDist")) val[1] = Boolean.parseBoolean(
                properties.getProperty("validationBikeDist").trim());
        if (properties.containsKey("referenceBikeDist"))
            getValData().getReferenceDist()[Modus.BIKE.getValue()] = properties.getProperty("referenceBikeDist").trim();
        if (properties.containsKey("validationPTDist")) val[2] = Boolean.parseBoolean(
                properties.getProperty("validationPTDist").trim());
        if (properties.containsKey("referencePTDist"))
            getValData().getReferenceDist()[Modus.PT.getValue()] = properties.getProperty("referencePTDist").trim();
        if (properties.containsKey("validationMIVDist")) val[3] = Boolean.parseBoolean(
                properties.getProperty("validationMIVDist").trim());
        if (properties.containsKey("referenceMIVDist"))
            getValData().getReferenceDist()[Modus.MIV.getValue()] = properties.getProperty("referenceMIVDist").trim();

        if (properties.containsKey("validationWalkTT")) val[4] = Boolean.parseBoolean(
                properties.getProperty("validationWalkTT").trim());
        if (properties.containsKey("referenceWalkTT"))
            getValData().getReferenceTT()[Modus.WALK.getValue()] = properties.getProperty("referenceWalkTT").trim();
        if (properties.containsKey("validationBikeTT")) val[5] = Boolean.parseBoolean(
                properties.getProperty("validationBikeTT").trim());
        if (properties.containsKey("referenceBikeTT"))
            getValData().getReferenceTT()[Modus.BIKE.getValue()] = properties.getProperty("referenceBikeTT").trim();
        if (properties.containsKey("validationPTTT")) val[6] = Boolean.parseBoolean(
                properties.getProperty("validationPTTT").trim());
        if (properties.containsKey("referencePTTT"))
            getValData().getReferenceTT()[Modus.PT.getValue()] = properties.getProperty("referencePTTT").trim();
        if (properties.containsKey("validationMIVTT")) val[7] = Boolean.parseBoolean(
                properties.getProperty("validationMIVTT").trim());
        if (properties.containsKey("referenceMIVTT"))
            getValData().getReferenceTT()[Modus.MIV.getValue()] = properties.getProperty("referenceMIVTT").trim();
        if (properties.containsKey("path")) getValData().setPath(properties.getProperty("path").trim());

      /*#############################################
        #             Traveltime-Parameter          #
        #############################################*/
        if (properties.containsKey("timeWalkApproach"))
            getCalcData().getTravelTimeParameter()[Modus.WALK.getValue()].setTimeApproach(
                    Integer.parseInt(properties.getProperty("timeWalkApproach").trim()));
        if (properties.containsKey("timeWalkDeparture"))
            getCalcData().getTravelTimeParameter()[Modus.WALK.getValue()].setTimeDeparture(
                    Integer.parseInt(properties.getProperty("timeWalkDeparture").trim()));
        if (properties.containsKey("timeBikeApproach"))
            getCalcData().getTravelTimeParameter()[Modus.BIKE.getValue()].setTimeApproach(
                    Integer.parseInt(properties.getProperty("timeBikeApproach").trim()));
        if (properties.containsKey("timeBikeDeparture"))
            getCalcData().getTravelTimeParameter()[Modus.BIKE.getValue()].setTimeDeparture(
                    Integer.parseInt(properties.getProperty("timeBikeDeparture").trim()));
        if (properties.containsKey("timePTApproach"))
            getCalcData().getTravelTimeParameter()[Modus.PT.getValue()].setTimeApproach(
                    Integer.parseInt(properties.getProperty("timePTApproach").trim()));
        if (properties.containsKey("timePTDeparture"))
            getCalcData().getTravelTimeParameter()[Modus.PT.getValue()].setTimeDeparture(
                    Integer.parseInt(properties.getProperty("timePTDeparture").trim()));
        if (properties.containsKey("timeMIVApproach"))
            getCalcData().getTravelTimeParameter()[Modus.MIV.getValue()].setTimeApproach(
                    Integer.parseInt(properties.getProperty("timeMIVApproach").trim()));
        if (properties.containsKey("timeMIVDeparture"))
            getCalcData().getTravelTimeParameter()[Modus.MIV.getValue()].setTimeDeparture(
                    Integer.parseInt(properties.getProperty("timeMIVDeparture").trim()));

        if (properties.containsKey("speedWalk")) getCalcData().getTravelTimeParameter()[Modus.WALK.getValue()].setSpeed(
                Double.parseDouble(properties.getProperty("speedWalk").trim()));
        if (properties.containsKey("speedBike")) getCalcData().getTravelTimeParameter()[Modus.BIKE.getValue()].setSpeed(
                Double.parseDouble(properties.getProperty("speedBike").trim()));
        if (properties.containsKey("speedPT")) getCalcData().getTravelTimeParameter()[Modus.PT.getValue()].setSpeed(
                Double.parseDouble(properties.getProperty("speedPT").trim()));
        if (properties.containsKey("speedMIV")) getCalcData().getTravelTimeParameter()[Modus.MIV.getValue()].setSpeed(
                Double.parseDouble(properties.getProperty("speedMIV").trim()));

      /*#############################################
        #             Matrix-Calculation            #
        #############################################*/
        if (properties.containsKey("distWalkCalc")) getCalcData().getCalculation()[0] = Boolean.parseBoolean(
                properties.getProperty("distWalkCalc").trim());
        if (properties.containsKey("distBikeCalc")) getCalcData().getCalculation()[1] = Boolean.parseBoolean(
                properties.getProperty("distBikeCalc").trim());
        if (properties.containsKey("distPTCalc")) getCalcData().getCalculation()[2] = Boolean.parseBoolean(
                properties.getProperty("distPTCalc").trim());
        if (properties.containsKey("distMIVCalc")) getCalcData().getCalculation()[3] = Boolean.parseBoolean(
                properties.getProperty("distMIVCalc").trim());
        if (properties.containsKey("timeWalkCalc")) getCalcData().getCalculation()[4] = Boolean.parseBoolean(
                properties.getProperty("timeWalkCalc").trim());
        if (properties.containsKey("timeBikeCalc")) getCalcData().getCalculation()[5] = Boolean.parseBoolean(
                properties.getProperty("timeBikeCalc").trim());
        if (properties.containsKey("timePTCalc")) getCalcData().getCalculation()[6] = Boolean.parseBoolean(
                properties.getProperty("timePTCalc").trim());
        if (properties.containsKey("timeMIVCalc")) getCalcData().getCalculation()[7] = Boolean.parseBoolean(
                properties.getProperty("timeMIVCalc").trim());

        //checkParameter(" in file: crusingspeed.properties.");
    }

    public void start(String[] args) {
        CruisingSpeedCalculate calculation;
        StringWriter sqlVektor;

        // command line arguments present, if there are no properties -> show help
        if (args != null && (args.length > 1 || properties == null)) {
            System.out.println("reading parameter from command line.");
            CommandLine lvCmd = cliParser(args);

            if (lvCmd == null) return;

            if (lvCmd.hasOption("newProperty")) {
                writeNewPropertyFile();
                System.exit(0);
            }

            if (!lvCmd.hasOption("property")) {   // write command line parameter into temporary property-file
                writePropertyFile(lvCmd);
                properties = new Properties();
                writePropertyFile(lvCmd, true);
            }

        }

        calcData = new CruisingSpeedCalcData();
        valData = new CruisingSpeedValData();

        readProperties();
        calculation = new CruisingSpeedCalculate(getCalcData());

      /*  try{System.in.read();}
        catch(IOException ex) {}*/

        for (int i = 0; i < 4; i++) {
            if (getCalcData().getCalculation()[i]) {
                double[][] dist = calculation.calcDistance(Modus.values()[i].toString());

                if (dist != null) {
                    if (calcData.isTop3()) TPS_Geometrics.calcTop3(dist);

                    sqlVektor = TPS_BasicConnectionClass.matrixToStringWriterSQL(dist, 0);
                    writeMatrix(sqlVektor, Modus.values()[i].getName(), DISTANCE);

                    if (getCalcData().getCalculation()[i + 4]) {
                        double[][] travelTime = calculation.getTravelTime(Modus.values()[i].toString());
                        if (travelTime != null) {
                            sqlVektor = TPS_BasicConnectionClass.matrixToStringWriterSQL(travelTime, 0);
                            writeMatrix(sqlVektor, Modus.values()[i].getName(), TRAVELTIME);
                        } else {
                            throw new RuntimeException("error in travel time calculation");
                        }
                    }
                } else {
                    throw new RuntimeException("error in distance calculation");
                }
            }


        }

        if (validation) {
            CruisingSpeedValidate cv = new CruisingSpeedValidate(this.valData, this.calcData);
            cv.start();
        }

        java.awt.EventQueue.invokeLater(() -> {});

        // Exit, OK.
        System.out.println("Exit(0)");
    }

    private void writeMatrix(StringWriter sqlVektor, String modus, int type) {
        //ResultSet rs = null;
        String name = generateRecordName(modus, type);
        System.out.println(name);
        String tableName = CruisingSpeed.properties.getProperty("matricesTable").trim();
        String query = "DELETE FROM " + tableName + " where matrix_name = '" + name + "'";
        dbCon.execute(query, this);
        System.out.println("delete from db");

        query = "INSERT INTO " + tableName + " (\"matrix_name\",\"matrix_values\")" + " VALUES('" + name + "',";
        query += sqlVektor.toString() + ")";

        dbCon.execute(query, this);

        System.out.println("wrote to db");
    }

    private void writeNewPropertyFile() {
      /*#############################################
        #           mandatory properties            #
        #############################################*/

        properties.setProperty("tazTable", "");
        properties.setProperty("matricesTable", "");
        properties.setProperty("record", "");
        properties.setProperty("modus", "");
        properties.setProperty("terrain", "");           
        
     /*#############################################
        #           optional properties            #
        #############################################*/
        properties.setProperty("top3", "true");
        properties.setProperty("indirectWayFactor", "true");
        properties.setProperty("zValues", "false");
        
      /*#############################################
        #       properties for VALIDATION           #
        #############################################*/
        properties.setProperty("validation", "false");
        properties.setProperty("validationWalkDist", "false");
        properties.setProperty("referenceWalkDist", "");
        properties.setProperty("validationBikeDist", "false");
        properties.setProperty("referenceBikeDist", "");
        properties.setProperty("validationPTDist", "false");
        properties.setProperty("referencePTDist", "");
        properties.setProperty("validationMIVDist", "false");
        properties.setProperty("referenceMIVDist", "");
        properties.setProperty("validationWalkTT", "false");
        properties.setProperty("referenceWalkTT", "");
        properties.setProperty("validationBikeTT", "false");
        properties.setProperty("referenceBikeTT", "");
        properties.setProperty("validationPTTT", "false");
        properties.setProperty("referencePTTT", "");
        properties.setProperty("validationMIVTT", "false");
        properties.setProperty("referenceMIVTT", "");
        properties.setProperty("path", System.getProperty("user.home"));
       
      /*#############################################
        #             Traveltime-Parameter          #
        #############################################*/
        properties.setProperty("timeWalkApproach", "0");
        properties.setProperty("timeWalkDeparture", "0");
        properties.setProperty("timeBikeApproach", "1");
        properties.setProperty("timeBikeDeparture", "1");
        properties.setProperty("timePTApproach", "5");
        properties.setProperty("timePTDeparture", "6");
        properties.setProperty("timeMIVApproach", "2");
        properties.setProperty("timeMIVDeparture", "2");

        properties.setProperty("speedWalk", "4.2");
        properties.setProperty("speedBike", "12.0");
        properties.setProperty("speedPT", "18.0");
        properties.setProperty("speedMIV", "30.0");

      /*#############################################
        #             Matrix-Calculation            #
        #############################################*/
        properties.setProperty("distWalkCalc", "false");
        properties.setProperty("distBikeCalc", "false");
        properties.setProperty("distPTCalc", "false");
        properties.setProperty("distMIVCalc", "false");
        properties.setProperty("timeWalkCalc", "false");
        properties.setProperty("timeBikeCalc", "false");
        properties.setProperty("timePTCalc", "false");
        properties.setProperty("timeMIVCalc", "false");

        try {
            File file = new File(CruisingSpeed.propertyFileName);
            file.createNewFile();

            try (FileOutputStream out = new FileOutputStream(file)) {
                properties.store(out, "");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writePropertyFile(CommandLine lvCmd, boolean temp) {
      /*#############################################
        #           mandatory properties            #
        #############################################*/

        properties.setProperty("tazTable", lvCmd.getOptionValue("tazTable"));
        properties.setProperty("matricesTable", lvCmd.getOptionValue("matricesTable"));
        properties.setProperty("record", lvCmd.getOptionValue("record"));
        properties.setProperty("modus", lvCmd.getOptionValue("modus"));
        properties.setProperty("terrain", lvCmd.getOptionValue("terrain"));

     /*#############################################
        #           optional properties            #
        #############################################*/
        if (lvCmd.hasOption("top3")) properties.setProperty("top3", lvCmd.getOptionValue("top3"));
        if (lvCmd.hasOption("indirectWayFactor")) properties.setProperty("indirectWayFactor",
                lvCmd.getOptionValue("indirectWayFactor"));
        if (lvCmd.hasOption("zValues")) properties.setProperty("zValues", lvCmd.getOptionValue("zValues"));

      /*#############################################
        #       properties for VALIDATION           #
        #############################################*/
        if (lvCmd.hasOption("validation")) properties.setProperty("validation", lvCmd.getOptionValue("validation"));
        if (lvCmd.hasOption("validationWalkDist")) properties.setProperty("validationWalkDist",
                lvCmd.getOptionValue("validationWalkDist"));
        if (lvCmd.hasOption("referenceWalkDist")) properties.setProperty("referenceWalkDist",
                lvCmd.getOptionValue("referenceWalkDist"));
        if (lvCmd.hasOption("validationBikeDist")) properties.setProperty("validationBikeDist",
                lvCmd.getOptionValue("validationBikeDist"));
        if (lvCmd.hasOption("referenceBikeDist")) properties.setProperty("referenceBikeDist",
                lvCmd.getOptionValue("referenceBikeDist"));
        if (lvCmd.hasOption("validationPTDist")) properties.setProperty("validationPTDist",
                lvCmd.getOptionValue("validationPTDist"));
        if (lvCmd.hasOption("referencePTDist")) properties.setProperty("referencePTDist",
                lvCmd.getOptionValue("referencePTDist"));
        if (lvCmd.hasOption("validationMIVDist")) properties.setProperty("validationMIVDist",
                lvCmd.getOptionValue("validationMIVDist"));
        if (lvCmd.hasOption("referenceMIVDist")) properties.setProperty("referenceMIVDist",
                lvCmd.getOptionValue("referenceMIVDist"));
        if (lvCmd.hasOption("validationWalkTT")) properties.setProperty("validationWalkTT",
                lvCmd.getOptionValue("validationWalkTT"));
        if (lvCmd.hasOption("referenceWalkTT")) properties.setProperty("referenceWalkTT",
                lvCmd.getOptionValue("referenceWalkTT"));
        if (lvCmd.hasOption("validationBikeTT")) properties.setProperty("validationBikeTT",
                lvCmd.getOptionValue("validationBikeTT"));
        if (lvCmd.hasOption("referenceBikeTT")) properties.setProperty("referenceBikeTT",
                lvCmd.getOptionValue("referenceBikeTT"));
        if (lvCmd.hasOption("validationPTTT")) properties.setProperty("validationPTTT",
                lvCmd.getOptionValue("validationPTTT"));
        if (lvCmd.hasOption("referencePTTT")) properties.setProperty("referencePTTT",
                lvCmd.getOptionValue("referencePTTT"));
        if (lvCmd.hasOption("validationMIVTT")) properties.setProperty("validationMIVTT",
                lvCmd.getOptionValue("validationMIVTT"));
        if (lvCmd.hasOption("referenceMIVTT")) properties.setProperty("referenceMIVTT",
                lvCmd.getOptionValue("referenceMIVTT"));
        if (lvCmd.hasOption("path")) properties.setProperty("path", lvCmd.getOptionValue("path"));

      /*#############################################
        #             Traveltime-Parameter          #
        #############################################*/
        if (lvCmd.hasOption("timeWalkApproach")) properties.setProperty("timeWalkApproach",
                lvCmd.getOptionValue("timeWalkApproach"));
        if (lvCmd.hasOption("timeWalkDeparture")) properties.setProperty("timeWalkDeparture",
                lvCmd.getOptionValue("timeWalkDeparture"));
        if (lvCmd.hasOption("timeBikeApproach")) properties.setProperty("timeBikeApproach",
                lvCmd.getOptionValue("timeBikeApproach"));
        if (lvCmd.hasOption("timeBikeDeparture")) properties.setProperty("timeBikeDeparture",
                lvCmd.getOptionValue("timeBikeDeparture"));
        if (lvCmd.hasOption("timePTApproach")) properties.setProperty("timePTApproach",
                lvCmd.getOptionValue("timePTApproach"));
        if (lvCmd.hasOption("timePTDeparture")) properties.setProperty("timePTDeparture",
                lvCmd.getOptionValue("timePTDeparture"));
        if (lvCmd.hasOption("timeMIVApproach")) properties.setProperty("timeMIVApproach",
                lvCmd.getOptionValue("timeMIVApproach"));
        if (lvCmd.hasOption("timeMIVDeparture")) properties.setProperty("timeMIVDeparture",
                lvCmd.getOptionValue("timeMIVDeparture"));

        if (lvCmd.hasOption("speedWalk")) properties.setProperty("speedWalk", lvCmd.getOptionValue("speedWalk"));
        if (lvCmd.hasOption("speedBike")) properties.setProperty("speedBike", lvCmd.getOptionValue("speedBike"));
        if (lvCmd.hasOption("speedPT")) properties.setProperty("speedPT", lvCmd.getOptionValue("speedPT"));
        if (lvCmd.hasOption("speedMIV")) properties.setProperty("speedMIV", lvCmd.getOptionValue("speedMIV"));

      /*#############################################
        #             Matrix-Calculation            #
        #############################################*/
        if (lvCmd.hasOption("timeWalkCalc")) properties.setProperty("timeWalkCalc",
                lvCmd.getOptionValue("timeWalkCalc"));
        if (lvCmd.hasOption("timeBikeCalc")) properties.setProperty("timeBikeCalc",
                lvCmd.getOptionValue("timeBikeCalc"));
        if (lvCmd.hasOption("timePTCalc")) properties.setProperty("timePTCalc", lvCmd.getOptionValue("timePTCalc"));
        if (lvCmd.hasOption("timeMIVCalc")) properties.setProperty("timeMIVCalc", lvCmd.getOptionValue("timeMIVCalc"));
        if (lvCmd.hasOption("distWalkCalc")) properties.setProperty("distWalkCalc",
                lvCmd.getOptionValue("distWalkCalc"));
        if (lvCmd.hasOption("distBikeCalc")) properties.setProperty("distBikeCalc",
                lvCmd.getOptionValue("distBikeCalc"));
        if (lvCmd.hasOption("distPTCalc")) properties.setProperty("distPTCalc", lvCmd.getOptionValue("distPTCalc"));
        if (lvCmd.hasOption("distMIVCalc")) properties.setProperty("distMIVCalc", lvCmd.getOptionValue("distMIVCalc"));

        try {
            File file;
            if (temp) {
                file = File.createTempFile("tmp", ".properties", new File(System.getProperty("java.io.tmpdir")));
                file.deleteOnExit();
            } else {
                file = new File(CruisingSpeed.propertyFileName);
                file.createNewFile();
            }

            try (FileOutputStream out = new FileOutputStream(file)) {
                properties.store(out, "");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writePropertyFile(CommandLine lvCmd) {
        writePropertyFile(lvCmd, false);
    }
}