package de.dlr.ivf.tapas.runtime.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import de.dlr.ivf.tapas.runtime.util.ServerControlState;

/**
 * Storage class for all server data. These are the same as in the database.
 * 
 * @author mark_ma
 * 
 */
public class SimulationServerData {
	
	
	public enum HashStatus{
		
		UNKNOWN(-2),
		INVALID(-1),
		NON_DOMINANT(0),
		DOMINANT(1),
		DEBUG(2);
		
		private final int status;
		
		HashStatus(int status){ this.status = status; }

        public int getValue() { return this.status; }
	}
	
	public enum ServerInfo{
		
		IDENTIFIER,
		RUNSINCE,
		PID,
		WORKON,
		SHUTDOWN
	}

	/**
	 * This format is used to format the date into dd.MM.yyyy kk:mm:ss.
	 */
	private static SimpleDateFormat SDF = new SimpleDateFormat(
			"dd.MM.yyyy kk:mm:ss");

	/**
	 * number of processors of the server
	 */
	private int cores;

	/**
	 * CPU usage
	 */
	private double cpu;
	/**
	 * IP address
	 */
	private InetAddress ip;
	/**
	 * name of the server
	 */
	private String name;
	/**
	 * is the server online (reachable)
	 */
	private boolean online;
	
	/**
	 * flag that indicates that this server is currently shutting down
	 */
	private boolean shuttingdown;
	

	/**
	 * Timestamp of the last update of the server
	 */
	private Timestamp timestamp;
	
	private boolean remote_control = false;

	private HashStatus hashstatus;
	
	private volatile ServerControlState serverstate;
	
	private String hashstring;
	
	@SuppressWarnings("serial")
	private Map<ServerInfo,String> serverprocessdefaultinfo = new HashMap<ServerInfo,String>() {
																	{put(ServerInfo.IDENTIFIER, "NA");
																	 put(ServerInfo.RUNSINCE, "NA");
																	 put(ServerInfo.PID, "NA");
																	 put(ServerInfo.WORKON, "NA");
																	 put(ServerInfo.SHUTDOWN, "NA");
																	}};
	private Map<ServerInfo,String> serverprocessinfo;																			
	
	/**
	 * This constructor sets all member values.
	 * 
	 * @param rs
	 *            ResultSet of a database select statement
	 * 
	 * @throws SQLException
	 *             This exception is thrown if anything with the ResultSet is
	 *             wrong, e.g. closed or too few values
	 * @throws UnknownHostException
	 *             This exception is thrown if the host can't be found
	 */
	public SimulationServerData(ResultSet rs) throws SQLException,
			UnknownHostException {
		ip = InetAddress.getByName(rs.getString("server_ip"));
		this.hashstatus = HashStatus.UNKNOWN;
		this.update(rs);
	}
	
	public SimulationServerData(InetAddress ip, String name){
		this.ip = ip;
		this.name = name;
	}

	/**
	 * @return number of processors
	 */
	public int getCores() {
		return cores;
	}
	
	public HashStatus getHashStatus() {
		return this.hashstatus;
	}
	
	public void setHashStatus(HashStatus status) {
		
		this.hashstatus = status;
	}
	
	public String getHashString() {
		
		return this.hashstring;
	}
	
	/**
	 * 
	 */
	public Map<ServerInfo,String> getServerProcessInfo(){
		if(this.serverprocessinfo == null || this.shuttingdown || !this.online)
			return this.serverprocessdefaultinfo;
		else
			return this.serverprocessinfo;
		
	}
	
	public void setServerProcessInfo(ResultSet serverinfo) throws SQLException{
		
		if(this.serverprocessinfo == null)
			this.serverprocessinfo = new HashMap<>();
		
		this.serverprocessinfo.put(ServerInfo.IDENTIFIER,serverinfo.getString("identifier"));
		this.serverprocessinfo.put(ServerInfo.RUNSINCE, serverinfo.getString("start_time"));
		this.serverprocessinfo.put(ServerInfo.PID, serverinfo.getString("p_id"));
		this.serverprocessinfo.put(ServerInfo.WORKON, serverinfo.getString("sim_key"));
					
		int hashstatus = serverinfo.getInt("sha512_status");
		
		switch (hashstatus) {		 
		 case -1: this.hashstatus = HashStatus.INVALID;
		 		  this.hashstring = "";
		 		  break;
		 case  0: this.hashstatus = HashStatus.NON_DOMINANT;
		 		  this.hashstring = serverinfo.getString("sha512");
		 		  break;
		 case  1: this.hashstatus = HashStatus.DOMINANT;
		 		  this.hashstring = serverinfo.getString("sha512");
		 		  break;
		 case  2: this.hashstatus = HashStatus.DEBUG;
		 		  this.hashstring = serverinfo.getString("sha512");
		 		  break;
		 default: this.hashstatus = HashStatus.UNKNOWN;
		 		  this.hashstring = "";
		}		
	}
	
	/**
	 * @return cpu usage
	 */
	public double getCpu() {
		return cpu;
	}

	/**
	 * @return formatted timestamp of the last server update
	 */
	public String getFormattedTimestamp() {
		if (timestamp != null)
			return SDF.format(timestamp);
		return "";
	}
	
	public ServerControlState getServerState() {
		
		return this.serverstate;
	}
	
	
	public void setServerState(ServerControlState state) {
		this.serverstate = state;
	}
	
	
	/**
	 * @return ip address
	 */
	public InetAddress getIp() {
		return ip;
	}

	/**
	 * @return server name
	 */
	public String getName() {
		return name;
	}

	/**
	 * tries to update the name of the server data.
	 * 
	 * @param name
	 * @param connection
     * @param db_table_servers
	 * @return <code>true</code> if the update was successful.
	 */
	public boolean setName(String name, Connection connection, String db_table_servers) {
		try {
			if (1 == connection.createStatement().executeUpdate(
					"UPDATE " + db_table_servers
							+ " SET server_name = '" + name
							+ "' WHERE server_ip = '" + ip.getHostAddress() + "'")) {
				this.name = name;
			} else {
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * @return timestamp of the last server update
	 */
	public Timestamp getTimestamp() {
		return timestamp;
	}

	/**
	 * @return online flag
	 */
	public boolean isOnline() {
		return online;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return name + " as " + ip + " -> " + online;
	}

	/**
	 * Updates all member values.
	 * 
	 * @param rs
	 *            ResultSet of a database select statement
	 * @throws SQLException
	 *             This exception is thrown if anything with the ResultSet is
	 *             wrong, e.g. closed or too few values
	 */
	public void update(ResultSet rs) throws SQLException {
		name = rs.getString("server_name");
		cpu = rs.getDouble("server_usage");
		online = rs.getBoolean("server_online");
		cores = rs.getInt("server_cores");
		timestamp = rs.getTimestamp("server_timestamp");
		remote_control = rs.getBoolean("remote_boot");
		if(this.serverprocessinfo != null) 
			serverprocessinfo.put(ServerInfo.SHUTDOWN, Boolean.toString(remote_control));	

		if(!online) {
			this.hashstatus = HashStatus.UNKNOWN;
			this.serverstate = ServerControlState.BOOT;
		}
	}
	
	public boolean isRemotelyControllable() {
		return this.remote_control;
	}

}
