package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.*;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * THis class provides all export functionality for the export to the "Mobilitätslabor"
 */
public class TPS_Sumo2MLExporter extends TPS_BasicConnectionClass {



    public static void main(String[] args) {

        if(args.length !=5)
            System.err.println("Wrong number of arguments! I need five: <sim key> <tsc base dir> <taz name in sumo template> <output dir> <write net (true/false)>);");
        String key = args[0];
        String tscBasePath = args[1];
        String tazFile = args[2];
        String output = args[3];
        String net = args[4];

        //cut tailing slashes
        if(tscBasePath.endsWith(File.separator))
            tscBasePath = tscBasePath.substring(0, tscBasePath.lastIndexOf(File.separatorChar));

        if(output.endsWith(File.separator))
            output = output.substring(0, output.lastIndexOf(File.separatorChar));

        //get all the needed strings and files as automatic as possible!
        TPS_Sumo2MLExporter worker = new TPS_Sumo2MLExporter();
        String coreSchema =  worker.dbCon.readSingleParameter(key,"DB_SCHEMA_CORE");
        String activityTableName = coreSchema+ worker.dbCon.readSingleParameter(key,"DB_TABLE_CONSTANT_ACTIVITY");
        String modeTableName = coreSchema +worker.dbCon.readSingleParameter(key,"DB_TABLE_CONSTANT_MODE");
        String tazTableName = coreSchema +worker.dbCon.readSingleParameter(key,"DB_TABLE_TAZ");
        String tripTable = worker.dbCon.readSingleParameter(key,"DB_TABLE_TRIPS")+"_"+key;
        String scenarioDestination= worker.dbCon.readSingleParameter(key,"SUMO_DESTINATION_FOLDER");
        String netName = tscBasePath+ File.separator+"scenario_workdir"+File.separator + scenarioDestination  + File.separator+"net.net.xml.gz";
        String tazName = tscBasePath+File.separator+"scenario_templates" + File.separator+worker.dbCon.readSingleParameter(key,"SUMO_TEMPLATE_FOLDER")  +File.separator+tazFile;
        int iteration = Integer.parseInt(worker.dbCon.readSingleParameter(key,"ITERATION"));
        String routeName = tscBasePath+File.separator+"scenario_workdir" +File.separator+scenarioDestination +File.separator+"iteration" +String.format("%03d",iteration)+File.separator+"oneshot"+File.separator+"vehroutes_oneshot_meso.rou.xml";
        String projectName = worker.dbCon.readSingleParameter(key,"PROJECT_NAME");
        String outputName = output+File.separator+projectName+"-net_attrib.csv";
        String edgeOutputName = output+File.separator+projectName+"-trip_attrib";

        worker.readActivityMapping(activityTableName);
        worker.readModeCodes(modeTableName);
        worker.processEdgeTypes(netName);
        worker.loadEdgeAttribute(netName);
        worker.processTAZList(tazName);
        worker.readTrips(tripTable);
        worker.loadRoutes(routeName);

        worker.writeEdgeAndTazAttributes(edgeOutputName);

        if(net.equalsIgnoreCase("true")) {
            worker.exportNetAttribute(netName, outputName);
            worker.exportNetGeoJSON(netName, outputName + ".json");
        }

    }

    final int TIME_BIN = 2*60*60; //two hours in seconds
    final double[] DIST_BINS ={500,1000,2500,5000,7500,10000,15000,20000,30000,9999999};
    Map<Integer,Integer> activityMap = new HashMap<>();
    Set<Integer> possibleActivities = new HashSet<>();
    Map<Integer,String> modeMap = new HashMap<>();

    public class EdgeAttribute{
        String id ="";
        double length = -1;
        int lastTime, lastActivity, lastDist; //buffer these values in case this becomes the last entry of a trip
        int[] timeCounts = new int[(24*60*60)/TIME_BIN];
        int[] distCount = new int[DIST_BINS.length];
        int[] activityCount = new int[possibleActivities.size()];

        int[] timeCountsStart = new int[(24*60*60)/TIME_BIN];
        int[] distCountStart = new int[DIST_BINS.length];
        int[] activityCountStart = new int[possibleActivities.size()];

        int[] timeCountsEnd = new int[(24*60*60)/TIME_BIN];
        int[] distCountEnd = new int[DIST_BINS.length];
        int[] activityCountEnd = new int[possibleActivities.size()];


        public void addTrip(int time, int distIndex, int purposeIndex){
            int tIndex = ((time + 24*60*60)%(24*60*60))/TIME_BIN;
            timeCounts[tIndex]++;
            distCount[distIndex]++;
            activityCount[purposeIndex]++;
            //buffer these values in case this becomes the last entry of a trip
            lastTime = time;
            lastActivity = purposeIndex;
            lastDist = distIndex;
        }

        public void addTripToStart(int time, int distIndex, int purposeIndex){
            int tIndex = ((time + 24*60*60)%(24*60*60))/TIME_BIN;
            timeCountsStart[tIndex]++;
            distCountStart[distIndex]++;
            activityCountStart[purposeIndex]++;
        }

        public void addLastEntryToEnd(){
            int tIndex = ((lastTime + 24*60*60)%(24*60*60))/TIME_BIN;
            timeCountsEnd[tIndex]++;
            distCountEnd[lastDist]++;
            activityCountEnd[lastActivity]++;
        }


        public String getCSVHeader(){
            StringBuilder s = new StringBuilder("id");
            for (int j =0; j<timeCounts.length; j++) {
                s.append("\t" + "t").append(j);
            }

            for (int j =0; j<distCount.length; j++) {
                s.append("\t" + "d").append(j);
            }

            for (int j =0; j<activityCount.length; j++) {
                s.append("\t" + "a").append(j);
            }

            for (int j =0; j<timeCountsStart.length; j++) {
                s.append("\t" + "start_t").append(j);
            }

            for (int j =0; j<distCountStart.length; j++) {
                s.append("\t" + "start_d").append(j);
            }

            for (int j =0; j<activityCountStart.length; j++) {
                s.append("\t" + "start_a").append(j);
            }

            for (int j =0; j<timeCountsEnd.length; j++) {
                s.append("\t" + "end_t").append(j);
            }

            for (int j =0; j<distCountEnd.length; j++) {
                s.append("\t" + "end_d").append(j);
            }

            for (int j =0; j<activityCountEnd.length; j++) {
                s.append("\t" + "end_a").append(j);
            }

            s.append("\n");
            return s.toString();
        }


        @Override
        public String toString() {
            StringBuilder s = new StringBuilder(this.id);
            for (Integer i : timeCounts) {
                s.append("\t").append(i.toString());
            }

            for (Integer i : distCount) {
                s.append("\t").append(i.toString());
            }

            for (Integer i : activityCount) {
                s.append("\t").append(i.toString());
            }

            for (Integer i : timeCountsStart) {
                s.append("\t").append(i.toString());
            }

            for (Integer i : distCountStart) {
                s.append("\t").append(i.toString());
            }

            for (Integer i : activityCountStart) {
                s.append("\t").append(i.toString());
            }

            for (Integer i : timeCountsEnd) {
                s.append("\t").append(i.toString());
            }

            for (Integer i : distCountEnd) {
                s.append("\t").append(i.toString());
            }

            for (Integer i : activityCountEnd) {
                s.append("\t").append(i.toString());
            }

            s.append("\n");
            return s.toString();
        }
    }


    public class TAZAttribute{
        String id ="";

        int[][] distCountStart = new int[(24*60*60)/TIME_BIN][DIST_BINS.length];
        int[][] activityCountStart = new int[(24*60*60)/TIME_BIN][possibleActivities.size()];

        int[][] distCountEnd = new int[(24*60*60)/TIME_BIN][DIST_BINS.length];
        int[][] activityCountEnd = new int[(24*60*60)/TIME_BIN][possibleActivities.size()];

        double[] performanceStart = new double[(24*60*60)/TIME_BIN];
        double[] performanceEnd = new double[(24*60*60)/TIME_BIN];
        double[] performanceNet = new double[(24*60*60)/TIME_BIN];
        int[][] personActivities= new int[(24*60*60)/TIME_BIN][possibleActivities.size()];
        int[][] personDistances= new int[(24*60*60)/TIME_BIN][DIST_BINS.length];


