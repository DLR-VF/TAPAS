package de.dlr.ivf.tapas.tools.persitence.db.dbImport;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Class to correct double person ids, which where found. 
 * Not needed anymore, unless we import faulty data again
 * @author hein_mh
 *
 */
public class DBCorrector {

	private Connection connection = null;
	private Statement statement = null;

	class Person{
		public int p_id;
		public int p_hh_id;
		public int p_group;
		public int p_sex;
		public int p_age;
		public int p_age_stba;
		public int p_work_id;
		public double p_working;
		public int p_abo;
		public double p_budget_pt;
		public double p_budget_it;
		public double p_budget_it_fi;
		public String p_key;
		
		Person(ResultSet rs){
			try{
				this.p_id 			= rs.getInt		( 1);
				this.p_hh_id		= rs.getInt		( 2);
				this.p_group		= rs.getInt		( 3);
				this.p_sex 			= rs.getInt		( 4);
				this.p_age 			= rs.getInt		( 5);
				this.p_age_stba		= rs.getInt		( 6);
				this.p_work_id 		= rs.getInt		( 7);
				this.p_working		= rs.getDouble	( 8);
				this.p_abo 			= rs.getInt		( 9);
				this.p_budget_pt	= rs.getDouble	(10);
				this.p_budget_it 	= rs.getDouble	(11);
				this.p_budget_it_fi = rs.getDouble	(12);
				this.p_key 			= rs.getString	(13);
			}
			catch(SQLException e){
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * method to open the db connection and to create the sqlStatement
	 */
	private void openConnection(String ip, int port, String DBName, String user, String passwd ){
		String url = "jdbc:postgresql://" + ip + ":"
		+ port+ "/"+ DBName;
		
		try{
			connection = java.sql.DriverManager.getConnection(url, user, passwd);	
			connection.setAutoCommit(true);
			statement = connection.createStatement();
		}
		catch(SQLException e){
			e.printStackTrace();
		}
	}
	
	
	private void closeConnection(){
		try{
			if(statement!=null){
				statement.close();
				statement =null;				
			}
			if(connection!=null){
				connection.close();
				connection = null;				
			}
		}
		catch(SQLException e){
			e.printStackTrace();
		}
	}

	private boolean correctIDinTable(Person p, String table, int newID){
		boolean result = false;
		
		try{
			String query = "UPDATE " + table + " SET p_id="+ newID 
			+" WHERE p_id= " + p.p_id 
			+ " AND p_hh_id=" + p.p_hh_id
			+ " AND p_key='" + p.p_key+"'";
			result = this.statement.executeUpdate(query)>0;
		}catch (SQLException e){
			e.printStackTrace();
		}
		
		return result;
	}
	
	private void correctIDs(String table){
		
		int idNew, oldMaxID, oldMinID, i;
		String query;
		ResultSet rs;
		Person p;
		boolean firstPassed, changed;
		
		try{
			query = "SELECT p_id FROM "+table+" ORDER BY p_id DESC LIMIT 1";
			rs = this.statement.executeQuery(query);
			if (rs.next()) {
				//get highest id in db and incement by one 
				
				idNew = rs.getInt(1) +1;
				
				query = "SELECT p_id FROM "+table+" WHERE p_key = 'MID2005_Y2030' ORDER BY p_id DESC LIMIT 1";
				rs = this.statement.executeQuery(query);
				if (rs.next()) {
					oldMaxID = rs.getInt(1) ;
				}
				else{
					return;
				}
				
				query = "SELECT p_id FROM "+table+" WHERE p_key = 'MID2005_Y2030' ORDER BY p_id  LIMIT 1";
				rs = this.statement.executeQuery(query);
				if (rs.next()) {
					oldMinID = rs.getInt(1) ;
				}
				else{
					return;
				}
				
				oldMinID=3607553; // manually determined
				
				//get all persons
				for(i = oldMinID; i<=oldMaxID; i=changed?i:i+1){
					changed = false;
					query = "SELECT * FROM "+table+" WHERE p_key = 'MID2005_Y2030' and p_id = '" + i +"' order by p_id";
					rs = this.statement.executeQuery(query);
					if (rs.next()){
						firstPassed = false;
						do{
							p = new Person(rs);
							if(firstPassed){
								System.out.println("\rID "+rs.getInt(1)+" not ok.");
								if(!this.correctIDinTable(p, table, idNew++)){
									System.out.println("Fatal SQL-Error");
									break;
								}
								changed = true;
								break;
							}
							else{
								
								firstPassed = true;
							}
						}while(rs.next());			
					}
				}
			}
		}
		catch(SQLException e){
			e.printStackTrace();
		}
		
	}
	
	/**
	 * main method
	 * @param args no args
	 */
	public static void main(String[] args) {
		DBCorrector correct = new DBCorrector();
		
		correct.openConnection("129.247.221.173", 5433, "tapas", "postgres", "postgres");
		
		correct.correctIDs("core.berlin_persons");
		
		correct.closeConnection();
	}

}
