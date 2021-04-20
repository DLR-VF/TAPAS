package de.dlr.ivf.scripts;

import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;

import java.sql.*;
import java.util.*;

public class GridResultsPreparator extends TPS_BasicConnectionClass {

    class RamonaTrip{
        int mode=-1,
                taz_start=-1, taz_end=-1,
                tapas_start_id=-1, tapas_end_id=-1;
        Point start , end ;
    }

    class Point implements Comparable<Point> {
        double x=0,y=0;
        int grid1km_id =-1;
        int grid500m_id =-1;
        public boolean equals(Object obj) {
            return this.x== ((Point)obj).x && this.y== ((Point)obj).y;
        }

        @Override
        public int compareTo(Point o) {
            if(this.x>o.x)
                return 1;
            else if(this.x<o.x)
                return -1;
            else if(this.y>o.y)
                return 1;
            else if(this.y<o.y)
                return -1;
            else //equal
                return 0;
        }
    }

    List<RamonaTrip> trips = new ArrayList<>();

    Map<Double,Map<Double,Point>> points = new HashMap<>();
    public void createRamonaTripTable(String name){

        String query = "DROP TABLE IF EXISTS " + name;
        this.dbCon.execute(query, this);

        query = "create table "+name+"(\n" +
                "                mode integer,\n" +
                "                taz_id_start integer,\n" +
                "                taz_id_end integer,\n" +
                "                grid1km_id_start integer,\n" +
                "                grid1km_id_end integer,\n" +
                "                grid500m_id_start integer,\n" +
                "                grid500m_id_end integer,\n" +
                "                start_coord geometry(POINT,4326),\n" +
                "                end_coord geometry(POINT,4326),\n" +
                "                loc_id_start integer,\n" +
                "                loc_id_end integer\n" +
                "        )";

        this.dbCon.execute(query, this);
        System.out.println("Created table: "+name);
    }

    public Point findPointInMap(Point p){
        Map<Double,Point> yPoints=this.points.get(p.x);
        if (yPoints ==null){
            yPoints = new HashMap<>();
            yPoints.put(p.y,p);
            this.points.put(p.x,yPoints);
            return p;
        }
        else{
            Point q= yPoints.get(p.y);
            if (q==null){
                yPoints.put(p.y,p);
                return p;
            }
            else{
                return q;
            }
        }
    }

    public void readTrips(String table){
        String query = "";
        int fetchSize=10000, rowCount=0;
        try {
            query = "select mode, taz_id_start, taz_id_end, loc_id_start, loc_id_end, lon_start,lat_start,lon_end,lat_end from "+table;

            Connection con = this.dbCon.getConnection(this);
            con.setAutoCommit(false);
            Statement st = con.createStatement();
            st.setFetchSize(fetchSize);
            ResultSet rs = st.executeQuery(query);

            while (rs.next()) {
                RamonaTrip tmp = new RamonaTrip();
                tmp.mode = rs.getInt("mode");
                tmp.taz_start = rs.getInt("taz_id_start");
                tmp.taz_end = rs.getInt("taz_id_end");
                tmp.tapas_end_id = rs.getInt("loc_id_start");
                tmp.tapas_end_id = rs.getInt("loc_id_end");
                Point point = new Point();

                //init start point
                point.x = rs.getDouble("lon_start");
                point.y = rs.getDouble("lat_start");
                //is it a new point?
                point = this.findPointInMap(point);
                tmp.start = point;

                //init end point
                point = new Point();
                point.x = rs.getDouble("lon_end");
                point.y = rs.getDouble("lat_end");
                //is it a new point?
                point = this.findPointInMap(point);
                tmp.end = point;

                this.trips.add(tmp);
                rowCount++;
            }
            rs.close();
            st.close();
            con.setAutoCommit(true);
            System.out.println("Read "+ this.trips.size()+" trips with "+this.points.size()+" different locations from "+table);
        } catch (SQLException e) {
            System.err.println("Error during query: " + query);
            e.printStackTrace();
        }
    }

