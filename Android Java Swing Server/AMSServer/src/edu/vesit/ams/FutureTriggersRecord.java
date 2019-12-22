package edu.vesit.ams;

import java.io.Serializable;


public class FutureTriggersRecord implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8859972700164314721L;
	private String presetTableName;
	private AttendanceRecord attendanceRecord;	//Composition/Aggregation(HAS-A) better to use as compared to Inheritance(IS-A) => FutureTriggersRecord HAS-A AttendanceRecord is better than FutureTriggersRecord IS-A AttendanceRecord
	
	public FutureTriggersRecord(String presetTableName, AttendanceRecord attendanceRecord)
	{
		this.presetTableName = presetTableName;
		this.attendanceRecord = attendanceRecord;
	}
	public String getPresetTableName()
	{
		return presetTableName;
	}
	public AttendanceRecord getAttendanceRecord()
	{
		return attendanceRecord;
	}
}
