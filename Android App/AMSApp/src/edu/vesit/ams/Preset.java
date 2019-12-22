package edu.vesit.ams;

import java.text.SimpleDateFormat;
import java.util.Locale;

import android.os.Parcel;
import android.os.Parcelable;

public class Preset implements Parcelable
{
	private String presetTableName, teacherUsername, teacherHashedPW, teacherSalt;
	private double DurationHours;
	private int numStudents;
	
	public static SimpleDateFormat timeUserDisplayFormat = new SimpleDateFormat("hh:mm a", Locale.ENGLISH), userDisplayAttendanceDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
	
	public Preset()
	{
		
	}
	
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
	/*************************************************************************/
	/*************************************************************************/
	
	//Tutorial:		http://shri.blog.kraya.co.uk/2010/04/26/android-parcel-data-to-pass-between-activities-using-parcelable-classes/
	
	@Override
	public int describeContents()
	{
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(presetTableName);
		dest.writeString(teacherUsername);
		dest.writeString(teacherHashedPW);
		dest.writeString(teacherSalt);
		dest.writeDouble(DurationHours);
		dest.writeInt(numStudents);
	}
	public Preset(Parcel in)
	{
		readFromParcel(in);
	}

	private void readFromParcel(Parcel in)
	{
		presetTableName = in.readString();
		teacherUsername = in.readString();
		teacherHashedPW = in.readString();
		teacherSalt = in.readString();
		DurationHours = in.readDouble();
		numStudents = in.readInt();
	}
	public static final Parcelable.Creator<Preset> CREATOR = new Parcelable.Creator<Preset>()
	{
		@Override
		public Preset createFromParcel(Parcel source)
		{
			return new Preset(source);
		}
		@Override
		public Preset[] newArray(int size)
		{
			return new Preset[size];
		}
	};
}
