package edu.vesit.ams;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class AttendanceRecord implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8952568449535183203L;
	private Date attendanceDate, lectureStartTime, lectureEndTime;
	private LinkedHashMap<Integer, String> attendance = new LinkedHashMap<Integer, String>();			//RollNum key => AttendanceValue(A, P, M(reason), O(reason), null(null values means no value=>used ONLY ONLY while trigger operations = as all trigger operations serve in ADD to OLD mode for attendance manipulation so therefore null is needed))
	
	public AttendanceRecord(Date attendanceDate, Date lectureStartTime, Date lectureEndTime)
	{
		this.attendanceDate = attendanceDate;
		this.lectureStartTime = lectureStartTime;
		this.lectureEndTime = lectureEndTime;
	}
	
	public Date getAttendanceDate()
	{
		return attendanceDate;
	}
	public Date getLectureStartTime()
	{
		return lectureStartTime;
	}
	public Date getLectureEndTime()
	{
		return lectureEndTime;
	}
	public LinkedHashMap<Integer,String> getAttendance()
	{
		return attendance;
	}
	
	public boolean addAttendanceOfNewRollNum(int rollNum, String attendanceValue)
	{
		attendance.put(rollNum, attendanceValue);
		return attendanceValue==null?attendance.get(rollNum)==null:attendance.get(rollNum).equals(attendanceValue);					//ensure proper insertion by reading after insertion
	}
	
	public String toString()
	{
		StringBuffer toReturnStr = new StringBuffer();
		ArrayList<String> absentees = new ArrayList<String>(), presentees = new ArrayList<String>(), medicalLeaves = new ArrayList<String>(), otherReasonsLeave = new ArrayList<String>();
		for(Map.Entry<Integer, String> entry:attendance.entrySet())
		{
			if(entry.getValue()==null)
			{
				//nothing to code here, Value(STring) would be null ONLY ONLY WHILE TRIGGER OPERATIONS => so dont put that Key anywhere(ignore it)
			}else
			{
				if(entry.getValue().equals("P"))
					presentees.add(entry.getKey().toString());
				else if(entry.getValue().equals("A"))
					absentees.add(entry.getKey().toString());
				else if(entry.getValue().startsWith("M"))
					medicalLeaves.add(entry.getKey().toString()+entry.getValue().substring(entry.getValue().indexOf('(')));
				else if(entry.getValue().startsWith("O"))
					otherReasonsLeave.add(entry.getKey().toString()+entry.getValue().substring(entry.getValue().indexOf('(')));
			}
		}
		if(presentees.size()==attendance.size())		//ALL are present
			toReturnStr.append("Presentees: ").append("ALL(1 to "+presentees.size()+")").append(System.getProperty("line.separator"));
		else
			toReturnStr.append("Presentees: ").append(presentees.size()==0?"None":presentees.toString()).append(System.getProperty("line.separator"));
		
		if(absentees.size()==attendance.size())		//ALL are absent
			toReturnStr.append("Absentees: ").append("ALL(1 to "+absentees.size()+")").append(System.getProperty("line.separator"));
		else
			toReturnStr.append("Absentees: ").append(absentees.size()==0?"None":absentees.toString()).append(System.getProperty("line.separator"));
		
		toReturnStr.append("Medical Leaves: ").append(medicalLeaves.size()==0?"None":medicalLeaves.toString()).append(System.getProperty("line.separator"));
		toReturnStr.append("Other Reasons Leaves: ").append(otherReasonsLeave.size()==0?"None":otherReasonsLeave.toString()).append(System.getProperty("line.separator"));
		return toReturnStr.toString();
	}
}
