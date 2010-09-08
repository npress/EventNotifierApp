import java.util.ArrayList;
/**
 * @author npress
 *
 */
public class BillingRejections extends Alert{

	/**
	 * 
	 */
	public BillingRejections(long count, char comparative_op, long time_quantity, 
			String time_unit, ArrayList<String[]> game_env, long[] subs, 
			char paused) {
		
		super(count, "errors",  comparative_op, time_quantity, 
				time_unit, "status", 
				game_env, "billing_transactions", subs, paused);
				
	}
	public BillingRejections(long count, char comparative_op, long time_quantity, 
			String time_unit, ArrayList<String[]> game_env, long[] subs, 
			long alert_id, char paused) {
		
		super(count, "errors",  comparative_op, time_quantity, 
				time_unit, "status", 
				game_env, "billing_transactions", subs, alert_id, paused);
				
	}
	/**
	 *main - 
	 *BillingExceptions- 
	 *void - 
	 */
	public static void main(String[] args) {
		String[] game_env_ary={"crimecraft", "development", "192.168.3.235"};
		long[] subs={4, 5, 8};
		ArrayList<String[]> game_env= new ArrayList(2);
		game_env.add(game_env_ary);
		BillingRejections regAlert=new BillingRejections
		(2, '=', 333333, "second", game_env, subs, '0');
		System.out.println(regAlert.getQueryString("crimecraft_development"));
		//System.out.println(regAlert.getNotified());
		

	}

	@Override
	protected String getQueryString(String database) {
		String query="select count(*) from "+database+"."+this.table+" where ("+this.field_to_query+"='-1' AND updated_at between DATE_ADD(UTC_TIMESTAMP(), INTERVAL -"+time_interval+" second) AND 	UTC_TIMESTAMP())";
		
		//String query="select count(*) from "+database+".billing_transactions where (status='-1' AND updated_at between DATE_ADD('2009-08-05 17:00:00.0', INTERVAL -333333 second) AND 	'2009-08-05 17:00:00.0')";
		return query;
	}

}
