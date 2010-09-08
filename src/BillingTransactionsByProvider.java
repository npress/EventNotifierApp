import java.util.ArrayList;


/**
 * @author npress
 *
 */
public class BillingTransactionsByProvider extends AlertByGroup{

	/**
	 * 
	 */
	public BillingTransactionsByProvider(long count, char comparative_op, long time_quantity, 
			String time_unit, ArrayList<String[]> game_env, String provider, 
			long[] subs, char paused) {
		super(count, "initiated transactions", comparative_op, time_quantity, 
				time_unit, "status", 
				game_env, "billing_transactions", provider, subs, paused);
		
	}
	public BillingTransactionsByProvider(long count, char comparative_op, long time_quantity, 
			String time_unit, ArrayList<String[]> game_env, String provider,
			long[] subs,long alert_id, char paused) {
		super(count, "initiated transactions", comparative_op, time_quantity, 
				time_unit, "status", 
				game_env, "billing_transactions", provider, subs, alert_id,
				paused);
		
	}
	/**WebUserRegistrationselse if(alert_type.equals("WebUserRegistrations")){
	    	return new WebUserRegistrations(count, comparative_op, time_quantity,
	    			time_unit, game_env, sublist);
		}
	 *main - 
	 *BillingTransactionsByProvider- 
	 *void - 
	 */
	public static void main(String[] args) {
		String[] game_env_ary={"crimecraft", "development", "192.168.3.235"};
		ArrayList<String[]> game_env= new ArrayList<String[]>(2);
		game_env.add(game_env_ary);
		long []subs={2, 3, 5};
		BillingTransactionsByProvider billingAlert=new 
		BillingTransactionsByProvider(2, '<', 14, "month", game_env, "paypal", 
				subs, '0');
		System.out.println(billingAlert.getQueryString("crimecraft_development"));
		//System.out.println(billingAlert.getNotified());

	}

	@Override
	protected String getQueryString(String database) {
		//String query= "select count(*) from "+database+".billing_transactions where (payment_type='"+this.group+"' AND updated_at between DATE_ADD('2009-08-05 17:00:00.0', INTERVAL -"+this.time_interval+" second) AND 	'2009-08-05 17:00:00.0')";
		String query= "select count(*) from "+database+".billing_transactions where (payment_type='"+this.group+"' AND updated_at between DATE_ADD(UTC_TIMESTAMP(), INTERVAL -"+this.time_interval+" second) AND UTC_TIMESTAMP())";
		
		return query;
	}

}
