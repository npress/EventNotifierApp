import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;


public class CustomizedAlert extends Alert {
	String aggregate;
	String[] conditions;
	
	public CustomizedAlert(long count, String count_unit, char comparative_op,
			long time_quantity, String time_unit, String field_to_query,
			ArrayList<String[]> game_env, String table, long[] subscribers,
			String[] conditions, String aggregate, char paused) {
		super(count, count_unit, comparative_op, time_quantity, time_unit,
				field_to_query, game_env, table, subscribers, paused);
		this.conditions=conditions;
		this.aggregate=aggregate;
	}

	public CustomizedAlert() {
		System.out.println("The default constructor for CustomizedAlert was called.");
	}

	public CustomizedAlert(long count, String count_unit, char comparative_op,
			long time_quantity, String time_unit, String field_to_query,
			ArrayList<String[]> game_env, String table, long[] subscribers,
			String[] conditions, String aggregate,
			long alert_id, char paused ) {
		super(count, count_unit, comparative_op, time_quantity, time_unit,
				field_to_query, game_env, table, subscribers, alert_id, paused);
		this.conditions=conditions;
		this.aggregate=aggregate;
		
	}

	@Override
	protected String getQueryString(String database) {
		String query = "select "+ this.aggregate + " from "+ database+"."+ getTable()+
		" WHERE "+getConditions()+" AND ("+this.field_to_query+" between " +
		"DATE_ADD(UTC_TIMESTAMP(), INTERVAL -"+time_interval+" SECOND) AND UTC_TIMESTAMP())";
		
		return query;
	}
	public String toString(){
		
		String query="";
		for(int i=0; i<game_env.size(); i++){
			String gameName=this.game_env.get(i)[0];
			String envir=this.game_env.get(i)[1];
			query+=getQueryString(this.getDBName(gameName, envir))+"\n";
		}
		return query;
	}
	protected String getConditions() {
		String condition="("+this.conditions[0]+")";
		for(int i=1; i<conditions.length; i++){
			condition+=" AND ("+this.conditions[i]+")";
		}
		
		return condition;
	}

	/*
	 * Returns the tablename on which the query is executed, but enables child classes
	 * to specify a more complicated table, such as the result of a JOIN statement.
	 */
	protected String getTable() {
		
		return this.table;
	}
	
	public void storeAlert(){
		super.storeAlert();
		try{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			this.connect = DriverManager.getConnection("jdbc:mysql://"+AlertSystem.alert_system_ip+"/?allowMultiQueries=true", AlertSystem.alert_system_user, AlertSystem.alert_system_passwd);
			statement=connect.createStatement();
			statement.executeUpdate("update alert_system.alerts set aggregate='"+this.aggregate+"' WHERE " +
					"id="+this.alert_id);
			for(int i=0; i<this.conditions.length; i++){
				statement = connect.createStatement();
				statement.executeUpdate("INSERT INTO alert_system.customized_alert_conditions" +
						"(`alert_id`," +
						" `condition`)" + 
	                    " VALUES ('"+this.alert_id+"', \""+this.conditions[i]+"\")"
	                    ,Statement.RETURN_GENERATED_KEYS);
			}
		}
		catch(Exception e){
			System.err.println("Error storing the CustomizedAlert to the db!");
			System.err.print(e.getMessage());
			System.exit(1);
			
		}
		finally{
			close();
		}
	}

	/**
	 *main  
	 *CustomizedAlert  
	 *void 
	 */
	public static void main(String[] args) {
		long[] subscribers= {(long)124, (long)126};
		String[] conditions={"activated_at IS NULL", "last_in_game_at is NOT NULL"};
		String aggregate= "count(*)";
		
		ArrayList<String[]> game_env= new ArrayList<String[]>();
		String[] e= new String[5];
		e[0]= "coho";
		e[1]= "development";
		e[2]="10.42.42.110";
		e[3]="npress";
		e[4]="npress123";
		game_env.add(e.clone()); //Any object that gets reinserted must be cloned before the last time.
		
		e[0] = "coho";
		game_env.add(e.clone());  
		CustomizedAlert customized=new CustomizedAlert(0, "users", '<',
				9, "month", "created_at",
				game_env, "users", subscribers,
				conditions, aggregate, '0'); 
		//customized.storeAlert();
		System.out.println(customized);
		//String note=customized.getNotified();
		//if (note!=null) System.out.println(note);
	}

}
