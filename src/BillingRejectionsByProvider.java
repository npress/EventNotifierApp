import java.util.ArrayList;


public class BillingRejectionsByProvider extends AlertByGroup{

	public BillingRejectionsByProvider(long count, char comparative_op, long time_quantity, 
			String time_unit, ArrayList<String[]> game_env, String provider, 
			long[] subs, char paused) {
		super(count, "rejected transactions", comparative_op, time_quantity, 
				time_unit, "status", 
				game_env, "billing_transactions", provider, subs, paused);
	}
	public BillingRejectionsByProvider(long count, char comparative_op, long time_quantity, 
			String time_unit, ArrayList<String[]> game_env, String provider, 
			long[] subs,long alert_id, char paused) {
		super(count, "rejected transactions", comparative_op, time_quantity, 
				time_unit, "status", 
				game_env, "billing_transactions", provider, subs, alert_id
				,paused);
	}
	/**
	 *main  
	 *BillingRejectionsByProvider  
	 *void 
	 */
	public static void main(String[] args) {
		String[] game_env_ary={"crimecraft", "development", "192.168.3.235"};
		ArrayList<String[]> game_env= new ArrayList<String[]>(2);
		game_env.add(game_env_ary);
		long[] subs ={3, 4, 5};
		BillingRejectionsByProvider billingAlert=new 
		BillingRejectionsByProvider(2, '<', 12, "month", game_env, "cc", subs, '0');
		System.out.println(billingAlert.getQueryString("crimecraft_development"));
		//System.out.println(billingAlert.getNotified());

	}

	@Override
	protected String getQueryString(String database) {
		String query="select count(*) from "+database+"."+this.table+" where (payment_type='"+this.group+"' AND status='-1' AND updated_at between DATE_ADD(UTC_TIMESTAMP(), INTERVAL -"+time_interval+" second) AND 	UTC_TIMESTAMP())";
		
		//String query="select count(*) from "+database+".billing_transactions where (status='-1' AND updated_at between DATE_ADD('2009-08-05 17:00:00.0', INTERVAL -333333 second) AND 	'2009-08-05 17:00:00.0')";
		
		return query;
	}

}
