package edu.vesit.DBDataHelper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.vesit.ams.AttendanceRecord;
import edu.vesit.ams.FutureTriggersRecord;

public class AttendanceTriggersExecutor							//actual trigger operation = trigger activation
{
	static Connection conn;
	static Statement stmt;
	static ResultSet rs;
	
	static Logger logger = LoggerFactory.getLogger(AttendanceTriggersExecutor.class);
	static String tempLogMsg = new String();
	
	public static boolean execute(String DBVendor, String DBServerIPPort, String dbName, boolean activateLeftOutBeforeTodayRecordsToo, boolean deleteWhenTriggerActivatedSuccessfully)
	{
		try
		{
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:"+DBVendor+"://"+DBServerIPPort+"/"+dbName, MainFrame.DBusername, MainFrame.DBpassword);
			logger.info("DB Connection Success for Trigger Exexution!");
			//System.out.println("[ "+edu.vesit.ams.AMSServer.logEventTimestampFormat.format(System.currentTimeMillis())+" - DBHelper ] DB Connection Success for Trigger Exexution!");
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rs = stmt.executeQuery("select * from future_triggers where to_insert_in_preset_table_attendanceDate"+(activateLeftOutBeforeTodayRecordsToo?"<=":"=")+"'"+TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(new Date())+"'");
			ArrayList<FutureTriggersRecord> allDesiredTriggersToActivate = new ArrayList<FutureTriggersRecord>();

			String presetTableName;
			AttendanceRecord attendanceRecord;
			while(rs.next())
			{
				presetTableName = rs.getString("preset_table_name");
				attendanceRecord = new AttendanceRecord(rs.getDate("to_insert_in_preset_table_attendanceDate"), rs.getTime("to_insert_in_preset_table_lectureStartTime"), rs.getTime("to_insert_in_preset_table_lectureEndTime"));
				
				int numStudents = Integer.parseInt(rs.getString("num_students"));
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
				allDesiredTriggersToActivate.add(new edu.vesit.ams.FutureTriggersRecord(presetTableName, attendanceRecord));
				presetTableName = null;
				attendanceRecord = null;
			}
			rs.close();
			
			for(int i=0;i<allDesiredTriggersToActivate.size();i++)
			{
				FutureTriggersRecord triggerRecord = allDesiredTriggersToActivate.get(i);
				
				//check if attendance Record already present or not
				String queryToExecute = "select count(*) from "+triggerRecord.getPresetTableName()+" where attendanceDate='"+TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(triggerRecord.getAttendanceRecord().getAttendanceDate())+"' and lectureStartTime='"+TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(triggerRecord.getAttendanceRecord().getLectureStartTime())+"' and lectureEndTime='"+TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(triggerRecord.getAttendanceRecord().getLectureEndTime())+"'";
				
				//System.out.print("[ "+edu.vesit.ams.AMSServer.logEventTimestampFormat.format(System.currentTimeMillis())+" - DBHelper ] Trigger Activation Results in ("+queryToExecute+") .... ");
				tempLogMsg = "Trigger Activation Results in ("+queryToExecute+") ....";
				rs = stmt.executeQuery(queryToExecute);
				rs.next();
				int cnt = rs.getInt(1);
				//System.out.println("(Count="+cnt+") "+(cnt==1?"Update":"Insert"));
				logger.debug(tempLogMsg += "(Count="+cnt+") "+(cnt==1?"Update":"Insert"));
				
				StringBuffer queryToExecuteStrBuffer;
				if(cnt==1)			//use update query
				{
					queryToExecuteStrBuffer = new StringBuffer("update "+triggerRecord.getPresetTableName()+" set ");
					for(Map.Entry<Integer, String> entry:triggerRecord.getAttendanceRecord().getAttendance().entrySet())
						if(entry.getValue()!=null)
							queryToExecuteStrBuffer.append("`"+entry.getKey()+"` = '"+entry.getValue()+"', ");
					queryToExecuteStrBuffer = new StringBuffer(queryToExecuteStrBuffer.substring(0, queryToExecuteStrBuffer.length()-2));		//remove off last comma
					queryToExecuteStrBuffer.append(" where attendanceDate='"+TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(triggerRecord.getAttendanceRecord().getAttendanceDate())+"' and lectureStartTime='"+TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(triggerRecord.getAttendanceRecord().getLectureStartTime())+"' and lectureEndTime='"+TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(triggerRecord.getAttendanceRecord().getLectureEndTime())+"'");
				}else				//use insert query
				{
					queryToExecuteStrBuffer = new StringBuffer("insert into "+triggerRecord.getPresetTableName()+"(attendanceDate, lectureStartTime, lectureEndTime, ");
					StringBuffer valuesOfInsertQuery = new StringBuffer("values('"+TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(triggerRecord.getAttendanceRecord().getAttendanceDate())+"', '"+TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(triggerRecord.getAttendanceRecord().getLectureStartTime())+"', '"+TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(triggerRecord.getAttendanceRecord().getLectureEndTime())+"', " );
					for(Map.Entry<Integer, String> entry:triggerRecord.getAttendanceRecord().getAttendance().entrySet())
						if(entry.getValue()!=null)
						{
							queryToExecuteStrBuffer.append("`"+entry.getKey()+"`, ");
							valuesOfInsertQuery.append("'"+entry.getValue()+"', ");
						}
					//removing last comma and space from query
					queryToExecuteStrBuffer = new StringBuffer(queryToExecuteStrBuffer.substring(0, queryToExecuteStrBuffer.length()-2)).append(") ");
					valuesOfInsertQuery = new StringBuffer(valuesOfInsertQuery.substring(0, valuesOfInsertQuery.length()-2)).append(")");
					queryToExecuteStrBuffer = queryToExecuteStrBuffer.toString().endsWith(",")?new StringBuffer(queryToExecuteStrBuffer.substring(0, queryToExecuteStrBuffer.length()-2)):queryToExecuteStrBuffer;		//remove off last comma
					queryToExecuteStrBuffer.append(valuesOfInsertQuery);
				}
				
				queryToExecute = queryToExecuteStrBuffer.toString();
				//System.out.print("[ "+edu.vesit.ams.AMSServer.logEventTimestampFormat.format(System.currentTimeMillis())+" - DBHelper ] Trigger Activation Operation Query("+queryToExecute+") execution initiated .... ");
				tempLogMsg = "Trigger Activation Operation Query("+queryToExecute+") execution initiated .... ";
				
				if(stmt.executeUpdate(queryToExecute)>0)
				{
					//System.out.println("Execution Success!");
					logger.debug(tempLogMsg += "Execution Success!");
					if(deleteWhenTriggerActivatedSuccessfully)
					{
						String deletionQuery = "delete from future_triggers where preset_table_name='"+triggerRecord.getPresetTableName()+"' and to_insert_in_preset_table_attendanceDate='"+TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(triggerRecord.getAttendanceRecord().getAttendanceDate())+"' and to_insert_in_preset_table_lectureStartTime='"+TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(triggerRecord.getAttendanceRecord().getLectureStartTime())+"' and to_insert_in_preset_table_lectureEndTime='"+TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(triggerRecord.getAttendanceRecord().getLectureEndTime())+"'";
						//System.out.print("[ "+edu.vesit.ams.AMSServer.logEventTimestampFormat.format(System.currentTimeMillis())+" - DBHelper ] Trigger Activation Done so delete Query's("+deletionQuery+") execution initiated .... ");
						tempLogMsg = "Trigger Activation Done so delete Query's("+deletionQuery+") execution initiated .... ";
						if(stmt.executeUpdate(deletionQuery)>0)
							logger.debug(tempLogMsg += "Execution Success!");
					}
				}
			}
			return true;
		}catch(Exception e)
		{
			logger.error(tempLogMsg += "Execution Failed!");
			e.printStackTrace();
			return false;
		}
	}
}
