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
public class WebUserRegistrations extends Alert{

	public static void main (String args[]) throws SecurityException, NoSuchFieldException{
		String[] game_env_ary={"crimecraft", "development", AlertSystem.alert_system_ip, AlertSystem.alert_system_user, AlertSystem.alert_system_passwd};
		ArrayList<String[]> game_env= new ArrayList(2);
		game_env.add(game_env_ary);
		game_env.add(game_env_ary);
		long [] subs= {(long)126};
		WebUserRegistrations regAlert=new WebUserRegistrations(301, '>', 32, "HOUR", game_env, subs, '0');
		
		System.out.println(regAlert);
		String notification= regAlert.getNotified();
		if(notification!=null) 
			System.out.println(notification);
		else System.out.println("the web user registration alert was null");
		System.out.println("Query:"+ regAlert.getQueryString ("crimecraft_development"));
		
	}
	
	public WebUserRegistrations(long count, char comparative_op, long time_quantity, 
			String time_unit,
			ArrayList<String[]> game_env, long[] subs, char paused) {
		super(count, "users", comparative_op, time_quantity, 
				time_unit, "created_at", 
				game_env, "users", subs, paused);
	}
	
	public WebUserRegistrations(long count, char comparative_op, long time_quantity, 
			String time_unit,
			ArrayList<String[]> game_env, long [] subs, long alert_id
			, char paused) {
		super(count, "users", comparative_op, time_quantity, 
				time_unit, "created_at", 
				game_env, "users", subs, alert_id, paused);
	}
	
	public String getQueryString(String database){
		String query= "select count(*) from "+database+"."+this.table+
		" WHERE "+this.field_to_query+" between DATE_ADD(UTC_TIMESTAMP(), " +
				"INTERVAL -"+time_interval+" SECOND) AND UTC_TIMESTAMP()";
		return query;
	}
	
	
}
