import java.util.ArrayList;



/**
 * @author npress
 *
 */
public class AdminConsoleExceptions extends Alert{

	/**
	 * 
	 */
	public AdminConsoleExceptions(long count, char comparative_op, long time_quantity, 
			String time_unit, ArrayList<String[]> game_env, long[] subs, char paused) {
		super(count, "errors", comparative_op, time_quantity, 
				time_unit, "created_at",  
				game_env, "logged_exceptions", subs, paused);
	}
	public AdminConsoleExceptions(long count, char comparative_op, long time_quantity, 
			String time_unit, ArrayList<String[]> game_env, long[] subs, 
			long alert_id, char paused) {
		super(count, "errors", comparative_op, time_quantity, 
				time_unit, "created_at",  
				game_env, "logged_exceptions", subs, alert_id, paused);
	}
	/**
	 *
	 */
	public static void main(String[] args) {
		String[] game_env_ary={"crimecraft", "development", "192.168.3.235"};
		ArrayList<String[]> game_env= new ArrayList<String[]>(2);
		long [] subs={1,2};
		game_env.add(game_env_ary);
		AdminConsoleExceptions adminExceptionAlert=
			new AdminConsoleExceptions(30, '<', 32, "month", game_env, subs, '0');
		System.out.println(adminExceptionAlert.getQueryString("crimecraft_development"));
		

	}

	@Override
	protected String getQueryString(String database) {
		//String query="select count(*) from "+database+".logged_exceptions where (created_at between DATE_ADD('2009-08-13 17:00:00.0', INTERVAL -333333 second) AND '2009-08-13 17:00:00.0')";
		String query="select count(*) from "+database+".logged_exceptions where ("+this.field_to_query+" between DATE_ADD(UTC_TIMESTAMP(), INTERVAL -"+this.time_interval+" second) AND UTC_TIMESTAMP())";
		
		return query;
	}
	@Override
	protected String getDBName(String gameName, String envir) {
		return gameName+"_admin_"+ envir;
		
	}

}
