import java.io.*;
import java.lang.reflect.Field;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * @author npress
 *
 */
public abstract class Alert{
	
	
	/**
	 * @author npress
	 *
	 */
	protected static final long DAYS_PER_MONTH = 30;
	private static final int TIME_INTERVAL_FOR_POLLING=3600; //duration in seconds after which we poll
	protected static final String ALERT_SYSTEM_IP="10.42.42.110";
	protected long alert_id;
	protected Connection connect = null;
	protected Statement statement = null;
	protected PreparedStatement preparedStatement = null;
	protected ResultSet resultSet, resultSet2 = null;
	
	protected static String user="npress", password="npress123";
	protected long count; //the number of errors/activations/accounts to be polled
	protected char comparative_op; //comparison operator to use for determining threshold or upperbound
	protected long time_quantity; //number of time units in which to poll
	protected String time_unit; //the time_unit in which to poll
	protected ArrayList<String[]> game_env; //array of two-index array
	protected String field_to_query;
	protected long query_result;
	protected String count_unit;
	protected long time_interval; //seconds used to calculate the rule
	protected String table;
	protected long[] subscribers; //holds the admin_user_id's of subscribers
	protected Date last_fulfilled;
	protected char paused;
	protected boolean exceptionFlag;
	
	protected int num_notifications;
	Alert(long count, String count_unit, char comparative_op, long time_quantity, 
			String time_unit, String field_to_query, 
			ArrayList<String[]> game_env, String table, long[] subscribers, char paused){
		
		this.exceptionFlag=false;
		this.count=count;
		this.comparative_op=comparative_op;
		this.time_quantity=time_quantity;
		this.time_unit=time_unit;
		this.field_to_query=field_to_query;
		this.game_env = game_env;
		this.query_result=0;
		this.count_unit=count_unit;
		this.table=table;
		this.subscribers=subscribers;
		this.paused= paused;
		calc_time_interval();
		
	}
	public Alert(){
		System.out.println("Calls implicit super constructor.");
	}
	
	protected void storeAlert(){
		try{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		
			// Setup the connection with the DB
			
			this.connect = DriverManager.getConnection("jdbc:mysql://"+this.ALERT_SYSTEM_IP+"/?allowMultiQueries=true", Alert.user, Alert.password);
	
			// Statements allow to issue SQL queries to the database
			statement = connect.createStatement();
			
			// Result set get the result of the SQL query
			statement.executeUpdate("INSERT INTO alert_system.alerts(`alert_type`, " +
					"`count`, `count_unit`, `comparative_op`, `time_quantity`, " +
					"`time_unit`, `field_to_query`, `field2_to_query`, `table`, " +
					"`group/provider`, `time_interval`, `paused`)" 
					+ 
            " VALUES ('"+this.getClass().getName()+"', '"+this.count+"', '"+
            this.count_unit+"', '"+this.comparative_op+"', '"+this.time_quantity+
            "', '"+this.time_unit+"', '"+this.field_to_query+"', '"+
            getField2ToQuery()+"', '"+this.table+"', '"+getGroup()+"', '"+
            this.time_interval+"', '"+this.paused+"') "
            ,Statement.RETURN_GENERATED_KEYS);
			
			resultSet=statement.getGeneratedKeys();           
	        while (resultSet.next()) {
				
				this.alert_id=resultSet.getLong(1);
				
			}
	        
	        //store game environment information for the alert
	        for(int i=0; i<this.game_env.size(); i++){
	        	String[] game_envAry=this.game_env.get(i);
	        	String game =game_envAry[0]; //store game name with alert id number
	        	String env=game_envAry[1]; //store game env with alert id number
	        	String ip=game_envAry[2];  //store ip for database with alert id number
	        	String user=game_envAry[3];
	        	String password=game_envAry[4];
	        	String gameInsertStr="INSERT INTO alert_system.alert_game_environments(`alert_id`, `game`, `environment`, `ip_address`, `user`, `password`)" + 
                " VALUES ('"+this.alert_id+"', '"+game+"', '"+env+"', '"+ip+"', '"+user+"', '"+password+"')";
	        	
	        	statement.executeUpdate(gameInsertStr
	                    ,Statement.RETURN_GENERATED_KEYS);
	        	
	        }
	        
	        for(int i=0; i<this.subscribers.length; i++){
	        	
	        	
	        	statement.executeUpdate("INSERT INTO alert_system.subscribers(`alert_id`, `admin_user_id`)" + 
	                    " VALUES ('"+this.alert_id+"', '"+this.subscribers[i]+"')"
	                    ,Statement.RETURN_GENERATED_KEYS);
	        	
	        }
	        
		}
		catch (Exception e) {
			System.err.println("Error printing the Alert record to the db!");
			System.err.print(e.getMessage());
			System.exit(1);
		} 
		finally {
			close();
		}
		
	}
	protected String getGroup() {
		// TODO Auto-generated method stub
		return "";
	}
	/*
	 * Method override in the child class, provided that field2_to_query exists.
	 */
	protected String getField2ToQuery(){
		if(this.getClass().isInstance("PercentAlert")){
			System.out.println("Is a PercentAlert class.");
		}
		return "";
	}
	Alert(long count, String count_unit, char comparative_op, long time_quantity, 
			String time_unit, String field_to_query, 
			ArrayList<String[]> game_env, String table, long[] subscribers, long alert_id,char paused){
		
		this(count, count_unit, comparative_op, time_quantity, 
				time_unit, field_to_query, game_env, table, subscribers, paused);
		this.alert_id=alert_id;
		
		
	}
	
	
	protected void calc_time_interval(){
		time_unit=time_unit.toLowerCase();
		if(time_unit.equals("second")){
			time_interval=time_quantity;
		}
		else if (time_unit.equals("minute")){
			time_interval=time_quantity*60;
		}
		else if(time_unit.equals("hour")){
			time_interval=time_quantity*3600;
		}
		else if (time_unit.equals("day")){
			time_interval=time_quantity*24*3600;
		}
		else if(time_unit.equals("month")){
			time_interval=time_quantity*24*3600*this.DAYS_PER_MONTH;
		}
		else System.err.print("The user has not selected a valid time unit.");
		
	}
	
