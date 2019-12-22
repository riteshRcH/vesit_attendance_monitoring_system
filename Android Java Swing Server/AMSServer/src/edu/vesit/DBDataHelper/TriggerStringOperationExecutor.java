package edu.vesit.DBDataHelper;

import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import edu.vesit.ams.AttendanceRecord;

public class TriggerStringOperationExecutor
{
	private HikariDataSource ds;
	//private Connection conn;
	//private Statement stmt;
	//private ResultSet rs;
	public boolean acceptTriggerStringOperations;
	
	public static SimpleDateFormat DBStoragelectureTimeFormat = new SimpleDateFormat("HH:mm"), timeUserDisplayFormat = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
	public static SimpleDateFormat userDisplayAttendanceDateFormat = new SimpleDateFormat("dd/MM/yyyy"), DBAttendanceDateStorageFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	public LinkedHashMap<String, String> currentSessionAwaitingUserConfirmationOperations = new LinkedHashMap<String, String>(100, 0.75F, true);	//(initialCapacity, float loadFactor, boolean accessOrder)
	//Key(String) = Client Threads unique Name => Value(String) = Client Threads requested operation identified by chosenMode~true/false(true if intermediate result sent else false)~opeartionSuccessIndicator(true for successfull deletion/insertion/updation else false)
	
	Logger logger = LoggerFactory.getLogger(TriggerStringOperationExecutor.class);