    public void readGeolocations(String geo1kmTable, String geo500mTable, String geo1kmTableLarge, String geo500mTableLarge){
        String query ="";
        int found1km =0, lost1km=0,found500m =0, lost500m=0,count =0;
        try {
            query = "Select gid from " + geo1kmTable + " where st_within(st_transform(st_setsrid(st_makepoint(?,?),4326),3035),the_geom)";
            PreparedStatement ps1km = this.dbCon.getConnection(this).prepareStatement(query);
            query = "Select gid from " + geo500mTable + " where st_within(st_transform(st_setsrid(st_makepoint(?,?),4326),3035),the_geom)";
            PreparedStatement ps500m = this.dbCon.getConnection(this).prepareStatement(query);
            ResultSet rs;
            for(Map<Double,Point> m: this.points.values()) {
                for (Point p : m.values()) {
                    count ++;
                    if(p.grid1km_id<0) {
                        ps1km.setDouble(1, p.x);
                        ps1km.setDouble(2, p.y);
                        rs = ps1km.executeQuery();
                        if (rs.next()) {
                            p.grid1km_id = rs.getInt("gid");
                            found1km++;
                            rs.close();
                        } else {
                            //try the large table
                            System.out.println("Trying large table at: " + p.x + "/" + p.y);
                            rs.close();
                            query = "Select gid from " + geo1kmTableLarge + " where st_within(st_transform(st_setsrid(st_makepoint(" + p.x + "," + p.y + "),4326),3035),the_geom)";
                            rs = this.dbCon.executeQuery(query, this);
                            if (rs.next()) {
                                p.grid1km_id = rs.getInt("gid");
                                found1km++;
                                rs.close();
                            } else {
                                rs.close();
                                System.out.println("nothing in 1km grid found at: " + p.x + "/" + p.y);
                                lost1km++;
                            }
                        }
                        rs.close();
                    }
                    if(p.grid500m_id<0) {
                        ps500m.setDouble(1, p.x);
                        ps500m.setDouble(2, p.y);
                        rs = ps500m.executeQuery();
                        if (rs.next()) {
                            p.grid500m_id = rs.getInt("gid");
                            found500m++;
                            rs.close();
                        } else {
                            //try the large table
                            System.out.println("Trying large table at: " + p.x + "/" + p.y);
                            rs.close();
                            query = "Select gid from " + geo500mTableLarge + " where st_within(st_transform(st_setsrid(st_makepoint(" + p.x + "," + p.y + "),4326),3035),the_geom)";
                            rs = this.dbCon.executeQuery(query, this);
                            if (rs.next()) {
                                p.grid500m_id = rs.getInt("gid");
                                found500m++;
                                rs.close();
                            } else {
                                rs.close();
                                System.out.println("nothing in 1km grid found at: " + p.x + "/" + p.y);
                                lost500m++;
                            }
                        }
                        rs.close();
                    }
                }
            }
            System.out.println("Found "+found1km +" 1km geolocs.");
            if(lost1km>0)
                System.out.println("But didn't found 1km geolocs for "+lost1km);
            System.out.println("Found "+found500m +" 500m geolocs.");
            if(lost500m>0)
                System.out.println("But didn't found 500m geolocs for "+lost500m);
        }catch (SQLException e) {
            System.err.println("Error during query: " + query);
            e.printStackTrace();
        }
    }

    public void insertTrips(String table){
        String query ="";
        try {
            query = "INSERT INTO "+table+" values(?,?,?,?,?,?,?,st_setsrid(st_makepoint(?,?),4326),st_setsrid(st_makepoint(?,?),4326),?,?)";
            PreparedStatement ps = this.dbCon.getConnection(this).prepareStatement(query);
            ResultSet rs;
            int size=10000, count=0;
            for(RamonaTrip t : this.trips){
                ps.setInt(1,t.mode);
                ps.setInt(2,t.taz_start);
                ps.setInt(3,t.taz_end);
                ps.setInt(4,t.start.grid1km_id);
                ps.setInt(5,t.end.grid1km_id);
                ps.setInt(6,t.start.grid500m_id);
                ps.setInt(7,t.end.grid500m_id);
                ps.setDouble(8,t.start.x);
                ps.setDouble(9,t.start.y);
                ps.setDouble(10,t.end.x);
                ps.setDouble(11,t.end.y);
                ps.setInt(12,t.tapas_start_id);
                ps.setInt(13,t.tapas_end_id);
                ps.addBatch();
                count++;
                if(count%size==0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
            System.out.println("commited "+count+" trips");

        }catch (SQLException e) {
            System.err.println("Error during query: " + query);
            e.printStackTrace();
        }
    }



    /**
     * @param args
     */
    public static void main(String[] args) {
        GridResultsPreparator worker = new GridResultsPreparator();
        String[] results={"ramona_s3_trips_georeferenced_autogen","ramona_s3a_trips_georeferenced_autogen"};
        String[] sims={"berlin_trips_2020y_11m_18d_10h_11m_18s_042ms","berlin_trips_2020y_11m_18d_17h_07m_19s_860ms"};



        for(int i=0; i < results.length;++i) {
            worker.trips.clear();
            worker.readTrips(sims[i]);
            worker.readGeolocations(args[2], args[3], args[4], args[5]);
            worker.createRamonaTripTable(results[i]);
            worker.insertTrips(results[i]);
        }

    }
}
