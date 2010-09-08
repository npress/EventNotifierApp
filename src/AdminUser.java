import java.lang.reflect.InvocationTargetException;
import java.sql.DriverManager;
import java.util.ArrayList;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.mail.MessagingException;


public class AdminUser {
	ArrayList<Long> alerts;
	String email, name;
	protected static Statement statement, statement2, statement3;
	protected static Connection connect;
	
	protected long admin_id;
	protected static ResultSet resultSet;
	protected ResultSet resultSet2;
	SendMailUsingAuthentication smtpMailSender= new SendMailUsingAuthentication();
	/*
	 * Establishes a new Admin User in the database and stores his or her alerts in
	 * the database.  Does not store duplicate users in the database.
	 */
	public AdminUser(String email, String name) {
		
		this.email=email;
		this.name=name;
		this.alerts=new ArrayList<Long>();
		try{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		
			// Setup the connection with the DB
			
			connect = DriverManager.getConnection("jdbc:mysql://"+AlertSystem.alert_system_ip+"/?allowMultiQueries=true", AlertSystem.alert_system_user, AlertSystem.alert_system_passwd);
	
			// Statements allow to issue SQL queries to the database
			statement = connect.createStatement();
			resultSet=statement.executeQuery("select id, name from alert_system.admin_users where `email`='"+this.email+"'" );
			if(resultSet.next()){
				System.out.println("This user email already exists in the system with AdminUser id " +resultSet.getString(1));		
				this.name = resultSet.getString("name"); //updates name, in case it is different.
				this.admin_id= resultSet.getLong("id");
			}
			else{// Result set get the result of the SQL query
				statement.executeUpdate("INSERT INTO alert_system.admin_users(`email`, `name`, `notification`, `sent`)" + 
	            " VALUES ('"+this.email+"', '"+this.name+"', NULL, '1')", Statement.RETURN_GENERATED_KEYS);
				
				resultSet=statement.getGeneratedKeys();           
		        
			
				if(resultSet.next()) {
					
					this.admin_id=resultSet.getLong(1);
					System.out.println("The user has been established with AdminUser id "+ this.admin_id);
				}
			}
			
		}
		catch (Exception e) {
			System.err.println("Error inserting admin_user_id record in admin_users.");
			System.err.print(e.getMessage());
			System.exit(1);
		} 
		finally {
			close();
		}
	}
	public void finalize(){
		close();
	}
	/*
	 * Constructs an admin user from name, email in the database, given an admin user id. 
	 */
	public AdminUser(long l) {
		this.admin_id=l;
		this.alerts=new ArrayList<Long>();
		try{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			connect = DriverManager.getConnection("jdbc:mysql://"+AlertSystem.alert_system_ip+"/?allowMultiQueries=true", AlertSystem.alert_system_user, AlertSystem.alert_system_passwd);
			statement = connect.createStatement();
			resultSet=statement.executeQuery("select `name`, `email`"
					+" from alert_system.admin_users WHERE id="+this.admin_id);
			if(resultSet.next()){
				this.name= resultSet.getString("name");
				this.email = resultSet.getString("email");
			}
			else System.err.println("User " +this.admin_id+"does not exist in the system.");
		}
		catch (Exception e) {
			System.err.println("Error reading name and email for AdminUser "+ this.admin_id);
			System.err.print(e.getMessage());
			System.exit(1);
		} 
		
		finally {
			close();
		}
	}
	protected Alert createAlertFromID(long id){
		
		try{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			connect = DriverManager.getConnection("jdbc:mysql://"+AlertSystem.alert_system_ip+"/?allowMultiQueries=true", AlertSystem.alert_system_user, AlertSystem.alert_system_passwd);
			statement = connect.createStatement();
			statement.execute("select `alert_type`, `count`, `comparative_op`, `time_quantity`," +
					" `time_unit`, `group/provider`, `count_unit`, " +
					"`table`, `field_to_query`, `aggregate`, `paused`, `join_clause`"
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
			char paused='0';
			String count_unit=null, table=null, field_to_query=null, 
			aggregate=null, join_clause=null;
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
			statement2 = connect.createStatement();
			resultSet = statement2.executeQuery("select `game`, `environment`, `ip_address`,"
					+"`user`, `password` from alert_system.alert_game_environments WHERE " +
							"alert_id="+id);
			
			System.out.println(resultSet.toString());		
			
			while(resultSet.next()){
				String[] gameEnvAry=new String[5];
				gameEnvAry[0]=resultSet.getString("game");
				gameEnvAry[1]=resultSet.getString("environment");
				gameEnvAry[2]=resultSet.getString("ip_address");
				gameEnvAry[3]=resultSet.getString("user");
				gameEnvAry[4]=resultSet.getString("password");
				
				game_env.add(gameEnvAry);
				
				
			}
			/**********Seek subscribers****/
			statement3=connect.createStatement();
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
			return instantiateAlertWithID(alert_type, count, comparative_op, time_quantity, 
					time_unit, game_env, group, sublist, count_unit, id, conditions, aggregate,
					field_to_query, table, paused, join_clause);//add setting for id
			
		}
		catch (Exception e) {
			System.err.println("Error seeking data for alert id "+ id);
			System.err.print(e.getMessage());
			System.exit(1);
		} 
		
		finally {
			close();
		}
		return null;
	}
	
	/*
	 * Instantiates a new alert based on its given alert_type, given
	 * a particular alert_id.
	 */
	static Alert instantiateAlertWithID(String alert_type, long count,
			char comparative_op, long time_quantity, String time_unit,
			ArrayList<String[]> game_env, String group, long[] sublist,
			String count_unit, long alert_id, String[] conditions,
			String aggregate, String field_to_query, String table, 
			char paused, String join_clause
			) {
		Alert alert;
		// TODO Auto-generated method stub
		if(alert_type.equals("CustomizedAlert")||(alert_type.equals("CustomizedJoinAlert")))
		{
			alert= instantiateCustomizedAlert(alert_type, count, comparative_op, time_quantity, time_unit, 
	    			game_env, group, sublist, count_unit, paused, field_to_query, table, 
	    			aggregate, conditions, join_clause);
		}
		else{
			alert= instantiateAlert(alert_type, count, comparative_op, time_quantity, time_unit,
				game_env, group, sublist, count_unit, paused);
		}
		alert.alert_id=alert_id;
		return alert;
	}
	/*
	 * Polls only the alerts associated with this particular Administrator,
	 * saves an updated notification string in admin_users table and sets sent to false.
	 * conditions for proceeding to notify:
	 * now()-last_fulfilled >= time_interval in the rule for the Alert
	 * 
	 */
	public void pollAlertsForUser(){
		
		this.alerts=populateAlertsToNotify();
		Alert alert;
		//stores an alert and notification string for each alert that
		//ought to be notified
		ArrayList<Object[]> alertsWithNotification=new ArrayList<Object[]>(); 
		Object[] objectAry=new Object[2]; 
		String notificationStr;
		
		System.out.println("this.alerts.size()="+ this.alerts.size());
		for(int i=0; i<alerts.size(); i++){
			
			alert=createAlertFromID(alerts.get(i).longValue()); 
			//constructs a new Alert from information in the database 
			//using a given Alert ID
			
			System.out.println("alerts in pollAlerts is: "+ alert.alert_id);
			
			notificationStr=alert.getNotified();
			System.out.println("got notified string");
			if(notificationStr!=null){
				
				objectAry[0]=alert;
                objectAry[1]=notificationStr;
                alertsWithNotification.add(objectAry.clone());
			}
			else if (notificationStr==null){
				System.out.println("Notification for "+alert.alert_id+" is null.");
				
			}
		}
		try{
			Class.forName("com.mysql.jdbc.Driver").newInstance();		
			// Setup the connection with the DB		
			connect = DriverManager.getConnection("jdbc:mysql://"+AlertSystem.alert_system_ip+"/?allowMultiQueries=true", AlertSystem.alert_system_user, AlertSystem.alert_system_passwd);
	
			// Statements allow to issue SQL queries to the database
			statement = connect.createStatement(
					ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY, 
                    ResultSet.HOLD_CURSORS_OVER_COMMIT
                    );
			statement2 = connect.createStatement(
					ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY, 
                    ResultSet.HOLD_CURSORS_OVER_COMMIT
                    );
			// add the string to notification next to this AdminUser's id
			// in the admin_users table , then set sent to false
			char sent;
			System.out.println(alertsWithNotification.size() + " alert(s) with notification.");
			for(int i=0; i<alertsWithNotification.size(); i++){
				
				alert= (Alert)((Object[])alertsWithNotification.get(i))[0];
				for (int j=0; j<alert.subscribers.length; j++){
						
						notificationStr = (String)((Object[])alertsWithNotification.get(i))[1];
						System.out.println("The notification is: "+ notificationStr);
						resultSet2=statement.executeQuery("SELECT sent FROM alert_system.admin_users WHERE id="+alert.subscribers[j]);
						if(resultSet2.next()){
							sent= resultSet2.getString("sent").charAt(0);
					    
							if(sent == '1'){
								
								
								statement.execute("update alert_system.admin_users "+
										"set notification='"+notificationStr+"', sent='0' "+
										"WHERE id="+alert.subscribers[j]);	
							}
							else {
								System.out.println("The subscriber is "+ alert.subscribers[j]);
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
			System.err.println("Error printing notification strings and sent status to admin_users.");
			System.err.print(e.getMessage());
			System.exit(1);
		} 
		finally {
			close();
		}
		
		
	}
	
protected void sendEmails() throws SQLException, MessagingException  {
		
		
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
				user_id=resultSet.getLong("id");
				sub_name=resultSet.getString("name");
				smtpMailSender.postMail(sub_email, "Your Notification Digest", "Dear "+sub_name+",\n\n"+notification+"\nBest,\n"+this.name, this.email);
				statement2.executeUpdate("UPDATE alert_system.admin_users set sent='1' where id= '"+user_id+"'" );	
			}
		
		
	}
	/*
	 * Creates an ArrayList of Alerts based on those alerts that 
	 * have the property that now-time_interval in the rule >= last_fulfilled
	 */
	public ArrayList<Long> populateAlertsToNotify(){
		//zero the alerts owned by this administrator
		this.alerts.clear();
		try{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		
			// Setup the connection with the DB		
			connect = DriverManager.getConnection("jdbc:mysql://"+AlertSystem.alert_system_ip+"/?allowMultiQueries=true", AlertSystem.alert_system_user, AlertSystem.alert_system_passwd);
			// Statements allow to issue SQL queries to the database
			statement = connect.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY, 
                    ResultSet.HOLD_CURSORS_OVER_COMMIT);
			
			String queryString="select admin_user_alerts.alert_id, " +
			"DATE_ADD(UTC_TIMESTAMP(), INTERVAL -alerts.time_interval SECOND), "+
			"alerts.last_fulfilled "+
			" FROM alert_system.admin_user_alerts "+ 
			"JOIN alert_system.alerts ON admin_user_alerts.alert_id = alerts.id "+
			"WHERE "+ 
			"(admin_user_alerts.admin_user_id="+this.admin_id+
			") AND "+ 
			"((DATE_ADD(UTC_TIMESTAMP(), INTERVAL -alerts.time_interval SECOND) >= "+
			"alerts.last_fulfilled) " +
			"OR (alerts.last_fulfilled is NULL))" +
			"AND (alerts.paused ='0')";
			resultSet2= statement.executeQuery(queryString);
			
			resultSet2=statement.getResultSet();
			System.out.println("admin_id: "+ this.admin_id);
			while(resultSet2.next()){
				long alertID= resultSet2.getLong("alert_id");
				System.out.println("The alertID in populateAlertsToNotify: "+ alertID);
				alerts.add(new Long(alertID));
				//get alert's last_fulfilled
				
			}   
			System.out.println(this.alerts.size()+" alerts to notify." );
			
		}
		catch (Exception e) {
			System.err.println("Error seeking id's owned by this AdminUser.");
			System.err.print(e.getMessage());
			System.exit(1);
		} 
		finally {
			close();
		}
		return alerts;
	}
	
	/*
	 * Deletes all alerts owned by this particular user.
	 */
	public void deleteAllAlerts(){
		ArrayList<Long> alerts_to_delete= new ArrayList<Long>();
		try{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			// Setup the connection with the DB
			connect = DriverManager.getConnection("jdbc:mysql://"+AlertSystem.alert_system_ip+"/?allowMultiQueries=true", AlertSystem.alert_system_user, AlertSystem.alert_system_passwd);
	
			// Statements allow to issue SQL queries to the database
			statement2 = connect.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY, 
                    ResultSet.HOLD_CURSORS_OVER_COMMIT);
			resultSet=statement2.executeQuery("SELECT alert_id from alert_system.admin_user_alerts WHERE admin_user_id='"+this.admin_id+"'");
			while(resultSet.next()){
				alerts_to_delete.add(resultSet.getLong("alert_id"));  
				//auto-boxing(Java5) turns long values into Long wrapper class values.
			}
			
			//deleteAlerts() called on given long[]
			Long[] alert_ids= new Long[alerts_to_delete.size()];
			alert_ids=alerts_to_delete.toArray(alert_ids);
			deleteAlerts(alert_ids);
		}
		catch (Exception e) {
			System.err.println("Error searching for alert id to delete.");
			System.err.print(e.getMessage());
			System.exit(1);
		} 
		finally {
			close();
		}
		
	
	}
	/*
	 * deleteAlerts() removes an alert from the database based on an alert_id
	 * returns boolean whether or not the alert_id was removed successfully, which
	 * depends on whether the alert_id was found in the database
	 */
	public boolean deleteAlerts(Long[] alert_ids){
		Statement statement=null ;
		try{
			
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			// Setup the connection with the DB
			connect = DriverManager.getConnection("jdbc:mysql://"+AlertSystem.alert_system_ip+"/?allowMultiQueries=true", AlertSystem.alert_system_user, AlertSystem.alert_system_passwd);
			// Statements allow to issue SQL queries to the database
			statement = connect.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY, 
                    ResultSet.HOLD_CURSORS_OVER_COMMIT);
			ArrayList<Long> alerts_to_delete= new ArrayList<Long>();;
			for(long alert_id: alert_ids)// check that the alerts are owned by the given user:
			{
				resultSet= statement.executeQuery("select * from alert_system.admin_user_alerts" +
						" WHERE alert_id='"+alert_id+"'");
				if(resultSet.next()){//exists in the database in the database
					
					resultSet= statement.executeQuery("select * from alert_system.admin_user_alerts" +
							" WHERE admin_user_id='"+this.admin_id+"' AND alert_id='"+alert_id+"'");
					if(resultSet.next()){
						
					
						alerts_to_delete.add(new Long(alert_id));
					}
					else System.err.println("The user does not have the permission to delete " +
								alert_id);
				}
				else System.err.println("Alert id "+alert_id+" does not exist or is not registered" +
						" to any AdminUser.");
			}
			
			long alert_id;
			for(int i=0; i<alerts_to_delete.size(); i++){
				alert_id=(Long)alerts_to_delete.get(i); //implicit type conversion by auto-unboxing
				statement.executeUpdate("delete from alert_system.alerts WHERE id='"+alert_id+"'");
			  	statement.executeUpdate("delete from alert_system.admin_user_alerts WHERE alert_id='"+alert_id+"'");   
			  	statement.executeUpdate("delete from alert_system.alert_game_environments WHERE alert_id='"+alert_id+"'");   
			  	statement.executeUpdate("delete from alert_system.subscribers WHERE alert_id='"+alert_id+"'");   
			  	statement.executeUpdate("delete from alert_system.customized_alert_conditions WHERE alert_id='"+alert_id+"'");  
			}
		}
		catch (Exception e) {
			System.err.println("Error searching for alert id to delete.");
			System.err.print(e.getMessage());
			System.exit(1);
		} 
		finally {
			try{if(statement!=null) 
				statement.close();
			}
			catch(Exception e){}
			close();
		}
		return true;
	}
	
	
	public Alert createAlert(String alert_type, long count, char comparative_op, 
			long time_quantity, String time_unit, ArrayList<String[]> game_env, 
			String group, String[] subscribers) 		
	{
		return createAlert(alert_type, count, comparative_op, 
			time_quantity, time_unit, game_env, 
			group, subscribers,null, null, null, null, null, null);
	}
	
	/*
	 * createAlert() -instantiates new alert based on its name and registers it to 
	 * this user.  loads alert into the database of alerts.
	 */
	public Alert createAlert(String alert_type, long count, char comparative_op, 
			long time_quantity, String time_unit, ArrayList<String[]> game_env, 
			String group, String[] subscribers,String count_unit, String field_to_query,
			String table, String aggregate, String[] conditions, String join_clause)
			
	{
		
	    Alert alert=null;
	    long[] sublist= subEmailToIDs(subscribers);
	    //if a subclass of CustomizedAlert, then instantiate CustomizedAlert
	    if(alert_type.equals("CustomizedAlert")|| alert_type.equals("CustomizedJoinAlert"))
	    	alert= instantiateCustomizedAlert(
	    			alert_type, count, comparative_op, time_quantity, time_unit, 
	    			game_env, group, sublist, count_unit, field_to_query, table, 
	    			aggregate, conditions, join_clause);
	    else
	    	alert = instantiateAlert(alert_type, count, comparative_op, time_quantity, 
	    		time_unit, game_env, group, sublist, count_unit) ;
	    	
	    alert.storeAlert();
	    registerAdmin(alert.alert_id);//registers the AdminUser as the administrator of the alert
		return alert;	
	}
	private Alert instantiateAlert(String alert_type, long count,
			char comparative_op, long time_quantity, String time_unit,
			ArrayList<String[]> game_env, String group, long[] sublist,
			String count_unit) {
		
		return instantiateAlert( alert_type,  count, comparative_op,  time_quantity,  
				time_unit,game_env, group,  sublist, count_unit, '0');
	}
	/*
	 * Instantiating for the first time without an alert_id.  Paused is 0.
	 * By default all alerts will be checked for notifications, and can be 
	 * paused later.
	 */
	protected Alert instantiateCustomizedAlert(String alert_type, long count,
			char comparative_op, long time_quantity, String time_unit,
			ArrayList<String[]> game_env, String group, long[] sublist,
			String count_unit, String field_to_query, String table,
			String aggregate, String[] conditions, String join_clause) {
		return instantiateCustomizedAlert(alert_type, count, comparative_op,
				time_quantity, time_unit, game_env, group, sublist, count_unit,
				'0', field_to_query, table, aggregate, conditions, join_clause);
	}
	
	public static CustomizedAlert instantiateCustomizedAlert(String alert_type, 
			long count, 
			char comparative_op, long time_quantity, String time_unit, 
			ArrayList<String[]> game_env, String group, long[] sublist, 
			String count_unit, char paused, String field_to_query, 
			String table, String aggregate, String [] conditions){
		
		return instantiateCustomizedAlert(alert_type, 
				count, comparative_op, time_quantity, time_unit, 
				game_env, group, sublist, 
				count_unit, paused, field_to_query, 
				table, aggregate,  conditions, null);
	}
	
	public static CustomizedAlert instantiateCustomizedAlert(String alert_type, 
			long count, 
			char comparative_op, long time_quantity, String time_unit, 
			ArrayList<String[]> game_env, String group, long[] sublist, 
			String count_unit, char paused, String field_to_query, 
			String table, String aggregate, String [] conditions, String join_clause){
		CustomizedAlert alert=null;
		if(alert_type.equals("CustomizedAlert")){
			alert = new CustomizedAlert(count, count_unit, comparative_op,
					time_quantity, time_unit, field_to_query,
					game_env, table, sublist,
					conditions, aggregate, paused);
		}
		else if(alert_type.equals("CustomizedJoinAlert")){
			alert= new CustomizedJoinAlert(count, count_unit, comparative_op,
					time_quantity, time_unit, field_to_query,
					game_env, table, sublist,
					conditions, aggregate, paused, join_clause);
		}
		return alert;
		
		
	}
	public static Alert instantiateAlert(String alert_type, long count, 
			char comparative_op, long time_quantity, String time_unit, 
			ArrayList<String[]> game_env, String group, long[] sublist, 
			String count_unit, char paused){
		
		Alert alert=null;
		
		
		if(alert_type.equals("BillingFulfillments")){
	    	alert= new BillingFulfillments(count, comparative_op, time_quantity,
	    			time_unit, game_env, sublist, paused);
	    	
	    }
	    
	    else if(alert_type.equals("WebUserActivations")){
	    	alert= new WebUserActivations(count, comparative_op, time_quantity,
	    			time_unit, game_env, sublist, paused);
	    	
	    	
	    }
	    else if(alert_type.equals("BillingTransactionsByProvider")){
	    	alert= new BillingTransactionsByProvider(count, comparative_op, time_quantity,
	    			time_unit, game_env, group, sublist, paused);
	    	
	    }
	    else if(alert_type.equals("BillingCompletionsByProvider")){
	    	alert= new BillingCompletionsByProvider(count, comparative_op, time_quantity,
	    			time_unit, game_env, group, sublist, paused);
	    
	    }
	    else if(alert_type.equals("BillingRejectionsByProvider")){
	    	alert= new BillingRejectionsByProvider(count, comparative_op, time_quantity,
	    			time_unit, game_env, group, sublist, paused);
		}
	    else if(alert_type.equals("WebUserRegistrations")){
	    	alert= new WebUserRegistrations(count, comparative_op, time_quantity,
	    			time_unit, game_env, sublist, paused);
		}
	    else if(alert_type.equals("WebExceptions")){
	    	alert= new WebExceptions(count, comparative_op, time_quantity,
	    			time_unit, game_env, sublist, paused);
		}
	    else if(alert_type.equals("AdminConsoleExceptions")){
	    	alert = new AdminConsoleExceptions(count, comparative_op, time_quantity,
	    			time_unit, game_env, sublist, paused);
		}
	    else if(alert_type.equals("BillingRejections")){
	    	alert = new AdminConsoleExceptions(count, comparative_op, time_quantity,
	    			time_unit, game_env, sublist, paused);
		}
	    else if(alert_type.equals("BillingFulfillments")){
	    	alert = new AdminConsoleExceptions(count, comparative_op, time_quantity,
	    			time_unit, game_env, sublist, paused);
		}
	    return alert;
	}
	protected void registerAdmin(long alert_id) {
		// TODO Auto-generated method stub
		try{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		
			// Setup the connection with the DB
			connect = DriverManager.getConnection("jdbc:mysql://"+AlertSystem.alert_system_ip+"/?allowMultiQueries=true", AlertSystem.alert_system_user, AlertSystem.alert_system_passwd);
			// Statements allow to issue SQL queries to the database
			statement = connect.createStatement();
			statement.executeUpdate("insert into alert_system.admin_user_alerts(`admin_user_id`, `alert_id`) values("+this.admin_id+","+alert_id+")");
			System.out.println("Alert is registered to admin user id: "+ this.admin_id);
		}
		catch (Exception e) {
			System.err.println("Error inserting admin_user_id and alert_id record in admin_user_alerts.");
			System.err.print(e.getMessage());
			System.exit(1);
		} 
		finally {
			close();
		}
		
	}
	/*
	 * Translates an array of email addresses into IDs.  If a user
	 * does not exist, create the user in the system.
	 */
	public static long[] subEmailToIDs(String[] subscribers){
		ArrayList<Long> subsAL= new ArrayList<Long>();
		
		try{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		
			// Setup the connection with the DB
			
			connect = DriverManager.getConnection("jdbc:mysql://"+AlertSystem.alert_system_ip+"/?allowMultiQueries=true", AlertSystem.alert_system_user, AlertSystem.alert_system_passwd);
	
			// Statements allow to issue SQL queries to the database
			statement = connect.createStatement(
					ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY, 
                    ResultSet.HOLD_CURSORS_OVER_COMMIT
                    );
			
			for(int i=0; i<subscribers.length; i++){
				
				
				// Result set get the result of the SQL query
				 resultSet = statement.executeQuery("select id from alert_system.admin_users WHERE email='"+subscribers[i]+"'");
		  	    
				if(resultSet.next()){
					subsAL.add(resultSet.getLong(1));
				}
				else{
					System.err.println("The subscriber "+ subscribers[i]+" does not exist.");
				}
				
			}
		}
		catch (Exception e) {
			System.err.println("Error searching for user id from email record in admin_users.");
			System.err.print(e.getMessage());
			System.exit(1);
		} 
		finally {
			close();
		}
		long[] subs= new long[subsAL.size()];
		
		for(int i=0; i<subsAL.size(); i++){
			subs[i] = subsAL.get(i);
		}
		
		return subs;
	}
	protected static void close() {
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
	
	/**
	 *main  
	 *AdminUser  
	 *void 
	 * 
	 */
	public static void main(String[] args) {
		
		
		//AdminUser aetna=new AdminUser((long)25);
		//Long[] alerts_to_delete={(long)5526};
		//aetna.deleteAllAlerts();
		//nema.deleteAlert(alerts_to_delete);
		String[] game_env_ary={"freakypets", "development", "10.42.42.110", 
				AlertSystem.alert_system_user, AlertSystem.alert_system_passwd};
	    ArrayList<String[]> game_env= new ArrayList<String[]>(2);
		game_env.add(game_env_ary);
		String[]subs={"npress@sleepygiant.com", "chris@sleepygiant.com"};
		//AdminUser nema= new AdminUser("npress@csulb.edu", "Nema Grace-Antonia Press");
		
		String[] conditionsJoinAlert={"users.updated_at IS NOT NULL", "game_accounts.last_in_game_at IS NOT NULL"};
		//String[] conditionsAlert={"updated_at IS NOT NULL", "screen_name LIKE '%Beta%'"};
		AdminUser nema = new AdminUser((long)2);
		AlertSystem as= new AlertSystem();
//		Alert alert=yann.createAlert("CustomizedJoinAlert", 0, '<', 9, 
//				"month", game_env, "", subs, "unactivated users", "created_at", 
//				"users", "count(*)", conditionsJoinAlert, 
//				"game_accounts JOIN users ON game_accounts.user_id= users.id");
//				
//		Long[] alert_ids= {(long)5677};
		nema.pollAlertsForUser();
	}

}