	public TriggerStringOperationExecutor(String DBVendor, String DBServerIPPort)
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
		//System.out.println("[ "+AMSServer.logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSDBDataHelper.LoginHandler] Connection for Trigger String Operation Execution established, ready to accept TriggerStringOperations ..");
		//logger.info("Connection for Trigger String Operation Execution established, ready to accept TriggerStringOperations ..");
		logger.info("Data Source for Trigger String Operation Execution established, ready to accept TriggerStringOperations ..");
		acceptTriggerStringOperations = true;
	}
	//removed synchronized modifier
	public ArrayList<edu.vesit.ams.FutureTriggersRecord> executeTriggerStringOperation(String clientServicingThreadUniqueName, String selectedDBName, int numStudents, String queryToExecute, String chosenMode)throws Exception
	{
		if(acceptTriggerStringOperations)
		{
			Statement stmt = ds.getConnection().createStatement();
			if(chosenMode.equals("TRIGGER SINGLE View") || chosenMode.equals("TRIGGER BATCH View"))
				return popualateIntermediateResult(numStudents, queryToExecute);
			else if(chosenMode.equals("TRIGGER SINGLE Delete") || chosenMode.equals("TRIGGER BATCH Delete"))
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
					return popualateIntermediateResult(numStudents, queryToExecute);					
				}
			}else if(chosenMode.equals("TRIGGER SINGLE Insert/Update"))
			{
				if(currentSessionAwaitingUserConfirmationOperations.containsKey(clientServicingThreadUniqueName) && currentSessionAwaitingUserConfirmationOperations.get(clientServicingThreadUniqueName).equals(chosenMode+"~"+Boolean.toString(true)+"~"+Boolean.toString(false)))
				{
					if(queryToExecute.startsWith("Command=deletingOldBeforeKeepNewOnly~"))				//Delete query before insertion of keep new only option during updation
					{
						queryToExecute = queryToExecute.substring(queryToExecute.indexOf('~')+1);
						if(stmt.executeUpdate(queryToExecute)>0)
						{
							//after deletion select that trigger Record/Tuple with conditions that would select that tuple which was deleted so ArrayList of size 0 sent back just to check successful deletion at AMSServer 
							return popualateIntermediateResult(numStudents, "select * from "+selectedDBName+".future_triggers"+queryToExecute.substring(queryToExecute.indexOf(" where")));
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
					return popualateIntermediateResult(numStudents, queryToExecute);
				}
			}else if(chosenMode.equals("TRIGGER BATCH Insert/Update"))
			{
				if(currentSessionAwaitingUserConfirmationOperations.containsKey(clientServicingThreadUniqueName) && currentSessionAwaitingUserConfirmationOperations.get(clientServicingThreadUniqueName).equals(chosenMode+"~"+Boolean.toString(true)+"~"+Boolean.toString(false)))
				{
					if(queryToExecute.startsWith("Command: isLastQuery=false~"))
					{
						String insertUpdateQueryToExecute = queryToExecute.substring(queryToExecute.indexOf('~')+1, queryToExecute.lastIndexOf('~'));
						if(stmt.executeUpdate(insertUpdateQueryToExecute)>0)
						{
							String whereCondition = queryToExecute.substring(queryToExecute.lastIndexOf("~")+1);
							//after insertion of non-last trigger record select that just inserted trigger record using the where condition given by AMSServer => format:		Command: isLastQuery=false~insert/Update Query~ where select condition for that insert/Update Query to confirm insertion/updation
							return popualateIntermediateResult(numStudents, "select * from "+selectedDBName+".future_triggers"+whereCondition);
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
							//after deletion select that trigger Record/Tuple with conditions that would select that tuple which was deleted so ArrayList of size 0 sent back just to check successful deletion at AMSServer 
							return popualateIntermediateResult(numStudents, "select * from "+selectedDBName+".future_triggers"+whereCondition);
						}
					}
					return null;
				}else
				{
					currentSessionAwaitingUserConfirmationOperations.put(clientServicingThreadUniqueName, chosenMode+"~"+Boolean.toString(true)+"~"+Boolean.toString(false));
					//currentSessionAwaitingUserConfirmationOperations.put(clientServicingThreadUniqueName, chosenMode+"~"+false);	//false because not yet sent intermediate result to client for further confirmation
					return popualateIntermediateResult(numStudents, queryToExecute);
				}
			}
		}else
			return null;
		return null;
	}
	private ArrayList<edu.vesit.ams.FutureTriggersRecord> popualateIntermediateResult(int numStudents, String queryToExecute)throws Exception
	{
		ArrayList<edu.vesit.ams.FutureTriggersRecord> allDesiredTuplesIntermediateResult = new ArrayList<edu.vesit.ams.FutureTriggersRecord>();
		//rs = stmt.executeQuery(queryToExecute);
		ResultSet rs = ds.getConnection().createStatement().executeQuery(queryToExecute);
		
		String presetTableName;
		AttendanceRecord attendanceRecord;
		while(rs.next())
		{
			presetTableName = rs.getString("preset_table_name");
			attendanceRecord = new AttendanceRecord(rs.getDate("to_insert_in_preset_table_attendanceDate"), rs.getTime("to_insert_in_preset_table_lectureStartTime"), rs.getTime("to_insert_in_preset_table_lectureEndTime"));
			
			String presenteesColumnValue =  rs.getString("presentees"), absenteesColumnValue =  rs.getString("absentees"), medical_leavesColumnValue = rs.getString("medical_leaves"), other_leavesColumnValue = rs.getString("other_leaves");
			if(presenteesColumnValue.equals("ALL"))
			{
				for(int i=1;i<=numStudents;i++)
					attendanceRecord.addAttendanceOfNewRollNum(i, "P");
			}else if(absenteesColumnValue.equals("ALL"))
			{
				for(int i=1;i<=numStudents;i++)
					attendanceRecord.addAttendanceOfNewRollNum(i, "A");
			}else
			{
				String presentees[] = presenteesColumnValue.split("[,]"), absentees[] = absenteesColumnValue.split("[,]");
				
				//sorting presentees and absentees so that i can perform binary search
				int i = 0;
				List<String> temp = Arrays.asList(presentees);
				Collections.sort(temp);
				presentees = new String[temp.size()];
				for(String s:temp)
					presentees[i++] = s;
				
				i = 0;
				temp = Arrays.asList(absentees);
				Collections.sort(temp);
				absentees = new String[temp.size()];
				for(String s:temp)
					absentees[i++] = s;

				String medical_leaves[] = medical_leavesColumnValue.split("[~]"), other_leaves[] = other_leavesColumnValue.split("[~]");
				
				LinkedHashMap<Integer, String> medicalReasons = new LinkedHashMap<Integer, String>(), otherReasons = new LinkedHashMap<Integer, String>();			//Integer RollNum => Reason
				if(!medical_leavesColumnValue.equals("None"))
					for(String s:medical_leaves)
						medicalReasons.put(Integer.parseInt(s.substring(0, s.indexOf('('))), s.substring(s.indexOf('(')+1, s.lastIndexOf(')')).trim());
				if(!other_leavesColumnValue.equals("None"))
					for(String s:other_leaves)
						otherReasons.put(Integer.parseInt(s.substring(0, s.indexOf('('))), s.substring(s.indexOf('(')+1, s.lastIndexOf(')')).trim());
				
				for(i=1;i<=numStudents;i++)
				{
					if(Arrays.binarySearch(presentees, Integer.toString(i))>=0)				//found in presentees
						attendanceRecord.addAttendanceOfNewRollNum(i, "P");
					else if(Arrays.binarySearch(absentees, Integer.toString(i))>=0)				//found in absentees
						attendanceRecord.addAttendanceOfNewRollNum(i, "A");
					else if(medicalReasons.containsKey(i))
						attendanceRecord.addAttendanceOfNewRollNum(i, "M("+medicalReasons.get(i)+")");
					else if(otherReasons.containsKey(i))
						attendanceRecord.addAttendanceOfNewRollNum(i, "O("+otherReasons.get(i)+")");
					else
						attendanceRecord.addAttendanceOfNewRollNum(i, null);								//this also takes care for "None" value in presentees/absentees/medical/other reasons => so in all 4 not found and defaults to here 
				}
			}
			allDesiredTuplesIntermediateResult.add(new edu.vesit.ams.FutureTriggersRecord(presetTableName, attendanceRecord));
			presetTableName = null;
			attendanceRecord = null;
		}
		rs.close();
		return allDesiredTuplesIntermediateResult;
	}
	public void toggleAcceptTriggerStringOperations()
	{
		this.acceptTriggerStringOperations = !this.acceptTriggerStringOperations;
		//System.out.println("[ "+AMSServer.logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSDBDataHelper.AttendanceRecord] "+(this.acceptTriggerStringOperations?"":"Trigger String Operations Execution Disabled, NOT ")+"Ready to accept trigger String operations!");
		logger.info((this.acceptTriggerStringOperations?"":"Trigger String Operations Execution Disabled, NOT ")+"Ready to accept trigger String operations!");
	}
	public HikariDataSource getDataSource()
	{
		return ds;
	}
}
