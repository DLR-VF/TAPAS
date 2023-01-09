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
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * THis class provides all export fuctionality for the export to the "Mobilitätslabor"
 */
public class TPS_Sumo2MLExporter extends TPS_BasicConnectionClass {

    private static final String NETNAME = "C:\\temp\\net.net.xml";
    private static final String TAZNAME = "S:\\tsc\\scenario_templates\\berlin_net\\Berlin_1223.taz.xml";
    private static final String ROUTENAME = "S:\\tsc\\scenario_workdir\\VMo4Orte_2022y_03m_22d_16h_31m_58s_526ms\\iteration000\\oneshot\\vehroutes_oneshot_meso.rou.xml";
    private static final String TRIP_TABLE = "berlin_trips_2022y_03m_22d_16h_31m_58s_526ms";
    private static final String ACTIVITY_TABLE_NAME = "core.global_activity_codes";
    private static final String MODE_TABLE_NAME = "core.global_mode_codes";
    private static final String OUTPUTNAME = "T:\\Temp\\net_attrib-new3.csv";
    private static final String EDGE_OUTPUTNAME = "T:\\Temp\\trip_attrib";

    public static void main(String[] args) {

        TPS_Sumo2MLExporter worker = new TPS_Sumo2MLExporter();
        //worker.readActivityMapping(ACTIVITY_TABLE_NAME);
        //worker.readModeCodes(MODE_TABLE_NAME);
        //worker.readTrips(TRIP_TABLE);
        //worker.loadRoutes(ROUTENAME);
        //worker.writeEdgeAttributes(EDGE_OUTPUTNAME);

        worker.exportNetAttribute(NETNAME,TAZNAME,OUTPUTNAME);
        worker.exportNetGeoJSON(NETNAME,TAZNAME,OUTPUTNAME+".json");

    }

    final int TIME_BIN = 2*60*60; //two hours in seconds
    final double[] DIST_BINS ={500,1000,2500,5000,7500,10000,15000,20000,30000,9999999};
    Map<Integer,Integer> activityMap = new HashMap<>();
    Set<Integer> possibleActivites = new HashSet<>();
    Map<Integer,String> modeMap = new HashMap<>();

    public class EdgeAttribute{
        String id ="";
        int[] timeCounts = new int[(24*60*60)/TIME_BIN];
        int[] distCount = new int[DIST_BINS.length];
        int[] activityCount = new int[possibleActivites.size()];

        public void addTrip(int time, int distIndex, int purposeIndex){
            int tIndex = ((time + 24*60*60)%(24*60*60))/TIME_BIN;
            timeCounts[tIndex]++;
            distCount[distIndex]++;
            activityCount[purposeIndex]++;
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
            s+="\n";
            return s;
        }
    }

    class TripAttribute{
        int activity;
        int mode;
    }


    Map<String, TripAttribute> trips = new HashMap<>();


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
                this.activityMap.put(zbeCode,tapasCode);
                possibleActivites.add(tapasCode);
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


    public void exportNetAttribute(String net, String tazFile, String output){


        try {

            Map<String,String> edge2TAZ = processTAZList (TAZNAME);

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

    public Map<String,String> processTAZList(String filename) throws ParserConfigurationException , SAXException , IOException {
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
        return edge2TAZ;
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

            // get vehicles
            NodeList list = doc.getElementsByTagName("vehicle");
            System.out.println("Tours to process:" + list.getLength());
            TripAttribute commuter = new TripAttribute();
            commuter.mode=2; // CAR
            commuter.activity = 211; //work, full time
            for (int temp = 0; temp < list.getLength() ; temp++) {
                if (temp % 1000 == 0) {
                    System.out.println("Processed tours:" + temp);
                }
                Node node = list.item(temp);


                if (node.getNodeType() == Node.ELEMENT_NODE) {

                    Element element = (Element) node;

                    // get edge's attribute
                    String id = element.getAttribute("id");
                    id = id.substring(0, id.lastIndexOf("_") ); //cut off the clone number
                    TripAttribute attr =null;
                    if(id.startsWith("-")){
                        //ignore commuting trips for now...
                        attr = null;//commuter;
                    }
                    else{
                        attr = this.trips.get(id);
                    }
                    if(attr!=null) {
                        int activity = 0;
                        if (activityMap.containsKey(attr.activity)) {
                            activity = activityMap.get(attr.activity);
                        }
                        int mode = attr.mode;
                        // get type
                        String routeLength = element.getAttribute("routeLength");
                        double length = Double.parseDouble(routeLength);
                        int distIndex = 0;
                        //since we have only 10 elements we just search lineary through the dist array
                        for (int i = 0; i < DIST_BINS.length; i++) {
                            if (length <= DIST_BINS[i] | i == DIST_BINS.length - 1) {
                                distIndex = i;
                                break;
                            }
                        }
                        NodeList routesByThisTour = element.getElementsByTagName("route");
                        for (int route = 0; route < routesByThisTour.getLength(); route++) {
                            Node routeNode = routesByThisTour.item(route);

                            if (routeNode.getNodeType() == Node.ELEMENT_NODE) {

                                Element routeElement = (Element) routeNode;

                                // get routes edgelist
                                String edges = routeElement.getAttribute("edges");
                                String edgeArray[] = edges.split(" ");
                                // get exit times
                                String exitTimes = routeElement.getAttribute("exitTimes");
                                String timeArray[] = exitTimes.split(" ");
                                if (edgeArray.length != timeArray.length) {
                                    System.out.println("Wrong format! " + id + " " + edges + " " + exitTimes);
                                    System.exit(-1);
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
                                }

                            }

                        }
                    }
                    else{
                        if(!id.startsWith("-")) {
                            System.out.println("TAPAS-Trip not found. ID: " + id);
                        }
                        else{
                            //ignore commuting trips
                        }
                    }
                }
            }
            System.out.println("Processed tours:" + list.getLength());
            System.out.println("Finished processing tours");
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    public void writeEdgeAttributes(String output){
        try{
            for(Map.Entry<Integer,String> e: this.modeMap.entrySet()) {
                FileWriter fw = new FileWriter(output+"_"+e.getValue()+".csv");
                fw.write("id\tt0\tt1\tt2\tt3\tt4\tt5\tt6\tt7\tt8\tt9\tt10\tt11\td0\td1\td2\td3\td4\td5\td6\td7\td8\td9\ta0\ta1\ta2\ta3\ta4\ta5\ta6\ta7\n");
                int i = 0;
                for (EdgeAttribute[] a : this.edgelist.values()) {
                    i++;
                    if (i % 1000 == 0) System.out.println("Processed edges:" + i);
                    fw.write(a[e.getKey()].toString());
                }
                fw.close();

                System.out.println("Processed edges:" + this.edgelist.size());

                System.out.println("Finished processing");
            }
        } catch (  IOException e) {
            e.printStackTrace();
        }
    }

    public void exportNetGeoJSON(String net, String tazFile, String output){
        try {

            double offX=0, offY=0, x=0,y=0;


            Map<String,String> edge2TAZ = processTAZList (tazFile);
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