        public void addTripToStart(int time, int distIndex, int purposeIndex, double distance){
            int tIndex = ((time + 24*60*60)%(24*60*60))/TIME_BIN;

            distCountStart[tIndex][distIndex]++;
            activityCountStart[tIndex][purposeIndex]++;
            performanceStart[tIndex]+=distance;
        }

        public void addTripToTrafficCount(int time, int distIndex, int purposeIndex){
            int tIndex = ((time + 24*60*60)%(24*60*60))/TIME_BIN;
            personActivities[tIndex][purposeIndex]++;
            personDistances[tIndex][distIndex]++;
        }

        public void addTripToEnd(int time, int distIndex, int purposeIndex, double distance){
            int tIndex = ((time + 24*60*60)%(24*60*60))/TIME_BIN;
            distCountEnd[tIndex][distIndex]++;
            activityCountEnd[tIndex][purposeIndex]++;
            performanceEnd[tIndex]+=distance;
        }

        public void addNetPerformance(int time, double distance){
            int tIndex = ((time + 24*60*60)%(24*60*60))/TIME_BIN;
            performanceNet[tIndex]+=distance;
        }

        public String getCSVHeader(){
            StringBuilder s = new StringBuilder("id");

            for (int i =0; i<performanceStart.length; i++) {
                s.append("\ttp_start_t").append(i);
            }

            for (int i =0; i<performanceEnd.length; i++) {
                s.append("\ttp_end_t").append(i);
            }

            for (int i =0; i<performanceNet.length; i++) {
                s.append("\ttp_local_t").append(i);
            }

            for (int j =0; j<personActivities.length; j++) {
                for (int i =0; i<personActivities[j].length; i++) {
                    s.append("\ttraffic_activities_t").append(j).append("_a").append(i);
                }
            }


              for (int j =0; j<personDistances.length; j++) {
                  for (int i =0; i<personDistances[j].length; i++) {
                        s.append("\ttraffic_distances_t").append(j).append("_d").append(i);
                  }
            }

            for (int j =0; j<distCountStart.length; j++) {
                for (int i =0; i<distCountStart[j].length; i++) {
                    s.append("\tstart_t").append(j).append("_d").append(i);
                }
            }
            for (int j =0; j<activityCountStart.length; j++) {
                for (int i =0; i<activityCountStart[j].length; i++) {
                    s.append("\tstart_t").append(j).append("_a").append(i);
                }
            }

            for (int j =0; j<distCountEnd.length; j++) {
                for (int i =0; i<distCountEnd[j].length; i++) {
                    s.append("\tend_t").append(j).append("_d").append(i);
                }
            }

            for (int j =0; j<activityCountEnd.length; j++) {
                for (int i =0; i<activityCountEnd[j].length; i++) {
                    s.append("\tend_t").append(j).append("_a").append(i);
                }
            }
            s.append("\n");

            return s.toString();
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder(this.id);

            for (Double i : performanceStart) {
                s.append("\t+").append((Integer.valueOf(i.intValue())).toString()); // this way the int gets nicely formated
            }

            for (Double i: performanceEnd) {
                s.append("\t+").append(Integer.valueOf((i.intValue())).toString());
            }

            for (Double i: performanceNet) {
                s.append("\t+").append(Integer.valueOf((i.intValue())).toString());
            }

            for (int[] personActivity : personActivities) {
                for (Integer i : personActivity) {
                    s.append("\t+").append(i.toString());
                }
            }

            for (int[] personDistance : personDistances) {
                for (Integer i : personDistance) {
                    s.append("\t+").append(i.toString());
                }
            }

            for (int[] ints : distCountStart) {
                for (Integer i : ints) {
                    s.append("\t").append(i.toString());
                }
            }
            for (int[] ints : activityCountStart) {
                for (Integer i : ints) {
                    s.append("\t").append(i.toString());
                }
            }

            for (int[] ints : distCountEnd) {
                for (Integer i : ints) {
                    s.append("\t").append(i.toString());
                }
            }

            for (int[] ints : activityCountEnd) {
                for (Integer i : ints) {
                    s.append("\t").append(i.toString());
                }
            }
            s.append("\n");
            return s.toString();
        }
    }



    class TripAttribute{
        int activity;
        int mode;
    }

    class PTRoute{
        String[] stops;
        int[] times;
        String id;
        String name;
    }

    Map<String, TripAttribute> trips = new HashMap<>();
    Map<String, PTRoute> ptVehicle = new HashMap<>();


    public void readTrips(String tripMap){
        String query = "";
        try {
            int activity, mode, p_id, hh_id, startTimeMin;
            System.out.println("Loading TAPAS trips");
            String id;
            //load the mapping
            query = "SELECT p_id,hh_id,start_time_min, activity, mode "+
                    "FROM "+tripMap;
            ResultSet rs = this.dbCon.executeQuery(query, this);
            while (rs.next()) {
                p_id = rs.getInt("p_id");
                hh_id = rs.getInt("hh_id");
                startTimeMin = rs.getInt("start_time_min");
                activity = rs.getInt("activity");
                mode = rs.getInt("mode");
                id = p_id+"_"+ hh_id+"_"+ startTimeMin;
                TripAttribute a = new  TripAttribute();
                a.activity=activity;
                a.mode = mode;
                this.trips.put(id,a);
            }
            rs.close();
            System.out.println("found " + this.trips.size() + " trip entries");
        } catch (SQLException e) {
            System.err.println("Error in sqlstatement: " + query);
            e.printStackTrace();
        }
    }

    public void readActivityMapping(String activity_codes){
        String query = "";
        try {
            int zbeCode, tapasCode;
            //load the mapping
            System.out.println("Loading activity attributes");
            query = "SELECT code_zbe, code_tapas "+
                    "FROM "+activity_codes;
            ResultSet rs = this.dbCon.executeQuery(query, this);
            while (rs.next()) {
                zbeCode = rs.getInt("code_zbe");
                tapasCode = rs.getInt("code_tapas");
                if(tapasCode >=0) {
                    this.activityMap.put(zbeCode, tapasCode);
                    possibleActivities.add(tapasCode);
                }
            }
            rs.close();
            System.out.println("found " + this.activityMap.size() + " activity entries");
        } catch (SQLException e) {
            System.err.println("Error in sqlstatement: " + query);
            e.printStackTrace();
        }
    }

    public void readModeCodes(String mode_codes){
        String query = "";
        try {
            String name;
            int code;
            System.out.println("Loading modes");
            //load the mapping
            query = "SELECT name, code_mct "+
                    "FROM "+mode_codes;
            ResultSet rs = this.dbCon.executeQuery(query, this);
            while (rs.next()) {
                name = rs.getString("name");
                code = rs.getInt("code_mct");
                this.modeMap.put(code,name);
            }
            rs.close();
            System.out.println("found " + this.modeMap.size() + " mode entries");
        } catch (SQLException e) {
            System.err.println("Error in sqlstatement: " + query);
            e.printStackTrace();
        }
    }

