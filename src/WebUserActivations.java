
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author npress
 *
 */
public class WebUserActivations extends PercentAlert{

	public static void main (String args[]) throws SecurityException, NoSuchFieldException{
		String[] game_env_ary={"crimecraft", "development", AlertSystem.alert_system_ip, AlertSystem.alert_system_user, AlertSystem.alert_system_passwd};
		ArrayList<String[]> game_env= new ArrayList(2);
		game_env.add(game_env_ary);
		long [] subs= {2, 4, 5};
		WebUserActivations regAlert=new WebUserActivations(10, '>', 32, 
				"month", game_env, subs, '0');
		System.out.println(regAlert);
		System.out.println(regAlert.getQueryString("crimecraft_development"));
		
		
	}
	/**A constructor that accepts a count, the number of items to poll, count_unit, the unit of entities
	 * to poll, a comparison operator which will be used to determine the truth value of the rule
	 * , a time_quantity that represents the amount of time_unit's.  time_unit- the unit of time,
	 * under which to conduct the poll, field_to_query and field2_to_query, the fields in the database to determine
	 * query results for, game_env, and ArrayList containing arrays of Strings representing (1) the game
	 * name, (2) the environment where the poll will be done, (3) the ip address of the server
	 * containing the database for that game environment pair.
	 * 
	 */
	public WebUserActivations(long count, char comparative_op, long time_quantity, 
			String time_unit,
			ArrayList<String[]> game_env, long[] subs, char paused) {
		super(count, "% activations", comparative_op, time_quantity, 
				time_unit, "activated_at", "created_at",  
				game_env, "users", subs, paused);
		
	}
	
	public WebUserActivations(long count, char comparative_op, long time_quantity, 
			String time_unit,
			ArrayList<String[]> game_env, long [] subs, long alert_id
			, char paused) {
		super(count, "% activations", comparative_op, time_quantity, 
				time_unit, "activated_at", "created_at",  
				game_env, "users", subs, alert_id, paused);
		
	}
	protected String getQueryString(String database){
		String queryStr="select count(*) from "+database+"."+this.table+" WHERE "+this.field2_to_query+" between DATE_ADD(UTC_TIMESTAMP(), INTERVAL -"+time_interval+" SECOND) AND UTC_TIMESTAMP()";
		queryStr+="; select count(*) from "+database+"."+this.table+" WHERE ("+this.field2_to_query+" between DATE_ADD(UTC_TIMESTAMP(), INTERVAL -"+time_interval+" SECOND) AND UTC_TIMESTAMP()) AND ("+this.field_to_query+" between DATE_ADD(UTC_TIMESTAMP(), INTERVAL -"+time_interval+" SECOND) AND UTC_TIMESTAMP())"; 
		//String queryStr= "select count(*) from "+database+".users WHERE "+this.field2_to_query+" between DATE_ADD('2009-04-17 17:00:00.0', INTERVAL -"+time_interval+" SECOND) AND '2009-04-17 19:00:00.0'";
		//queryStr+="; select count(*) from "+database+".users WHERE ("+this.field2_to_query+" between DATE_ADD('2009-04-17 17:00:00.0', INTERVAL -"+time_interval+" SECOND) AND '2009-04-17 17:00:00.0') AND ("+this.field_to_query+" between DATE_ADD('2009-04-17 17:00:00.0', INTERVAL -"+time_interval+" SECOND) AND '2009-04-17 17:00:00.0')";
		return queryStr;
	}
	

}
