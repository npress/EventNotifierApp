import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 
 */

/**
 * @author npress
 *
 */
public class BillingFulfillments extends PercentAlert{

	
	public BillingFulfillments(long count, char comparative_op, long time_quantity, 
			String time_unit,
			ArrayList<String[]> game_env, long[] subs, char paused) {
		super(count, "% fulfilled transactions", comparative_op, time_quantity, 
				time_unit, "fulfillment_action", "billing_transaction_id",  
				game_env, "billing_fulfillment_logs", subs, paused);
		
	}
	public BillingFulfillments(long count, char comparative_op, long time_quantity, 
			String time_unit,
			ArrayList<String[]> game_env, long[] subs, long alert_id
			, char paused) {
		super(count, "% fulfilled transactions", comparative_op, time_quantity, 
				time_unit, "fulfillment_action", "billing_transaction_id",  
				game_env, "billing_fulfillment_logs", subs, alert_id, paused);
		
	}
	/**
	 *main - 
	 *BillingFulfillments-  GROUP BY billing_transaction_id;
	 *void - 
	 */
	public static void main(String[] args) {
		long[] subs={1,2};
		String[] game_env_ary={"freakypets", "development", AlertSystem.alert_system_ip, "npress", "npress123"};
		ArrayList<String[]> game_env= new ArrayList(2);
		game_env.add(game_env_ary);
		AlertSystem as= new AlertSystem();
		
		BillingFulfillments regAlert=(BillingFulfillments)as.createAlertFromID(5838) ;
		//regAlert.storeAlert();
		//System.out.println(regAlert.getQueryString("crimecraft_development"));
		HashMap hash= new HashMap<String, Object>();
		hash.put("game_env", game_env);
		
		as.editAlert(5838, hash);
		System.out.println(regAlert.getNotified());

	}
	protected String getQueryString(String database){
		String queryStr="select count(*) from (select distinct("+this.field2_to_query+")" +
				" from "+database+"."+table+"  where (log_type='txn') AND" +
				"("+this.field2_to_query+" is not null) AND " +
						"(updated_at between DATE_ADD(UTC_TIMESTAMP(), INTERVAL -"+time_interval+" second) " +
								"AND UTC_TIMESTAMP())) as t";
		queryStr+= "; " +
				"select count(*) from (select distinct("+this.field2_to_query+") from "+database+"."+table
				+" WHERE (updated_at between DATE_ADD(UTC_TIMESTAMP(), INTERVAL -"+time_interval+" second) " +
				"AND UTC_TIMESTAMP())AND ("+this.field_to_query+"='END') AND log_type='txn' AND " 
				+this.field2_to_query+" is not null) as t;";		
		System.out.println(queryStr);
		return queryStr;
	}

}
