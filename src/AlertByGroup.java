import java.util.ArrayList;
import java.util.HashMap;


public abstract class AlertByGroup extends Alert{
	
	protected String group;
	
	public AlertByGroup(long count, String count_unit, char comparative_op, long time_quantity, 
			String time_unit, String field_to_query, 
			ArrayList<String[]> game_env, String table, String provider, 
			long[] subscribers, char paused) {
		super(count, count_unit, comparative_op, time_quantity, time_unit, 
				field_to_query, game_env, table, subscribers, paused);
		this.group=provider;
	}
	public AlertByGroup(long count, String count_unit, char comparative_op, long time_quantity, 
			String time_unit, String field_to_query, 
			ArrayList<String[]> game_env, String table, String provider, 
			long[] subscribers,long alert_id, char paused) {
		super(count, count_unit, comparative_op, time_quantity, time_unit, 
				field_to_query, game_env, table, subscribers, alert_id,
				paused);
		this.group=provider;
	}
	public String getGroup(){
		return this.group;
	}
	
	public void finalize()throws Throwable{
		
		super.finalize();
		group=null;
	}
	protected String getCountUnit() {
		
		return this.count_unit+" for "+ this.group;
	}

}
