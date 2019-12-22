package edu.vesit.ams;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class Preset
{
	private String presetTableName, teacherUsername, teacherHashedPW, teacherSalt;
	private double DurationHours;
	private int numStudents;
	
	public static SimpleDateFormat timeUserDisplayFormat = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
	
	public String getPresetTableName()
	{
		return presetTableName;
	}
	public void setPresetTableName(String presetTableName)
	{
		this.presetTableName = presetTableName;
	}
	/*************************************************************************/
	public double getDurationHours()
	{
		return DurationHours;
	}
	public void setDurationHours(double durationHours)
	{
		DurationHours = durationHours;
	}
	/*************************************************************************/
	public String getTeacherUsername()
	{
		return teacherUsername;
	}
	public void setTeacherUsername(String teacherUsername)
	{
		this.teacherUsername = teacherUsername;
	}
	/*************************************************************************/
	public String getTeacherHashedPW()
	{
		return teacherHashedPW;
	}
	public void setTeacherHashedPW(String teacherHashedPW)
	{
		this.teacherHashedPW = teacherHashedPW;
	}
	/*************************************************************************/
	public String getTeacherSalt()
	{
		return teacherSalt;
	}
	public void setTeacherSalt(String teacherSalt)
	{
		this.teacherSalt = teacherSalt;
	}
	/*************************************************************************/
	public int getNumStudents()
	{
		return numStudents;
	}
	public void setNumStudents(int numStudents)
	{
		this.numStudents = numStudents;
	}
	/*************************************************************************/
	public String toString()
	{
		return presetTableName+System.getProperty("line.separator")+"Type: "+presetTableName.substring(presetTableName.lastIndexOf('_')+1)+System.getProperty("line.separator")+"DurationsHours: "+DurationHours+System.getProperty("line.separator")+"numStudents: "+numStudents;
	}
}