    public void loadEdgeAttribute(String net){


        try {
            System.out.println("Loading net attributes");
            this.parseXMLSaxDocument(net,new NetLengthHandlerSax());

            System.out.println("Finished processing");

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }



    Map<String,String[]> edgeTypesMap = new HashMap<>();

    public void exportNetAttribute(String net, String output){


        try {


            // parse net XML file
            System.out.println("Exporting net attributes to "+output);

            //prepare output
            FileWriter fw = new FileWriter(output);
            fw.write("id\tmotorway\ttrunk\tprimary\tsecondary\ttertiary\tminor\tpath\tcycleway\trailway\ttaz\n");

            System.out.println("Loading net attributes");
            this.parseXMLSaxDocument(net,new NetEdgeTypeHandlerSax());

            for (Map.Entry<String,String[]> e: this.edgeTypesMap.entrySet()) {

                    String taz = edge2TAZ.get(e.getKey());
                    if( taz ==null) taz = "-1";
                    try {
                        fw.write(writeOneCSVElement(e.getKey(), e.getValue(), taz));
                    }catch(NullPointerException ex){
                        ex.printStackTrace();
                        //stupid null
                    }

            }

            fw.flush();
            fw.close();


            System.out.println("Finished processing");

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    public class NetEdgeTypeHandlerSax extends DefaultHandler {

        private StringBuilder currentValue = new StringBuilder();

        String currentID;
        String types[];
        int attributesFound=0;

        @Override
        public void startDocument() {
            System.out.println("Start Document");
        }

        @Override
        public void endDocument() {
            System.out.println("End Document");
            System.out.println("Processed attributes:"+attributesFound);

        }

        @Override
        public void startElement(
                String uri,
                String localName,
                String qName,
                Attributes attributes) {

            // reset the tag value
            currentValue = new StringBuilder();

            if (qName.equalsIgnoreCase("edge")) {
                // get tag's attribute by name
                currentID = attributes.getValue("id");
                // get type
                String type = attributes.getValue("type");
                types = type.split("[|]");
                //tokenize types
                if(type.length()>0 && types.length>0) {
                    for (int i = 0; i < types.length; i++) {
                        types[i] = edgeTypes.get(types[i]);
                        if (types[i] == null) {
                            types[i] = "minor"; //unclassified
                        }
                    }
                }
                else{
                    types = new String[1];
                    types[0] = "minor"; //unclassified
                }
            }
        }

        @Override
        public void endElement(String uri,
                               String localName,
                               String qName) {

            if (qName.equalsIgnoreCase("edge")) {

                edgeTypesMap.put(currentID,types);
                if(attributesFound%1000==0) {
                    System.out.println("processing edge:" + attributesFound);
                }
            }
        }

        // http://www.saxproject.org/apidoc/org/xml/sax/ContentHandler.html#characters%28char%5B%5D,%20int,%20int%29
        // SAX parsers may return all contiguous character data in a single chunk,
        // or they may split it into several chunks
        @Override
        public void characters(char ch[], int start, int length) {

            // The characters() method can be called multiple times for a single text node.
            // Some values may missing if assign to a new string

            // avoid doing this
            // value = new String(ch, start, length);

            // better append it, works for single or multiple calls
            currentValue.append(ch, start, length);

        }

    }

    public class NetLengthHandlerSax extends DefaultHandler {

        private StringBuilder currentValue = new StringBuilder();

        String currentID;
        double totalLengthOfLanes;
        double totalLengthOfNet=0;
        int numOfLanes,attributesFound=0;

        @Override
        public void startDocument() {
            System.out.println("Start Document");
        }

        @Override
        public void endDocument() {
            System.out.println("End Document");

            System.out.println("Processed attributes:"+attributesFound);

            System.out.println("Total net length in km:"+ ((int)(totalLengthOfNet/1000)));
        }

        @Override
        public void startElement(
                String uri,
                String localName,
                String qName,
                Attributes attributes) {

            // reset the tag value
            currentValue = new StringBuilder();

            if (qName.equalsIgnoreCase("edge")) {
                // get tag's attribute by name
                currentID = attributes.getValue("id");
                totalLengthOfLanes =0;
                numOfLanes=0;
            }
            if (qName.equalsIgnoreCase("lane")) {
                 String len = attributes.getValue("length");
                 if(len!=null && len.length()>0) {
                     totalLengthOfLanes += Double.parseDouble(len);
                     numOfLanes++;
                 }
            }
        }

        @Override
        public void endElement(String uri,
                               String localName,
                               String qName) {

            if (qName.equalsIgnoreCase("edge")) {
                if (numOfLanes > 0) {
                    totalLengthOfLanes /= numOfLanes; // calc average
                    totalLengthOfNet += totalLengthOfLanes;
                }
                else{
                    totalLengthOfLanes =0; //unknown length
                }
                attributesFound++;
                EdgeAttribute[] edge = edgelist.get(currentID);

                if (edge == null) {
                    edge = new EdgeAttribute[modeMap.size()];
                    for (int k = 0; k < edge.length; k++) {
                        edge[k] = new EdgeAttribute();
                        edge[k].id = currentID;
                    }
                    edgelist.put(currentID, edge);
                }
                for (EdgeAttribute edgeAttribute : edge) {
                    edgeAttribute.length = totalLengthOfLanes;
                }
                if(attributesFound%1000==0) {
                    System.out.println("processing edge:" + attributesFound);
                }
            }
        }

        // http://www.saxproject.org/apidoc/org/xml/sax/ContentHandler.html#characters%28char%5B%5D,%20int,%20int%29
        // SAX parsers may return all contiguous character data in a single chunk,
        // or they may split it into several chunks
        @Override
        public void characters(char ch[], int start, int length) {

            // The characters() method can be called multiple times for a single text node.
            // Some values may missing if assign to a new string

            // avoid doing this
            // value = new String(ch, start, length);

            // better append it, works for single or multiple calls
            currentValue.append(ch, start, length);

        }

    }

    public void parseXMLSaxDocument(String fileName, DefaultHandler handler) throws ParserConfigurationException , SAXException , IOException {

        FileInputStream fs = new FileInputStream(fileName);
        SAXParserFactory factory = SAXParserFactory.newInstance();
        // XXE attack, see https://rules.sonarsource.com/java/RSPEC-2755
        SAXParser saxParser = factory.newSAXParser();
        if(fileName.endsWith(".gz")||fileName.endsWith(".zip")){

            saxParser.parse(new GZIPInputStream(fs), handler);

        }
        else {
            saxParser.parse(fs, handler);
        }
        fs.close();

    }

    public Document openXMLDocument(String fileName) throws ParserConfigurationException , SAXException , IOException {

        Document doc;
        FileInputStream fs = new FileInputStream(fileName);
        if(fileName.endsWith(".gz")||fileName.endsWith(".zip")){
            doc = openXMLDocument(new GZIPInputStream(fs));
        }
        else {
            doc = openXMLDocument(fs);
        }
        fs.close();

        if (doc==null) {
            System.err.println("Error parsing file: "+fileName);
            System.exit(-1);
        }
        return doc;
    }
    public Document openXMLDocument(InputStream stream) throws ParserConfigurationException , SAXException , IOException {
        // Instantiate the Factory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        dbf.setNamespaceAware(false);
        dbf.setValidating(false);

        // optional, but recommended
        // process XML securely, avoid attacks like XML External Entities (XXE)
        //dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder db = dbf.newDocumentBuilder();

        // parse taz XML file
        Document doc = db.parse(stream);
        // optional, but recommended
        // http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
        doc.getDocumentElement().normalize();
        return doc;
    }

    Map<String, EdgeAttribute[]> edgelist = new HashMap<>();
    Map<String, TAZAttribute[]> tazlist = new HashMap<>();



    public class TAZHandlerSax extends DefaultHandler {

        private StringBuilder currentValue = new StringBuilder();

        int tazFound=0;

        @Override
        public void startDocument() {
            System.out.println("Start Document");
        }

        @Override
        public void endDocument() {
            System.out.println("End Document");

            System.out.println("Processed taz:"+tazFound);

        }

        @Override
        public void startElement(
                String uri,
                String localName,
                String qName,
                Attributes attributes) {

            // reset the tag value
            currentValue = new StringBuilder();

            if (qName.equalsIgnoreCase("taz")) {
                // get tag's attribute by name
                String id = attributes.getValue("id");
                // get type
                String edges = attributes.getValue("edges");
                //tokenize edges
                String[] edge = edges.split(" ");
                for(String t: edge) {
                    edge2TAZ.put(t, id);
                }
                tazFound++;

                //Add the taz to the attribute map
                TAZAttribute[] att = new TAZAttribute[TPS_Sumo2MLExporter.this.modeMap.size()];
                for (int k = 0; k < att.length; k++) {
                    att[k] = new TAZAttribute();
                    att[k].id = id;
                }
                TPS_Sumo2MLExporter.this.tazlist.put(id, att);
            }
        }

        @Override
        public void endElement(String uri,
                               String localName,
                               String qName) {

            if (qName.equalsIgnoreCase("edge")) {
               if(tazFound%1000==0) {
                    System.out.println("processing taz:" + tazFound);
                }
            }
        }

        // http://www.saxproject.org/apidoc/org/xml/sax/ContentHandler.html#characters%28char%5B%5D,%20int,%20int%29
        // SAX parsers may return all contiguous character data in a single chunk,
        // or they may split it into several chunks
        @Override
        public void characters(char ch[], int start, int length) {

            // The characters() method can be called multiple times for a single text node.
            // Some values may missing if assign to a new string

            // avoid doing this
            // value = new String(ch, start, length);

            // better append it, works for single or multiple calls
            currentValue.append(ch, start, length);

        }

    }


    Map<String, String> edge2TAZ = new HashMap<>();

    public void processTAZList(String filename)  {
        try{
            System.out.println("Processing tazes");
            this.parseXMLSaxDocument(filename,new TAZHandlerSax());

            System.out.println("Finished processing");

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    Map<String,String> edgeTypes = new HashMap<>();

    public class NetTypeHandlerSax extends DefaultHandler {

        private StringBuilder currentValue = new StringBuilder();

        int attributesFound=0;

        @Override
        public void startDocument() {
            System.out.println("Start Document");
        }

        @Override
        public void endDocument() {
            System.out.println("End Document");
            System.out.println("Processed attributes:"+attributesFound);

        }

        @Override
        public void startElement(
                String uri,
                String localName,
                String qName,
                Attributes attributes) {

            // reset the tag value
            currentValue = new StringBuilder();

            if (qName.equalsIgnoreCase("type")) {
                attributesFound++;
                String id = attributes.getValue("id");
                String[] types = id.split("[|]");
                // get type
                for(String t: types){
                    if(t.startsWith("cycleway")){
                        edgeTypes.put(t,"cycleway");
                        continue;
                    }
                    if(t.startsWith("railway")){
                        edgeTypes.put(t,"railway");
                        continue;
                    }
                    if( t.startsWith("highway.raceway") |
                            t.startsWith("highway.motorway") |
                            t.startsWith("highway.motorway_link")
                    ){
                        edgeTypes.put(t,"motorway");
                        continue;
                    }
                    if( t.startsWith("highway.trunk_link") |
                            t.startsWith("highway.trunk")
                    ){
                        edgeTypes.put(t,"trunk");
                        continue;
                    }

                    if( t.startsWith("highway.primary") |
                            t.startsWith("highway.primary_link")
                    ){
                        edgeTypes.put(t,"primary");
                        continue;
                    }
                    if( t.startsWith("highway.secondary") |
                            t.startsWith("highway.secondary_link")
                    ){
                        edgeTypes.put(t,"secondary");
                        continue;
                    }
                    if( t.startsWith("highway.tertiary") |
                            t.startsWith("highway.tertiary_link")
                    ){
                        edgeTypes.put(t,"tertiary");
                        continue;
                    }
                    if( t.startsWith("highway.unclassified") |
                            t.startsWith("highway.residential")|
                            t.startsWith("highway.living_street")|
                            t.startsWith("highway.service")
                    ){
                        edgeTypes.put(t,"minor");
                        continue;
                    }
                    edgeTypes.put(t,"path");
                }
            }
        }

        @Override
        public void endElement(String uri,
                               String localName,
                               String qName) {

            if (qName.equalsIgnoreCase("type")) {

                if(attributesFound%1000==0) {
                    System.out.println("processing type:" + attributesFound);
                }
            }
        }

        // http://www.saxproject.org/apidoc/org/xml/sax/ContentHandler.html#characters%28char%5B%5D,%20int,%20int%29
        // SAX parsers may return all contiguous character data in a single chunk,
        // or they may split it into several chunks
        @Override
        public void characters(char ch[], int start, int length) {

            // The characters() method can be called multiple times for a single text node.
            // Some values may missing if assign to a new string

            // avoid doing this
            // value = new String(ch, start, length);

            // better append it, works for single or multiple calls
            currentValue.append(ch, start, length);

        }

    }


    public void processEdgeTypes(String net) {

        try {


            // parse net XML file
            System.out.println("Processing edge types");

            this.parseXMLSaxDocument(net,new NetTypeHandlerSax());
            System.out.println("Finished processing");

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

    }

    public void loadRoutes(String routes){
        try {


            System.out.println("Processing pt vehicles");
            //loadSumoPublicTransport(doc);

            // parse net XML file

            this.parseXMLSaxDocument(routes,new PTLineTripLengthHandlerSax());
            System.out.println("Finished processing pt");

            System.out.println("Finished processing pt vehicles");

            System.out.println("Processing vehicles/persons");
            this.parseXMLSaxDocument(routes,new VehiclePersonHandlerSax());

            System.out.println("Finished processing vehicles person tours");
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isPT(String type){
        type = type.toLowerCase();
        if("bus".equals(type))
            return true;
        if("subway".equals(type))
            return true;
        if("train".equals(type))
            return true;
        if("tram".equals(type))
            return true;
        if("light_rail".equals(type))
            return true;

        return false;
    }

    Map<String,Double> tripLengthMap = new HashMap<>();

    public class PTLineTripLengthHandlerSax extends DefaultHandler {

        private StringBuilder currentValue = new StringBuilder();

        int linesFound=0;
        int tripsFound =0;
        PTRoute pt = null;
        String line = "", heading = "", id = "";
        double length =0;

        @Override
        public void startDocument() {
            System.out.println("Start Document");
        }

        @Override
        public void endDocument() {
            System.out.println("Processed lines:"+linesFound);
            System.out.println("Processed trips lengths:"+tripsFound);
            System.out.println("End Document");

        }

        @Override
        public void startElement(
                String uri,
                String localName,
                String qName,
                Attributes attributes) {

            // reset the tag value
            currentValue = new StringBuilder();

            if (qName.equalsIgnoreCase("vehicle")) {
                // get trip attribute
                id = attributes.getValue("id");
                String type = attributes.getValue("type");
                if(isPT(type)) {

                    pt = new PTRoute();
                    pt.id= id;
                }
                else{
                    id = id.substring(0, id.lastIndexOf("_") ); //cut off the clone number
                }
                String routeLength = attributes.getValue("routeLength");
                if (routeLength != null && routeLength.length() > 0) {
                    length = Double.parseDouble(routeLength);
                }
            }

            if (qName.equalsIgnoreCase("person")) {
                // get trip attribute
                id = attributes.getValue("id");
                id = id.substring(0, id.lastIndexOf("_") ); //cut off the clone number
                length = 0;
            }

            if (qName.equalsIgnoreCase("ride")||qName.equalsIgnoreCase("walk")

                ) { //sum up thip length
                String routeLength = attributes.getValue("routeLength");
                if (routeLength != null && routeLength.length() > 0 && !routeLength.equals("-1.00")) {
                    double legLength = Double.parseDouble(routeLength);
                    length += Math.max(0, legLength); // teleports have a length of -1 m!
                }
            }

            if (qName.equalsIgnoreCase("route") && pt !=null) {
                String edges = attributes.getValue("edges");
                pt.stops = edges.split(" ");
                // get exit times
                String exitTimes = attributes.getValue("exitTimes");
                String[] timeArray = exitTimes.split(" ");
                if (pt.stops.length != timeArray.length) {
                    System.out.println("Wrong format! " + pt.id + " " + edges + " " + exitTimes);
                    System.exit(-1);
                }
                pt.times = new int[timeArray.length];
                for (int j = 0; j < timeArray.length; j++) {
                    pt.times[j] =(int) (Double.parseDouble(timeArray[j]) + 0.5);
                }
            }
            if (qName.equalsIgnoreCase("param") && pt !=null) {
                if(pt.times!=null && pt.stops!=null){
                    //now extract the route of this vehicle
                    String key = attributes.getValue("key");
                    if (key.equals("gtfs.route_name")) {
                        line = attributes.getValue("value");
                    }
                    if (key.equals("gtfs.trip_headsign")) {
                        heading = attributes.getValue("value");
                    }
                }
            }
        }

        @Override
        public void endElement(String uri,
                               String localName,
                               String qName) {

            if (qName.equalsIgnoreCase("vehicle")) {

                if (pt != null) {
                    linesFound++;
                    if(linesFound%1000==0) {
                        System.out.println("processing lines:" + linesFound);
                    }
                    if (line.compareTo("") == 0) {
                        line = "Unknown line";
                    }
                    if (heading.compareTo("") == 0) {
                        heading = " Unknown heading";
                    }
                    pt.name = line + " - " + heading;
                    ptVehicle.put(pt.id, pt);
                    //delete infos for next element
                    pt = null;
                    line = "";
                    heading = "";
                }
                tripsFound++;
                tripLengthMap.put(id,length);
                id = "";
                length =0;
                if(tripsFound%1000==0) {
                    System.out.println("processing trip lengths:" + tripsFound);
                }
            }
            if (qName.equalsIgnoreCase("person")) {
                tripsFound++;
                tripLengthMap.put(id,length);
                id = "";
                length =0;
                if(tripsFound%1000==0) {
                    System.out.println("processing trip lengths:" + tripsFound);
                }
            }

        }

        @Override
        public void characters(char ch[], int start, int length) {
            currentValue.append(ch, start, length);
        }
    }


    public class VehiclePersonHandlerSax extends DefaultHandler {

        private StringBuilder currentValue = new StringBuilder();

        int tripsFound =0,startTime =Integer.MIN_VALUE , endTime = Integer.MIN_VALUE;
        String line = "", heading = "", id = "", startTAZ ="", endTAZ="";
        double length =0;
        int activity = 0, mode =0;
        TripAttribute commuter = new TripAttribute();
        TripAttribute ptVehicle = new TripAttribute();
        TripAttribute attr;
        boolean firstLeg =true;
        Set<String> visitedTazes;
        EdgeAttribute[] last = null;

        @Override
        public void startDocument() {
            System.out.println("Start Document");
            commuter.mode=2; // CAR
            commuter.activity = 211; //work, full time
            ptVehicle.mode=5; // PT
            ptVehicle.activity = 799; //any activity
        }

        @Override
        public void endDocument() {
            System.out.println("Processed trips:"+tripsFound);
            System.out.println("End Document");

        }

        @Override
        public void startElement(
                String uri,
                String localName,
                String qName,
                Attributes attributes) {

            // reset the tag value
            currentValue = new StringBuilder();

            if (qName.equalsIgnoreCase("vehicle") || qName.equalsIgnoreCase("person")
            ) {
                // get trip attribute
                id = attributes.getValue("id");
                String type = attributes.getValue("type");
                if (type ==null || !isPT(type)) {
                    id = id.substring(0, id.lastIndexOf("_")); //cut off the clone number
                    if (id.startsWith("-")) {
                        attr = commuter;
                    } else {
                        attr = trips.get(id);
                    }
                } else {
                    attr = ptVehicle;
                }
                length = tripLengthMap.get(id);
                if (activityMap.containsKey(attr.activity)) {
                    activity = activityMap.get(attr.activity);
                }
                mode = attr.mode;
                firstLeg = true;
                visitedTazes = new HashSet<>();
                startTAZ = "";
                endTAZ = "";
                startTime = Integer.MIN_VALUE;
                endTime = Integer.MIN_VALUE;
                last = null;
            }

            if (qName.equalsIgnoreCase("param")) {
                String tazName = attributes.getValue("value"); //will return an empty sting if not present
                // get start
                if (startTAZ.compareTo("") == 0 &&
                        attributes.getValue("key").compareTo("taz_id_start") == 0)
                    startTAZ = tazName;
                //get end
                if (endTAZ.compareTo("") == 0 &&
                        attributes.getValue("key").compareTo("taz_id_end") == 0)
                    endTAZ = tazName;
            }

            if (qName.equalsIgnoreCase("route") || qName.equalsIgnoreCase("walk")) {
                //walk, bike or car
                String edges = attributes.getValue("edges");
                String[] edgeArray = edges.split(" ");
                int distIndex = TPS_Sumo2MLExporter.this.getLengthBin(length);


                // get exit times
                String exitTimes = attributes.getValue("exitTimes");
                String[] timeArray = exitTimes.split(" ");
                if (edgeArray.length != timeArray.length) {
                    System.out.println("Wrong format! " + id + " " + edges + " " + exitTimes);
                }
                for (int j = 0; j < edgeArray.length; j++) {
                    int time = (int) (Double.parseDouble(timeArray[j]) + 0.5);
                    EdgeAttribute[] att = TPS_Sumo2MLExporter.this.edgelist.get(edgeArray[j]);
                    //check if we know this edge and add it if necessary
                    if (att == null) {
                        att = new EdgeAttribute[TPS_Sumo2MLExporter.this.modeMap.size()];
                        for (int k = 0; k < att.length; k++) {
                            att[k] = new EdgeAttribute();
                            att[k].id = edgeArray[j];
                        }
                        TPS_Sumo2MLExporter.this.edgelist.put(edgeArray[j], att);
                    }
                    att[mode].addTrip(time, distIndex, activity);
                    String taz = edge2TAZ.get(edgeArray[j]);
                    if (taz != null) {
                        visitedTazes.add(taz); //set will keep them unique
                        TAZAttribute[] attTaz = TPS_Sumo2MLExporter.this.tazlist.get(taz);
                        if (attTaz != null) { //
                            if (att[mode].length < 0) {
                                System.out.println("Unknown net length for edge " + edgeArray[j]);
                            } else {
                                attTaz[mode].addNetPerformance(time, att[mode].length);
                            }
                        } else {
                            System.out.println("Unknown taz: " + taz);
                        }
                    }

                    if (firstLeg && j == 0) {
                        att[mode].addTripToStart(time, distIndex, activity);
                        startTime = time;
                        firstLeg = false;
                    }
                    last = att; // will be overwritten next loop
                    endTime = time;
                }
            }
            if(qName.equalsIgnoreCase("ride")){
                // get pt- route
                String from = attributes.getValue("from");
                String to = attributes.getValue("to");

                //get length
                int distIndex = TPS_Sumo2MLExporter.this.getLengthBin(length);

                //get vehicle
                String ptVehicle = attributes.getValue("intended");
                PTRoute pt = TPS_Sumo2MLExporter.this.ptVehicle.get(ptVehicle);
                if(pt==null || from.equalsIgnoreCase(to)) {
                    if(pt == null)
                        System.err.println("Line not found: "+ptVehicle);
                    else{
                        //System.err.println(String.format("Haltestellenklatscher: Same start %s / stop %s found on line %s, name %s.", from,to,ptVehicle, pt.name));
                    }

                }
                else{

                    //find start and stop index of the line
                    int start=-1;
                    int stop = -1;
                    for(int i=0; i< pt.stops.length; i++){
                        if(pt.stops[i].equals(from)){
                            start = i;
                        }
                        if(pt.stops[i].equals(to)){
                            stop = i;
                        }
                    }

                    if(start == -1 || stop ==-1){
                        System.err.println(String.format("Start %s (index: %d) or stop %s (index %d) not found on line %s, name %s.", from,start,to,stop,ptVehicle, pt.name));
                    }
                    else {
                        if (start > stop) { //swap!!!
                            int tmp = start;
                            start = stop;
                            stop = tmp;
                        }

                        // get the edges
                        for (int j = start; j <= stop; j++) {
                            int time = pt.times[j];
                            EdgeAttribute[] att = TPS_Sumo2MLExporter.this.edgelist.get(pt.stops[j]);
                            //check if we know this edge and add it if necessary
                            if (att == null) {
                                att = new EdgeAttribute[TPS_Sumo2MLExporter.this.modeMap.size()];
                                for (int k = 0; k < att.length; k++) {
                                    att[k] = new EdgeAttribute();
                                    att[k].id = pt.stops[j];
                                }
                                TPS_Sumo2MLExporter.this.edgelist.put(pt.stops[j], att);
                            }
                            att[mode].addTrip(time, distIndex, activity);

                            String taz = edge2TAZ.get(pt.stops[j]);
                            if (taz != null) {
                                visitedTazes.add(taz); //set will keep them unique
                                TAZAttribute[] attTaz = TPS_Sumo2MLExporter.this.tazlist.get(taz);
                                if (attTaz != null) { //
                                    if (att[mode].length < 0) {
                                        System.out.println("Unknown net length for edge " + pt.stops[j]);
                                    } else {
                                        attTaz[mode].addNetPerformance(time, att[mode].length);
                                    }
                                } else {
                                    System.out.println("Unknown taz: " + taz);
                                }
                            }

                            if (firstLeg && j == 0) {
                                att[mode].addTripToStart(time, distIndex, activity);
                                startTime = time;
                                firstLeg = false;
                            }
                            last = att;
                            endTime = time;
                        }
                    }
                }
            }
        }

        @Override
        public void endElement(String uri,
                               String localName,
                               String qName) {

            if (qName.equalsIgnoreCase("vehicle")|| qName.equalsIgnoreCase("person")) {
                int distIndex = TPS_Sumo2MLExporter.this.getLengthBin(length);
                if(last !=null)
                    last[mode].addLastEntryToEnd(); //make the internally buffered last entry the End entry
                if (startTAZ.compareTo("")!=0 && endTAZ.compareTo("")!=0 &&
                        startTime!=Integer.MIN_VALUE && endTime != Integer.MIN_VALUE) {
                    //store taz start/end values
                    addTAZInfos(startTAZ, startTime, distIndex, activity, mode, true, length);
                    addTAZInfos(endTAZ, endTime, distIndex, activity, mode, false, length);
                    //now store the intermediate traffic infos:
                    if (!visitedTazes.isEmpty()){
                        for (String s : visitedTazes.toArray(new String[0])) {
                            TAZAttribute[] att = tazlist.get(s);
                            if (att != null) {
                                att[mode].addTripToTrafficCount(startTime, distIndex, activity);
                            }
                        }
                    }
                }

                tripsFound++;
                if(tripsFound%1000==0) {
                    System.out.println("processing trips:" + tripsFound);
                }
                //clear values
                id = "";
                length =0;
                firstLeg = true;
                visitedTazes = new HashSet<>();
                startTAZ = "";
                endTAZ = "";
                startTime = Integer.MIN_VALUE;
                endTime = Integer.MIN_VALUE;
                last = null;

            }

        }

        @Override
        public void characters(char ch[], int start, int length) {
            currentValue.append(ch, start, length);
        }
    }


    private void addTAZInfos(String name, int time, int distIndex, int activity, int mode, boolean start, double dist){
        TAZAttribute[] att = this.tazlist.get(name);
        //check if we know this edge and add it if necessary
        if (att == null) {
            att = new TAZAttribute[this.modeMap.size()];
            for (int k = 0; k < att.length; k++) {
                att[k] = new TAZAttribute();
                att[k].id = name;
            }
            this.tazlist.put(name, att);
        }
        if(start)
            att[mode].addTripToStart(time, distIndex, activity,dist);
        else
            att[mode].addTripToEnd(time, distIndex, activity,dist);

    }

    private int getLengthBin(double val)
    {
        int distIndex =-1;
        //since we have only 10 elements we just search lineary through the dist array
        for (int i = 0; i < DIST_BINS.length; i++) {
            if (val <= DIST_BINS[i] | i == DIST_BINS.length - 1) {
                distIndex = i;
                break;
            }
        }
        return distIndex;
    }

    public void writeEdgeAndTazAttributes(String output){
        try{
            System.out.println("Writing results for edges and tazes.");
            for(Map.Entry<Integer,String> e: this.modeMap.entrySet()) {
                FileWriter fw = new FileWriter(output+"_"+e.getValue()+".csv");
                EdgeAttribute t = new EdgeAttribute();
                fw.write(t.getCSVHeader());
                int i = 0;
                for (EdgeAttribute[] a : this.edgelist.values()) {
                    i++;
                    if (i % 1000 == 0) System.out.println("Saved edges:" + i);
                    fw.write(a[e.getKey()].toString());
                }
                fw.close();

                System.out.println("Saved edges:" + this.edgelist.size());

                fw = new FileWriter(output+"_taz_"+e.getValue()+".csv");
                TAZAttribute taz = new TAZAttribute();
                fw.write(taz.getCSVHeader());
                i = 0;
                for (TAZAttribute[] a : this.tazlist.values()) {
                    i++;
                    if (i % 10 == 0) System.out.println("Saved taz:" + i);
                    fw.write(a[e.getKey()].toString());
                }
                fw.close();

                System.out.println("Saved taz:" + this.tazlist.size());


                System.out.println("Finished processing");
            }
        } catch (  IOException e) {
            e.printStackTrace();
        }
    }

    public void exportNetGeoJSON(String net, String output){
        try {

            double offX=0, offY=0, x, y;


            // parse net XML file

            Document doc =openXMLDocument(net);
            System.out.println("Exporting net as geojson");

            System.out.println("Root Element :" + doc.getDocumentElement().getNodeName());

            Deg2UTM[] netBoundary=  new Deg2UTM[2];
            //get the net offset
            NodeList locationParams = doc.getElementsByTagName("location");
            if(locationParams.getLength()>0) {
                Node node = locationParams.item(0);
                Element element = (Element) node;

                // get offset's attribute
                String offset = element.getAttribute("netOffset");
                if(offset!= null) {
                    String[] offsets = offset.split(",");
                    if (offsets.length == 2) {
                        offX = Double.parseDouble(offsets[0]);
                        offY = Double.parseDouble(offsets[1]);
                    }
                }
                // get original boundary attribute. We need this to determine the UTM-zone
                offset = element.getAttribute("origBoundary");
                if(offset!= null) {
                    String[] offsets = offset.split(",");
                    if (offsets.length == 4) {
                        netBoundary[0]= new Deg2UTM( Double.parseDouble(offsets[1]), Double.parseDouble(offsets[0]));
                        netBoundary[1]= new Deg2UTM( Double.parseDouble(offsets[3]), Double.parseDouble(offsets[2]));
                    }
                }
            }


            //    <location netOffset="-339139.35,-5778999.45" convBoundary="0.00,0.00,94258.17,85580.61" origBoundary="12.629591,51.877564,14.475032,53.389797" projParameter="+proj=utm +zone=33 +ellps=WGS84 +datum=WGS84 +units=m +no_defs"/>

            // get <edge>
            NodeList list = doc.getElementsByTagName("edge");
            System.out.println("Edges to process:" + list.getLength());
            //prepare output
            FileWriter fw = new FileWriter(output);
            fw.write("{ \"type\": \"FeatureCollection\",\n" +
                   "  \"features\": [");
            int count =0;
            for (int temp = 0; temp < list.getLength() ; temp++) {
                if(count %1000 ==0){
                    System.out.println("Processed edges:"+count);
                    fw.flush();
                }
                count++;
                Node node = list.item(temp);

                if (node.getNodeType() == Node.ELEMENT_NODE) {

                    Element element = (Element) node;

                    // get edge's attribute
                    String id = element.getAttribute("id");
                    // get type
                    String type = element.getAttribute("type");

                    //convert types
                    //tokenize types
                    String[] types = type.split("[|]");
                    for(int i =0; i<types.length;i++) {
                        types[i] = edgeTypes.get(types[i]);

                    }
                   //     if (t.startsWith("highway.")) {
                            String preString;

                            if(temp==0)
                                preString=""; //first element has no prestring
                            else if(temp<list.getLength()-1) {
                                preString = ",\n";
                            }
                            else{
                                preString = "\n"; //last element has no comma
                            }

                            //get shape
                            String shape = element.getAttribute("shape");
                            String[] coords = shape.split(" ");
                            String taz = edge2TAZ.get(id);
                            if( taz ==null) taz = "-1";
                            if(coords.length>1 ) {
                                //add offset
                                for(int i=0; i< coords.length; i++){
                                    String[] cs= coords[i].split(",");
                                    if (cs.length == 2) {
                                        x = Double.parseDouble(cs[0])-offX;
                                        y = Double.parseDouble(cs[1])-offY;
                                        UTM2Deg convert = new UTM2Deg(netBoundary[0].Zone+" "+netBoundary[0].Letter+" "+x+" "+y);
                                        coords[i]= convert.longitude+"," + convert.latitude;
                                    }
                                }

                                fw.write(preString+writeOneElement(id, types, taz,coords));
                            }
                            //break;
                        //}
                    //}
                }
            }
            fw.write("]\n}\n");
            fw.close();
            System.out.println("Processed edges:"+count);

            System.out.println("Finished processing");

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    /*

{
  "type": "Feature",
  "properties": { "EdgeID": 123, "StreetType": 4, "taz": 1223 },
  "geometry": {
    "type": "LineString",
    "coordinates": [
      [30, 10],
      [10, 30]
    ]
  }
}


 */

    static private String writeOneCSVElement(String id, String[] types, String taz) {
        boolean motorway = false,
                trunk = false,
                primary = false,
                secondary = false,
                tertiary = false,
                minor = false,
                path = false,
                cycleway = false,
                railway = false;

        for(String t : types) {
            switch (t) {
                case "cycleway":
                    cycleway = true;
                    break;
                case "railway":
                    railway = true;
                    break;
                case "motorway":
                    motorway = true;
                    break;
                case "trunk":
                    trunk = true;
                    break;
                case "primary":
                    primary = true;
                    break;
                case "secondary":
                    secondary = true;
                    break;
                case "tertiary":
                    tertiary = true;
                    break;
                case "minor":
                    minor = true;
                    break;
                case "path":
                    path = true;
                    break;
            }
        }

        String buffer =id+"\t";
        buffer +=motorway?"1\t":"0\t";
        buffer +=trunk?"1\t":"0\t";
        buffer +=primary?"1\t":"0\t";
        buffer +=secondary?"1\t":"0\t";
        buffer +=tertiary?"1\t":"0\t";
        buffer +=minor?"1\t":"0\t";
        buffer +=path?"1\t":"0\t";
        buffer +=cycleway?"1\t":"0\t";
        buffer +=railway?"1\t":"0\t";

        buffer +=taz+"\n";
        return buffer;
    }



    static private String writeOneElement(String id, String[] types, String taz, String[] coords) {
        StringBuilder buffer =
                new StringBuilder("{\n" +
                        "\t\"type\": \"Feature\",\n" +
                        "\t\"properties\": { \"EdgeID\": \"" + id + "\", \"StreetType\": [");
        for(int i =0; i< types.length; i++) {
            buffer.append("\"").append(types[i]).append("\"");
            if(i< types.length-1){
                buffer.append(",");
            }
        }

        buffer.append("], \"TAZ\": \"").append(taz).append("\"},\n").append("\t\"geometry\": {\n").append("\t\t\"type\": \"LineString\",\n").append("\t\t\"coordinates\": [\n");
            for(int i =0; i< coords.length; i++){
                buffer.append("\t\t\t[").append(coords[i]).append("]");
                if(i< coords.length-1){
                    buffer.append(",\n");
                }
                else{ //last element has no comma
                    buffer.append("\n");
                }
            }
        buffer.append("\t\t]\n").append("\t}\n}");
        return buffer.toString();
    }


    /*
    from https://stackoverflow.com/questions/176137/java-convert-lat-lon-to-utm
     */
    private static class Deg2UTM
    {
        double Easting;
        double Northing;
        int Zone;
        char Letter;
        private  Deg2UTM(double Lat,double Lon)
        {
            Zone= (int) Math.floor(Lon/6+31);
            if (Lat<-72)
                Letter='C';
            else if (Lat<-64)
                Letter='D';
            else if (Lat<-56)
                Letter='E';
            else if (Lat<-48)
                Letter='F';
            else if (Lat<-40)
                Letter='G';
            else if (Lat<-32)
                Letter='H';
            else if (Lat<-24)
                Letter='J';
            else if (Lat<-16)
                Letter='K';
            else if (Lat<-8)
                Letter='L';
            else if (Lat<0)
                Letter='M';
            else if (Lat<8)
                Letter='N';
            else if (Lat<16)
                Letter='P';
            else if (Lat<24)
                Letter='Q';
            else if (Lat<32)
                Letter='R';
            else if (Lat<40)
                Letter='S';
            else if (Lat<48)
                Letter='T';
            else if (Lat<56)
                Letter='U';
            else if (Lat<64)
                Letter='V';
            else if (Lat<72)
                Letter='W';
            else
                Letter='X';
            Easting=0.5*Math.log((1+Math.cos(Lat*Math.PI/180)*Math.sin(Lon*Math.PI/180-(6*Zone-183)*Math.PI/180))/(1-Math.cos(Lat*Math.PI/180)*Math.sin(Lon*Math.PI/180-(6*Zone-183)*Math.PI/180)))*0.9996*6399593.62/Math.pow((1+Math.pow(0.0820944379, 2)*Math.pow(Math.cos(Lat*Math.PI/180), 2)), 0.5)*(1+ Math.pow(0.0820944379,2)/2*Math.pow((0.5*Math.log((1+Math.cos(Lat*Math.PI/180)*Math.sin(Lon*Math.PI/180-(6*Zone-183)*Math.PI/180))/(1-Math.cos(Lat*Math.PI/180)*Math.sin(Lon*Math.PI/180-(6*Zone-183)*Math.PI/180)))),2)*Math.pow(Math.cos(Lat*Math.PI/180),2)/3)+500000;
            Easting=Math.round(Easting*100)*0.01;
            Northing = (Math.atan(Math.tan(Lat*Math.PI/180)/Math.cos((Lon*Math.PI/180-(6*Zone -183)*Math.PI/180)))-Lat*Math.PI/180)*0.9996*6399593.625/Math.sqrt(1+0.006739496742*Math.pow(Math.cos(Lat*Math.PI/180),2))*(1+0.006739496742/2*Math.pow(0.5*Math.log((1+Math.cos(Lat*Math.PI/180)*Math.sin((Lon*Math.PI/180-(6*Zone -183)*Math.PI/180)))/(1-Math.cos(Lat*Math.PI/180)*Math.sin((Lon*Math.PI/180-(6*Zone -183)*Math.PI/180)))),2)*Math.pow(Math.cos(Lat*Math.PI/180),2))+0.9996*6399593.625*(Lat*Math.PI/180-0.005054622556*(Lat*Math.PI/180+Math.sin(2*Lat*Math.PI/180)/2)+4.258201531e-05*(3*(Lat*Math.PI/180+Math.sin(2*Lat*Math.PI/180)/2)+Math.sin(2*Lat*Math.PI/180)*Math.pow(Math.cos(Lat*Math.PI/180),2))/4-1.674057895e-07*(5*(3*(Lat*Math.PI/180+Math.sin(2*Lat*Math.PI/180)/2)+Math.sin(2*Lat*Math.PI/180)*Math.pow(Math.cos(Lat*Math.PI/180),2))/4+Math.sin(2*Lat*Math.PI/180)*Math.pow(Math.cos(Lat*Math.PI/180),2)*Math.pow(Math.cos(Lat*Math.PI/180),2))/3);
            if (Letter<'M')
                Northing = Northing + 10000000;
            Northing=Math.round(Northing*100)*0.01;
        }
    }

    private static class UTM2Deg
    {
        double latitude;
        double longitude;
        private  UTM2Deg(String UTM)
        {
            String[] parts=UTM.split(" ");
            int Zone=Integer.parseInt(parts[0]);
            char Letter=parts[1].toUpperCase(Locale.ENGLISH).charAt(0);
            double Easting=Double.parseDouble(parts[2]);
            double Northing=Double.parseDouble(parts[3]);
            double Hem;
            if (Letter>'M')
                Hem='N';
            else
                Hem='S';
            double north;
            if (Hem == 'S')
                north = Northing - 10000000;
            else
                north = Northing;
            latitude = (north/6366197.724/0.9996+(1+0.006739496742*Math.pow(Math.cos(north/6366197.724/0.9996),2)-0.006739496742*Math.sin(north/6366197.724/0.9996)*Math.cos(north/6366197.724/0.9996)*(Math.atan(Math.cos(Math.atan(( Math.exp((Easting - 500000) / (0.9996*6399593.625/Math.sqrt((1+0.006739496742*Math.pow(Math.cos(north/6366197.724/0.9996),2))))*(1-0.006739496742*Math.pow((Easting - 500000) / (0.9996*6399593.625/Math.sqrt((1+0.006739496742*Math.pow(Math.cos(north/6366197.724/0.9996),2)))),2)/2*Math.pow(Math.cos(north/6366197.724/0.9996),2)/3))-Math.exp(-(Easting-500000)/(0.9996*6399593.625/Math.sqrt((1+0.006739496742*Math.pow(Math.cos(north/6366197.724/0.9996),2))))*( 1 -  0.006739496742*Math.pow((Easting - 500000) / (0.9996*6399593.625/Math.sqrt((1+0.006739496742*Math.pow(Math.cos(north/6366197.724/0.9996),2)))),2)/2*Math.pow(Math.cos(north/6366197.724/0.9996),2)/3)))/2/Math.cos((north-0.9996*6399593.625*(north/6366197.724/0.9996-0.006739496742*3/4*(north/6366197.724/0.9996+Math.sin(2*north/6366197.724/0.9996)/2)+Math.pow(0.006739496742*3/4,2)*5/3*(3*(north/6366197.724/0.9996+Math.sin(2*north/6366197.724/0.9996 )/2)+Math.sin(2*north/6366197.724/0.9996)*Math.pow(Math.cos(north/6366197.724/0.9996),2))/4-Math.pow(0.006739496742*3/4,3)*35/27*(5*(3*(north/6366197.724/0.9996+Math.sin(2*north/6366197.724/0.9996)/2)+Math.sin(2*north/6366197.724/0.9996)*Math.pow(Math.cos(north/6366197.724/0.9996),2))/4+Math.sin(2*north/6366197.724/0.9996)*Math.pow(Math.cos(north/6366197.724/0.9996),2)*Math.pow(Math.cos(north/6366197.724/0.9996),2))/3))/(0.9996*6399593.625/Math.sqrt((1+0.006739496742*Math.pow(Math.cos(north/6366197.724/0.9996),2))))*(1-0.006739496742*Math.pow((Easting-500000)/(0.9996*6399593.625/Math.sqrt((1+0.006739496742*Math.pow(Math.cos(north/6366197.724/0.9996),2)))),2)/2*Math.pow(Math.cos(north/6366197.724/0.9996),2))+north/6366197.724/0.9996)))*Math.tan((north-0.9996*6399593.625*(north/6366197.724/0.9996 - 0.006739496742*3/4*(north/6366197.724/0.9996+Math.sin(2*north/6366197.724/0.9996)/2)+Math.pow(0.006739496742*3/4,2)*5/3*(3*(north/6366197.724/0.9996+Math.sin(2*north/6366197.724/0.9996)/2)+Math.sin(2*north/6366197.724/0.9996 )*Math.pow(Math.cos(north/6366197.724/0.9996),2))/4-Math.pow(0.006739496742*3/4,3)*35/27*(5*(3*(north/6366197.724/0.9996+Math.sin(2*north/6366197.724/0.9996)/2)+Math.sin(2*north/6366197.724/0.9996)*Math.pow(Math.cos(north/6366197.724/0.9996),2))/4+Math.sin(2*north/6366197.724/0.9996)*Math.pow(Math.cos(north/6366197.724/0.9996),2)*Math.pow(Math.cos(north/6366197.724/0.9996),2))/3))/(0.9996*6399593.625/Math.sqrt((1+0.006739496742*Math.pow(Math.cos(north/6366197.724/0.9996),2))))*(1-0.006739496742*Math.pow((Easting-500000)/(0.9996*6399593.625/Math.sqrt((1+0.006739496742*Math.pow(Math.cos(north/6366197.724/0.9996),2)))),2)/2*Math.pow(Math.cos(north/6366197.724/0.9996),2))+north/6366197.724/0.9996))-north/6366197.724/0.9996)*3/2)*(Math.atan(Math.cos(Math.atan((Math.exp((Easting-500000)/(0.9996*6399593.625/Math.sqrt((1+0.006739496742*Math.pow(Math.cos(north/6366197.724/0.9996),2))))*(1-0.006739496742*Math.pow((Easting-500000)/(0.9996*6399593.625/Math.sqrt((1+0.006739496742*Math.pow(Math.cos(north/6366197.724/0.9996),2)))),2)/2*Math.pow(Math.cos(north/6366197.724/0.9996),2)/3))-Math.exp(-(Easting-500000)/(0.9996*6399593.625/Math.sqrt((1+0.006739496742*Math.pow(Math.cos(north/6366197.724/0.9996),2))))*(1-0.006739496742*Math.pow((Easting-500000)/(0.9996*6399593.625/Math.sqrt((1+0.006739496742*Math.pow(Math.cos(north/6366197.724/0.9996),2)))),2)/2*Math.pow(Math.cos(north/6366197.724/0.9996),2)/3)))/2/Math.cos((north-0.9996*6399593.625*(north/6366197.724/0.9996-0.006739496742*3/4*(north/6366197.724/0.9996+Math.sin(2*north/6366197.724/0.9996)/2)+Math.pow(0.006739496742*3/4,2)*5/3*(3*(north/6366197.724/0.9996+Math.sin(2*north/6366197.724/0.9996)/2)+Math.sin(2*north/6366197.724/0.9996)*Math.pow(Math.cos(north/6366197.724/0.9996),2))/4-Math.pow(0.006739496742*3/4,3)*35/27*(5*(3*(north/6366197.724/0.9996+Math.sin(2*north/6366197.724/0.9996)/2)+Math.sin(2*north/6366197.724/0.9996)*Math.pow(Math.cos(north/6366197.724/0.9996),2))/4+Math.sin(2*north/6366197.724/0.9996)*Math.pow(Math.cos(north/6366197.724/0.9996),2)*Math.pow(Math.cos(north/6366197.724/0.9996),2))/3))/(0.9996*6399593.625/Math.sqrt((1+0.006739496742*Math.pow(Math.cos(north/6366197.724/0.9996),2))))*(1-0.006739496742*Math.pow((Easting-500000)/(0.9996*6399593.625/Math.sqrt((1+0.006739496742*Math.pow(Math.cos(north/6366197.724/0.9996),2)))),2)/2*Math.pow(Math.cos(north/6366197.724/0.9996),2))+north/6366197.724/0.9996)))*Math.tan((north-0.9996*6399593.625*(north/6366197.724/0.9996-0.006739496742*3/4*(north/6366197.724/0.9996+Math.sin(2*north/6366197.724/0.9996)/2)+Math.pow(0.006739496742*3/4,2)*5/3*(3*(north/6366197.724/0.9996+Math.sin(2*north/6366197.724/0.9996)/2)+Math.sin(2*north/6366197.724/0.9996)*Math.pow(Math.cos(north/6366197.724/0.9996),2))/4-Math.pow(0.006739496742*3/4,3)*35/27*(5*(3*(north/6366197.724/0.9996+Math.sin(2*north/6366197.724/0.9996)/2)+Math.sin(2*north/6366197.724/0.9996)*Math.pow(Math.cos(north/6366197.724/0.9996),2))/4+Math.sin(2*north/6366197.724/0.9996)*Math.pow(Math.cos(north/6366197.724/0.9996),2)*Math.pow(Math.cos(north/6366197.724/0.9996),2))/3))/(0.9996*6399593.625/Math.sqrt((1+0.006739496742*Math.pow(Math.cos(north/6366197.724/0.9996),2))))*(1-0.006739496742*Math.pow((Easting-500000)/(0.9996*6399593.625/Math.sqrt((1+0.006739496742*Math.pow(Math.cos(north/6366197.724/0.9996),2)))),2)/2*Math.pow(Math.cos(north/6366197.724/0.9996),2))+north/6366197.724/0.9996))-north/6366197.724/0.9996))*180/Math.PI;
            latitude=Math.round(latitude*10000000);
            latitude=latitude/10000000;
            longitude =Math.atan((Math.exp((Easting-500000)/(0.9996*6399593.625/Math.sqrt((1+0.006739496742*Math.pow(Math.cos(north/6366197.724/0.9996),2))))*(1-0.006739496742*Math.pow((Easting-500000)/(0.9996*6399593.625/Math.sqrt((1+0.006739496742*Math.pow(Math.cos(north/6366197.724/0.9996),2)))),2)/2*Math.pow(Math.cos(north/6366197.724/0.9996),2)/3))-Math.exp(-(Easting-500000)/(0.9996*6399593.625/Math.sqrt((1+0.006739496742*Math.pow(Math.cos(north/6366197.724/0.9996),2))))*(1-0.006739496742*Math.pow((Easting-500000)/(0.9996*6399593.625/Math.sqrt((1+0.006739496742*Math.pow(Math.cos(north/6366197.724/0.9996),2)))),2)/2*Math.pow(Math.cos(north/6366197.724/0.9996),2)/3)))/2/Math.cos((north-0.9996*6399593.625*( north/6366197.724/0.9996-0.006739496742*3/4*(north/6366197.724/0.9996+Math.sin(2*north/6366197.724/0.9996)/2)+Math.pow(0.006739496742*3/4,2)*5/3*(3*(north/6366197.724/0.9996+Math.sin(2*north/6366197.724/0.9996)/2)+Math.sin(2* north/6366197.724/0.9996)*Math.pow(Math.cos(north/6366197.724/0.9996),2))/4-Math.pow(0.006739496742*3/4,3)*35/27*(5*(3*(north/6366197.724/0.9996+Math.sin(2*north/6366197.724/0.9996)/2)+Math.sin(2*north/6366197.724/0.9996)*Math.pow(Math.cos(north/6366197.724/0.9996),2))/4+Math.sin(2*north/6366197.724/0.9996)*Math.pow(Math.cos(north/6366197.724/0.9996),2)*Math.pow(Math.cos(north/6366197.724/0.9996),2))/3)) / (0.9996*6399593.625/Math.sqrt((1+0.006739496742*Math.pow(Math.cos(north/6366197.724/0.9996),2))))*(1-0.006739496742*Math.pow((Easting-500000)/(0.9996*6399593.625/Math.sqrt((1+0.006739496742*Math.pow(Math.cos(north/6366197.724/0.9996),2)))),2)/2*Math.pow(Math.cos(north/6366197.724/0.9996),2))+north/6366197.724/0.9996))*180/Math.PI+Zone*6-183;
            longitude=Math.round(longitude*10000000);
            longitude=longitude/10000000;
        }
    }
}