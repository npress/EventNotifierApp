import java.util.ArrayList;

/**
 * @author npress
 *
 */
public class WebExceptions extends Alert{

	/**
	 * 
	 */
	public WebExceptions(long count, char comparative_op, long time_quantity, 
		String time_unit, ArrayList<String[]> game_env, long[] subs, 
		char paused) {
	
	super(count, "errors", comparative_op, time_quantity, 
			time_unit, "last_occurance", 
			game_env, "logged_exceptions", subs, paused);
			
	}
	public WebExceptions(long count, char comparative_op, long time_quantity, 
			String time_unit, ArrayList<String[]> game_env, long[] subs, 
			long alert_id, char paused) {
		
		super(count, "errors", comparative_op, time_quantity, 
				time_unit, "last_occurance", 
				game_env, "logged_exceptions", subs, alert_id, paused);
				
		}

	/**
	 *main - 
	 *WebExceptions- 
	 *void - 
	 */
	public static void main(String[] args) {
		String[] game_env_ary={"crimecraft", "development", "192.168.3.235"};
		ArrayList<String[]> game_env= new ArrayList(2);
		game_env.add(game_env_ary);
		long [] subs={1, 4, 6};
		WebExceptions regAlert=new WebExceptions(2, '<', 32, "HOUR", 
				game_env, subs, '0');
		System.out.println(regAlert);
		System.out.println(regAlert.getNotified());
	}

	@Override
	protected String getQueryString(String database) {
		String query ="select count(*) from "+database+"."+this.table+" where "+this.field_to_query+" between DATE_ADD(UTC_TIMESTAMP(), INTERVAL -"+this.time_interval+" second) AND UTC_TIMESTAMP()";
		return query;
	}

}
