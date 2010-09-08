import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.mail.MessagingException;


public class AlertSystem {
	protected static Statement statement;
	protected static Statement statement2;
	protected static Statement statement3;
	protected static Connection connect;
	protected static ResultSet resultSet;
	protected ArrayList<Long> alerts; //represents all alerts present in db
	protected static SendMailUsingAuthentication smtpMailSender;
	protected static String from_address="development@sleepygiant.com";
	protected static String alert_system_ip="10.42.42.110"
		, alert_system_user="npress"
		, alert_system_passwd="npress123";
	
	
	public AlertSystem(){
		this(null, null, null);
	}
	public AlertSystem(String alert_system_ip, String alert_system_user, String alert_system_passwd) {
		
		if(alert_system_ip!=null && alert_system_user!=null && alert_system_passwd!=null)
		{	
			alert_system_ip=alert_system_ip;
			alert_system_user=alert_system_user;
			alert_system_passwd= alert_system_passwd;
		}
		else{
			//read in the alert system ip, username and password from a file.
			
			
			try{
				BufferedReader in = new BufferedReader(new FileReader("alert_system.txt")); 
				String str; 
				if((str = in.readLine()) != null)
					this.alert_system_ip=str;
				if((str = in.readLine()) != null)
					this.alert_system_user=str;
				if((str = in.readLine()) != null)
					this.alert_system_passwd=str;
				
				in.close(); 
				
				
			}
			catch(IOException e){
				System.err.println("Error reading the file containing alert_system_ip, " +
						"alert_system_user and alert_system_passwd.");
				System.err.println(e.getMessage());
			}
			
		}
		smtpMailSender = new SendMailUsingAuthentication();
		alerts= new ArrayList<Long>();
		try{
			Class.forName("com.mysql.jdbc.Driver").newInstance();		
				// Setup the connection with the DB		
			connect = DriverManager.getConnection("jdbc:mysql://"+Alert.ALERT_SYSTEM_IP+"/?allowMultiQueries=true", "npress", "npress123");
		
				// Statements allow to issue SQL queries to the database
			statement= connect.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
	                ResultSet.CONCUR_READ_ONLY, 
	                ResultSet.HOLD_CURSORS_OVER_COMMIT);
			
		}
		catch (Exception e) {
			System.err.println("Error in connecting to the database.");
			System.err.print(e.getMessage());
			System.exit(1);
		} 
			//no close is performed until the object AlertSystem.
		
	}
	public void finalize(){
		closeAll();
	}
	protected void closeAll() {
		try {
			if (resultSet != null) {
				resultSet.close();
			}

			if (statement != null) {
				statement.close();
			}
			if (statement2 != null) {
				statement2.close();
			}
			if (statement3 != null) {
				statement3.close();
			}
			if (connect != null) {
				connect.close();
			}
		} catch (Exception e) {

		}
	}
	
	
	
	protected Alert createAlertFromID(long id){
		try{
			statement= connect.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
	                ResultSet.CONCUR_READ_ONLY, 
	                ResultSet.HOLD_CURSORS_OVER_COMMIT);
			statement.execute("select `alert_type`, `count`, `comparative_op`, `time_quantity`, " +
					"`time_unit`, `group/provider`, `count_unit`, `table`, " +
					"`field_to_query`, `aggregate`, `paused`, `join_clause`"
					+" from alert_system.alerts WHERE id="+id);
			resultSet=statement.getResultSet();
			
			String alert_type=null;
			long count=-1;
			char comparative_op=' ';
			long time_quantity=-1;
			String time_unit= null;
			ArrayList<String[]> game_env= new ArrayList<String[]>();
			ArrayList<Long> sublistAL= new ArrayList<Long>();
			long[] sublist;
			String group= null;
			String count_unit=null, table=null, field_to_query=null, 
			aggregate=null, join_clause=null;
			char paused='0';
			while(resultSet.next()){
				alert_type=resultSet.getString("alert_type");
				count=resultSet.getLong("count");
				comparative_op=resultSet.getString("comparative_op").charAt(0);
				time_quantity=resultSet.getLong("time_quantity");
				time_unit=resultSet.getString("time_unit");
				group=resultSet.getString("group/provider");
				count_unit=resultSet.getString("count_unit");
				table=resultSet.getString("table");
				field_to_query=resultSet.getString("field_to_query");
				aggregate=resultSet.getString("aggregate");
				paused=resultSet.getString("paused").charAt(0);
				join_clause=resultSet.getString("join_clause");
			}
			statement2 = connect.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
	                ResultSet.CONCUR_READ_ONLY, 
	                ResultSet.HOLD_CURSORS_OVER_COMMIT);
			resultSet = statement2.executeQuery("select `game`, `environment`, `ip_address`" +
					",`user`, `password`"
					+" from alert_system.alert_game_environments WHERE alert_id="+id);
			
					
			
			while(resultSet.next()){
				String[] gameEnvAry=new String[5];
				gameEnvAry[0]=resultSet.getString("game");
				gameEnvAry[1]=resultSet.getString("environment");
				gameEnvAry[2]=resultSet.getString("ip_address");
				gameEnvAry[3] = resultSet.getString("user");
				gameEnvAry[4] = resultSet.getString("password");
				game_env.add(gameEnvAry);
			
			}
			/**********Seek subscribers****/
			statement3=connect.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
	                ResultSet.CONCUR_READ_ONLY, 
	                ResultSet.HOLD_CURSORS_OVER_COMMIT);
			resultSet=statement3.executeQuery("select admin_user_id"
					+" from alert_system.subscribers WHERE alert_id="+id);
			
			
			while(resultSet.next()){
				sublistAL.add(resultSet.getLong("admin_user_id"));
			}
			sublist=new long[sublistAL.size()];
			for(int i=0; i<sublistAL.size(); i++)
				sublist[i]=((Long)sublistAL.get(i)).longValue();
			
			/**********seek conditions************/
			String [] conditions=null;
			if(alert_type.equals("CustomizedAlert")|| alert_type.equals("CustomizedJoinAlert"))
			{
				statement3=connect.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
			
	                ResultSet.CONCUR_READ_ONLY, 
	                ResultSet.HOLD_CURSORS_OVER_COMMIT);
				resultSet=statement3.executeQuery("select `condition` from " +
						"alert_system.customized_alert_conditions where alert_id="+id);
				ArrayList<String> conditionsAL=new ArrayList<String>(2);
				
				while(resultSet.next()){
					conditionsAL.add(new String (resultSet.getString("condition")));
				}
				conditions= new String[conditionsAL.size()];
				conditionsAL.toArray(conditions);
			}
			
			return AdminUser.instantiateAlertWithID(alert_type, count, comparative_op, time_quantity, 
					time_unit, game_env, group, sublist, count_unit, id, conditions,
			aggregate, field_to_query, table, paused, join_clause);
		}
		catch (Exception e) {
			System.err.println("Error seeking data for alert id "+ id);
			System.err.print(e.getMessage());
			System.exit(1);
		} 
		
		finally {
			this.closeStatements();
		}
		return null;
	}
	
	/*
	 * Checks all alerts to determine the notification string of each.
	 * Stores the notification string in the table alert_system.admin_users
	 * for each person who has a relevant alert that she or he is subscribed to.
	 */
	public void pollAlerts(){
		this.alerts=populateAlertsToNotify();
		Alert alert;
		//stores an alert and notification string for each alert that
		//ought to be notified
		ArrayList<Object[]> alertsToNotify=new ArrayList<Object[]>(); 
		Object[] objectAry=new Object[2]; 
		String notificationStr;
		
		
		for(int i=0; i<alerts.size(); i++){
			
			alert=createAlertFromID(alerts.get(i).longValue()); 
			//constructs a new Alert from information in the database 
			//using a given Alert ID
			
			System.out.println("alerts in pollAlerts is: "+ alert.alert_id);
			notificationStr=alert.getNotified();
			if(notificationStr!=null){
				objectAry[0]=alert;
                objectAry[1]=notificationStr;
				alertsToNotify.add(objectAry.clone());
			}
		}
		try{
			statement= connect.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
	                ResultSet.CONCUR_READ_ONLY, 
	                ResultSet.HOLD_CURSORS_OVER_COMMIT);
			
			// add the string to notification next to this AdminUser's id
			// in the admin_users table , then set sent to false
			char sent;
			for(int i=0; i<alertsToNotify.size(); i++){
				alert= (Alert)((Object[])alertsToNotify.get(i))[0];
				for (int j=0; j<alert.subscribers.length; j++){
						
						notificationStr = (String)((Object[])alertsToNotify.get(i))[1];
						System.out.println("The notification is: "+ notificationStr);
						resultSet=statement.executeQuery("SELECT sent FROM alert_system.admin_users WHERE id="+alert.subscribers[j]);
						if(resultSet.next()){
							sent= resultSet.getString("sent").charAt(0);
					    
							if(sent =='1'){
								
								
								System.out.println("The subscriber is "+ alert.subscribers[j]);
								System.out.println("The alert id is "+alert.alert_id);
								
								statement.execute("update alert_system.admin_users "+
										"set notification='"+notificationStr+"', sent='0' "+
										"WHERE id="+alert.subscribers[j]);	
							}
							else {
								System.out.println("The subscriber is "+ alert.subscribers[j]);
								System.out.println("The alert id is "+alert.alert_id);
								
									statement.execute("update alert_system.admin_users "+
										"set `notification`=CONCAT(`notification`, '"+notificationStr+"') "+
										"WHERE id="+alert.subscribers[j]);
								//statement.execute("update alert_system.admin_users set notification=CONCAT(`notification`, \"On game: crimecraft running on environment: development, 5 percent is < 99 in last 24 month(s)\n\") WHERE id=1");
							}
						}
						else System.err.println("No result set from sent inquiry.");
						
				}
			}
			sendEmails();
			
		}
		catch (Exception e) {
			System.err.println("Error polling alerts in pollAlerts().");
			System.err.print(e.getMessage());
			System.exit(1);
		} 
		finally {
			closeStatements();
		}
		return;
	}
	
	protected void sendEmails(){
		
		try{
			statement2= connect.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
			ResultSet.CONCUR_READ_ONLY, 
            ResultSet.HOLD_CURSORS_OVER_COMMIT);
			//for each subscriber in the table, send out the notification in an email
			//set the sent string to true
			statement3= connect.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY, 
		            ResultSet.HOLD_CURSORS_OVER_COMMIT);;
			resultSet=statement3.executeQuery("SELECT id, email, name, notification from alert_system.admin_users WHERE sent='0'");
			long user_id;
			String sub_email, notification, sub_name;
			while(resultSet.next()){
				sub_email=resultSet.getString("email");
				notification=resultSet.getString("notification");
				sub_name=resultSet.getString("name");
				user_id=resultSet.getLong("id");
				smtpMailSender.postMail(sub_email, "Your Notification Digest", "Dear "+sub_name+",\n\n"+notification+"\nThese results were generated by the AlertSystem.", AlertSystem.from_address);
				statement2.executeUpdate("UPDATE alert_system.admin_users set sent='1' where id= '"+user_id+"'" );	
			}
		}
		catch (Exception e) {
			System.err.println("Error sending emails in sendEmails().");
			System.err.print(e.getMessage());
			System.exit(1);
		} 
		finally {
			closeStatements();
		}
		
	}
	protected ArrayList<Long> populateAlertsToNotify() {
		//zero the alerts owned by this administrator
		this.alerts.clear();
		try{
			statement= connect.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
	                ResultSet.CONCUR_READ_ONLY, 
	                ResultSet.HOLD_CURSORS_OVER_COMMIT);
			
		String queryString="select id, " +
			"DATE_ADD(UTC_TIMESTAMP(), INTERVAL -alerts.time_interval SECOND), "+
			"alerts.last_fulfilled "+
			" FROM alert_system.alerts "+ 
			"WHERE (" +
				"(alerts.paused ='0') AND " +
				"("+
					"(DATE_ADD(UTC_TIMESTAMP(), INTERVAL -alerts.time_interval SECOND) >= "+
					"alerts.last_fulfilled)" +
					" OR (alerts.last_fulfilled is NULL)" +
				")" +
			")";
			resultSet= statement.executeQuery(queryString);
			
			resultSet=statement.getResultSet();
			
			while(resultSet.next()){
				long alertID= resultSet.getLong("id");
				System.out.println("The alertID added in populateAlertsToNotify: "+ alertID);
				alerts.add(new Long(alertID));	
			}   
			
			
		}
		catch (Exception e) {
			System.err.println("Error seeking alert id's to poll in populateAlertsToNotify().");
			System.err.print(e.getMessage());
			System.exit(1);
		} 
		finally {
			closeStatements();
		}
		return alerts;
	}
	private static void closeStatements() {
		try {
			if (resultSet != null) {
				resultSet.close();
			}

			if (statement != null) {
				statement.close();
			}
			if (statement2 != null) {
				statement2.close();
			}
			if (statement3 != null) {
				statement3.close();
			}
			
		} catch (Exception e) {

		}
	}
	/*
	 * Stores a new admin user in the database, calling the constructor
	 * from AdminUSer, which denies users with the same email account from
	 * being stored in the database.
	 */
	public AdminUser createAdminUser(String email, String name){
		return new AdminUser(email, name);
	}
	/* create an AdminUser from an email search string.  If the given user does not
	 * exist, returns null.  Otherwise, calls constructor in
	 * AdminUser, which inserts the email address, name and AdminUser id.
	 * 
	 */
	public AdminUser getAdminUser(String email){
		AdminUser user = null;
		try{
			statement=connect.createStatement();
			resultSet= statement.executeQuery("select id from alert_system.admin_users where " +
					"email='"+email+"'");
		
			if(resultSet.next())
				user=new AdminUser(resultSet.getLong("id")); 
		}
		catch(Exception e){
			System.err.println("Error searching for admin in database by email.");
			System.err.print(e.getMessage());
			System.exit(1);
		}
		finally{
			closeStatements();
		}
		return user;
	}
	/*
	 * Searches for the AdminUser under a particular email address, deletes
	 * the user from the system.  If the user is found, returns true, otherwise false.
	 * If this user is the only administrator of a particular alert, the alerts
	 * are deleted
	 */
	public boolean deleteAdminUser(String email){
		AdminUser user= getAdminUser(email);
		if (user == null) return false;
		user.deleteAllAlerts();
		int numDeletions;
		try{
			statement=connect.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
	                ResultSet.CONCUR_READ_ONLY, 
	                ResultSet.HOLD_CURSORS_OVER_COMMIT);
			numDeletions=statement.executeUpdate("delete from alert_system.admin_users WHERE id='"+user.admin_id+"'");
			if(numDeletions==0)
				return false;
		}
		catch(Exception e){
			System.err.println("Error searching for admin id to delete.");
			System.err.print(e.getMessage());
			System.exit(1);
		}
		finally{
			closeStatements();
		}
		return true;
	}
	/* Removes a set of alerts by alert_id from four database tables.  If one of the alerts
	 * is not found, the method returns false, and true otherwise to signify the deletion task
	 * was complete.
	 */
	public boolean deleteAlerts(long[] alert_ids){
		boolean result= true;
		try{
			// Statements allow to issue SQL queries to the database
			statement3= connect.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
	                ResultSet.CONCUR_READ_ONLY, 
	                ResultSet.HOLD_CURSORS_OVER_COMMIT);
			// Result set get the result of the SQL query
			for(int i=0; i<alert_ids.length; i++){
				resultSet=statement3.executeQuery("select id from alert_system.alerts WHERE id="+alert_ids[i]);
				if(!resultSet.next()){
					result=false;
				}
			}
			long alert_id;
			for(int i=0; i<alert_ids.length; i++){
				alert_id=alert_ids[i];
				statement3.executeUpdate("delete from alert_system.alerts WHERE id='"+alert_id+"'");
			  	statement3.executeUpdate("delete from alert_system.admin_user_alerts WHERE alert_id='"+alert_id+"'");   
			  	statement3.executeUpdate("delete from alert_system.alert_game_environments WHERE alert_id='"+alert_id+"'");   
			  	statement3.executeUpdate("delete from alert_system.subscribers WHERE alert_id='"+alert_id+"'"); 
			  	statement3.executeUpdate("delete from alert_system.customized_alert_conditions WHERE alert_id='"+alert_id+"'");  
			}
			
		}
		catch (Exception e) {
			System.err.println("Error searching for alert id to delete.");
			System.err.print(e.getMessage());
			System.exit(1);
		} 
		finally {
			closeStatements();
		}
		return result;
	}
	/*
	 * Instantiates the given alert in the system and registers it to the specified
	 * AdminUser.  Only used for predefined alerts.
	 */
	public Alert createAlert(AdminUser user, String alert_type, long count, char comparative_op, long time_quantity, 
			String time_unit, ArrayList<String[]> game_env, String group, 
			String[] subscribers){
		if(alert_type.equals("CustomizedAlert") || alert_type.equals("CustomizedJoinAlert")){
			System.err.println("You must specify the field_to_query (usually a 'created_at'" +
					" field, etc.), the table, the aggregate and the conditions in order " +
					"to create a CustomizedAlert.");
			System.exit(1);
			
		}
		return user.createAlert(alert_type, count, comparative_op, time_quantity, time_unit, game_env, group, subscribers);
	}
	/*
	 * Instantiates the given alert in the system, and registers it to the specificied
	 * AdminUser.  Only used for CustomizedAlerts.
	 */
	public Alert createAlert(AdminUser user, String alert_type, long count, char comparative_op, 
			long time_quantity, String time_unit, ArrayList<String[]> game_env, 
			String group, String[] subscribers,String count_unit, String field_to_query,
			String table, String aggregate, String[] conditions){
		return user.createAlert(alert_type, count, comparative_op, time_quantity, 
				time_unit, game_env, group, subscribers, count_unit, field_to_query, 
				table, aggregate, conditions,null);
	}
	/*
	 * Instantiates the given alert in the system, and registers it to the specificied
	 * AdminUser.  Only used for CustomizedJoinAlerts.
	 */
	public Alert createAlert(AdminUser user, String alert_type, long count, char comparative_op, 
			long time_quantity, String time_unit, ArrayList<String[]> game_env, 
			String group, String[] subscribers,String count_unit, String field_to_query,
			String table, String aggregate, String[] conditions, String join_clause){
		return user.createAlert(alert_type, count, comparative_op, time_quantity, 
				time_unit, game_env, group, subscribers, count_unit, field_to_query, 
				table, aggregate, conditions, join_clause);
	}
	public boolean editAdminUser(long admin_id, String email, String name){
		try{
			statement=connect.createStatement();
			resultSet=statement.executeQuery("select * from alert_system.admin_users "+
					"WHERE id='"+admin_id+"'");
			if(!resultSet.next()){
				//admin id does not exist.
				return false;
			}
			else{
				if(!email.equals(resultSet.getString("email"))){
					//if updating email address, check that user does not already
					//exist with this email address
					if (this.search(email) ){
						System.err.println("This email already exists for another user.");
						return false;
					}
				}
			}
			statement.executeUpdate("update alert_system.admin_users "+
						"set email='"+email+"', name='"+name+"' "+
						"WHERE id='"+admin_id+"'");
		}
		catch (Exception e) {
			System.err.println("Error editting AdminUser information in editAdminUser().");
			System.err.print(e.getMessage());
			System.exit(1);
		} 
		finally {
			closeStatements();
		}
		return true;
	}
	public Alert editAlert(long alert_id, HashMap<String, Object> hash){
		return editAlert(alert_id, hash, false);
	}
	/*
	 * Enables the user to edit the 
	 * long count, char comparative_op, long time_quantity, String time_unit, 
	 * String group, ArrayList<String[]>game_env, long[] subscribers, String[] conditions 
	 * of an alert based on a given alert id.
	 * hash contains a list of properties to change.
	 * input: save_current- indicates whether the method will save the present conditions, game-
	 * environment combinations or subscribers, depending on which are included in hash.
	 */
	public Alert editAlert(long alert_id, HashMap<String, Object> hash, boolean save_current){
		
		Alert alert=null;
		try{
			statement= connect.createStatement();
			
			if(hash.containsKey("count")){
				System.out.println("changing count");
				long count= (Long)hash.get("count");
				statement.executeUpdate("update alert_system.alerts "+
											"set count='"+count+"' "+
											"WHERE id="+alert_id);	
			}
			if(hash.containsKey("comparative_op")){
				System.out.println("changing comparative op");
				char comparative_op=(Character)hash.get("comparative_op");
				statement.executeUpdate("update alert_system.alerts "+
						"set comparative_op='"+comparative_op+"' "+
						"WHERE id="+alert_id);
			}
			
			
			if(hash.containsKey("time_quantity")){
				System.out.println("changing time quantity");
					Long time_quantity=(Long)hash.get("time_quantity");
					statement.executeUpdate("update alert_system.alerts "+
							"set time_quantity='"+time_quantity+"' "+
							"WHERE id="+alert_id);
					
			}
			if(hash.containsKey("time_unit")){
					String time_unit=(String)hash.get("time_unit");
					statement.executeUpdate("update alert_system.alerts "+
							"set time_unit='"+time_unit+"' "+
							"WHERE id="+alert_id);
			}
			if(hash.containsKey("group")){
				String group=(String)hash.get("group");
				statement.executeUpdate("update alert_system.alerts "+
						"set `group/provider`='"+group+"' "+
						"WHERE id="+alert_id);
			}
			
			if(hash.containsKey("game_env")){
				ArrayList<String[]> game_env=((ArrayList<String[]>)hash.get("game_env"));
				Iterator<String[]> game_env_it= game_env.iterator();
				if(!save_current)
					statement.executeUpdate("delete FROM "+
						"alert_system.alert_game_environments WHERE alert_id"+
						"='"+alert_id+"'");
				
				while(game_env_it.hasNext()){
					String[] game_envAry=game_env_it.next();
		        	String game =game_envAry[0]; //store game name with alert id number
		        	String env=game_envAry[1]; //store game env with alert id number
		        	String ip=game_envAry[2];  //store ip for database with alert id number
		        	String game_user=game_envAry[3];  //store ip for database with alert id number
		        	String game_password=game_envAry[4];
				  //store ip for database with alert id number
		        	
					statement.executeUpdate("INSERT INTO alert_system.alert_game_environments(`alert_id`, `game`, " +
							"`environment`, `ip_address`, `user`, `password`, `last_fulfilled`)" + 
		                    " VALUES ('"+alert_id+"', '"+game+"', '"+env+"', '"+ip+"', '"+game_user+
		                    "', '"+game_password+"', NULL)"
		                    ,Statement.RETURN_GENERATED_KEYS);
					
				}
			}
			if(hash.containsKey("subscribers")){
				long[] subscribers=null;
				Object subs=hash.get("subscribers")  ;
				if(subs instanceof long[])
					subscribers=(long[])hash.get("subscribers");
				else if (subs instanceof String[] ){
					subscribers=AdminUser.subEmailToIDs((String[])subs);
				}
				if(!save_current)
					statement.executeUpdate("delete FROM "+
						"alert_system.subscribers WHERE alert_id"+
						"='"+alert_id+"'");
				
				for(long sub: subscribers){
					statement.executeUpdate("INSERT INTO alert_system.subscribers(`alert_id`, `admin_user_id`)" + 
		                    " VALUES ('"+alert_id+"', '"+sub+"')");
		
				}
				System.out.println("Updated subscribers");
			}
			if(hash.containsKey("conditions")|| hash.containsKey("aggregate")){
				
				//checks to see that this alert is of type CustomizedAlert
				String alertType=null; 
				String queryStr="select `alert_type` FROM alert_system.alerts WHERE " +
				"id='"+alert_id+"'";
				resultSet=statement.executeQuery(queryStr);
				
				while(resultSet.next()){
					alertType= resultSet.getString("alert_type");
				}
				
				
				if(alertType.equals("CustomizedAlert")|| 
						alertType.equals("CustomizedJoinAlert")){
					if(hash.containsKey("conditions")){
						String[] conditions=(String[])hash.get("conditions");
						if(!save_current)
							statement.executeUpdate("delete FROM "+
								"alert_system.customized_alert_conditions WHERE alert_id"+
								"='"+alert_id+"'");
						for(String condition: conditions){
							
							String updateString= "INSERT INTO alert_system.customized_alert_conditions(`alert_id`, `condition`)" + 
		                    " VALUES ('"+alert_id+"', \""+condition+"\")";
							
							statement.executeUpdate(updateString);
							
				
						}
					}
					if (hash.containsKey("aggregate")){
						String aggregate=(String)hash.get("aggregate");
		
						statement.executeUpdate("update alert_system.alerts "+
								"set aggregate='"+aggregate+"' "+
								"WHERE id="+alert_id);
						
					}
					
					if(hash.containsKey("table"))
					{
						String table=(String)hash.get("table");
						
						statement.executeUpdate("update alert_system.alerts "+
								"set table='"+table+"' "+
								"WHERE id="+alert_id);
						System.out.println("Assigned table");	
					}
					if(hash.containsKey("field_to_query"))
					{
						String field_to_query=(String)hash.get("field_to_query");
						
						statement.executeUpdate("update alert_system.alerts "+
								"set field_to_query='"+field_to_query+"' "+
								"WHERE id="+alert_id);
						System.out.println("Assigned field_to_query");	
					}
					if(alertType.equals("CustomizedJoinAlert"))
						if(hash.containsKey("join_clause"))
						{
							String join_clause=(String)hash.get("join_clause");
							statement.executeUpdate("update alert_system.alerts "+
									"set join_clause='"+join_clause+"' "+
									"WHERE id="+alert_id);
							System.out.println("Assigned aggregate");	
						}
				}
				else System.err.println("You cannot update the conditions, aggregate or table for an alert that is not" +
						"of type CustomizedAlert.");
				
			}
			//update last_fulfilled, as the rule has changed.
			statement.executeUpdate("update alert_system.alerts "+
					"set last_fulfilled=null "+
					"WHERE id="+alert_id);
			if(!(hash.size()==1 && hash.containsKey("game_env") && save_current))
				statement.executeUpdate("update alert_system.alert_game_environments set last_fulfilled=null where" +
					" alert_id="+alert_id);
			alert=this.createAlertFromID(alert_id);
			
			statement=connect.createStatement();
			if(hash.containsKey("time_quantity") ||hash.containsKey("time_unit")){
				System.out.println("Changing time interval.");
				alert.calc_time_interval();
				statement.executeUpdate("update alert_system.alerts "+
						"set time_interval='"+alert.time_interval+"' "+
						"WHERE id="+alert_id);
				System.out.println("changed time interval");
			}
			
			
			
		}
		catch(Exception e){
			System.err.println("Error editting the alert in editAlert().");
			System.err.print(e.getMessage());
			System.exit(1);
		}
		finally{
			closeStatements();
		}
		return alert;
	}
	
	public void pauseAlert(long alert_id){
		try{
			statement= connect.createStatement();
			statement.executeUpdate("update alert_system.alerts "+
					"set paused='1'  WHERE id="+alert_id);
		}
		catch (Exception e){
			System.err.println("Error editting the alert in editAlert().");
			System.err.print(e.getMessage());
			System.exit(1);
		}
		finally{
			closeStatements();
		}
	}
	public void unpauseAlert(long alert_id){
		try{
			statement= connect.createStatement();
			statement.executeUpdate("update alert_system.alerts "+
					"set paused='0'  WHERE id="+alert_id);
		}
		catch (Exception e){
			System.err.println("Error editting the alert in editAlert().");
			System.err.print(e.getMessage());
			System.exit(1);
		}
		finally{
			closeStatements();
		}
	}
	/*
	 * Unsubscribes user from a given Alert, given her or his email address.
	 * No deletion takes place if the user is not a subscriber of this alert.
	 */
	public void unsubscribe(long alert_id, String[] email){
		long []ids= AdminUser.subEmailToIDs(email);
		
		try{
			for (int i=0; i<ids.length; i++){
				
				statement=connect.createStatement();
				resultSet=statement.executeQuery("select * from alert_system.subscribers WHERE alert_id='"+alert_id+"' AND admin_user_id='"+ids[i]+"'");
			
				if(!resultSet.next()){
					System.err.println(email[i] +" is not a subscriber to alert "+ alert_id);
					
				}
				else {
					statement.executeUpdate("delete from alert_system.subscribers WHERE alert_id='"+alert_id+"' AND admin_user_id='"+ids[i]+"'");
				}
			}
			
		}
		catch(Exception e){
			System.err.println("Error seeking user email in unsubscribe().");
			System.err.print(e.getMessage());
			System.exit(1);
		}
		finally{
			closeStatements();
		}
		
		
	}
	/*
	 * Searches for a particular AdminUser based on his or her email address.
	 * If the user exists in the system, returns true, otherwise, false.
	 */
	public boolean search(String email) {
		try{
		statement= connect.createStatement();
		// Result set get the result of the SQL query
		 resultSet = statement.executeQuery("select id from alert_system.admin_users WHERE email='"+email+"'");
 	    long id;
		if(resultSet.next()){
			id=resultSet.getLong("id");
			System.out.println ("User "+email+ " exists as AdminUser "+id);
			
		}
		else{ 
			System.err.println("Subscriber "+email +" is unfound");
			return false;
		}
		
		}
		catch (SQLException e){
			System.err.println("Error searching for a user by email in search().");
		}
		finally{
			closeStatements();
		}
		return true;
	}
	/*
	 * Unsubscribes the user from any Alert to which he or she is subscribed.
	 */
	protected boolean unsubscribe(String email) {
		boolean subscribed=false;
		try{
			statement=connect.createStatement();
			int deletions= statement.executeUpdate("USE alert_system");
			deletions=statement.executeUpdate("Delete subscribers  FROM " +
					"(subscribers LEFT OUTER JOIN admin_users ON subscribers.admin_user_id=" +
					"admin_users.id ) WHERE admin_users.email='"+email+"'");
			
			if(deletions==0)
				subscribed=false;
		}
		catch (SQLException e){
			System.err.println("Error searching for a user by email in search().");
		}
		finally{
			closeStatements();
		}
		
		return subscribed;
		
	}
	/**
	 *main  
	 *args - alert_system ip, username and
	 * password.
	 *void 
	 */
	public static void main(String[] args) {
		AlertSystem as=new AlertSystem();
		as.pollAlerts();
		/*if(args!=null && args.length== 3)
			as = new AlertSystem(args[0],args[1], args[2]);
		else as= new AlertSystem();
		AdminUser npress= as.createAdminUser("npress@sleepygiant.com", "NEMA");
		AdminUser yann=as.createAdminUser("ykherian@sleepygiant.com", "Yann K");
		AdminUser robin=as.createAdminUser("robin.liao@sleepygiant.com", "Robin L.");
		AdminUser bob = as.createAdminUser("btsai@sleepygiant.com", "Bob Tsai");
		String [] subscribers={"npress@sleepygiant.com"};
		
		//as.deleteAdminUser("nema.press@sleepygiant.com");
		//as.deleteAdminUser("npress@sleepygiant.com");
		//AdminUser nemap= as.createAdminUser("nema.press@sleepygiant.com", "Nema P");
		
		
		//AdminUser chris=as.createAdminUser("chris@sleepygiant.com", "Chris Bielinski");
		Alert[] testAlerts= new Alert[11];
		ArrayList<String[]> game_env= new ArrayList<String[]>();
		String[] game_envirARY= new String[5];
		game_envirARY[0]= "crimecraft";
		game_envirARY[1]= "development";
		game_envirARY[2]=alert_system_ip;
		game_envirARY[3]=alert_system_user;
		game_envirARY[4]=alert_system_passwd;
		game_env.add(game_envirARY.clone());
		/*
		testAlerts[5]= as.createAlert(yann, "AdminConsoleExceptions", 13, '<', 33, "month", 
				game_env, "", subscribers);
		game_env= new ArrayList<String[]>();
		game_envirARY= new String[5];
		
		
		game_envirARY[1]= "development";
		game_envirARY[2]=alert_system_ip;
		game_envirARY[3]=alert_system_user;
		game_envirARY[4]=alert_system_passwd;
		game_envirARY[0] = "animaljam";
		game_env.add(game_envirARY.clone());   //must clone because it adds the actual object in mem.
		
		String[] conditionsJoinAlert={"users.id IS NOT NULL", "users.updated_at IS NOT NULL", "game_accounts.last_in_game_at IS NOT NULL"};
		String[] conditionsAlert={"updated_at IS NOT NULL", "screen_name LIKE '%Beta%'"};
		
		
		
		/*testAlerts[0] =as.createAlert(yann, "CustomizedAlert", 0, '<', 9, 
				"month", game_env, "", subscribers, "users", "created_at", 
				"users", "count(*)", conditionsAlert);
		
		testAlerts[1] = as.createAlert(yann, "CustomizedJoinAlert", 0, '<', 9, 
				"month", game_env, "", subscribers, "unactivated users", "created_at", 
				"users", "count(*)", conditionsJoinAlert, 
				"game_accounts JOIN users ON game_accounts.user_id= users.id");
		
		//as.pollAlerts();
		as.unsubscribe("chris@sleepygiant.com");
		/*testAlerts[2]=as.createAlert(yann, "WebUserActivations", 13, '<', 33, "month", 
				game_env, "", subscribers);
		testAlerts[3]=as.createAlert(yann, "WebExceptions", 13, '<', 33, "month", 
				game_env, "", subscribers);
		testAlerts[4]=as.createAlert(yann, "BillingRejections", 13, '<', 33, "month", 
				game_env, "", subscribers);
		
		testAlerts[6] = as.createAlert(yann, "BillingTransactionsByProvider", 13, '<', 33, "month", 
				game_env, "cc", subscribers);
		testAlerts [7]= as.createAlert(yann, "BillingCompletionsByProvider", 13, '<', 33, "month", 
				game_env, "paypal_cyber_source", subscribers);
		testAlerts[8] = as.createAlert(yann, "BillingRejectionsByProvider", 13, '<', 33, "month", 
				game_env, "cyber_source", subscribers);
		testAlerts[9] = as.createAlert(yann, "BillingFulfillments", 13, '<', 33, "month", 
				game_env, "", subscribers);
		testAlerts[10] = as.createAlert(yann, "WebUserRegistrations", 13, '<', 33, "month", 
				game_env, "", subscribers);*/
		
		//as.pollAlerts();
		//for(Alert a: testAlerts)
		//System.out.println(as.createAlertFromID(5731));
		//long[] sublist= AdminUser.subEmailToIDs(subscribers);
		//HashMap<String, Object> hash= new HashMap<String, Object>();
		//hash.put("count", (long)23);
		//hash.put("comparative_op", '<');
		//hash.put("time_quantity", (long)20);
		//hash.put("time_unit", "month");
		//hash.put("group", "");
		//hash.put("game_env", game_env);
		//as.unsubscribe(alert_id, email);
		//as.unsubscribe("chris@sleepygiant.com");
		//hash.put("subscribers", subscribers);
		//hash.put("aggregate", "count(*)");
		//hash.put("conditions", conditionsAlert);
		//as.editAlert(5626, hash, true);

	
	   
		
		
			
	


	}
	
}
