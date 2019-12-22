package edu.vesit.ams;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Xml;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ViewPresetsActivity extends Activity
{
	ArrayList<Preset> allPresets, teacherAllotedPresets, teacherNonAllotedPresets;
	boolean showOnlyTeachersAllotedPresets = true;
	String teacherUsername, teacherSelectedDBName, teacherPlainPW;
	
	Button btnReloadActivityWithDifferentMode;
	ListView listViewShowPresetsWRTMode;
	
	File startTimesXMLFileName, endTimesXMLFileName;
	ArrayList<String> selectedPresetsStartTimes = new ArrayList<String>(), selectedPresetsEndTimes = new ArrayList<String>();
	ArrayAdapter<String> showWRTModePresetsAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		boolean customTitleSupported = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_view_presets);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		if(customTitleSupported)
		{
			getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);
			TextView myTitleText = (TextView)findViewById(R.id.myTitle);
			myTitleText.setText("View Presets");
			//myTitleText.setTextColor(Color.rgb(0,0,139));
		}
		
		initViews();
		
		Bundle receivedData = getIntent().getExtras();
		teacherUsername = receivedData.getString("edu.vesit.ams.MainActivity.teacherUsername");
		teacherSelectedDBName = receivedData.getString("edu.vesit.ams.MainActivity.teacherSelectedDBName");
		teacherPlainPW = receivedData.getString("edu.vesit.ams.MainActivity.teacherPlainPW");
		allPresets = receivedData.getParcelableArrayList("edu.vesit.ams.MainActivity.allPresets");
		teacherAllotedPresets = receivedData.getParcelableArrayList("edu.vesit.ams.MainActivity.teacherAllotedPresets");
		teacherNonAllotedPresets = receivedData.getParcelableArrayList("edu.vesit.ams.MainActivity.teacherNonAllotedPresets");
		
		showWRTModePresetsAdapter = new ArrayAdapter<String>(ViewPresetsActivity.this, android.R.layout.simple_list_item_1);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(ViewPresetsActivity.this);
		builder.setTitle("Choose Viewing Presets Mode ..");
		builder.setMessage("Alloted Presets = Shows only those presets which are alloted to you i.e those which are shown once you login"+System.getProperty("line.separator")+System.getProperty("line.separator")+"Non-Alloted Presets = Shows those presets which could be accessed on behalf of another teacher(Eg: Engagement Lecture)");
		builder.setPositiveButton("Alloted Presets", new DialogInterface.OnClickListener()
		{	
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				showOnlyTeachersAllotedPresets = true;
				showWRTModePresetsAdapter.clear();
				for(Preset p:teacherAllotedPresets)
					showWRTModePresetsAdapter.add(p.getPresetTableName());
				showWRTModePresetsAdapter.notifyDataSetChanged();
			}
		});
		builder.setNegativeButton("Non-Alloted Presets", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				showOnlyTeachersAllotedPresets = false;
				showWRTModePresetsAdapter.clear();
				for(Preset p:teacherNonAllotedPresets)
					showWRTModePresetsAdapter.add(p.getPresetTableName());
				showWRTModePresetsAdapter.notifyDataSetChanged();
			}
		});
		builder.setCancelable(false);
		builder.create().show();
		
		/*********************************************************************************************************************************/
		
													/*			ADAPT LISTVIEW'S ADAPTER WRT MODE			*/
		listViewShowPresetsWRTMode.setAdapter(showWRTModePresetsAdapter);
		listViewShowPresetsWRTMode.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int which, long id)
			{
				final String selectedPresetTableName = parent.getItemAtPosition(which).toString();
				startTimesXMLFileName = new File(MainActivity.appDataPresetsStartTimesAppDir, teacherSelectedDBName+"_"+selectedPresetTableName+".xml");
				endTimesXMLFileName = new File(MainActivity.appDataPresetsEndTimesAppDir, teacherSelectedDBName+"_"+selectedPresetTableName+".xml");
				
				AlertDialog.Builder builder = new AlertDialog.Builder(ViewPresetsActivity.this);
				builder.setTitle("Preset Details ..");
				for(Preset p:(showOnlyTeachersAllotedPresets?teacherAllotedPresets:teacherNonAllotedPresets))
					if(p.getPresetTableName().equals(parent.getItemAtPosition(which).toString()))
					{
						builder.setMessage(p.toString());
						break;
					}
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
				{	
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				});
				builder.setNegativeButton("Mod End Times", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						if(endTimesXMLFileName.exists())
						{
							try
							{
								selectedPresetsEndTimes.clear();
								
								XmlPullParser xpp = Xml.newPullParser();
								xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
							    xpp.setInput(new FileReader(endTimesXMLFileName));
							
							    int eventType = xpp.getEventType();
							    while(eventType != XmlPullParser.END_DOCUMENT)
							    {
							        if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("EndTime"))
							        	selectedPresetsEndTimes.add(xpp.nextText());
							        eventType = xpp.next(); //move to next element
							    }
							    
							    selectedPresetsEndTimes.add("Add another");
							    
							}catch(Exception e)
							{
								e.printStackTrace();
							}
							Intent i = new Intent();
							i.setAction("edu.vesit.ams.DispLectureTimesActivity");
							Bundle extras = new Bundle();
							extras.putString("edu.vesit.ams.ViewPresetsActivity.selectedPresetTableName", selectedPresetTableName);
							extras.putBoolean("edu.vesit.ams.ViewPresetsActivity.isStartTime", false);
							extras.putString("edu.vesit.ams.ViewPresetsActivity.endTimesXMLFileName", endTimesXMLFileName.getAbsolutePath());
							extras.putStringArrayList("edu.vesit.ams.ViewPresetsActivity.selectedPresetsEndTimes", selectedPresetsEndTimes);
							i.putExtras(extras);
							startActivity(i);
						}else
							Toast.makeText(ViewPresetsActivity.this.getBaseContext(), "Error, No end Times data found!", Toast.LENGTH_SHORT).show();
					}
				});
				builder.setNeutralButton("Mod Start Times", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						if(startTimesXMLFileName.exists())
						{
							try
							{
								selectedPresetsStartTimes.clear();
								
								XmlPullParser xpp = Xml.newPullParser();
								xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
							    xpp.setInput(new FileReader(startTimesXMLFileName));
							
							    int eventType = xpp.getEventType();
							    while(eventType != XmlPullParser.END_DOCUMENT)
							    {
							        if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("StartTime"))
							        	selectedPresetsStartTimes.add(xpp.nextText());
							        eventType = xpp.next(); //move to next element
							    }
							    
							    selectedPresetsStartTimes.add("Add another");
							}catch(Exception e)
							{
								e.printStackTrace();
							}
							Intent i = new Intent();
							i.setAction("edu.vesit.ams.DispLectureTimesActivity");
							Bundle extras = new Bundle();
							extras.putString("edu.vesit.ams.ViewPresetsActivity.selectedPresetTableName", selectedPresetTableName);
							extras.putBoolean("edu.vesit.ams.ViewPresetsActivity.isStartTime", true);
							extras.putString("edu.vesit.ams.ViewPresetsActivity.startTimesXMLFileName", startTimesXMLFileName.getAbsolutePath());
							extras.putStringArrayList("edu.vesit.ams.ViewPresetsActivity.selectedPresetsStartTimes", selectedPresetsStartTimes);
							i.putExtras(extras);
							startActivity(i);
						}else
							Toast.makeText(ViewPresetsActivity.this.getBaseContext(), "Error, No start Times data found!", Toast.LENGTH_SHORT).show();
					}
				});
				builder.create().show();
			}
		});
		
		/*********************************************************************************************************************************/
		
		btnReloadActivityWithDifferentMode.setOnClickListener(new View.OnClickListener()
		{	
			@Override
			public void onClick(View v)
			{
				Intent i = getIntent();
				finish();
				startActivity(i);
			}
		});
	}
	private void initViews()
	{
		btnReloadActivityWithDifferentMode = (Button)findViewById(R.id.btnReloadActivityWithDifferentMode);
		listViewShowPresetsWRTMode = (ListView)findViewById(R.id.listViewShowPresetsWRTMode);
	}
}
