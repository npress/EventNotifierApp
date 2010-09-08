import java.util.ArrayList;
import java.util.HashMap;



/**
 * @author npress
 *
 */
public class BillingCompletionsByProvider extends AlertByGroup {

	/**
	 * BillingCompletionsByProvider
	 * @param 
	 */
	public BillingCompletionsByProvider(long count, char comparative_op, long time_quantity, 
			String time_unit, ArrayList<String[]> game_env, String provider, 
			long[] subs, char paused) {
		super(count, "completed transactions", comparative_op, time_quantity, 
				time_unit, "status", 
				game_env, "billing_transactions", provider, subs, paused);
	}
	public BillingCompletionsByProvider(long count, char comparative_op, 
			long time_quantity, 
			String time_unit, ArrayList<String[]> game_env, String provider, 
			long[] subs, long alert_id, char paused) {
		super(count, "completed transactions", comparative_op, time_quantity, 
				time_unit, "status", 
				game_env, "billing_transactions", provider, subs, alert_id, paused);
	}
	/**
	 *main  
	 *void 
	 */
	public static void main(String[] args) {
		long[] subs={1,2};
		String[] game_env_ary={"animaljam", "development", AlertSystem.alert_system_ip,
				"npress", "npress123"};
		ArrayList<String[]> game_env= new ArrayList(2);
		game_env.add(game_env_ary);
		AlertSystem as= new AlertSystem();
		
		BillingCompletionsByProvider regAlert=(BillingCompletionsByProvider )
		as.createAlertFromID(5621) ;
		//regAlert.storeAlert();
		//System.out.println(regAlert.getQueryString("crimecraft_development"));
		HashMap hash= new HashMap<String, Object>();
		hash.put("game_env", game_env);
		
		//as.editAlert(5621, hash);
		System.out.println(regAlert.getNotified());

	}

	@Override
	protected String getQueryString(String database) {
		String query="select count(*) from "+database+"."+table+" where (status='1' AND payment_type='"+this.group+"' AND created_at between DATE_ADD(UTC_TIMESTAMP(), INTERVAL -"+time_interval+" second) AND UTC_TIMESTAMP())";
		return query;
	}

}
