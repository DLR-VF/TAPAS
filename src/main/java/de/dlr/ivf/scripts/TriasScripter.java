package de.dlr.ivf.scripts;

import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TriasScripter extends TPS_BasicConnectionClass {

    private final static String HOST = "http://v1.api.efa.de";
    private final static String FILEPATH = "T:\\Daten\\Trias_BSW\\output";
    private final static String REGION = "braunschweig";
    private final static String REQ_TIME = "2017-08-01T12:00:00";

    private final String USER_AGENT = "Mozilla/5.0";
    List<TazElement> tazes = new ArrayList<>();

    public static void main(String[] args) {
        TriasScripter worker = new TriasScripter();
        worker.readTazes(REGION);
        worker.scriptTrias(HOST, FILEPATH, REQ_TIME);
    }

    /**
     * Read all tazes for the given region from the TAPAS-db
     *
     * @param Region
     */
    void readTazes(String Region) {
        String query = "";
        try {
            query = "SELECT taz_id, st_X(taz_coordinate) as x, st_Y(taz_coordinate) as y FROM core." + Region + "_taz";
            ResultSet rs = this.dbCon.executeQuery(query, this);
            while (rs.next()) {
                TazElement taz = new TazElement();
                taz.id = rs.getInt("taz_id");
                taz.x = rs.getDouble("x");
                taz.y = rs.getDouble("y");
                tazes.add(taz);
            }

        } catch (SQLException e) {
            System.out.println("SQL error! Query: " + query);
            e.printStackTrace();
        }
    }

    /**
     * Method to script the trias interface. I generates the whole O/D-matrix derived fom the pre-loades Tazes. The diagonal elements are NOT calculated.
     *
     * @param host   The web host for the trias interface
     * @param output The folder to store the output
     * @param time   The time for the departure requestr
     */
    public void scriptTrias(String host, String output, String time) {

        FileWriter fileOut;


        try {
            URL obj = new URL(host);

            System.out.println("\nSending 'POST' request to URL : " + host);
            String fileName;
            for (TazElement o : this.tazes) {
                fileName = output + "\\result" + String.format("%04d", o.id) + ".txt";
                if (new File(fileName).isFile()) // file exists!
                    continue;
                fileOut = new FileWriter(fileName);
                for (TazElement d : this.tazes) {
                    if (o.id == d.id) // skip diagonal!
                        continue;

                    String urlParameters = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                            "<Trias xmlns=\"trias\" xmlns:siri=\"http://www.siri.org.uk/siri\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"1.0\">	" +
                            "	<ServiceRequest>		" + "		<siri:RequestTimestamp>" + time +
                            "Z</siri:RequestTimestamp>" + "		<siri:RequestorRef>mdv</siri:RequestorRef>" +
                            "		<RequestPayload>" + "			<TripRequest>" + "				<Origin>" +
                            "					<LocationRef>" + "						<GeoPosition>\n" +
                            "							<Longitude>" + o.x + "</Longitude>\n" +
                            "							<Latitude>" + o.y + "</Latitude>\n" +
                            "						</GeoPosition>" + "					</LocationRef>" +
                            "				<DepArrTime>" + time + "</DepArrTime>" + "				</Origin>" +
                            "				<Destination>" + "					<LocationRef>" +
                            "						<GeoPosition>\n" + "							<Longitude>" +
                            d.x + "</Longitude>\n" + "							<Latitude>" + d.y + "</Latitude>\n" +
                            "						</GeoPosition>" + "					</LocationRef>" +
                            "				</Destination>" + "			</TripRequest>" +
                            "		</RequestPayload>" + "	</ServiceRequest>" + "</Trias>\n";
                    HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                    //add reuqest header
                    con.setRequestMethod("POST");
                    con.setRequestProperty("User-Agent", USER_AGENT);
                    con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                    con.setDoOutput(true);

                    DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                    // Send post request
                    wr.writeBytes(urlParameters);
                    wr.flush();
                    wr.close();

                    int responseCode = con.getResponseCode();
                    System.out.println("\nSending request for origin " + o.id + " (Lon " + o.x + ", Lat " + o.y +
                            ") - destination " + d.id + " (Lon " + d.x + ", Lat " + d.y + ")");
                    //System.out.println("Post parameters : " + urlParameters);
                    System.out.println("Response Code : " + responseCode);

                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    //StringBuffer response = new StringBuffer();

                    while ((inputLine = in.readLine()) != null) {
                        //response.append(inputLine);
                        fileOut.write(inputLine + "\n");
                    }
                    in.close();
                    fileOut.flush();
                    con.disconnect();
                    //print result
                    //System.out.println(response.toString());
					/*
					// build the tmp-file
					tmpXml = new FileWriter(output+"\\tmp.xml");
					
					tmpXml.write(
							"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
							"<Trias xmlns=\"trias\" xmlns:siri=\"http://www.siri.org.uk/siri\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"0.1\">	" +
							"	<ServiceRequest>		" +
							"		<siri:RequestTimestamp>"+time+"Z</siri:RequestTimestamp>" +
							"		<siri:RequestorRef>mdv</siri:RequestorRef>" +
							"		<RequestPayload>" +
							"			<TripRequest>" +
							"				<Origin>" +
							"					<LocationRef>" +
							"						<GeoPosition>\n");
					tmpXml.write(
							"							<Longitude>"+o.x+"</Longitude>\n");		
					tmpXml.write(
							"							<Latitude>"+o.y+"</Latitude>\n");
					tmpXml.write(
							"						</GeoPosition>"+
							"					</LocationRef>"+
							"				<DepArrTime>"+time+"</DepArrTime>"+
							"				</Origin>"+
							"				<Destination>"+
							"					<LocationRef>"+
							"						<GeoPosition>\n");
					tmpXml.write(
							"							<Longitude>"+d.x+"</Longitude>\n");		
					tmpXml.write(
							"							<Latitude>"+d.y+"</Latitude>\n");
					tmpXml.write(
							"						</GeoPosition>"+
							"					</LocationRef>"+
							"				</Destination>"+
							"			</TripRequest>"+
							"		</RequestPayload>"+
							"	</ServiceRequest>"+
							"</Trias>\n");

					tmpXml.close();
					p = Runtime.getRuntime().exec("T:\\test-trias\\curl\\curl.exe -X POST -d @\"T:\\test-trias\\output\\tmp.xml\" http://test.api.efa.de");
					
					bri = new BufferedReader (new InputStreamReader(p.getInputStream()));
					bre = new BufferedReader (new InputStreamReader(p.getErrorStream()));
					while ((line = bri.readLine()) != null) {
						System.out.println(line);
						fileOut.write(line);
					}
					bri.close();
					while ((line = bre.readLine()) != null) {
						System.out.println(line);
					}
					bre.close();
					
					
			        p.waitFor();
			        */

                }
                fileOut.close();
            }

        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }// catch (InterruptedException e) {
        // TODO Auto-generated catch block
        //	e.printStackTrace();
        //}

    }

    class TazElement implements Comparable<TazElement> {
        int id;
        double x;
        double y;
        String stationName;

        @Override
        public int compareTo(TazElement o) {
            return this.id - o.id;
        }
    }
}
