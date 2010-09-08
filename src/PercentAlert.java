import java.sql.DriverManager;
import java.util.ArrayList;


public abstract class PercentAlert extends Alert{

	protected String field2_to_query; //denomenator for the percentage
	public PercentAlert(long count, String count_unit, char comparative_op, long time_quantity, 
			String time_unit, String field_to_query, String field2_to_query,
			ArrayList<String[]> game_env, String table, long[] subscribers, char paused) {
		super(count, count_unit, comparative_op, time_quantity, 
				time_unit, field_to_query, 
				game_env, table, subscribers, paused);
		this.field2_to_query=field2_to_query;
		
	}
	protected boolean isMember(Class<?> classType){
		return classType.isMemberClass();
	}
	
	
	
	public PercentAlert(long count, String count_unit, char comparative_op, long time_quantity, 
			String time_unit, String field_to_query, String field2_to_query,
			ArrayList<String[]> game_env, String table, long[] subscribers, long alert_id, char paused) {
		super(count, count_unit, comparative_op, time_quantity, time_unit,
				field_to_query, game_env, table, subscribers, alert_id, paused);
		
		this.field2_to_query=field2_to_query;
		
	}


	@Override
	public String getNotificationData() {
		this.exceptionFlag=false;
		long numerator=0, denomenator=0;
		String result=null;
		boolean printNotification=false;
		this.query_result=0; //zeroing out query_result from former queries
		Object[] game_env_ary = this.game_env.toArray();
		String database;
		String game_name, environment, ip_address, game_user, game_password;
		num_notifications=0;
		for(int i=0; i<game_env_ary.length; i++){
			try {
				game_name=((String[]) game_env_ary[i])[0];
				environment=((String[]) game_env_ary[i])[1];
				// This will load the MySQL driver, each DB has its own driver
				Class.forName("com.mysql.jdbc.Driver").newInstance();
				// Setup the connection with the DB
				database=getDBName(game_name, environment);
				//database=((String[]) game_env_ary[i])[0]+"_"+((String[]) game_env_ary[i])[1];
				
				ip_address=((String[]) game_env_ary[i])[2];
				game_user=((String[]) game_env_ary[i])[3];
				game_password=((String[]) game_env_ary[i])[4];
				
				this.connect = DriverManager.getConnection("jdbc:mysql://"+ip_address+"/?allowMultiQueries=true", game_user, game_password);
				// Statements allow to issue SQL queries to the database
				statement = connect.createStatement();
				
				//check if we should poll this game_environment combination:
				String query=
					"select * from (alert_system.alert_game_environments JOIN " +
					"alert_system.alerts ON alert_game_environments.alert_id = " +
					"alerts.id )" +
					"where ip_address='"+ip_address+"' AND game='"+game_name+"'" +
					"AND alert_id='"+this.alert_id+"' AND environment='"
					+environment+"' AND " +
				"("+
					"(DATE_ADD(UTC_TIMESTAMP(), INTERVAL -alerts.time_interval SECOND) >= "+
					"alert_game_environments.last_fulfilled)" +
					" OR (alert_game_environments.last_fulfilled is NULL)" +
				")";
				resultSet=statement.executeQuery(query);
				
				if(!resultSet.next()){
					System.out.println("This game_environment is already fulfilled for " +
							"the " +
							"time interval of "+ this.time_quantity +" "+ this.time_unit+"(s).");
					continue;
							
				}
				
				// Result set get the result of the SQL query
				String queryStr=this.getQueryString(database);
				statement.execute(queryStr);
				resultSet= statement.getResultSet();
					if(resultSet.next()) {
						denomenator = resultSet.getInt(1);
						
					}
				    if(statement.getMoreResults())
				    	resultSet=statement.getResultSet();
				    else{ 
				    	System.err.println(queryStr+"\nis a malformed query.");
				    	System.exit(1);
				    }
					if (resultSet.next()) {
						numerator = resultSet.getInt(1);
						
					}
					this.query_result = (denomenator!=0)?100*numerator/denomenator: 100;
					switch(this.comparative_op){
					case '<':
						if(count<query_result)
							printNotification=true;
						break;
					case '>':
						if(count>query_result){
							printNotification=true;
							
						}
						break;
					case '=':
						if(count == query_result)
							printNotification=true;
						break;
					}
					if(printNotification){
						result=(result!=null)?result: "";
						result+= "On game: "+((String[]) game_env_ary[i])[0]+" running on environment: "+((String[]) game_env_ary[i])[1]+", "+count+" "+count_unit+" is "+ this.comparative_op +
					" "+query_result+"% in last "+time_quantity+ " " +time_unit + "(s)\n" ;
					
						//this query is true for this particular game_environment combination
						//set last_fulfilled
						statement.executeUpdate("update " +
								"alert_system.alert_game_environments set last_fulfilled=UTC_TIMESTAMP() " +
								"where ip_address='"+ip_address+"' AND game='"+game_name+"'" +
								"AND alert_id='"+this.alert_id+"' AND environment='"+environment+"'"
								);
						num_notifications++;
					}
					
			}
			catch (Exception e) {
				System.err.println("ERROR!");
				System.err.print(e.getMessage());
				this.exceptionFlag=true;
			} 
			finally {
				close();
			}
		}
		return result;
		
	}
	
	@Override
	protected String getField2ToQuery(){
		return this.field2_to_query;
	}
}
