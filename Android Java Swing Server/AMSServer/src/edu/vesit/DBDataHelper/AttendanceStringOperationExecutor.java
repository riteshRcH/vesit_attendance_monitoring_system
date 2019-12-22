package edu.vesit.DBDataHelper;

import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import edu.vesit.ams.AttendanceRecord;

public class AttendanceStringOperationExecutor
{
	private HikariDataSource ds;
	//private Connection conn;
	//private Statement stmt;
	//private ResultSet rs;
	public boolean acceptAttendanceStringOperations;
	
	public static SimpleDateFormat DBStoragelectureTimeFormat = new SimpleDateFormat("HH:mm"), timeUserDisplayFormat = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
	public static SimpleDateFormat userDisplayAttendanceDateFormat = new SimpleDateFormat("dd/MM/yyyy"), DBAttendanceDateStorageFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	public LinkedHashMap<String, String> currentSessionAwaitingUserConfirmationOperations = new LinkedHashMap<String, String>(100, 0.75F, true);	//(initialCapacity, float loadFactor, boolean accessOrder)
	//Key(String) = Client Threads unique Name => Value(String) = Client Threads requested operation identified by chosenMode~true/false(true if intermediate result sent else false)~opeartionSuccessIndicator(true for successfull deletion/insertion/updation else false)
	
	Logger logger = LoggerFactory.getLogger(AttendanceStringOperationExecutor.class);

