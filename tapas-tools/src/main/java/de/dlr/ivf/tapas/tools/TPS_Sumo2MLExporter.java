package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * THis class provides all export fuctionality for the export to the "Mobilitätslabor"
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

        //gett all the needed strings and files as automatic as possible!
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
        worker.processTAZList(tazName,tazTableName);
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
    Set<Integer> possibleActivites = new HashSet<>();
    Map<Integer,String> modeMap = new HashMap<>();

    public class EdgeAttribute{
        String id ="";
        int lastTime, lastActivity, lastDist; //buffer these values in case this becomes the last entry of a trip
        int[] timeCounts = new int[(24*60*60)/TIME_BIN];
        int[] distCount = new int[DIST_BINS.length];
        int[] activityCount = new int[possibleActivites.size()];

        int[] timeCountsStart = new int[(24*60*60)/TIME_BIN];
        int[] distCountStart = new int[DIST_BINS.length];
        int[] activityCountStart = new int[possibleActivites.size()];

        int[] timeCountsEnd = new int[(24*60*60)/TIME_BIN];
        int[] distCountEnd = new int[DIST_BINS.length];
        int[] activityCountEnd = new int[possibleActivites.size()];


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
            String s = "id";
            for (int j =0; j<timeCounts.length; j++) {
                s += "\t" + "t"+j;
            }

            for (int j =0; j<distCount.length; j++) {
                s += "\t" + "d"+j;
            }

            for (int j =0; j<activityCount.length; j++) {
                s += "\t" + "a"+j;
            }

            for (int j =0; j<timeCountsStart.length; j++) {
                s += "\t" + "start_t"+j;
            }

            for (int j =0; j<distCountStart.length; j++) {
                s += "\t" + "start_d"+j;
            }

            for (int j =0; j<activityCountStart.length; j++) {
                s += "\t" + "start_a"+j;
            }

            for (int j =0; j<timeCountsEnd.length; j++) {
                s += "\t" + "end_t"+j;
            }

            for (int j =0; j<distCountEnd.length; j++) {
                s += "\t" + "end_d"+j;
            }

            for (int j =0; j<activityCountEnd.length; j++) {
                s += "\t" + "end_a"+j;
            }

            s+="\n";
            return s;
        }


        @Override
        public String toString() {
            String s = this.id;
            for (Integer i : timeCounts) {
                s += "\t" + i.toString();
            }

            for (Integer i : distCount) {
                s += "\t" + i.toString();
            }

            for (Integer i : activityCount) {
                s += "\t" + i.toString();
            }

            for (Integer i : timeCountsStart) {
                s += "\t" + i.toString();
            }

            for (Integer i : distCountStart) {
                s += "\t" + i.toString();
            }

            for (Integer i : activityCountStart) {
                s += "\t" + i.toString();
            }

            for (Integer i : timeCountsEnd) {
                s += "\t" + i.toString();
            }

            for (Integer i : distCountEnd) {
                s += "\t" + i.toString();
            }

            for (Integer i : activityCountEnd) {
                s += "\t" + i.toString();
            }

            s+="\n";
            return s;
        }
    }


    public class TAZAttribute{
        String id ="";

        int[] timeCountsStart = new int[(24*60*60)/TIME_BIN];
        int[][] distCountStart = new int[timeCountsStart.length][DIST_BINS.length];
        int[][] activityCountStart = new int[timeCountsStart.length][possibleActivites.size()];

        int[] timeCountsEnd = new int[(24*60*60)/TIME_BIN];
        int[][] distCountEnd = new int[timeCountsStart.length][DIST_BINS.length];
        int[][] activityCountEnd = new int[timeCountsStart.length][possibleActivites.size()];



        public void addTripToStart(int time, int distIndex, int purposeIndex){
            int tIndex = ((time + 24*60*60)%(24*60*60))/TIME_BIN;
            timeCountsStart[tIndex]++;
            distCountStart[tIndex][distIndex]++;
            activityCountStart[tIndex][purposeIndex]++;
        }

        public void addTripToEnd(int time, int distIndex, int purposeIndex){
            int tIndex = ((time + 24*60*60)%(24*60*60))/TIME_BIN;
            timeCountsEnd[tIndex]++;
            distCountEnd[tIndex][distIndex]++;
            activityCountEnd[tIndex][purposeIndex]++;
        }

        public String getCSVHeader(){
            String s = "id";
            for (int j =0; j<timeCountsStart.length; j++) {
                s += "\t" + "t"+j;
            }

            for (int j =0; j<timeCountsStart.length; j++) {
                for (int i =0; i<distCountStart[j].length; i++) {
                    s += "\tstart_t"+j+"_d"+i;
                }
            }
            for (int j =0; j<timeCountsStart.length; j++) {
                for (int i =0; i<activityCountStart[j].length; i++) {
                    s += "\tstart_t"+j+"_a"+i;
                }
            }
            for (Integer i : timeCountsEnd) {
                s += "\t" + i.toString();
            }

            for (int j =0; j<timeCountsEnd.length; j++) {
                for (int i =0; i<distCountEnd[j].length; i++) {
                    s += "\tend_t"+j+"_d"+i;
                }
            }

            for (int j =0; j<timeCountsEnd.length; j++) {
                for (int i =0; i<activityCountEnd[j].length; i++) {
                    s += "\tend_t"+j+"_a"+i;
                }
            }
            s+="\n";

            return s;
        }

        @Override
        public String toString() {
            String s = this.id;

            for (Integer i : timeCountsStart) {
                s += "\t" + i.toString();
            }

            for (int j =0; j<timeCountsStart.length; j++) {
                for (Integer i : distCountStart[j]) {
                    s += "\t" + i.toString();
                }
            }
            for (int j =0; j<timeCountsStart.length; j++) {
                for (Integer i : activityCountStart[j]) {
                    s += "\t" + i.toString();
                }
            }
            for (Integer i : timeCountsEnd) {
                s += "\t" + i.toString();
            }

            for (int j =0; j<timeCountsEnd.length; j++) {
                for (Integer i : distCountEnd[j]) {
                    s += "\t" + i.toString();
                }
            }

            for (int j =0; j<timeCountsEnd.length; j++) {
                for (Integer i : activityCountEnd[j]) {
                    s += "\t" + i.toString();
                }
            }
            s+="\n";
            return s;
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
                id = Integer.toString(p_id)+"_"+ Integer.toString(hh_id)+"_"+ Integer.toString(startTimeMin);
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
            query = "SELECT code_zbe, code_tapas "+
                    "FROM "+activity_codes;
            ResultSet rs = this.dbCon.executeQuery(query, this);
            while (rs.next()) {
                zbeCode = rs.getInt("code_zbe");
                tapasCode = rs.getInt("code_tapas");
                if(tapasCode >=0) {
                    this.activityMap.put(zbeCode, tapasCode);
                    possibleActivites.add(tapasCode);
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


    public void exportNetAttribute(String net, String output){


        try {


            // parse net XML file
            Document doc = openXMLDocument(net);

            System.out.println("Root Element :" + doc.getDocumentElement().getNodeName());

            Map<String,String> edgeTypes = processEdgeTypes(doc);

            // get <edge>
            NodeList list = doc.getElementsByTagName("edge");
            System.out.println("Edges to process:" + list.getLength());
            //prepare output
            FileWriter fw = new FileWriter(output);
            fw.write("id\tmotorway\ttrunk\tprimary\tsecondary\ttertiary\tminor\tpath\tcycleway\trailway\ttaz\n");
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
                    String[] types = type.split("[|]");
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
                    String taz = edge2TAZ.get(id);
                    if( taz ==null) taz = "-1";
                    if(id!=null & types != null & types.length > 0){
                        try {
                            fw.write(writeOneCSVElement(id, types, taz));
                        }catch(NullPointerException e){
                            e.printStackTrace();
                            //stupid null
                        }
                    }
                    else{
                        System.out.println("Error at edge: "+id+ "types: "+types+ " taz: "+taz);
                    }
                }
            }
            System.out.println("Processed edges:"+count);
            fw.flush();
            fw.close();

            System.out.println("Processed edges:"+list.getLength());

            System.out.println("Finished processing");

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    public Document openXMLDocument(String fileName) throws ParserConfigurationException , SAXException , IOException {

        Document doc = null;
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

        // optional, but recommended
        // process XML securely, avoid attacks like XML External Entities (XXE)
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
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



    Map<String, String> edge2TAZ = new HashMap<>();

    public void processTAZList(String filename, String tableName)  {
        try{
            Document tazDoc = openXMLDocument(filename);

            System.out.println("Root Element :" + tazDoc.getDocumentElement().getNodeName());
            // get <edge>
            //prepare output
            Map<String,String> edge2TAZ = new HashMap<>();
            NodeList tazlist = tazDoc.getElementsByTagName("taz");
            System.out.println("TAZ to process:" + tazlist.getLength());

            //prepare output

            for (int temp = 0; temp < tazlist.getLength() ; temp++) {
                if(temp %1000 ==0)System.out.println("Processed taz:"+temp);
                Node node = tazlist.item(temp);

                if (node.getNodeType() == Node.ELEMENT_NODE) {

                    Element element = (Element) node;

                    // get edge's attribute
                    String id = element.getAttribute("id");

                    // get type
                    String edges = element.getAttribute("edges");
                    //tokenize edges
                    String[] edge = edges.split(" ");
                    for(String t: edge) {
                        edge2TAZ.put(t, id);
                    }
                }
            }
            System.out.println("Processed taz:"+tazlist.getLength());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String,String> processEdgeTypes(Document doc) throws ParserConfigurationException , SAXException , IOException {
        Map<String,String> edgeTypes = new HashMap<>();

        NodeList list = doc.getElementsByTagName("type");
        System.out.println("Types to process:" + list.getLength());
        for (int temp = 0; temp < list.getLength() ; temp++) {

            Node node = list.item(temp);

            if (node.getNodeType() == Node.ELEMENT_NODE) {

                Element element = (Element) node;

                // get edge's attribute
                String id = element.getAttribute("id");
                String types[] = id.split("[|]");
                // get type
                String prio = element.getAttribute("priority");
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
        return edgeTypes;
    }

    public void loadRoutes(String routes){
        try {


            Document doc =openXMLDocument(routes);

            System.out.println("Root Element :" + doc.getDocumentElement().getNodeName());

            //get pt vehicles
            System.out.println("Processing pt vehicles");
            loadSumoPublicTransport(doc);
            System.out.println("Finished processing pt vehicles");

            // get vehicles
            System.out.println("Processing vehicles");
            processSumoType(doc,"vehicle",false);
            System.out.println("Finished processing vehicle tours");
            // get persons (walk/bike/bus)
            System.out.println("Processing persons");
            processSumoType(doc,"person", false);
            System.out.println("Finished processing person tours");
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isPT(String type){
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

    //public transport vehicels are "disguised" as normal vehicles
    private void loadSumoPublicTransport(Document doc) {
        NodeList list = doc.getElementsByTagName("vehicle");
        System.out.println("Potential pt vehicles to process:" + list.getLength());
        int ptFound = 0;

        for (int temp = 0; temp < list.getLength() ; temp++) {
            if (temp % 1000 == 0) {
                System.out.println("Processed pt vehicles:" + temp);
            }
            Node node = list.item(temp);


            if (node.getNodeType() == Node.ELEMENT_NODE) {

                Element element = (Element) node;

                // get edge's attribute
                String id = element.getAttribute("id");
                String type = element.getAttribute("type");
                if(this.isPT(type)){
                    //now extract the route of this vehicle
                    NodeList routesByThisTour = element.getElementsByTagName("route");
                    PTRoute pt = new PTRoute();
                    pt.id= id;
                    for (int route = 0; route < routesByThisTour.getLength(); route++) {
                        Node routeNode = routesByThisTour.item(route);

                        if (routeNode.getNodeType() == Node.ELEMENT_NODE) {

                            Element routeElement = (Element) routeNode;

                            // get routes edgelist
                            String edges = routeElement.getAttribute("edges");
                            pt.stops = edges.split(" ");
                            // get exit times
                            String exitTimes = routeElement.getAttribute("exitTimes");
                            String timeArray[] = exitTimes.split(" ");
                            if (pt.stops.length != timeArray.length) {
                                System.out.println("Wrong format! " + id + " " + edges + " " + exitTimes);
                                System.exit(-1);
                            }
                            pt.times = new int[timeArray.length];
                            for (int j = 0; j < timeArray.length; j++) {
                                pt.times[j] =(int) (Double.parseDouble(timeArray[j]) + 0.5);
                            }
                            break;
                        }
                    }
                    if(pt.times!=null && pt.stops!=null){
                        //now extract the route of this vehicle
                        String line ="Unknown line", heading = " Unknown heading";
                        NodeList paramsByThisTour = element.getElementsByTagName("param");
                        for (int param = 0; param < paramsByThisTour.getLength(); param++) {
                            Node paramNode = paramsByThisTour.item(param);

                            if (paramNode.getNodeType() == Node.ELEMENT_NODE) {

                                Element routeElement = (Element) paramNode;

                                String key = routeElement.getAttribute("key");
                                if (key.equals("gtfs.route_name")) {
                                    line = routeElement.getAttribute("value");
                                }
                                if (key.equals("gtfs.trip_headsign")) {
                                    heading = routeElement.getAttribute("value");
                                }
                            }
                        }
                        pt.name = line+ " - "+heading;
                    }
                    this.ptVehicle.put(id,pt);
                }
            }
        }
        System.out.println("Processed pt vehicles:" + this.ptVehicle.size());
    }



    private void processSumoType(Document doc, String elementName, boolean usePT) {
        NodeList list = doc.getElementsByTagName(elementName);
        System.out.println("Tours to process:" + list.getLength());
        TripAttribute commuter = new TripAttribute();
        commuter.mode=2; // CAR
        commuter.activity = 211; //work, full time
        TripAttribute ptVehicle = new TripAttribute();
        ptVehicle.mode=5; // PT
        ptVehicle.activity = 799; //any activity

        for (int temp = 0; temp < list.getLength() ; temp++) {
            if (temp % 1000 == 0) {
                System.out.println("Processed tours:" + temp);
            }
            Node node = list.item(temp);


            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String startTAZ = "", endTAZ = "";
                int startTime=Integer.MIN_VALUE, endTime=Integer.MIN_VALUE;

                Element element = (Element) node;

                // get edge's attribute
                String id = element.getAttribute("id");
                String tmp = element.getAttribute("depart");
                if(tmp.compareTo("")!=0)
                    startTime =  (int) (Double.parseDouble(tmp) + 0.5);
                tmp = element.getAttribute("arrival");
                if(tmp.compareTo("")!=0)
                    endTime =  (int) (Double.parseDouble(tmp) + 0.5);
                String type = element.getAttribute("type");
                TripAttribute attr =null;
                if(!this.isPT(type)){
                    id = id.substring(0, id.lastIndexOf("_") ); //cut off the clone number
                    if(id.startsWith("-")){
                        attr = commuter;
                    }
                    else{
                        attr = this.trips.get(id);
                    }
                }
                else{ //vehicle id is the pt-vehicle and does not correspond to a tapas trip
                    if(usePT) {
                        attr = ptVehicle;
                    }
                }
                if(attr!=null) {
                    int activity = 0;
                    if (activityMap.containsKey(attr.activity)) {
                        activity = activityMap.get(attr.activity);
                    }
                    int mode = attr.mode;
                    NodeList routesByThisTour = element.getChildNodes();
                    // get length
                    String routeLength = element.getAttribute("routeLength");
                    int distIndex = -1;
                    if (routeLength != null && routeLength.length() > 0) {
                        double length = Double.parseDouble(routeLength);
                        distIndex = this.getLengthBin(length);
                    } else {
                        // we have to sum up all leg lengths
                        double length = 0, legLength;
                        for (int i = 0; i < routesByThisTour.getLength(); i++) {
                            Node leg = routesByThisTour.item(i);
                            legLength = 0;
                            if (leg.getNodeType() == Node.ELEMENT_NODE) {
                                Element routeElement = (Element) leg;
                                //get length
                                routeLength = routeElement.getAttribute("routeLength"); //
                                if (routeLength != null && routeLength.length() > 0 && !routeLength.equals("-1.00")) {
                                    legLength = Double.parseDouble(routeLength);
                                    legLength = Math.max(0, legLength); // teleports have a length of -1m!
                                }
                            }
                            length += legLength;
                        }
                        distIndex = this.getLengthBin(length);
                    }

                    //now process the edges
                    boolean lookForBegin = true;
                    EdgeAttribute[] last =null;
                    for (int i = 0; i < routesByThisTour.getLength(); i++) {
                        Node leg = routesByThisTour.item(i);
                        String name = leg.getNodeName();

                        if (name.equals("route")) {
                            last = parseEdgeInfos(id, activity, mode, distIndex, leg, lookForBegin);
                        } else if (name.equals("walk")) {
                            last = parseEdgeInfos(id, activity, mode, distIndex, leg, lookForBegin);
                        } else if (name.equals("ride")) {
                            last = parsePTInfos(id, activity, mode, distIndex, leg, lookForBegin);
                        } else if (name.equals("param")){ //determine start and end taz
                            if (leg.getNodeType() == Node.ELEMENT_NODE) {
                                Element paramElement = (Element) leg;
                                // get start
                                if (startTAZ.compareTo("")==0 &&
                                    paramElement.getAttribute("key").compareTo("taz_id_start")==0)
                                    startTAZ = paramElement.getAttribute("value"); //will return an empty sting if not present
                                //get end
                                if (endTAZ.compareTo("")==0 &&
                                    paramElement.getAttribute("key").compareTo("taz_id_end")==0)
                                    endTAZ = paramElement.getAttribute("value"); //will return an empty sting if not present
                            }
                        }
                        if(lookForBegin && last != null){
                            lookForBegin = false;
                        }
                    }
                    last[mode].addLastEntryToEnd(); //make the internally buffered last entry the End entry

                    if (startTAZ.compareTo("")!=0 && endTAZ.compareTo("")!=0 &&
                            startTime!=Integer.MIN_VALUE && endTime != Integer.MIN_VALUE){
                        //store taz values
                        addTAZInfos(startTAZ,startTime, distIndex,activity,mode,true);
                        addTAZInfos(endTAZ,endTime, distIndex,activity,mode,false);
                    }
                    else{
                        System.err.println("Incomplete tripinfo! id: "+id+" startTAZ: "+startTAZ+" endTaz: "+endTAZ+ " start time: "+startTime+" end time: "+endTime);
                    }
                }

            }
        }
        System.out.println("Processed tours: " + list.getLength());
    }

    private void addTAZInfos(String name, int time, int distIndex, int activity, int mode, boolean start){
        TAZAttribute att[] = this.tazlist.get(name);
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
            att[mode].addTripToStart(time, distIndex, activity);
        else
            att[mode].addTripToEnd(time, distIndex, activity);

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
    private EdgeAttribute[] parseEdgeInfos(String id, int activity, int mode, int distIndex, Node routeNode,boolean returnFirst) {
            EdgeAttribute[] last = null;
            if (routeNode.getNodeType() == Node.ELEMENT_NODE) {

                Element routeElement = (Element) routeNode;

                // get routes edgelist
                String edges = routeElement.getAttribute("edges");
                String edgeArray[] = edges.split(" ");

                //get length if not given globally
                if(distIndex<0){
                    String routeLength = routeElement.getAttribute("routeLength");
                    if(routeLength!=null&& routeLength.length()>0 && !routeLength.equals("-1.00")) {
                        double length = Double.parseDouble(routeLength);
                        distIndex = this.getLengthBin(length);
                    }
                    else {
                        System.out.println("Wrong distance! " + id + " " + edges + " " + routeLength);
                        return null;
                    }
                }

                // get exit times
                String exitTimes = routeElement.getAttribute("exitTimes");
                String timeArray[] = exitTimes.split(" ");
                if (edgeArray.length != timeArray.length) {
                    System.out.println("Wrong format! " + id + " " + edges + " " + exitTimes);
                    return null;
                }
                for (int j = 0; j < edgeArray.length; j++) {
                    int time = (int) (Double.parseDouble(timeArray[j]) + 0.5);
                    EdgeAttribute att[] = this.edgelist.get(edgeArray[j]);
                    //check if we know this edge and add it if necessary
                    if (att == null) {
                        att = new EdgeAttribute[this.modeMap.size()];
                        for (int k = 0; k < att.length; k++) {
                            att[k] = new EdgeAttribute();
                            att[k].id = edgeArray[j];
                        }
                        this.edgelist.put(edgeArray[j], att);
                    }
                    att[mode].addTrip(time, distIndex, activity);
                    if(returnFirst && j == 0){
                        att[mode].addTripToStart(time, distIndex, activity);
                    }
                    last = att; // will be overwritten next loop
                }
            }
            return last;
    }

    private EdgeAttribute[] parsePTInfos(String id, int activity, int mode, int distIndex, Node routeNode,boolean returnFirst) {
        EdgeAttribute[] last = null;
            if (routeNode.getNodeType() == Node.ELEMENT_NODE) {

                Element routeElement = (Element) routeNode;

                // get route
                String from = routeElement.getAttribute("from");
                String to = routeElement.getAttribute("to");
                //get length
                String len = routeElement.getAttribute("routeLength");
                if(len== null || len.length()==0 || len.equals("-1.00"))
                    return null; //teleport

                //get length if not given golbally
                if(distIndex<0){
                    double length = Double.parseDouble(len);
                    distIndex = this.getLengthBin(length);
                }

                //get vehicle
                String ptVehicle = routeElement.getAttribute("vehicle");
                PTRoute pt = this.ptVehicle.get(ptVehicle);
                if(pt==null) {
                    System.err.println("Line not found: "+ptVehicle);
                    return null;
                }

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
                    return null;
                }
                if(start>stop){ //swap!!!
                    int tmp = start;
                    start = stop;
                    stop = tmp;
                }

                // get the edges
                for (int j = start; j <= stop; j++) {
                    int time = pt.times[j];
                    EdgeAttribute att[] = this.edgelist.get(pt.stops[j]);
                    //check if we know this edge and add it if necessary
                    if (att == null) {
                        att = new EdgeAttribute[this.modeMap.size()];
                        for (int k = 0; k < att.length; k++) {
                            att[k] = new EdgeAttribute();
                            att[k].id = pt.stops[j];
                        }
                        this.edgelist.put(pt.stops[j], att);
                    }
                    att[mode].addTrip(time, distIndex, activity);
                    if(returnFirst && j == start){
                        att[mode].addTripToStart(time, distIndex, activity);
                    }
                    else if(!returnFirst && j== stop){
                        last = att;
                    }
                }
            }
        return last;
    }


    public void writeEdgeAndTazAttributes(String output){
        try{
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

            double offX=0, offY=0, x=0,y=0;


            // parse net XML file

            Document doc =openXMLDocument(net);

            System.out.println("Root Element :" + doc.getDocumentElement().getNodeName());

            Map<String,String> edgeTypes = processEdgeTypes(doc);

            Deg2UTM netBoundary[]=  new Deg2UTM[2];
            //get the net offset
            NodeList locationParams = doc.getElementsByTagName("location");
            if(locationParams.getLength()>0) {
                Node node = locationParams.item(0);
                Element element = (Element) node;

                // get offset's attribute
                String offset = element.getAttribute("netOffset");
                if(offset!= null) {
                    String offsets[] = offset.split(",");
                    if (offsets.length == 2) {
                        offX = Double.parseDouble(offsets[0]);
                        offY = Double.parseDouble(offsets[1]);
                    }
                }
                // get original boundary attribute. We need this to determine the UTM-zone
                offset = element.getAttribute("origBoundary");
                if(offset!= null) {
                    String offsets[] = offset.split(",");
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
                            String preString="";

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
                                    String cs[]= coords[i].split(",");
                                    if (cs.length == 2) {
                                        x = Double.parseDouble(cs[0])-offX;
                                        y = Double.parseDouble(cs[1])-offY;
                                        UTM2Deg convert = new UTM2Deg(Integer.toString(netBoundary[0].Zone)+" "+netBoundary[0].Letter+" "+Double.toString(x)+" "+Double.toString(y));
                                        coords[i]= Double.toString(convert.longitude)+"," + Double.toString(convert.latitude);
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
        String buffer =
            "{\n"+
            "\t\"type\": \"Feature\",\n"+
            "\t\"properties\": { \"EdgeID\": \""+id+"\", \"StreetType\": [";
        for(int i =0; i< types.length; i++) {
            buffer =buffer+"\""+types[i]+"\"";
            if(i< types.length-1){
                buffer =buffer+",";
            }
        }

        buffer = buffer+"], \"TAZ\": \""+taz+"\"},\n"+
            "\t\"geometry\": {\n"+
            "\t\t\"type\": \"LineString\",\n"+
            "\t\t\"coordinates\": [\n";
            for(int i =0; i< coords.length; i++){
                buffer =buffer+"\t\t\t["+coords[i]+"]";
                if(i< coords.length-1){
                    buffer =buffer+",\n";
                }
                else{ //last element has no comma
                    buffer =buffer+"\n";
                }
            }
        buffer =buffer+"\t\t]\n"+
            "\t}\n}";
        return buffer;
    }


    /*
    from https://stackoverflow.com/questions/176137/java-convert-lat-lon-to-utm
     */
    private class Deg2UTM
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

    private class UTM2Deg
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