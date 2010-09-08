import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * 
 */

/**
 * @author npress
 *
 */
public class CustomizedJoinAlert extends CustomizedAlert{

	protected String join_clause;
	/**
	 * CustomizedJoinAlert.java
	 * @param 
	 */
	public CustomizedJoinAlert(long count, String count_unit, char comparative_op,
			long time_quantity, String time_unit, String field_to_query,
			ArrayList<String[]> game_env, String table, long[] subscribers,
			String[] conditions, String aggregate, char paused, String join_clause) {
		super(count, count_unit, comparative_op, time_quantity, time_unit,
				field_to_query, game_env, table, subscribers,conditions, aggregate, paused);
		this.join_clause=join_clause;
	}
	public CustomizedJoinAlert(long count, String count_unit, char comparative_op,
			long time_quantity, String time_unit, String field_to_query,
			ArrayList<String[]> game_env, String table, long[] subscribers,
			String[] conditions, String aggregate,
			long alert_id, char paused, String join_clause ) {
		super(count, count_unit, comparative_op, time_quantity, time_unit, field_to_query, 
				game_env, table, subscribers, conditions, aggregate, alert_id, paused);
		this.join_clause=join_clause;
		
	}
	@Override
	protected String getQueryString(String database) {
		String query = "USE "+database+"; select "+ this.aggregate + " from ("+ getTable()+
		") WHERE "+getConditions()+" AND ("+this.table+"."+this.field_to_query+" between " +
		"DATE_ADD(UTC_TIMESTAMP(), INTERVAL -"+time_interval+" SECOND) AND UTC_TIMESTAMP())";
		
		return query;
	}
	protected String getTable(){
		return this.join_clause;
	}
	public void storeAlert(){
		super.storeAlert();
		try{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			this.connect = DriverManager.getConnection("jdbc:mysql://"+AlertSystem.alert_system_ip+"/?allowMultiQueries=true", AlertSystem.alert_system_user, AlertSystem.alert_system_passwd);
			statement=connect.createStatement();
			statement.executeUpdate("update alert_system.alerts set join_clause='"+this.join_clause+
					"' WHERE id="+this.alert_id);
			
		}
		catch(Exception e){
			System.err.println("Error storing the CustomizedJoinAlert to the db!");
			System.err.print(e.getMessage());
			System.exit(1);
			
		}
		finally{
			close();
		}
	}
	/**
	 *main  
	 *CustomizedJoinAlert  
	 *void 
	 */
	public static void main(String[] args) {
		long[] subscribers= {(long)124, (long)126};
		String[] conditions={"users.activated_at IS NULL", "game_accounts.last_in_game_at is NOT NULL"};
		String aggregate= "count(*)";
		
		ArrayList<String[]> game_env= new ArrayList<String[]>();
		String[] e= new String[5];
		e[0]= "animaljam";
		e[1]= "development";
		e[2]="10.42.42.110";
		e[3]="npress";
		e[4]="npress123";
		game_env.add(e.clone()); //Any object that gets reinserted must be cloned before the last time.
		
		e[0] = "freakypets";
		game_env.add(e.clone());  
		CustomizedJoinAlert cja=new CustomizedJoinAlert(0, "users", '<',
				9, "month", "created_at",
				game_env, "users", subscribers,
				conditions, aggregate, '0', 
				"game_accounts JOIN users ON game_accounts.user_id= users.id");
		//cja.storeAlert();
		
		System.out.println(cja);
		//System.out.println(cja);
		

	}

}