	public AttendanceStringOperationExecutor(String DBVendor, String DBServerIPPort)
	{
		HikariConfig config = new HikariConfig();
		config.setMinimumPoolSize(22);
		config.setMaximumPoolSize(100);
		config.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
		config.addDataSourceProperty("url", "jdbc:"+DBVendor+"://"+DBServerIPPort);
		config.addDataSourceProperty("user", MainFrame.DBusername);
		config.addDataSourceProperty("password", MainFrame.DBpassword);

		ds = new HikariDataSource(config);
		
		/*Class.forName("com.mysql.jdbc.Driver");
		conn = DriverManager.getConnection("jdbc:"+DBVendor+"://"+DBServerIPPort, MainFrame.DBusername, MainFrame.DBpassword);
		stmt = conn.createStatement();*/
		//System.out.println("[ "+AMSServer.logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSDBDataHelper.LoginHandler] Connection for Attendance String Operation Execution established, ready to accept AttendanceStringOperations ..");
		//logger.info("Connection for Attendance String Operation Execution established, ready to accept AttendanceStringOperations ..");
		logger.info("Data Source for Attendance String Operation Execution established, ready to accept AttendanceStringOperations ..");
		acceptAttendanceStringOperations = true;
	}
	//removed synchronized modifier
	public ArrayList<edu.vesit.ams.AttendanceRecord> executeAttendanceStringOperation(String clientServicingThreadUniqueName, String selectedDBNameDotPresetTableName, int numStudents, String queryToExecute, String chosenMode)throws Exception
	{
		if(acceptAttendanceStringOperations)
		{
			Statement stmt = ds.getConnection().createStatement();
			if(chosenMode.equals("SINGLE View") || chosenMode.equals("BATCH View"))
				return popualateIntermediateResult(selectedDBNameDotPresetTableName, numStudents, queryToExecute);
			else if(chosenMode.equals("SINGLE Delete") || chosenMode.equals("BATCH Delete"))
			{
				if(currentSessionAwaitingUserConfirmationOperations.containsKey(clientServicingThreadUniqueName) && currentSessionAwaitingUserConfirmationOperations.get(clientServicingThreadUniqueName).equals(chosenMode+"~"+Boolean.toString(true)+"~"+Boolean.toString(false)))
				{
					if(stmt.executeUpdate(queryToExecute)>0)	//num(rows) affected>0 for deletion
						currentSessionAwaitingUserConfirmationOperations.put(clientServicingThreadUniqueName, chosenMode+"~"+Boolean.toString(true)+"~"+Boolean.toString(true));
					return null;
				}else
				{
					currentSessionAwaitingUserConfirmationOperations.put(clientServicingThreadUniqueName, chosenMode+"~"+Boolean.toString(true)+"~"+Boolean.toString(false));
					//currentSessionAwaitingUserConfirmationOperations.put(clientServicingThreadUniqueName, chosenMode+"~"+false);	//false because not yet sent intermediate result to client for further confirmation
					return popualateIntermediateResult(selectedDBNameDotPresetTableName, numStudents, queryToExecute);					
				}
			}else if(chosenMode.equals("SINGLE Insert/Update"))
			{
				if(currentSessionAwaitingUserConfirmationOperations.containsKey(clientServicingThreadUniqueName) && currentSessionAwaitingUserConfirmationOperations.get(clientServicingThreadUniqueName).equals(chosenMode+"~"+Boolean.toString(true)+"~"+Boolean.toString(false)))
				{
					if(queryToExecute.startsWith("Command=deletingOldBeforeKeepNewOnly~"))				//Delete query before insertion of keep new only option during updation
					{
						queryToExecute = queryToExecute.substring(queryToExecute.indexOf('~')+1);
						if(stmt.executeUpdate(queryToExecute)>0)
						{
							String temp = queryToExecute.substring(queryToExecute.indexOf(" where"));
							temp = "select * from "+selectedDBNameDotPresetTableName+temp;							//after deletion select that attendance Record/Tuple with conditions that would select that tuple which was deleted so ArrayList of size 0 sent back just to check successful deletion at AMSServer 
							return popualateIntermediateResult(selectedDBNameDotPresetTableName, numStudents, temp);
						}
					}else
					{
						if(stmt.executeUpdate(queryToExecute)>0)	//num(rows) affected>0 for insertion/updation
							currentSessionAwaitingUserConfirmationOperations.put(clientServicingThreadUniqueName, chosenMode+"~"+Boolean.toString(true)+"~"+Boolean.toString(true));
					}
					return null;
				}else
				{
					currentSessionAwaitingUserConfirmationOperations.put(clientServicingThreadUniqueName, chosenMode+"~"+Boolean.toString(true)+"~"+Boolean.toString(false));
					//currentSessionAwaitingUserConfirmationOperations.put(clientServicingThreadUniqueName, chosenMode+"~"+false);	//false because not yet sent intermediate result to client for further confirmation
					return popualateIntermediateResult(selectedDBNameDotPresetTableName, numStudents, queryToExecute);
				}
			}else if(chosenMode.equals("BATCH Insert/Update"))
			{
				if(currentSessionAwaitingUserConfirmationOperations.containsKey(clientServicingThreadUniqueName) && currentSessionAwaitingUserConfirmationOperations.get(clientServicingThreadUniqueName).equals(chosenMode+"~"+Boolean.toString(true)+"~"+Boolean.toString(false)))
				{
					if(queryToExecute.startsWith("Command: isLastQuery=false~"))
					{
						String insertUpdateQueryToExecute = queryToExecute.substring(queryToExecute.indexOf('~')+1, queryToExecute.lastIndexOf('~'));
						if(stmt.executeUpdate(insertUpdateQueryToExecute)>0)
						{
							String whereCondition = queryToExecute.substring(queryToExecute.lastIndexOf("~")+1);
							//after insertion of non-last attendance record select that just inserted attendance record using the where condition given by AMSServer => format:		Command: isLastQuery=false~insert/Update Query~ where select condition for that insert/Update Query to confirm insertion/updation
							return popualateIntermediateResult(selectedDBNameDotPresetTableName, numStudents, "select * from "+selectedDBNameDotPresetTableName+whereCondition);
						}
					}else if(queryToExecute.startsWith("Command: isLastQuery=true~"))
					{
						queryToExecute = queryToExecute.substring(queryToExecute.indexOf('~')+1);
						if(stmt.executeUpdate(queryToExecute)>0)
							currentSessionAwaitingUserConfirmationOperations.put(clientServicingThreadUniqueName, chosenMode+"~"+Boolean.toString(true)+"~"+Boolean.toString(true));
					}else if(queryToExecute.startsWith("Command=deletingOldBeforeKeepNewOnly~"))
					{
						queryToExecute = queryToExecute.substring(queryToExecute.indexOf('~')+1);
						if(stmt.executeUpdate(queryToExecute)>0)
						{
							String whereCondition = queryToExecute.substring(queryToExecute.indexOf(" where"));
							//after deletion select that attendance Record/Tuple with conditions that would select that tuple which was deleted so ArrayList of size 0 sent back just to check successful deletion at AMSServer 
							return popualateIntermediateResult(selectedDBNameDotPresetTableName, numStudents, "select * from "+selectedDBNameDotPresetTableName+whereCondition);
						}
					}
					return null;
				}else
				{
					currentSessionAwaitingUserConfirmationOperations.put(clientServicingThreadUniqueName, chosenMode+"~"+Boolean.toString(true)+"~"+Boolean.toString(false));
					//currentSessionAwaitingUserConfirmationOperations.put(clientServicingThreadUniqueName, chosenMode+"~"+false);	//false because not yet sent intermediate result to client for further confirmation
					return popualateIntermediateResult(selectedDBNameDotPresetTableName, numStudents, queryToExecute);
				}
			}
		}else
			return null;
		return null;
	}
	private ArrayList<edu.vesit.ams.AttendanceRecord> popualateIntermediateResult(String selectedDBNameDotPresetTableName, int numStudents, String queryToExecute)throws Exception
	{
		ArrayList<edu.vesit.ams.AttendanceRecord> allDesiredTuplesIntermediateResult = new ArrayList<edu.vesit.ams.AttendanceRecord>();
		//rs = stmt.executeQuery(queryToExecute);
		ResultSet rs = ds.getConnection().createStatement().executeQuery(queryToExecute);
		
		AttendanceRecord attendanceRecord;
		while(rs.next())
		{
			attendanceRecord = new AttendanceRecord(rs.getDate("attendanceDate"), rs.getTime("lectureStartTime"), rs.getTime("lectureEndTime"));
			for(int i=1;i<=numStudents;i++)
				attendanceRecord.addAttendanceOfNewRollNum(i, rs.getString(Integer.toString(i)));
			allDesiredTuplesIntermediateResult.add(attendanceRecord);
			attendanceRecord = null;
		}
		rs.close();
		return allDesiredTuplesIntermediateResult;
	}
	public void toggleAcceptAttendanceStringOperations()
	{
		this.acceptAttendanceStringOperations = !this.acceptAttendanceStringOperations;
		//System.out.println("[ "+AMSServer.logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSDBDataHelper.AttendanceRecord] "+(this.acceptAttendanceStringOperations?"":"Attendance String Operations Execution Disabled, NOT ")+"Ready to accept attendance String operations!");
		logger.info((this.acceptAttendanceStringOperations?"":"Attendance String Operations Execution Disabled, NOT ")+"Ready to accept attendance String operations!");
	}
	public HikariDataSource getDataSource()
	{
		return ds;
	}
}