	public void finalize()throws Throwable{
		time_unit=null; //the time_unit in which to poll
		game_env=null; //array of two-index array
		field_to_query=null;
		count_unit=null;
		super.finalize();
	}
	/*
	 * toString() - returns a string representing the query the AdminUser will
	 * perform when he or she calls getNotified()
	 */
	public String toString(){
		return ""+count+" "+count_unit+" is "+ this.comparative_op +
		" [query_result] in last "+time_quantity+ " " +time_unit+"(s)" ;
		
		//TODO: add game_environment vector
	}
	public String getNotified(){
		String result=this.getNotificationData();
		if(result!=null){
			result=this.getClass().getName()+" #"+this.alert_id+" "+ result;
			if(this.num_notifications==this.game_env.size())
				update("last_fulfilled");  //if all game-environment combinations were 
										   //updated in getNotificationData()
			System.out.println("The query is fulfilled and alert id is "+ this.alert_id);
		}
		return result;
	
	}
	public String getNotificationData() {
		this.exceptionFlag=false;
		String result=null;
		boolean printNotification;
		this.query_result=0; //zeroing out query_result from former queries
		Object[] game_env_ary = this.game_env.toArray();
		String database, ip_address, game_user, game_password, game_name, environment;
		num_notifications=0;
		for(int i=0; i<game_env_ary.length; i++){
			printNotification=false;
			try {
				game_name=((String[]) game_env_ary[i])[0];
				environment=((String[]) game_env_ary[i])[1];
				// This will load the MySQL driver, each DB has its own driver
				Class.forName("com.mysql.jdbc.Driver").newInstance();
				// Setup the connection with the DB
				
				database= getDBName(((String[]) game_env_ary[i])[0], ((String[]) game_env_ary[i])[1]);
				ip_address=((String[]) game_env_ary[i])[2];
				game_user=((String[]) game_env_ary[i])[3];
				game_password=((String[]) game_env_ary[i])[4];
				this.connect = DriverManager.getConnection("jdbc:mysql://"+ip_address+"/?allowMultiQueries=true", game_user, game_password);
				
				// Statements allow to issue SQL queries to the database
				statement = connect.createStatement();
				//check if we should poll this game_environment combination:
				String query=
					"select * from (alert_system.alert_game_environments JOIN " +
					"alert_system.alerts ON alert_game_environments.alert_id = " +
					"alerts.id )" +
					"where ip_address='"+ip_address+"' AND game='"+game_name+"'" +
					"AND alert_id='"+this.alert_id+"' AND environment='"
					+environment+"' AND " +
				"("+
					"(DATE_ADD(UTC_TIMESTAMP(), INTERVAL -alerts.time_interval SECOND) >= "+
					"alert_game_environments.last_fulfilled)" +
					" OR (alert_game_environments.last_fulfilled is NULL)" +
				")";
				resultSet=statement.executeQuery(query);
				
				if(!resultSet.next()){
					System.out.println("This game_environment is already fulfilled for " +
							"the " +
							"time interval of "+ this.time_quantity +" "+ this.time_unit+"(s).");
					continue;
							
				}
				// Result set get the result of the SQL query
				System.out.println("\nQuery String: "+this.getQueryString(database));
				statement.execute(this.getQueryString(database));
				resultSet=statement.getResultSet();
				while(resultSet==null){  
					//allows for the user to set the database as the first statement
				    statement.getMoreResults();
					resultSet=statement.getResultSet();
				}
				while (resultSet.next()) {
					
					this.query_result = resultSet.getInt(1);
				}
				System.out.println("The query result is : "+ this.query_result);
				switch(this.comparative_op){
				case '<':
					if(count<query_result)
						printNotification=true;
					break;
				case '>':
					if(count>query_result){
						printNotification=true;
					}
					break;
				case '=':
					if(count == query_result)
						printNotification=true;
					break;
				}
				if(printNotification){
					
					//assign last_fulfilled	
					result=(result!=null)?result: "";
					result+= "On game: "+((String[]) game_env_ary[i])[0]+" running on environment: "
					+((String[]) game_env_ary[i])[1]+", "+count+" "+getCountUnit()+" is "+ this.comparative_op +
					" "+query_result+" in last "+time_quantity+ " " +time_unit + "(s)\n" ;
					//this query is true for this particular game_environment combination
					//set last_fulfilled
					statement.executeUpdate("update " +
							"alert_system.alert_game_environments set last_fulfilled=UTC_TIMESTAMP() " +
							"where ip_address='"+ip_address+"' AND game='"+game_name+"'" +
							"AND alert_id='"+this.alert_id+"' AND environment='"+environment+"'"
							);
					this.num_notifications++;
				}
				
				
			}
			catch (Exception e) {
				System.err.println("ERROR in "+this.getClass().getName()+".getNotificationData() for Alert #"+this.alert_id);
				System.err.print(e.getMessage());
				this.exceptionFlag=true;
			} 
			finally {
				close();
			}
			}
		return result;
		
		
	}
	protected String getCountUnit() {
		
		return this.count_unit;
	}
	/*
	 * Returns false if the time_interval in the rule exceeds the difference in the time
	 * between now and last_fulfilled.
	 */
	/*protected boolean fulfilledTooRecently() {
		try{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		
			// Setup the connection with the DB
			
			this.connect = DriverManager.getConnection("jdbc:mysql://"+this.ALERT_SYSTEM_IP+"/?allowMultiQueries=true", "npress", "npress123");
	
			// Statements allow to issue SQL queries to the database
			statement = connect.createStatement();
			
			// Result set get the result of the SQL query
			resultSet=statement.executeQuery("select from alert_system.alerts SET "+setString+" WHERE id="+this.alert_id);
	  	        
		}
		catch (Exception e) {
			System.err.println("Error searching for last_fulfilled in Alert record.");
			System.err.print(e.getMessage());
			System.exit(1);
		} 
		finally {
			close();
		}
		return false;
	}*/
	protected void update(String field){
		String setString="";
		if(field.equals("last_fulfilled")) 
			setString=field+"=UTC_TIMESTAMP()";
		
		try{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		
			// Setup the connection with the DB
			
			this.connect = DriverManager.getConnection("jdbc:mysql://"+this.ALERT_SYSTEM_IP+"/?allowMultiQueries=true", Alert.user, Alert.password);
	
			// Statements allow to issue SQL queries to the database
			statement = connect.createStatement();
			
			// Result set get the result of the SQL query
			statement.executeUpdate("UPDATE alert_system.alerts SET "+setString+" WHERE id="+this.alert_id);
	  	        
		}
		catch (Exception e) {
			System.err.println("Error updating the Alert record in the db!");
			System.err.print(e.getMessage());
			System.exit(1);
		} 
		finally {
			close();
		}
	}
	protected String getDBName(String gameName, String envir) {
		return gameName+"_"+ envir;
		
	}
	protected abstract String getQueryString(String database);
	protected void close() {
		try {
			if (resultSet != null) {
				resultSet.close();
			}

			if (statement != null) {
				statement.close();
			}

			if (connect != null) {
				connect.close();
			}
		} catch (Exception e) {

		}
	}
	
		
	
}
