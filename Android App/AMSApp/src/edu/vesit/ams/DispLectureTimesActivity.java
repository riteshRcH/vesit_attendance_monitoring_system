package edu.vesit.ams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class DispLectureTimesActivity extends Activity
{
	TextView txtviewShowChosenPreset;
	ListView listViewShowPresetsLectureTime;
	
	boolean isStartTime;
	File endTimesXMLFileName, startTimesXMLFileName; 
	ArrayList<String> selectedPresetsEndTimes = new ArrayList<String>(), selectedPresetsStartTimes = new ArrayList<String>();
	String selectedPresetTableName, originalSelectedPresetsEndTimesStringRepresentation,originalSelectedPresetsStartTimesStringRepresentation;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		boolean customTitleSupported = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_disp_lecture_times);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		if(customTitleSupported)
		{
			getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);
			TextView myTitleText = (TextView)findViewById(R.id.myTitle);
			myTitleText.setText("Mod Lecture Times");
			//myTitleText.setTextColor(Color.rgb(0,0,139));
		}
		
		initViews();
		
		Bundle receivedData = getIntent().getExtras();
		isStartTime = receivedData.getBoolean("edu.vesit.ams.ViewPresetsActivity.isStartTime");
		txtviewShowChosenPreset.setText("Chosen Preset: "+(selectedPresetTableName = receivedData.getString("edu.vesit.ams.ViewPresetsActivity.selectedPresetTableName")));
		if(isStartTime)
		{
			startTimesXMLFileName = new File(receivedData.getString("edu.vesit.ams.ViewPresetsActivity.startTimesXMLFileName"));
			selectedPresetsStartTimes = receivedData.getStringArrayList("edu.vesit.ams.ViewPresetsActivity.selectedPresetsStartTimes");
			Collections.sort(selectedPresetsStartTimes);
			originalSelectedPresetsStartTimesStringRepresentation = selectedPresetsStartTimes.toString();
		}else
		{
			endTimesXMLFileName = new File(receivedData.getString("edu.vesit.ams.ViewPresetsActivity.endTimesXMLFileName"));
			selectedPresetsEndTimes = receivedData.getStringArrayList("edu.vesit.ams.ViewPresetsActivity.selectedPresetsEndTimes");
			Collections.sort(selectedPresetsEndTimes);
			originalSelectedPresetsEndTimesStringRepresentation = selectedPresetsEndTimes.toString();
		}
		
		/*****************************************************************************************************************************************************/
		
		final ArrayAdapter<String> showSelectedPresetsLectureTimesAdapter = new ArrayAdapter<String>(DispLectureTimesActivity.this, android.R.layout.simple_list_item_1);
		for(String s:(isStartTime?selectedPresetsStartTimes:selectedPresetsEndTimes))
			showSelectedPresetsLectureTimesAdapter.add(s);
		listViewShowPresetsLectureTime.setAdapter(showSelectedPresetsLectureTimesAdapter);
		listViewShowPresetsLectureTime.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int which, long id)
			{
				final String selectedTime = parent.getItemAtPosition(which).toString();
				if(selectedTime.equals("Add another"))
					showAndSetAptTimePickerDialog(selectedTime, false);
				else
				{
					AlertDialog.Builder builder = new AlertDialog.Builder(DispLectureTimesActivity.this);
					builder.setTitle("Modify Lecture Time ..");
					builder.setMessage("Lecture "+(isStartTime?"Start":"End")+" Time: "+parent.getItemAtPosition(which).toString());
					builder.setPositiveButton("Change", new DialogInterface.OnClickListener()
					{	
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							dialog.dismiss();
							showAndSetAptTimePickerDialog(selectedTime, true);
						}
					});
					builder.setNegativeButton("Delete", new DialogInterface.OnClickListener()
					{					
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							if(isStartTime)
								selectedPresetsStartTimes.remove(selectedTime);
							else
								selectedPresetsEndTimes.remove(selectedTime);
							showSelectedPresetsLectureTimesAdapter.clear();
							for(String s:(isStartTime?selectedPresetsStartTimes:selectedPresetsEndTimes))
								showSelectedPresetsLectureTimesAdapter.add(s);
							showSelectedPresetsLectureTimesAdapter.notifyDataSetChanged();
						}
					});
					builder.create().show();
				}
			}
			private void showAndSetAptTimePickerDialog(final String selectedTime, final boolean changeNeeded)
			{
				final Calendar cal = Calendar.getInstance();
				TimePickerDialog  timePickerDialog = new TimePickerDialog(DispLectureTimesActivity.this, new TimePickerDialog.OnTimeSetListener()
				{
					@Override
					public void onTimeSet(TimePicker view, int hourOfDay, int minute)
					{
						cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
						cal.set(Calendar.MINUTE, minute);
						if(isStartTime)
						{
							if(selectedPresetsStartTimes.contains(Preset.timeUserDisplayFormat.format(cal.getTime())))
								Toast.makeText(getBaseContext(), "Start Time already presemt", Toast.LENGTH_SHORT).show();
							else
							{
								if(changeNeeded)
									selectedPresetsStartTimes.set(selectedPresetsStartTimes.indexOf(selectedTime), Preset.timeUserDisplayFormat.format(cal.getTime()));
								else
									selectedPresetsStartTimes.add(Preset.timeUserDisplayFormat.format(cal.getTime()));
								
								Collections.sort(selectedPresetsStartTimes);
								showSelectedPresetsLectureTimesAdapter.clear();
								for(String s:selectedPresetsStartTimes)
									showSelectedPresetsLectureTimesAdapter.add(s);
								showSelectedPresetsLectureTimesAdapter.notifyDataSetChanged();
							}
						}else
						{
							if(selectedPresetsEndTimes.contains(Preset.timeUserDisplayFormat.format(cal.getTime())))
								Toast.makeText(getBaseContext(), "End Time already presemt", Toast.LENGTH_SHORT).show();
							else
							{
								if(changeNeeded)
									selectedPresetsEndTimes.set(selectedPresetsEndTimes.indexOf(selectedTime), Preset.timeUserDisplayFormat.format(cal.getTime()));
								else
									selectedPresetsEndTimes.add(Preset.timeUserDisplayFormat.format(cal.getTime()));
								
								Collections.sort(selectedPresetsEndTimes);
								showSelectedPresetsLectureTimesAdapter.clear();
								for(String s:selectedPresetsEndTimes)
									showSelectedPresetsLectureTimesAdapter.add(s);
								showSelectedPresetsLectureTimesAdapter.notifyDataSetChanged();
							}
						}
					}
				}, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false);
				timePickerDialog.show();
			}
		});
		/*****************************************************************************************************************************************************/	
	}
	
	public void onBackPressed()
	{
		try
		{
			if(isStartTime)
			{
				//removing off duplicates
				selectedPresetsStartTimes = new ArrayList<String>(new HashSet<String>(selectedPresetsStartTimes));
				Collections.sort(selectedPresetsStartTimes);
				if(selectedPresetsStartTimes.toString().equals(originalSelectedPresetsStartTimesStringRepresentation))
					Toast.makeText(getBaseContext(), "No need to save as no modifications were made.", Toast.LENGTH_SHORT).show();
				else
				{
					FileOutputStream fos = new FileOutputStream(startTimesXMLFileName);
					FileChannel fileChannel = fos.getChannel();
					fileChannel.truncate(0);
					fileChannel.close();
					fos.close();
					
					PrintWriter pw = new PrintWriter(new FileWriter(startTimesXMLFileName));
	    			pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
	    			pw.println("<StartTimes>");
	    			for(String s:selectedPresetsStartTimes)
	    				if(!(s.equals("Add another")))
	    					pw.println("<StartTime>"+s+"</StartTime>");
	    			pw.println("</StartTimes>");
	    			pw.close();
	    			Toast.makeText(getBaseContext(), "Successfully saved your modifications", Toast.LENGTH_SHORT).show();
				}
			}else
			{
				//removing off duplicates
				selectedPresetsEndTimes = new ArrayList<String>(new HashSet<String>(selectedPresetsEndTimes));
				Collections.sort(selectedPresetsEndTimes);
				if(selectedPresetsEndTimes.toString().equals(originalSelectedPresetsEndTimesStringRepresentation))
					Toast.makeText(getBaseContext(), "No need to save as no modifications were made.", Toast.LENGTH_SHORT).show();
				else
				{
					FileOutputStream fos = new FileOutputStream(endTimesXMLFileName);
					FileChannel fileChannel = fos.getChannel();
					fileChannel.truncate(0);
					fileChannel.close();
					fos.close();
					
					PrintWriter pw = new PrintWriter(new FileWriter(endTimesXMLFileName));
	    			pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
	    			pw.println("<EndTimes>");
	    			for(String s:selectedPresetsEndTimes)
	    				if(!(s.equals("Add another")))
	    					pw.println("<EndTime>"+s+"</EndTime>");
	    			pw.println("</EndTimes>");
	    			pw.close();
	    			Toast.makeText(getBaseContext(), "Successfully saved your modifications", Toast.LENGTH_SHORT).show();
				}
			}
			super.onBackPressed();
			finish();
		}catch(Exception e)
		{
			e.printStackTrace();
			Toast.makeText(getBaseContext(), "An error occured while saving your modifications .. please retry", Toast.LENGTH_SHORT).show();
		}
	}

	private void initViews()
	{
		txtviewShowChosenPreset = (TextView)findViewById(R.id.txtViewShowChosenPresetTableName);
		listViewShowPresetsLectureTime = (ListView)findViewById(R.id.listViewShowPresetsLectureTimes);
	}
}
