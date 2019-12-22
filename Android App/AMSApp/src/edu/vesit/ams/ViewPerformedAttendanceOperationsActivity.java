package edu.vesit.ams;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ViewPerformedAttendanceOperationsActivity extends Activity
{
	Button btnViewShowPerformedAttendanceOperationsMode;
	
	ListView listViewShowPerformedAttendanceOperationsWRTMode;
	ArrayAdapter<String> showWRTModePerformedAttendanceOperationsAdapter;
	
	boolean isViewModeForAllotedPresetsOnly = true;
	
	ArrayList<String> teacherAllotedPresetsPerformedAttendanceStrings = new ArrayList<String>(), teacherNonAllotedPresetsPerformedAttendanceStrings = new ArrayList<String>();
	ArrayList<String> timeStamps = new ArrayList<String>(), attendanceOperationStrings = new ArrayList<String>();
	ArrayList<Boolean> operationSuccessIndicators = new ArrayList<Boolean>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		boolean customTitleSupported = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_view_performed_attendance_operations);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		if(customTitleSupported)
		{
			getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);
			TextView myTitleText = (TextView)findViewById(R.id.myTitle);
			myTitleText.setText("Performed Attendance Operations");
			//myTitleText.setTextColor(Color.rgb(0,0,139));
		}
		
		/********************************************************************************************************************************/
		
		initViews();
		
		/********************************************************************************************************************************/
		
		populatePerformedAttendanceStrings(MainActivity.appDataTeachersAllotedPresetHistoryAttendanceStringsAppFile);
		populatePerformedAttendanceStrings(MainActivity.appDataTeachersNonAllotedPresetHistoryAttendanceStringsAppFile);
		
		/********************************************************************************************************************************/
		
		AlertDialog.Builder builder = new AlertDialog.Builder(ViewPerformedAttendanceOperationsActivity.this);
		builder.setTitle("Choose View Mode..");
		builder.setMessage("Alloted Presets = Shows only those presets which are alloted to you i.e those which are shown once you login"+System.getProperty("line.separator")+System.getProperty("line.separator")+"Non-Alloted Presets = Shows those presets which could be accessed on behalf of another teacher(Eg: Engagement Lecture)");
		builder.setPositiveButton("Alloted Presets", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				isViewModeForAllotedPresetsOnly = true;
				btnViewShowPerformedAttendanceOperationsMode.setText("Chosen Mode: Alloted Presets, Click to change");
				updateListViewWRTMode();
			}
		});
		builder.setNegativeButton("Non-Alloted Presets", new DialogInterface.OnClickListener()
		{	
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				isViewModeForAllotedPresetsOnly = false;
				btnViewShowPerformedAttendanceOperationsMode.setText("Chosen Mode: Non-Alloted Presets, Click to change");
				updateListViewWRTMode();
			}
		});
		builder.setCancelable(false);
		builder.create().show();
		
		/********************************************************************************************************************************/
		
		btnViewShowPerformedAttendanceOperationsMode.setOnClickListener(new View.OnClickListener()
		{	
			@Override
			public void onClick(View v)
			{
				Intent i = getIntent();
				finish();
				startActivity(i);
			}
		});
		
		/********************************************************************************************************************************/
		
		showWRTModePerformedAttendanceOperationsAdapter = new ArrayAdapter<String>(ViewPerformedAttendanceOperationsActivity.this, android.R.layout.simple_list_item_1);
		listViewShowPerformedAttendanceOperationsWRTMode.setAdapter(showWRTModePerformedAttendanceOperationsAdapter);
		
		listViewShowPerformedAttendanceOperationsWRTMode.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int which, long id)
			{
				if(!parent.getItemAtPosition(which).toString().equals("None"))
				{
					AlertDialog.Builder builder = new AlertDialog.Builder(ViewPerformedAttendanceOperationsActivity.this);
					builder.setTitle("Attendance Operation Details");
					
					StringBuffer attendanceOperationDetailsStr = new StringBuffer();
					attendanceOperationDetailsStr.append(">TimeStamp: "+timeStamps.get(which)).append(System.getProperty("line.separator"));
					attendanceOperationDetailsStr.append(">Was successful: "+(operationSuccessIndicators.get(which)?"Yes":"No")).append(System.getProperty("line.separator"));
					
					String attendanceStringComponents[] = attendanceOperationStrings.get(which).split("[~]");
					attendanceOperationDetailsStr.append(">Type: "+attendanceStringComponents[0]).append(System.getProperty("line.separator"));
					attendanceOperationDetailsStr.append(">Mode: "+attendanceStringComponents[1]).append(System.getProperty("line.separator"));
					attendanceOperationDetailsStr.append(">DB Name: "+attendanceStringComponents[2]).append(System.getProperty("line.separator"));
					attendanceOperationDetailsStr.append(">Preset Name: "+attendanceStringComponents[3]).append(System.getProperty("line.separator"));
					attendanceOperationDetailsStr.append(">Num of Students: "+attendanceStringComponents[4]).append(System.getProperty("line.separator"));
					String chosenMode = attendanceStringComponents[1];
					if(chosenMode.equals("SINGLE Delete") || chosenMode.equals("SINLGE View"))
					{
						attendanceOperationDetailsStr.append(">Attendance Date: "+attendanceStringComponents[5]).append(System.getProperty("line.separator"));
						attendanceOperationDetailsStr.append(">Start Time: "+attendanceStringComponents[6]).append(System.getProperty("line.separator"));
						attendanceOperationDetailsStr.append(">End Time: "+attendanceStringComponents[7]).append(System.getProperty("line.separator"));
					}else if(chosenMode.equals("BATCH Delete") || chosenMode.equals("BATCH View"))
					{
						attendanceOperationDetailsStr.append(">Attendance Start Date: "+attendanceStringComponents[5]).append(System.getProperty("line.separator"));
						attendanceOperationDetailsStr.append(">Attendance End Date: "+attendanceStringComponents[6]).append(System.getProperty("line.separator"));
						attendanceOperationDetailsStr.append(">Start Time: "+attendanceStringComponents[7]).append(System.getProperty("line.separator"));
						attendanceOperationDetailsStr.append(">End Time: "+attendanceStringComponents[8]).append(System.getProperty("line.separator"));
					}else if(chosenMode.equals("SINGLE Insert/Update"))
					{
						attendanceOperationDetailsStr.append(">Attendance Date: "+attendanceStringComponents[5]).append(System.getProperty("line.separator"));
						attendanceOperationDetailsStr.append(">Start Time: "+attendanceStringComponents[6]).append(System.getProperty("line.separator"));
						attendanceOperationDetailsStr.append(">End Time: "+attendanceStringComponents[7]).append(System.getProperty("line.separator"));
						attendanceOperationDetailsStr.append(">"+attendanceStringComponents[8]+": "+attendanceStringComponents[9]).append(System.getProperty("line.separator"));
						attendanceOperationDetailsStr.append(">Medical Leaves: "+attendanceStringComponents[10].trim().replace('`', ',')).append(System.getProperty("line.separator"));
						attendanceOperationDetailsStr.append(">Other Leaves: "+attendanceStringComponents[11].trim().replace('`', ',')).append(System.getProperty("line.separator"));
					}else if(chosenMode.equals("BATCH Insert/Update"))
					{
						attendanceOperationDetailsStr.append(">Attendance Start Date: "+attendanceStringComponents[5]).append(System.getProperty("line.separator"));
						attendanceOperationDetailsStr.append(">Attendance End Date: "+attendanceStringComponents[6]).append(System.getProperty("line.separator"));
						attendanceOperationDetailsStr.append(">Start Time: "+attendanceStringComponents[7]).append(System.getProperty("line.separator"));
						attendanceOperationDetailsStr.append(">End Time: "+attendanceStringComponents[8]).append(System.getProperty("line.separator"));
						attendanceOperationDetailsStr.append(">"+attendanceStringComponents[9]+": "+attendanceStringComponents[10]).append(System.getProperty("line.separator"));
						attendanceOperationDetailsStr.append(">Medical Leaves: "+attendanceStringComponents[11].trim().replace('`', ',')).append(System.getProperty("line.separator"));
						attendanceOperationDetailsStr.append(">Other Leaves: "+attendanceStringComponents[12].trim().replace('`', ',')).append(System.getProperty("line.separator"));
					}
					
					builder.setMessage(attendanceOperationDetailsStr.toString().trim());
					builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
					{	
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							dialog.dismiss();
						}
					});
					builder.create().show();
				}
			}
		});
	}

	private void initViews()
	{
		btnViewShowPerformedAttendanceOperationsMode = (Button)findViewById(R.id.btnViewShowPerformedAttendanceOperationsMode);
		listViewShowPerformedAttendanceOperationsWRTMode = (ListView)findViewById(R.id.listViewShowPerformedAttendanceOperationsWRTMode);
	}

	private void populatePerformedAttendanceStrings(File f)
	{
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(f));
			String temp = new String();
			while((temp = br.readLine())!=null)
					if(f.equals(MainActivity.appDataTeachersAllotedPresetHistoryAttendanceStringsAppFile))
						teacherAllotedPresetsPerformedAttendanceStrings.add(temp);
					else if(f.equals(MainActivity.appDataTeachersNonAllotedPresetHistoryAttendanceStringsAppFile))
						teacherNonAllotedPresetsPerformedAttendanceStrings.add(temp);
			br.close();
		}catch(Exception e)
		{
			e.printStackTrace();
			Toast.makeText(getBaseContext(), "An exception occured while reading historical data .. please retry!"+System.getProperty("line.separator")+(e.getMessage()==null?"No error description found":e.getMessage()), Toast.LENGTH_LONG).show();
		}
	}
	private void updateListViewWRTMode()
	{
		if((isViewModeForAllotedPresetsOnly?teacherAllotedPresetsPerformedAttendanceStrings:teacherNonAllotedPresetsPerformedAttendanceStrings).size()==0)
			showWRTModePerformedAttendanceOperationsAdapter.add("None");
		else
		{
			for(String s:(isViewModeForAllotedPresetsOnly?teacherAllotedPresetsPerformedAttendanceStrings:teacherNonAllotedPresetsPerformedAttendanceStrings))
			{
				String temp = s.substring(s.indexOf('[')+1);				//removing off 1st sqaure bracket
				int indexOfSecondSquareBracketOfTimeStamp = temp.indexOf(']');
				String timeStamp = s.substring(0, indexOfSecondSquareBracketOfTimeStamp+2).trim();
				
				String tempArr[] = s.substring(indexOfSecondSquareBracketOfTimeStamp+1).trim().split(" .. ");
				boolean attendanceOperationSuccessIndicator = Boolean.parseBoolean(tempArr[1].trim());
				String attendanceStringComponents[] = tempArr[0].trim().split("~");
				
				timeStamps.add(timeStamp);
				operationSuccessIndicators.add(attendanceOperationSuccessIndicator);
				attendanceOperationStrings.add(tempArr[0].trim());
				showWRTModePerformedAttendanceOperationsAdapter.add(timeStamp+" "+attendanceStringComponents[1]);			//shows timestamp and mode for each performed attendance operation 
			}
		}
		showWRTModePerformedAttendanceOperationsAdapter.notifyDataSetChanged();
	}
}
