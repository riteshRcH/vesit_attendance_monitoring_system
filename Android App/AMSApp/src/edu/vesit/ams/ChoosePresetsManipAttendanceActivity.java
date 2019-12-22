package edu.vesit.ams;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class ChoosePresetsManipAttendanceActivity extends Activity
{
	String teacherSelectedDBName;
	ArrayList<Preset> allPresets = new ArrayList<Preset>();
	File zippedServerDataFile;
	
	String earlierPresetsAllotmentXMLMD5 = "noMD5";					//to check if xml file changed between choosing presets => to avoid again and again DOM parsing of XML
	String earlierStartTimesXMLFileNameMD5 = "noMD5";
	String earlierEndTimesXMLFileNameMD5 = "noMD5";
	File xmlFilePresetsAllotmentForSelectedDB, startTimesXMLFileName, endTimesXMLFileName;
	
	ArrayAdapter<String> presetNameDataAdapter, selectedPresetStartTimesAdapter, selectedPresetEndTimesAdapter;
	
	String selectedPresetTableName;
	double retrievedDurationHours = 1.0;
	int retrievedNumStudents;
	Date selectedLectureStartTime, selectedLectureEndTime;
	
	String teacherUsername, teacherPlainPW;
	
	TextView txtviewShowChosenPreset, myTitleText, txtviewLabelAttendanceDate, txtviewLabelEndAttendanceDate;
	Spinner spinnerChoosePreset, spinnerChooseStartTime, spinnerChooseEndTime;
	Button btnReloadActivity, btnUpdateFetchPresets, btnChoosePreset, btnChooseLectureStartTime, btnChooseLectureEndTime, btnToGetMedicalLeaveReasons, btnToGetOtherReasons, btnReviewAndSubmitOperationString;
	EditText editTextRollNums, editTextRollNumsMedicalLeave, editTextRollNumsOtherReasons;
	DatePicker attendanceDatePicker, attendanceEndDatePicker;
	View separator4, separator5;
	
	CheckBox chkBoxIsPresentees, chkBoxAllRollNums, chkBoxOperatingOnBehalf, chkBoxDontConsiderLectureTimes;
	TimePicker lectureStartTimePicker = null;
	TimePicker lectureEndTimePicker = null;
	
	LinkedHashMap<Integer, String> medicalReasons = new LinkedHashMap<Integer, String>(), otherReasons = new LinkedHashMap<Integer, String>();
	
	String modesOfOperation[] = { "SINGLE View", "BATCH View", "SINGLE Delete", "BATCH Delete", "SINGLE Insert/Update", "BATCH Insert/Update", "TRIGGER SINGLE View", "TRIGGER BATCH View", "TRIGGER SINGLE Delete", "TRIGGER BATCH Delete", "TRIGGER SINGLE Insert/Update", "TRIGGER BATCH Insert/Update" };
	String chosenMode;
	
	boolean isChosenStartTimeCustom = false, isChosenEndTimeCustom = false, needToParsePresetsXMLAgainDueToOnBehalfChange = false;
	
	char attendanceStringANDTriggerOperationStringEntitiesSeparationChar = '~', medicalOtherReasonsSeparationChar = '`';
	
	//boolean canFinishActivity = false;			//used in onBackPresse(), need this as android OS is executing code out of asynctask(i.e dialog.dismiss() and finishing activity) before dismissing progressDialog = race condition
	
	String enteredRollNums[], enteredMedicalLeaveRollNums[], enteredOtherReasonsRollNums[];
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		boolean customTitleSupported = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);;
		setContentView(R.layout.activity_choose_presets_manip_attendance);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		if(customTitleSupported)
		{
			getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);
			myTitleText = (TextView) findViewById(R.id.myTitle);
			myTitleText.setText("Feed new Absentees");
			//myTitleText.setTextColor(Color.rgb(0,0,139));
		}
		
		getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.bg));
		
		presetNameDataAdapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_item);
		presetNameDataAdapter.setDropDownViewResource(R.layout.multiline_spinner_dropdown_item);
		presetNameDataAdapter.setNotifyOnChange(false);
		
		Bundle receivedData = getIntent().getExtras();
		teacherSelectedDBName = receivedData.getString("DBName");
		teacherUsername = receivedData.getString("teacherUsername");
		teacherPlainPW = receivedData.getString("teacherPlainPW");
		
		xmlFilePresetsAllotmentForSelectedDB = new File(MainActivity.serverDataPresetsAppDir, teacherSelectedDBName+"_presets_allotment.xml");
		
		/*******************************************************************************************************************************************************/
		
		initViews();
		
		/*******************************************************************************************************************************************************/
		
		btnReloadActivity.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent i = getIntent();
				finish();
				startActivity(i);
			}
		});
		
		/*******************************************************************************************************************************************************/
		
		btnUpdateFetchPresets.setOnClickListener(new View.OnClickListener() 
		{
			@Override
			public void onClick(View v)
			{
		    	AsyncTask<Void, String, Exception> bgTaskFetchUpdatePresets = new AsyncTask<Void, String, Exception>()
		    	{
		    		ProgressDialog progressDialog = null;
		    		boolean receivedServerDataZipFileSuccess = false, needUpdation = true;
		    	    	       				
		    	    @Override
		    	    protected void onPreExecute()
		    	    {
		    	    	progressDialog = new ProgressDialog(ChoosePresetsManipAttendanceActivity.this);
		    	    	progressDialog.setTitle("Processing ..");
		    	    	progressDialog.setMessage("Please wait.");
		    	    	progressDialog.setCancelable(false);
		    	    	progressDialog.setIndeterminate(true);
		    	    	progressDialog.show();
		    	    }
		    	    	    	    							
		    	    @Override
		    	    protected Exception doInBackground(Void ...params)
		    	    {
		    	    	try
		    	    	{
		    	    		/*LoginActivity.toServerOOStrm.writeObject("PUT_ABSENTEES "+txtviewGetAbsenteeString.getText().toString());
		    	    		LoginActivity.toServerOOStrm.flush();
		    	    		Object serverReply = (String)LoginActivity.fromServerOIS.readObject();
		    	    		if(serverReply!=null)
		    	    			absenteesReceivedByServerSuccess = serverReply.equals("Absentee Updation Success");*/
		    	    		LoginActivity.toServerOOStrm.writeObject("GET_SERVER_DATA_ZIP");
		    	    		LoginActivity.toServerOOStrm.flush();
		    	    		
		    	    		zippedServerDataFile = new File(MainActivity.AMSAppDir, "serverData.zip");
		    	    		needUpdation = true;
		    	    		
		    	    		Object serverReply = LoginActivity.fromServerOIS.readObject();
		    	    		Log.d("received md5", (String)serverReply);
		    	    		if(serverReply instanceof String)
		    	    		{
		    	    			if(!zippedServerDataFile.exists())
		    	    			{
		    	    				Log.d("zip doesnt exist, and hence need updation:", Boolean.valueOf(needUpdation).toString());
		    	    				needUpdation = true;
		    	    			}else
		    	    			{
		    	    				needUpdation = !(((String)serverReply).split("[:]")[1]).toString().equals(getMD5OfParam(zippedServerDataFile));
		    	    				Log.d("checking", ((String)serverReply).split("[:]")[1]+" and "+getMD5OfParam(zippedServerDataFile));
		    	    				Log.d("zip exists, and need updation: ", Boolean.valueOf(needUpdation).toString());
		    	    			}
		    	    		}
		    	    		if(needUpdation)
		    	    		{
		    	    			LoginActivity.toServerOOStrm.writeObject(Boolean.valueOf(needUpdation));		//inform server whether client needs updation of server data or not
		    	    			
		    	    			deleteFolderContents(MainActivity.DATAAppDir);
		    	    			
			    	    		FileOutputStream fos = new FileOutputStream(zippedServerDataFile);
			    	    		while(true)
			    	    		{
			    	    			serverReply = LoginActivity.fromServerOIS.readObject();
			    	    			if(serverReply!=null)
			    	    				if(serverReply instanceof byte[])
			    	    					fos.write((byte[])serverReply);
			    	    				else if(serverReply instanceof String && ((String)serverReply).startsWith("FINISHED_SENDING_ZIP"))
			    	    				{
			    	    					fos.flush();
			    	    					fos.close();
			    	    					publishProgress("Verifying with md5..");
			    	    					String receivedServerReply = (String)serverReply; 
			    	    					String calculatedMD5 = getMD5OfParam(zippedServerDataFile);
			    	    					String receivedMD5 = receivedServerReply.split("[:]")[1];
			    	    					//Integrity verified and unzipped successfully
			    	    					ZipUnzipDir obj = new ZipUnzipDir();
			    	    					MainActivity.serverDataAppDir.mkdirs();
			    	    					receivedServerDataZipFileSuccess = calculatedMD5.equals(receivedMD5) && obj.unzipToDir(zippedServerDataFile, MainActivity.serverDataAppDir);
			    	    					obj = null;
			    	    					break;
			    	    				}
			    	    		}
		    	    		}else
		    	    		{
		    	    			receivedServerDataZipFileSuccess = true;
		    	    			LoginActivity.toServerOOStrm.writeObject(Boolean.valueOf(needUpdation));		//inform server whether client needs updation of server data or not
		    	    			Log.d("send to server updation needed", Boolean.valueOf(needUpdation).toString());
		    	    			publishProgress("Display Toast .. already up to date server Data");
		    	    		}
		    	    	}catch (Exception e) 
		    	    	{
		    	    		Log.e("Exception", e.getMessage()==null?"null":e.getMessage());
		    	    		return e;
		    	    	}
						return null;
		    	    }
		    	    
		    	    @Override
		    	    protected void onProgressUpdate(String ...progressParams)
		    	    {
		    	    	if(progressParams[0].equals("Display Toast .. already up to date server Data"))
		    	    		Toast.makeText(getBaseContext(), "Already UP TO DATE DATA!", Toast.LENGTH_LONG).show();
		    	    	else
		    	    		progressDialog.setMessage(progressParams[0]);
		    	    }

					@Override
		    	    protected void onPostExecute(Exception exceptionOccured)
					{
						if(progressDialog!=null)
							progressDialog.dismiss();
						
						if(exceptionOccured==null)
						{
							if(needUpdation)
								if(receivedServerDataZipFileSuccess)
									Toast.makeText(getBaseContext(), "Successfully received server Data.", Toast.LENGTH_SHORT).show();
								else
									Toast.makeText(getBaseContext(), "There was an error in receiving/interpreting Presets sent by server .. please retry", Toast.LENGTH_SHORT).show();
						}else
							Toast.makeText(getBaseContext(), "Exception occured while fetching/updating presets .. please retry.\nException Details: "+exceptionOccured.getMessage(), Toast.LENGTH_LONG).show();
					}
		    	};
		    	bgTaskFetchUpdatePresets.execute();
			}
		});
	/*******************************************************************************************************************************************************/
		
		showAndGetModeOfOperationAlertDialog();
		
	/*******************************************************************************************************************************************************/
		
		chkBoxOperatingOnBehalf.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{	
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				needToParsePresetsXMLAgainDueToOnBehalfChange = true;						//on Behalf changed so need to parse again the presets xml file
			}
		});
		
		chkBoxDontConsiderLectureTimes.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{	
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				btnChooseLectureStartTime.setEnabled(!isChecked);
				btnChooseLectureEndTime.setEnabled(!isChecked);
				spinnerChooseStartTime.setEnabled(!isChecked);
				spinnerChooseEndTime.setEnabled(!isChecked);
				lectureStartTimePicker.setEnabled(!isChecked);
				lectureEndTimePicker.setEnabled(!isChecked);
			}
		});
		
	/*******************************************************************************************************************************************************/
		
		spinnerChoosePreset.setAdapter(presetNameDataAdapter);
		
		btnChoosePreset.setOnClickListener(new View.OnClickListener()
		{	
			@Override
			public void onClick(View v)
			{
				if(xmlFilePresetsAllotmentForSelectedDB.exists())
				{
					if(!earlierPresetsAllotmentXMLMD5.equals(getMD5OfParam(xmlFilePresetsAllotmentForSelectedDB)) || needToParsePresetsXMLAgainDueToOnBehalfChange)
					{
						try
						{
							presetNameDataAdapter.clear();
							allPresets.clear();
							
							//Parse XML	using XMLPUllParser(Android provided is more efficient as after few insertions of start times DOM parsing would be mem inefficient) as XML File has been changed since last parsing
							XmlPullParser xpp = Xml.newPullParser();
							xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
						    xpp.setInput(new FileReader(xmlFilePresetsAllotmentForSelectedDB));
						
						    int eventType = xpp.getEventType();
						    Preset preset = null;
						    while(eventType != XmlPullParser.END_DOCUMENT)
						    {
						        if (eventType == XmlPullParser.START_TAG)
						        {
						            if (xpp.getName().equals("Preset"))
						            	preset = new Preset();
						            else if(xpp.getName().equals("presetTableName"))
						            	preset.setPresetTableName(xpp.nextText());
						            else if(xpp.getName().equals("DurationHours"))
						            	preset.setDurationHours(Integer.parseInt(xpp.nextText()));
						            else if(xpp.getName().equals("TeacherUsername"))
						            	preset.setTeacherUsername(xpp.nextText());
						            else if(xpp.getName().equals("TeacherHashedPW"))
						            	preset.setTeacherHashedPW(xpp.nextText());
						            else if(xpp.getName().equals("TeacherSalt"))
						            	preset.setTeacherSalt(xpp.nextText());
						            else if(xpp.getName().equals("numStudents"))
						            	preset.setNumStudents(Integer.parseInt(xpp.nextText()));
						            
						        }else if(eventType==XmlPullParser.END_TAG && xpp.getName().equals("Preset"))
						        {
						        	allPresets.add(preset);
						        	preset = null;
						        }
						        eventType = xpp.next(); //move to next element
						    }
						    
				        	for(Preset p:allPresets)
				        	{
				        		//show only those presets for matching teacher's username and password only if engagement lecture is not selected
				        		if(chkBoxOperatingOnBehalf.isChecked())
				        		{
				        			if(!(p.getTeacherUsername().equals(teacherUsername) && p.getTeacherHashedPW().equals(getSHA1OfParam(teacherPlainPW + getMD5OfParam(p.getTeacherSalt() + teacherPlainPW) + p.getTeacherSalt()))))
				        				presetNameDataAdapter.add(p.toString());
				        		}else
				        		{
				        			if(p.getTeacherUsername().equals(teacherUsername) && p.getTeacherHashedPW().equals(getSHA1OfParam(teacherPlainPW + getMD5OfParam(p.getTeacherSalt() + teacherPlainPW) + p.getTeacherSalt())))
				        				presetNameDataAdapter.add(p.toString());
				        		}
				        		
				        		//check if start times file is present for that preset
				        		if(!MainActivity.appDataPresetsStartTimesAppDir.exists())
				        			MainActivity.appDataPresetsStartTimesAppDir.mkdirs();
				        		File f1 = new File(MainActivity.appDataPresetsStartTimesAppDir, teacherSelectedDBName+"_"+p.getPresetTableName()+".xml");
				        		if(!f1.exists())
				        		{
				        			f1.createNewFile();
				        			PrintWriter pw = new PrintWriter(new FileWriter(f1));
				        			pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
				        			pw.println("<StartTimes>");
				        			pw.println("</StartTimes>");
				        			pw.close();
				        		}
				        		//check if end times file is present for that preset
				        		if(!MainActivity.appDataPresetsEndTimesAppDir.exists())
				        			MainActivity.appDataPresetsEndTimesAppDir.mkdirs();
				        		File f2 = new File(MainActivity.appDataPresetsEndTimesAppDir, teacherSelectedDBName+"_"+p.getPresetTableName()+".xml");
				        		if(!f2.exists())
				        		{
				        			f2.createNewFile();
				        			PrintWriter pw = new PrintWriter(new FileWriter(f2));
				        			pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
				        			pw.println("<EndTimes>");
				        			pw.println("</EndTimes>");
				        			pw.close();
				        		}
				        	}
				        	presetNameDataAdapter.notifyDataSetChanged();
				        	
				        	earlierPresetsAllotmentXMLMD5 = getMD5OfParam(xmlFilePresetsAllotmentForSelectedDB);
				        	needToParsePresetsXMLAgainDueToOnBehalfChange = false;									//changed to false as its latest parsing done here just now, it would be true again if user changed onBehalf checkbox state(see chkbox's onCheckedChangeListener)
						}catch(Exception e)
						{
							Toast.makeText(getBaseContext(), "Exception occured: "+e.getMessage(), Toast.LENGTH_SHORT).show();
							e.printStackTrace();
						}
					}
					
					spinnerChoosePreset.performClick();
					
				}else
					Toast.makeText(getBaseContext(), "Presets Data Doesnt exists,  please fetch/update server data", Toast.LENGTH_LONG).show();
			}
		});
		
		//If spinner needed not to be visible => Keep width and Height of spinner to 0 dip and keep visibility as invisible not as gone => only then this listener works
		spinnerChoosePreset.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View v, int pos, long id)
			{
				String selectedSpinnerItem = parent.getItemAtPosition(pos).toString();
				/*BufferedReader br = new BufferedReader(new StringReader(selectedSpinnerItem));
				try
				{
					txtviewShowChosenPreset.setText("Chosen Preset: "+(selectedPresetTableName = br.readLine()));
					br.close();
				}catch (IOException e)
				{
					e.printStackTrace();
				}*/
				for(Preset p:allPresets)
					if(p.toString().equals(selectedSpinnerItem))
					{
						txtviewShowChosenPreset.setText("Chosen Preset: "+(selectedPresetTableName = p.getPresetTableName()));
						retrievedDurationHours = p.getDurationHours();
						retrievedNumStudents = p.getNumStudents();
						startTimesXMLFileName = new File(MainActivity.appDataPresetsStartTimesAppDir, teacherSelectedDBName+"_"+selectedPresetTableName+".xml");
						endTimesXMLFileName = new File(MainActivity.appDataPresetsEndTimesAppDir, teacherSelectedDBName+"_"+selectedPresetTableName+".xml");
					}
				btnChooseLectureStartTime.setEnabled(true);
				btnChooseLectureEndTime.setEnabled(true);
				chkBoxDontConsiderLectureTimes.setEnabled(true);
				
				//for that condition when a preset is chosen and they are already enabled but preset is again chosen so again they get enabled so disbling if chkBoxDontConsiderLectureTimes is checked
				btnChooseLectureStartTime.setEnabled(!chkBoxDontConsiderLectureTimes.isChecked());
				btnChooseLectureEndTime.setEnabled(!chkBoxDontConsiderLectureTimes.isChecked());
				spinnerChooseEndTime.setEnabled(!chkBoxDontConsiderLectureTimes.isChecked());
				spinnerChooseStartTime.setEnabled(!chkBoxDontConsiderLectureTimes.isChecked());
				lectureEndTimePicker.setEnabled(!chkBoxDontConsiderLectureTimes.isChecked());
				lectureStartTimePicker.setEnabled(!chkBoxDontConsiderLectureTimes.isChecked());
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
				if(txtviewShowChosenPreset.getText().equals("Chosen Preset: None"))
				{
					Toast.makeText(ChoosePresetsManipAttendanceActivity.this.getBaseContext(), "A preset must be compulsorily chosen!", Toast.LENGTH_SHORT).show();
					spinnerChoosePreset.performClick();
				}
			}
		});
	/*******************************************************************************************************************************************************/
		selectedPresetStartTimesAdapter = new ArrayAdapter<String>(ChoosePresetsManipAttendanceActivity.this.getBaseContext(), android.R.layout.simple_spinner_item);
		selectedPresetStartTimesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		selectedPresetStartTimesAdapter.setNotifyOnChange(false);
		
		spinnerChooseStartTime.setAdapter(selectedPresetStartTimesAdapter);
		
		btnChooseLectureStartTime.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if(startTimesXMLFileName.exists())
				{
					if(!earlierStartTimesXMLFileNameMD5.equals(getMD5OfParam(startTimesXMLFileName)) || needToParsePresetsXMLAgainDueToOnBehalfChange)		//parse again only if MD5 changed
					{
						try
						{
							selectedPresetStartTimesAdapter.clear();
							ArrayList<String> temp = new ArrayList<String>();
							
							XmlPullParser xpp = Xml.newPullParser();
							xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
						    xpp.setInput(new FileReader(startTimesXMLFileName));
						
						    int eventType = xpp.getEventType();
						    while(eventType != XmlPullParser.END_DOCUMENT)
						    {
						        if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("StartTime"))
						        	temp.add(xpp.nextText());
						        eventType = xpp.next(); //move to next element
						    }
						    
						    temp.add("Other(Auto-save enabled)");
						    //Removing off duplicates
						    temp = new ArrayList<String>(new HashSet<String>(temp));
						    Collections.sort(temp);
						    
						    for(String s:temp)
						    	selectedPresetStartTimesAdapter.add(s);
						    selectedPresetStartTimesAdapter.notifyDataSetChanged();
						    
						    earlierStartTimesXMLFileNameMD5 = getMD5OfParam(startTimesXMLFileName);
						    needToParsePresetsXMLAgainDueToOnBehalfChange = false;
						}catch(Exception e)
						{
							e.printStackTrace();
						}
					}
					spinnerChooseStartTime.performClick();
				}else
					Toast.makeText(ChoosePresetsManipAttendanceActivity.this.getBaseContext(), "Error, No start Times data found!", Toast.LENGTH_SHORT).show();
			}
		});
		
		spinnerChooseStartTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View v, int pos, long id)
			{
				String selectedSpinnerItem = parent.getItemAtPosition(pos).toString();
				if(selectedSpinnerItem.equals("Other(Auto-save enabled)"))
				{
					lectureStartTimePicker.setVisibility(View.VISIBLE);
					btnChooseLectureStartTime.setText("Custom/New Start Time");
					isChosenStartTimeCustom = true;
				}else
				{
					isChosenStartTimeCustom = false;
					lectureStartTimePicker.setVisibility(View.GONE);
					try
					{
						selectedLectureStartTime = Preset.timeUserDisplayFormat.parse(selectedSpinnerItem);
						btnChooseLectureStartTime.setText("Chosen Start Time: "+selectedSpinnerItem);
						
						Calendar cal = Calendar.getInstance();
						cal.setTime(selectedLectureStartTime);
						cal.add(Calendar.HOUR_OF_DAY, (int)retrievedDurationHours);
						cal.add(Calendar.MINUTE, (int)((retrievedDurationHours%1)*60));				//retrievedDurationHours%1 gives fractional part * 60 => minutes to add
						
						lectureEndTimePicker.setCurrentHour(cal.get(Calendar.HOUR_OF_DAY));
						lectureEndTimePicker.setCurrentMinute(cal.get(Calendar.MINUTE));
						btnChooseLectureEndTime.setText("Chosen End Time: "+Preset.timeUserDisplayFormat.format((selectedLectureEndTime = cal.getTime())));
					}catch (ParseException e)
					{
						e.printStackTrace();
					}
				}
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
				
			}
		});
		
		lectureStartTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener()
		{
			@Override
			public void onTimeChanged(TimePicker view, int hourOfDay, int minute)
			{
				Calendar cal = Calendar.getInstance();
				cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
				cal.set(Calendar.MINUTE, minute);
				try
				{
					selectedLectureStartTime = Preset.timeUserDisplayFormat.parse(Integer.toString(hourOfDay)+":"+Integer.toString(minute)+" "+(cal.get(Calendar.AM_PM) == Calendar.AM?"AM":"PM"));
				}catch (ParseException e)
				{
					e.printStackTrace();
				}
				//btnChooseLectureStartTime.setText("Chosen Start Time: "+Preset.timeUserDisplayFormat.format(selectedLectureStartTime));
				//btnChooseLectureStartTime.setText("Custom/New Start Time");
				
				cal.setTime(selectedLectureStartTime);
				cal.add(Calendar.HOUR_OF_DAY, (int)retrievedDurationHours);
				cal.add(Calendar.MINUTE, (int)((retrievedDurationHours%1)*60));				//retrievedDurationHours%1 gives fractional part * 60 => minutes to add
				
				lectureEndTimePicker.setCurrentHour(cal.get(Calendar.HOUR_OF_DAY));
				lectureEndTimePicker.setCurrentMinute(cal.get(Calendar.MINUTE));
				btnChooseLectureEndTime.setText("Chosen End Time: "+Preset.timeUserDisplayFormat.format((selectedLectureEndTime = cal.getTime())));
			}
		});
	/*******************************************************************************************************************************************************/
		selectedPresetEndTimesAdapter = new ArrayAdapter<String>(ChoosePresetsManipAttendanceActivity.this.getBaseContext(), android.R.layout.simple_spinner_item);
		selectedPresetEndTimesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		selectedPresetEndTimesAdapter.setNotifyOnChange(false);
		
		spinnerChooseEndTime.setAdapter(selectedPresetEndTimesAdapter);
		
		btnChooseLectureEndTime.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if(endTimesXMLFileName.exists())
				{
					if(!earlierEndTimesXMLFileNameMD5.equals(getMD5OfParam(endTimesXMLFileName)) || needToParsePresetsXMLAgainDueToOnBehalfChange)		//parse again only if MD5 changed and if on behalf checkbox state has changed 
					{
						try
						{
							selectedPresetEndTimesAdapter.clear();
							ArrayList<String> temp = new ArrayList<String>();
							
							XmlPullParser xpp = Xml.newPullParser();
							xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
						    xpp.setInput(new FileReader(endTimesXMLFileName));
						
						    int eventType = xpp.getEventType();
						    while(eventType != XmlPullParser.END_DOCUMENT)
						    {
						        if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("EndTime"))
						        	temp.add(xpp.nextText());
						        eventType = xpp.next(); //move to next element
						    }
						    
						    temp.add("Other(Auto-save enabled)");
						    //removing off duplicates just to ensure
						    temp = new ArrayList<String>(new HashSet<String>(temp));
						    
						    for(String s:temp)
						    	selectedPresetEndTimesAdapter.add(s);
						    selectedPresetEndTimesAdapter.notifyDataSetChanged();
						    
						    earlierEndTimesXMLFileNameMD5 = getMD5OfParam(endTimesXMLFileName);
						    needToParsePresetsXMLAgainDueToOnBehalfChange = false;
						}catch(Exception e)
						{
							e.printStackTrace();
						}
					}
					spinnerChooseEndTime.performClick();
				}else
					Toast.makeText(ChoosePresetsManipAttendanceActivity.this.getBaseContext(), "Error, No end Times data found!", Toast.LENGTH_SHORT).show();
			}
		});
		
		spinnerChooseEndTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View v, int pos, long id)
			{
				String selectedSpinnerItem = parent.getItemAtPosition(pos).toString();
				if(selectedSpinnerItem.equals("Other(Auto-save enabled)"))
				{
					lectureEndTimePicker.setVisibility(View.VISIBLE);
					btnChooseLectureEndTime.setText("Custom/New End Time");
					isChosenEndTimeCustom = true;
				}else
				{
					isChosenEndTimeCustom = false;
					lectureEndTimePicker.setVisibility(View.GONE);
					try
					{
						selectedLectureEndTime = Preset.timeUserDisplayFormat.parse(selectedSpinnerItem);
						btnChooseLectureEndTime.setText("Chosen End Time: "+selectedSpinnerItem);
					}catch (ParseException e)
					{
						e.printStackTrace();
					}
				}
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
				
			}
		});
		
		lectureEndTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener()
		{	
			@Override
			public void onTimeChanged(TimePicker view, int hourOfDay, int minute)
			{
				btnChooseLectureEndTime.setText("Custom/New End Time");					//when both are other and user changes start time by timePicker and button text changes! .. and if again custom end time needed then doing btnChooseLectureEndTime.setText("Custom/New End Time");
				Calendar cal = Calendar.getInstance();
				cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
				cal.set(Calendar.MINUTE, minute);
				try
				{
					selectedLectureEndTime = Preset.timeUserDisplayFormat.parse(Integer.toString(hourOfDay)+":"+Integer.toString(minute)+" "+(cal.get(Calendar.AM_PM) == Calendar.AM?"AM":"PM"));
				}catch (ParseException e)
				{
					e.printStackTrace();
				}
				
				//btnChooseLectureEndTime.setText("Chosen End Time: "+Preset.timeUserDisplayFormat.format(selectedLectureEndTime));
				//btnChooseLectureStartTime.setText("Custom/New End Time");
			}
		});
		
	/*******************************************************************************************************************************************************/
		chkBoxAllRollNums.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				editTextRollNums.setEnabled(!isChecked);
				editTextRollNumsMedicalLeave.setEnabled(!isChecked);
				editTextRollNumsOtherReasons.setEnabled(!isChecked);
				btnToGetMedicalLeaveReasons.setEnabled(!isChecked);
				btnToGetOtherReasons.setEnabled(!isChecked);
			}
		});
		editTextRollNums.setOnFocusChangeListener(new View.OnFocusChangeListener()
		{
			@Override
			public void onFocusChange(View v, boolean hasFocus)
			{
				if(!hasFocus && !editTextRollNums.getText().toString().trim().equals("") && !editTextRollNums.getText().toString().trim().equals(null))
					editTextRollNums.setText(removeAllSpacesOfParamString(joinArrayElementsByComma(getDistinctValuesOfStringArray(removeAllSpacesOfParamString(editTextRollNums.getText().toString().trim()).replaceAll(",+", ",").split("[,]"), true))));
			}
		});
		editTextRollNumsMedicalLeave.setOnFocusChangeListener(new View.OnFocusChangeListener()
		{
			@Override
			public void onFocusChange(View v, boolean hasFocus)
			{
				if(!hasFocus && !editTextRollNumsMedicalLeave.getText().toString().trim().equals("") && !editTextRollNumsMedicalLeave.getText().toString().trim().equals(null))
					editTextRollNumsMedicalLeave.setText(removeAllSpacesOfParamString(joinArrayElementsByComma(getDistinctValuesOfStringArray(removeAllSpacesOfParamString(editTextRollNumsMedicalLeave.getText().toString().trim()).replaceAll(",+", ",").split("[,]"), true))));
			}
		});
		editTextRollNumsOtherReasons.setOnFocusChangeListener(new View.OnFocusChangeListener()
		{
			@Override
			public void onFocusChange(View v, boolean hasFocus)
			{
				if(!hasFocus && !editTextRollNumsOtherReasons.getText().toString().trim().equals("") && !editTextRollNumsOtherReasons.getText().toString().trim().equals(null))
					editTextRollNumsOtherReasons.setText(removeAllSpacesOfParamString(joinArrayElementsByComma(getDistinctValuesOfStringArray(removeAllSpacesOfParamString(editTextRollNumsOtherReasons.getText().toString().trim()).replaceAll(",+", ",").split("[,]"), true))));		//remove off 1st and last square bracket
			}
		});
	/*******************************************************************************************************************************************************/
		btnToGetMedicalLeaveReasons.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if(editTextRollNumsMedicalLeave.getText().toString().trim().equals("") || editTextRollNumsMedicalLeave.getText().toString().trim().equals(null))
					Toast.makeText(ChoosePresetsManipAttendanceActivity.this, "Enter atleast 1 roll number 1st in medical reasons field", Toast.LENGTH_SHORT).show();
				else
				{
					if(medicalReasons.size()==0)
						for(String rn:editTextRollNumsMedicalLeave.getText().toString().trim().split("[,]"))
							medicalReasons.put(Integer.parseInt(rn), "None specified");
					else
					{
						LinkedHashMap<Integer, String> temp = new LinkedHashMap<Integer, String>();
						for(Map.Entry<Integer, String> entry:medicalReasons.entrySet())
							temp.put(entry.getKey(), entry.getValue());
						medicalReasons.clear();
						for(String rn:editTextRollNumsMedicalLeave.getText().toString().trim().split("[,]"))
						{
							int rollNumKey = Integer.parseInt(rn);
							if(temp.containsKey(rollNumKey))
								medicalReasons.put(rollNumKey, temp.get(rollNumKey));
							else
								medicalReasons.put(rollNumKey, "None specified");
						}
					}
					final TextView txtViewsShowRollNums[] = new TextView[medicalReasons.size()];
					final EditText editTextsGetReasons[] = new EditText[medicalReasons.size()];
					
					//Input Dialog to get medical reasons per entered roll Number that has taken medical leave
					AlertDialog.Builder builder = new AlertDialog.Builder(ChoosePresetsManipAttendanceActivity.this);
					builder.setTitle("Enter Medical Leave Reasons");
					builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							dialog.dismiss();
						}
					});
					builder.setPositiveButton("Save", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							for(int i=0;i<txtViewsShowRollNums.length;i++)
							{
								String medicalReason = editTextsGetReasons[i].getText().toString().trim();
								medicalReasons.put(Integer.parseInt(txtViewsShowRollNums[i].getText().toString()), medicalReason.equals("")?"None Specified":medicalReason);
							}
						}
					});
					
					LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					ScrollView scrollView = (ScrollView)inflater.inflate(R.layout.medical_other_reasons__other_input_dialogs_scrollview, null);
					
					LinearLayout wrappingLinearLayout = new LinearLayout(ChoosePresetsManipAttendanceActivity.this);
					wrappingLinearLayout.setOrientation(LinearLayout.VERTICAL);
					
					int i = 0;
					for(Map.Entry<Integer, String> entry:medicalReasons.entrySet())
					{
						LinearLayout layout = new LinearLayout(ChoosePresetsManipAttendanceActivity.this);
						layout.setOrientation(LinearLayout.HORIZONTAL);
						layout.setWeightSum(1.0F);
						
						LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.2F);
						txtViewsShowRollNums[i] = new TextView(ChoosePresetsManipAttendanceActivity.this);
						txtViewsShowRollNums[i].setText(entry.getKey().toString());
						layout.addView(txtViewsShowRollNums[i], layoutParams1);
						
						LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8F);
						editTextsGetReasons[i] = new EditText(ChoosePresetsManipAttendanceActivity.this);
						if(medicalReasons.get(entry.getKey()).equals("None specified"))
							editTextsGetReasons[i].setHint(medicalReasons.get(entry.getKey()));
						else
							editTextsGetReasons[i].setText(medicalReasons.get(entry.getKey()));
						layout.addView(editTextsGetReasons[i], layoutParams2);
						
						wrappingLinearLayout.addView(layout);
						
						i++;
					}
					
					//alertDialogGetMedicalReasons.setContentView(R.layout.medical_other_reasons_input_dialog);
					scrollView.addView(wrappingLinearLayout);
					builder.setView(scrollView);
					AlertDialog alertDialogGetMedicalReasons = builder.create();
					alertDialogGetMedicalReasons.show();
				}
			}
		});
	/*******************************************************************************************************************************************************/
		btnToGetOtherReasons.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if(editTextRollNumsOtherReasons.getText().toString().trim().equals("") || editTextRollNumsOtherReasons.getText().toString().trim().equals(null))
					Toast.makeText(ChoosePresetsManipAttendanceActivity.this, "Enter atleast 1 roll number 1st in other reasons field", Toast.LENGTH_SHORT).show();
				else
				{
					if(otherReasons.size()==0)
						for(String rn:editTextRollNumsOtherReasons.getText().toString().trim().split("[,]"))
							otherReasons.put(Integer.parseInt(rn), "None specified");
					else
					{
						LinkedHashMap<Integer, String> temp = new LinkedHashMap<Integer, String>();
						for(Map.Entry<Integer, String> entry:otherReasons.entrySet())
							temp.put(entry.getKey(), entry.getValue());
						otherReasons.clear();
						for(String rn:editTextRollNumsOtherReasons.getText().toString().trim().split("[,]"))
						{
							int rollNumKey = Integer.parseInt(rn);
							if(temp.containsKey(rollNumKey))
								otherReasons.put(rollNumKey, temp.get(rollNumKey));
							else
								otherReasons.put(rollNumKey, "None specified");
						}
					}
					final TextView txtViewsShowRollNums[] = new TextView[otherReasons.size()];
					final EditText editTextsGetReasons[] = new EditText[otherReasons.size()];
					
					//Input Dialog to get other reasons per entered roll Number that has taken other reasons leave
					AlertDialog.Builder builder = new AlertDialog.Builder(ChoosePresetsManipAttendanceActivity.this);
					builder.setTitle("Enter Other Reasons");
					builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							dialog.dismiss();
						}
					});
					builder.setPositiveButton("Save", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							for(int i=0;i<txtViewsShowRollNums.length;i++)
							{
								String otherReason = editTextsGetReasons[i].getText().toString().trim();
								otherReasons.put(Integer.parseInt(txtViewsShowRollNums[i].getText().toString()), otherReason.equals("")?"None Specified":otherReason);
							}
						}
					});
					
					LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					ScrollView scrollView = (ScrollView)inflater.inflate(R.layout.medical_other_reasons__other_input_dialogs_scrollview, null);
					
					LinearLayout wrappingLinearLayout = new LinearLayout(ChoosePresetsManipAttendanceActivity.this);
					wrappingLinearLayout.setOrientation(LinearLayout.VERTICAL);
					
					int i = 0;
					for(Map.Entry<Integer, String> entry:otherReasons.entrySet())
					{
						LinearLayout layout = new LinearLayout(ChoosePresetsManipAttendanceActivity.this);
						layout.setOrientation(LinearLayout.HORIZONTAL);
						layout.setWeightSum(1.0F);
						
						LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.2F);
						txtViewsShowRollNums[i] = new TextView(ChoosePresetsManipAttendanceActivity.this);
						txtViewsShowRollNums[i].setText(entry.getKey().toString());
						layout.addView(txtViewsShowRollNums[i], layoutParams1);
						
						LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8F);
						editTextsGetReasons[i] = new EditText(ChoosePresetsManipAttendanceActivity.this);
						if(entry.getValue().equals("None specified"))
							editTextsGetReasons[i].setHint(entry.getValue());
						else
							editTextsGetReasons[i].setText(entry.getValue());
						layout.addView(editTextsGetReasons[i], layoutParams2);
						
						wrappingLinearLayout.addView(layout);
						
						i++;
					}
					
					scrollView.addView(wrappingLinearLayout);
					builder.setView(scrollView);
					AlertDialog alertDialogGetMedicalReasons = builder.create();
					alertDialogGetMedicalReasons.show();
				}
			}
		});
	/*******************************************************************************************************************************************************/	
		btnReviewAndSubmitOperationString.setOnClickListener(new View.OnClickListener()
		{	
			@Override
			public void onClick(View v)
			{
				if(txtviewShowChosenPreset.getText().equals("Choose Preset"))
					Toast.makeText(ChoosePresetsManipAttendanceActivity.this.getBaseContext(), "A preset must be chosen 1st", Toast.LENGTH_SHORT).show();
				else
				{
					if(!chkBoxDontConsiderLectureTimes.isChecked() && btnChooseLectureStartTime.getText().equals("Choose Lecture Start Time"))
						Toast.makeText(ChoosePresetsManipAttendanceActivity.this.getBaseContext(), "Start Time must be chosen", Toast.LENGTH_SHORT).show();
					else
					{
						if(!chkBoxDontConsiderLectureTimes.isChecked() && btnChooseLectureEndTime.getText().equals("Choose Lecture End Time"))
							Toast.makeText(ChoosePresetsManipAttendanceActivity.this.getBaseContext(), "End Time must be chosen", Toast.LENGTH_SHORT).show();
						else
						{
							try
							{
								if(chosenMode.equals("SINGLE Insert/Update") || chosenMode.equals("BATCH Insert/Update") || chosenMode.equals("TRIGGER SINGLE Insert/Update") || chosenMode.equals("TRIGGER BATCH Insert/Update"))
								{
									if(chkBoxAllRollNums.isChecked())
									{
										enteredRollNums = new String[retrievedNumStudents];
										for(int i=1;i<=retrievedNumStudents;i++)
											enteredRollNums[i-1] = Integer.toString(i);
										createReviewAlertDialog(enteredRollNums, null, null);
									}else
									{
										if(!chkBoxAllRollNums.isChecked() && editTextRollNums.getText().toString().length()==0 && editTextRollNumsMedicalLeave.getText().toString().length()==0 && editTextRollNumsOtherReasons.getText().toString().length()==0)
											throw new Exception("Enter atleast 1 absentee(s)/presentee(s)/medical leaves/other leaves roll number OR choose all roll numbers");
										else
										{
											enteredRollNums = editTextRollNums.getText().toString().trim().split("[,]");
											enteredMedicalLeaveRollNums = editTextRollNumsMedicalLeave.getText().toString().trim().split("[,]");
											enteredOtherReasonsRollNums = editTextRollNumsOtherReasons.getText().toString().trim().split("[,]");
											ArrayList<String> arrList = new ArrayList<String>();
											if(editTextRollNums.getText().toString().trim().length()>0 && editTextRollNumsMedicalLeave.getText().toString().length()>0 && (arrList = getIntersectionOfParams(enteredRollNums, enteredMedicalLeaveRollNums)).size()>0)
												throw new Exception("Cant submit to server due to duplicate entries in medical leaves and absentees: "+arrList.toString());
											else if(editTextRollNums.getText().toString().trim().length()>0 && editTextRollNumsOtherReasons.getText().toString().length()>0 && (arrList = getIntersectionOfParams(enteredRollNums, enteredOtherReasonsRollNums)).size()>0)
												throw new Exception("Cant submit to server due to duplicate entries in medical leaves and other reasons: "+arrList.toString());
											else if(editTextRollNumsMedicalLeave.getText().toString().length()>0 && editTextRollNumsOtherReasons.getText().toString().length()>0 && (arrList = getIntersectionOfParams(enteredMedicalLeaveRollNums, enteredOtherReasonsRollNums)).size()>0)
												throw new Exception("Cant submit to server due to duplicate entries in medical leaves and other reasons: "+arrList.toString());
											else
											{
												if(editTextRollNums.getText().toString().length()>0)
													for(String rollNum:enteredRollNums)
													{
														int rn = Integer.parseInt(rollNum);
														if(rn<1 || rn>retrievedNumStudents)
															throw new Exception("Cant submit to server due to out of range/invalid roll number("+rn+"), enter valid roll numbers in range: 1 to "+retrievedNumStudents+" in Absentees");
													}
												if(editTextRollNumsMedicalLeave.getText().toString().length()>0)
													for(String rollNum:enteredMedicalLeaveRollNums)
													{
														int rn = Integer.parseInt(rollNum);
														if(rn<1 || rn>retrievedNumStudents)
															throw new Exception("Cant submit to server due to out of range/invalid roll number("+rn+"), enter valid roll numbers in range: 1 to "+retrievedNumStudents+" in Medical Leave Absentees");
													}
												if(editTextRollNumsOtherReasons.getText().toString().length()>0)
													for(String rollNum:enteredOtherReasonsRollNums)
													{
														int rn = Integer.parseInt(rollNum);
														if(rn<1 || rn>retrievedNumStudents)
															throw new Exception("Cant submit to server due to out of range/invalid roll number("+rn+"), enter valid roll numbers in range: 1 to "+retrievedNumStudents+" in Other Reasons Absentees");
													}
												if(chosenMode.equals("BATCH Insert/Update") || chosenMode.equals("BATCH View") || chosenMode.equals("BATCH Delete"))
												{
													Calendar cal1 = Calendar.getInstance(), cal2 = Calendar.getInstance();
													cal1.set(attendanceDatePicker.getYear(), attendanceDatePicker.getMonth(), attendanceDatePicker.getDayOfMonth());
													cal2.set(attendanceEndDatePicker.getYear(), attendanceEndDatePicker.getMonth(), attendanceEndDatePicker.getDayOfMonth());
													if(cal1.getTime().after(cal2.getTime()))
														throw new Exception("Start date must be a date before End date");
												}
												createReviewAlertDialog(enteredRollNums, enteredMedicalLeaveRollNums, enteredOtherReasonsRollNums);
											}
										}
									}
								}else if(chosenMode.equals("SINGLE Delete") || chosenMode.equals("BATCH Delete") || chosenMode.equals("SINGLE View") || chosenMode.equals("BATCH View") || chosenMode.equals("TRIGGER SINGLE Delete") || chosenMode.equals("TRIGGER BATCH Delete") || chosenMode.equals("TRIGGER SINGLE View") || chosenMode.equals("TRIGGER BATCH View"))
									createReviewAlertDialog(null, null, null);
							}catch(Exception e)
							{
								e.printStackTrace();
								Toast.makeText(ChoosePresetsManipAttendanceActivity.this.getBaseContext(), e.getMessage()==null?"No error Description found":e.getMessage(), Toast.LENGTH_LONG).show();
							}
						}
					}
				}
			}
		});
	/*******************************************************************************************************************************************************/
	}
	protected String[] getDistinctValuesOfStringArray(String[] input, boolean inputAreNumbersAsString)
	{
		HashSet<String> tempHashSet = new HashSet<String>(Arrays.asList(input));
		String[] distinctValuesInput = tempHashSet.toArray(new String[tempHashSet.size()]);		//duplicate roll numbers excluded
		if(inputAreNumbersAsString)
		{
			int[] nums = new int[distinctValuesInput.length];
			for(int i=0;i<nums.length;i++)
				nums[i] = Integer.parseInt(distinctValuesInput[i]);
			Arrays.sort(nums);
			for(int i=0;i<nums.length;i++)
				distinctValuesInput[i] = Integer.toString(nums[i]);
		}else
			Arrays.sort(distinctValuesInput);			//lexogrphically sorting(dictionary)
		return distinctValuesInput;
	}
	String joinArrayElementsByComma(String[] input)
	{
		StringBuffer toReturnStr = new StringBuffer();
		for(int i=0;i<(input.length-1);i++)
			toReturnStr.append(input[i]).append(',');
		toReturnStr.append(input[input.length-1]);
		return toReturnStr.toString();
	}
	private String removeAllSpacesOfParamString(String string)
	{
		String toReturnStr = string.replaceAll("\\s+", "");
		return toReturnStr;
	}
	private ArrayList<String> getIntersectionOfParams(String arr1[], String arr2[])
	{
		ArrayList<String> intersectionResult = new ArrayList<String>();
		Arrays.sort(arr1);
		for(int i=0;i<arr1.length;i++)
			if(Arrays.binarySearch(arr2, arr1[i])>=0)			//found in arr2, searchKey = arr1[i]
				intersectionResult.add(arr1[i]);
		return intersectionResult;
	}
	private void initViews()
	{
		btnReloadActivity = (Button)findViewById(R.id.btnReloadActivity);
		
		chkBoxOperatingOnBehalf = (CheckBox)findViewById(R.id.chkBoxOperatingOnBehalf);
		
		txtviewShowChosenPreset = (TextView)findViewById(R.id.txtviewShowChosenPreset);
		
		spinnerChoosePreset = (Spinner)findViewById(R.id.spinnerChoosePreset);
		
		btnUpdateFetchPresets = (Button)findViewById(R.id.btnUpdateFetchPresets);
		
		btnChoosePreset = (Button)findViewById(R.id.btnChoosePreset);
		
		chkBoxDontConsiderLectureTimes = (CheckBox)findViewById(R.id.chkBoxDontConsiderLectureTimes);
		
		spinnerChooseStartTime = (Spinner)findViewById(R.id.spinnerChooseStartTime);
		
		btnChooseLectureEndTime = (Button)findViewById(R.id.btnChooseLectureEndTime);
		
		btnChooseLectureStartTime = (Button)findViewById(R.id.btnChooseLectureStartTime);
		
		lectureStartTimePicker = (TimePicker)findViewById(R.id.lectureStartTimePicker);
		lectureEndTimePicker = (TimePicker)findViewById(R.id.lectureEndTimePicker);
		
		spinnerChooseEndTime = (Spinner)findViewById(R.id.spinnerChooseEndTime);
		
		editTextRollNums = (EditText)findViewById(R.id.editTextRollNumsAbsentees);
		editTextRollNumsMedicalLeave = (EditText)findViewById(R.id.editTextRollNumsMedicalLeave);
		editTextRollNumsOtherReasons = (EditText)findViewById(R.id.editTextRollNumsOtherReasons);
		
		btnToGetMedicalLeaveReasons = (Button)findViewById(R.id.btnToGetMedicalLeaveReasons);
		btnToGetOtherReasons = (Button)findViewById(R.id.btnToGetOtherReasons);
		
		chkBoxAllRollNums = (CheckBox)findViewById(R.id.chkBoxAllRollNums);
		
		attendanceDatePicker = (DatePicker)findViewById(R.id.attendanceDatePicker);
		attendanceEndDatePicker = (DatePicker)findViewById(R.id.attendanceEndDatePicker);
		
		chkBoxIsPresentees = (CheckBox)findViewById(R.id.checkBoxIsPresentees);
		btnReviewAndSubmitOperationString = (Button)findViewById(R.id.btnReviewAndSubmitOperationString);
		
		separator4 = (View)findViewById(R.id.separator4);
		separator5 = (View)findViewById(R.id.separator5);
		
		txtviewLabelAttendanceDate = (TextView)findViewById(R.id.txtviewLabelAttendanceDate);
		txtviewLabelEndAttendanceDate = (TextView)findViewById(R.id.txtviewLabelEndAttendanceDate);
	}
	private void showAndGetModeOfOperationAlertDialog()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(ChoosePresetsManipAttendanceActivity.this);
		builder.setTitle("Choose Mode .."+System.getProperty("line.separator")+"(BATCH View=SEARCH Mode)");
		builder.setSingleChoiceItems(modesOfOperation, 4, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				chosenMode = modesOfOperation[which];
				myTitleText.setText("Mode: "+chosenMode);
				if(chosenMode.equals("SINGLE Delete") || chosenMode.equals("BATCH Delete") || chosenMode.equals("SINGLE View") || chosenMode.equals("BATCH View") || chosenMode.equals("TRIGGER SINGLE Delete") || chosenMode.equals("TRIGGER BATCH Delete") || chosenMode.equals("TRIGGER SINGLE View") || chosenMode.equals("TRIGGER BATCH View"))
				{
					chkBoxAllRollNums.setEnabled(false);								chkBoxAllRollNums.setVisibility(View.GONE);
					chkBoxIsPresentees.setEnabled(false);								chkBoxIsPresentees.setVisibility(View.GONE);
					editTextRollNums.setEnabled(false);									editTextRollNums.setVisibility(View.GONE);
					editTextRollNumsMedicalLeave.setEnabled(false);						editTextRollNumsMedicalLeave.setVisibility(View.GONE);
					editTextRollNumsOtherReasons.setEnabled(false);						editTextRollNumsOtherReasons.setVisibility(View.GONE);
					btnToGetMedicalLeaveReasons.setEnabled(false);						btnToGetMedicalLeaveReasons.setVisibility(View.GONE);
					btnToGetOtherReasons.setEnabled(false);								btnToGetOtherReasons.setVisibility(View.GONE);
					separator4.setVisibility(View.GONE);
					separator5.setVisibility(View.GONE);
					
					if(chosenMode.equals("SINGLE Delete") || chosenMode.equals("TRIGGER SINGLE Delete"))
					{
						txtviewLabelAttendanceDate.setText("Attendance Date: ");
						txtviewLabelEndAttendanceDate.setVisibility(View.GONE);
						attendanceEndDatePicker.setEnabled(false);
						attendanceEndDatePicker.setVisibility(View.GONE);
						chkBoxDontConsiderLectureTimes.setVisibility(View.GONE);
					}else if(chosenMode.equals("BATCH Delete") || chosenMode.equals("TRIGGER BATCH Delete"))
					{
						txtviewLabelAttendanceDate.setText("Attendance Start Date(Inclusive): ");
						txtviewLabelEndAttendanceDate.setVisibility(View.VISIBLE);
						attendanceEndDatePicker.setEnabled(true);
						attendanceEndDatePicker.setVisibility(View.VISIBLE);
						chkBoxDontConsiderLectureTimes.setVisibility(View.VISIBLE);
					}else if(chosenMode.equals("SINGLE View") || chosenMode.equals("TRIGGER SINGLE View"))
					{
						txtviewLabelAttendanceDate.setText("Attendance Date: ");
						txtviewLabelEndAttendanceDate.setVisibility(View.GONE);
						attendanceEndDatePicker.setEnabled(false);
						attendanceEndDatePicker.setVisibility(View.GONE);
						chkBoxDontConsiderLectureTimes.setVisibility(View.GONE);
					}else if(chosenMode.equals("BATCH View") || chosenMode.equals("TRIGGER BATCH View"))
					{
						txtviewLabelAttendanceDate.setText("Attendance Start Date(Inclusive): ");
						txtviewLabelEndAttendanceDate.setVisibility(View.VISIBLE);
						attendanceEndDatePicker.setEnabled(true);
						attendanceEndDatePicker.setVisibility(View.VISIBLE);
						chkBoxDontConsiderLectureTimes.setVisibility(View.VISIBLE);
					}
				}else if(chosenMode.equals("SINGLE Insert/Update") || chosenMode.equals("BATCH Insert/Update") || chosenMode.equals("TRIGGER SINGLE Insert/Update") || chosenMode.equals("TRIGGER BATCH Insert/Update"))
				{
					chkBoxAllRollNums.setEnabled(true);									chkBoxAllRollNums.setVisibility(View.VISIBLE);
					chkBoxIsPresentees.setEnabled(true);								chkBoxIsPresentees.setVisibility(View.VISIBLE);
					editTextRollNums.setEnabled(true);									editTextRollNums.setVisibility(View.VISIBLE);
					editTextRollNumsMedicalLeave.setEnabled(true);						editTextRollNumsMedicalLeave.setVisibility(View.VISIBLE);
					editTextRollNumsOtherReasons.setEnabled(true);						editTextRollNumsOtherReasons.setVisibility(View.VISIBLE);
					btnToGetMedicalLeaveReasons.setEnabled(true);						btnToGetMedicalLeaveReasons.setVisibility(View.VISIBLE);
					btnToGetOtherReasons.setEnabled(true);								btnToGetOtherReasons.setVisibility(View.VISIBLE);
					separator4.setVisibility(View.VISIBLE);
					separator5.setVisibility(View.VISIBLE);
					
					if(chosenMode.equals("SINGLE Insert/Update") || chosenMode.equals("TRIGGER SINGLE Insert/Update"))
					{
						txtviewLabelAttendanceDate.setText("Attendance Date: ");
						txtviewLabelEndAttendanceDate.setVisibility(View.GONE);
						attendanceEndDatePicker.setEnabled(false);
						attendanceEndDatePicker.setVisibility(View.GONE);
					}else if(chosenMode.equals("BATCH Insert/Update") || chosenMode.equals("TRIGGER BATCH Insert/Update"))
					{
						txtviewLabelAttendanceDate.setText("Attendance Start Date(Inclusive): ");
						txtviewLabelEndAttendanceDate.setVisibility(View.VISIBLE);
						attendanceEndDatePicker.setEnabled(true);
						attendanceEndDatePicker.setVisibility(View.VISIBLE);
					}
					chkBoxDontConsiderLectureTimes.setVisibility(View.GONE);
				}
				dialog.dismiss();
			}
		});
		builder.setCancelable(false);
		builder.show();
	}
	void createReviewAlertDialog(final String[] enteredAbsentRollNums, String[] enteredMedicalLeaveRollNums, String[] enteredOtherReasonsRollNums) throws Exception
	{	
		StringBuffer reviewStr = new StringBuffer("Mode: ");
		reviewStr.append(chosenMode).append(System.getProperty("line.separator"));
		
		reviewStr.append(System.getProperty("line.separator"));							//New Line Separtor
		
		reviewStr.append("Preset: ").append(selectedPresetTableName).append(System.getProperty("line.separator"));
		
		reviewStr.append(System.getProperty("line.separator"));							//New Line Separtor
		
		reviewStr.append("Operating on behalf: ").append(chkBoxOperatingOnBehalf.isChecked()?"Yes":"No").append(System.getProperty("line.separator"));
		
		reviewStr.append(System.getProperty("line.separator"));							//New Line Separtor
		
		int day = attendanceDatePicker.getDayOfMonth(), month = attendanceDatePicker.getMonth(), year = attendanceDatePicker.getYear();
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM(MMM)/yyyy", Locale.ENGLISH);
		Calendar cal = Calendar.getInstance();
		cal.set(year, month, day);
		
		if(chosenMode.equals("SINGLE View") || chosenMode.equals("SINGLE Delete") || chosenMode.equals("SINGLE Insert/Update") || chosenMode.equals("TRIGGER SINGLE View") || chosenMode.equals("TRIGGER SINGLE Delete") || chosenMode.equals("TRIGGER SINGLE Insert/Update"))
			reviewStr.append("Attendance Date: ").append(sdf.format(cal.getTime())).append(System.getProperty("line.separator"));
		else if(chosenMode.equals("BATCH View") || chosenMode.equals("BATCH Delete") || chosenMode.equals("BATCH Insert/Update") || chosenMode.equals("TRIGGER BATCH View") || chosenMode.equals("TRIGGER BATCH Delete") || chosenMode.equals("TRIGGER BATCH Insert/Update"))
		{
			reviewStr.append("Attendance Start Date: ").append(sdf.format(cal.getTime())).append(System.getProperty("line.separator"));
			
			cal.clear();
			day = attendanceEndDatePicker.getDayOfMonth();
			month = attendanceEndDatePicker.getMonth();
			year = attendanceEndDatePicker.getYear();
			cal.set(year, month, day);
			reviewStr.append("Attendance End Date: ").append(sdf.format(cal.getTime())).append(System.getProperty("line.separator"));
		}
		
		reviewStr.append(System.getProperty("line.separator"));							//New Line Separtor
		
		/*cal.clear();
		cal.set(Calendar.HOUR_OF_DAY, lectureStartTimePicker.getCurrentHour());
		cal.set(Calendar.MINUTE, lectureStartTimePicker.getCurrentMinute());
		
		reviewStr += "Start Time:" + Preset.startTimeUserDisplayFormat.format(cal.getTime()) + System.getProperty("line.separator");
		
		cal.clear();
		cal.set(Calendar.HOUR_OF_DAY, lectureEndTimePicker.getCurrentHour());
		cal.set(Calendar.MINUTE, lectureEndTimePicker.getCurrentMinute());
		
		reviewStr += "End Time:" + Preset.startTimeUserDisplayFormat.format(cal.getTime()) + System.getProperty("line.separator");*/
		reviewStr.append("Start Time: ");
		if(chkBoxDontConsiderLectureTimes.isChecked())
			reviewStr.append("None").append(System.getProperty("line.separator"));
		else
		{
			if(isChosenStartTimeCustom)
			{
				cal.clear();
				cal.set(Calendar.HOUR_OF_DAY, lectureStartTimePicker.getCurrentHour());
				cal.set(Calendar.MINUTE, lectureStartTimePicker.getCurrentMinute());
				reviewStr.append(Preset.timeUserDisplayFormat.format(selectedLectureStartTime = cal.getTime())).append(System.getProperty("line.separator"));
			}else
				reviewStr.append(Preset.timeUserDisplayFormat.format(selectedLectureStartTime)).append(System.getProperty("line.separator"));
		}
		reviewStr.append("End Time: ");
		if(chkBoxDontConsiderLectureTimes.isChecked())
			reviewStr.append("None");
		else
		{
			if(isChosenEndTimeCustom)
			{
				cal.clear();
				cal.set(Calendar.HOUR_OF_DAY, lectureEndTimePicker.getCurrentHour());
				cal.set(Calendar.MINUTE, lectureEndTimePicker.getCurrentMinute());
				reviewStr.append(Preset.timeUserDisplayFormat.format(selectedLectureEndTime = cal.getTime())).append(System.getProperty("line.separator"));
			}else
				reviewStr.append(Preset.timeUserDisplayFormat.format(selectedLectureEndTime)).append(System.getProperty("line.separator"));
		}
		
		reviewStr.append(System.getProperty("line.separator"));							//New Line Separtor
		
		if((chosenMode.equals("SINGLE Insert/Update") || chosenMode.equals("BATCH Insert/Update") || chosenMode.equals("TRIGGER SINGLE Insert/Update") || chosenMode.equals("TRIGGER BATCH Insert/Update")) && enteredAbsentRollNums!=null && enteredAbsentRollNums.length>0)
		{
			reviewStr.append(chkBoxIsPresentees.isChecked()?"Presentees: ":"Absentees: ").append(chkBoxAllRollNums.isChecked()?"ALL Roll Numbers(1 to "+retrievedNumStudents+")":(editTextRollNums.getText().toString().trim().length()==0?"None":Arrays.deepToString(enteredAbsentRollNums))).append(System.getProperty("line.separator"));
			
			//for that condition where in medical leave roll numbers are entered but for each of them no reason is specified so linkedhashmap doesnt get populated as its populated in reasons feeding listener, same for other reasons
			if(medicalReasons.size()==0 && editTextRollNumsMedicalLeave.getText().toString().length()>0)
				for(String rn:editTextRollNumsMedicalLeave.getText().toString().trim().split("[,]"))
					medicalReasons.put(Integer.parseInt(rn), "None specified");
			
			if(otherReasons.size()==0 && editTextRollNumsOtherReasons.getText().toString().length()>0)
				for(String rn:editTextRollNumsOtherReasons.getText().toString().trim().split("[,]"))
					otherReasons.put(Integer.parseInt(rn), "None specified");
			
			//for that condition where in medical leave roll numbers are entered and populated into medicalReasons by button click but then rollNums are changed again (say from 7 to 17) so medicalReasons doesnt get changed if "Enter Medical Reasons" button is NOT clicked and it still shows 7 not 17, same for other reasons too
			//FOR MEDICAL REASONS
			if(editTextRollNumsMedicalLeave.getText().toString().length()>0)
			{
				LinkedHashMap<Integer, String> temp = new LinkedHashMap<Integer, String>();
				for(Map.Entry<Integer, String> entry:medicalReasons.entrySet())
					temp.put(entry.getKey(), entry.getValue());
				medicalReasons.clear();
				for(String rn:editTextRollNumsMedicalLeave.getText().toString().trim().split("[,]"))
				{
					int rollNumKey = Integer.parseInt(rn);
					if(temp.containsKey(rollNumKey))
						medicalReasons.put(rollNumKey, temp.get(rollNumKey));
					else
						medicalReasons.put(rollNumKey, "None specified");
			}
			}
			//FOR OTHER REASONS
			if(editTextRollNumsOtherReasons.getText().toString().length()>0)
			{
				LinkedHashMap<Integer, String> temp = new LinkedHashMap<Integer, String>();
				for(Map.Entry<Integer, String> entry:otherReasons.entrySet())
					temp.put(entry.getKey(), entry.getValue());
				otherReasons.clear();
				for(String rn:editTextRollNumsOtherReasons.getText().toString().trim().split("[,]"))
				{
					int rollNumKey = Integer.parseInt(rn);
					if(temp.containsKey(rollNumKey))
						otherReasons.put(rollNumKey, temp.get(rollNumKey));
					else
						otherReasons.put(rollNumKey, "None specified");
				}
			}
			
			reviewStr.append("Medical Leave: ");
			if(enteredMedicalLeaveRollNums!=null && enteredMedicalLeaveRollNums.length>0 && editTextRollNumsMedicalLeave.getText().toString().length()>0)
			{
				for(Map.Entry<Integer, String> entry:medicalReasons.entrySet())
					reviewStr.append(entry.getKey().toString()).append("(").append(entry.getValue()).append("), ");
				reviewStr = new StringBuffer(reviewStr.substring(0, reviewStr.length()-2));								//remove off last space and comma
			}else
				reviewStr.append("None");
			reviewStr.append(System.getProperty("line.separator"));
			
			reviewStr.append("Other Reasons: ");
			if(enteredOtherReasonsRollNums!=null && enteredOtherReasonsRollNums.length>0 && editTextRollNumsOtherReasons.getText().toString().length()>0)
			{
				for(Map.Entry<Integer, String> entry:otherReasons.entrySet())
					reviewStr.append(entry.getKey().toString()).append("(").append(entry.getValue()).append("), ");
				reviewStr = new StringBuffer(reviewStr.substring(0, reviewStr.length()-2));								//remove off last space and comma
			}else
				reviewStr.append("None");
			reviewStr.append(System.getProperty("line.separator"));
		}
		
		reviewStr.append(System.getProperty("line.separator"));							//New Line Separtor
		
		AlertDialog.Builder builder = new AlertDialog.Builder(ChoosePresetsManipAttendanceActivity.this);
		builder.setPositiveButton("Submit to Server", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				Toast.makeText(ChoosePresetsManipAttendanceActivity.this.getBaseContext(), "Submitting to Server initiated ..", Toast.LENGTH_SHORT).show();
				if(chosenMode.equals("SINGLE View") || chosenMode.equals("BATCH View") || chosenMode.equals("SINGLE Delete")|| chosenMode.equals("BATCH Delete")|| chosenMode.equals("SINGLE Insert/Update")|| chosenMode.equals("BATCH Insert/Update"))
				{
					Log.d("AsyncTaskSelection", "AttendanceOperation");
		    		new bgTaskSubmitAttendanceString().execute();
				}else if(chosenMode.equals("TRIGGER SINGLE View") || chosenMode.equals("TRIGGER BATCH View") || chosenMode.equals("TRIGGER SINGLE Delete")|| chosenMode.equals("TRIGGER BATCH Delete")|| chosenMode.equals("TRIGGER SINGLE Insert/Update")|| chosenMode.equals("TRIGGER BATCH Insert/Update"))
				{
					Log.d("AsyncTaskSelection", "TriggerStringOperation");
					new bgTaskSubmitTriggerOperationString().execute();
				}
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
			}
		});
		builder.setMessage(reviewStr);
		builder.setCancelable(false);
		AlertDialog alertDialogReviewSubmit = builder.create();
		alertDialogReviewSubmit.setTitle("Review and Submit");
		alertDialogReviewSubmit.show();
	}
	private String formulateAttendanceStringOrTriggerOperationString(String chosenMode)
    {
		if(chkBoxDontConsiderLectureTimes.isChecked())			//NOT Considering lecture Start and End Times so putting dummy values of 12am in both selectedLectureStartTime, selectedLectureEndTime so that it doesnt give null pointer exception
		{
			Calendar cal = Calendar.getInstance();
			cal.clear();
			selectedLectureStartTime = selectedLectureEndTime = cal.getTime();
		}
		
		//Same STRINGS FOR BOTH ATTENDANCE STRINGS AND TRIGGER OPERATION STRING EXCEPT MODE VALUE IS DIFFERENT
    	String temp;
    	
    	StringBuffer attendanceStringORTriggerOperationStringToReturn = new StringBuffer();
    	attendanceStringORTriggerOperationStringToReturn.append(chosenMode.startsWith("TRIGGER ")?"TriggerStringOperation":"AttendanceStringOperation").append(attendanceStringANDTriggerOperationStringEntitiesSeparationChar);
    	attendanceStringORTriggerOperationStringToReturn.append(chosenMode).append(attendanceStringANDTriggerOperationStringEntitiesSeparationChar);
		attendanceStringORTriggerOperationStringToReturn.append(teacherSelectedDBName).append(attendanceStringANDTriggerOperationStringEntitiesSeparationChar);
		attendanceStringORTriggerOperationStringToReturn.append(selectedPresetTableName).append(attendanceStringANDTriggerOperationStringEntitiesSeparationChar);
		attendanceStringORTriggerOperationStringToReturn.append(Integer.toString(retrievedNumStudents)).append(attendanceStringANDTriggerOperationStringEntitiesSeparationChar);
		attendanceStringORTriggerOperationStringToReturn.append(attendanceDatePicker.getDayOfMonth()+"/"+(attendanceDatePicker.getMonth()+1)+"/"+attendanceDatePicker.getYear()).append(attendanceStringANDTriggerOperationStringEntitiesSeparationChar);
		if(chosenMode.equals("SINGLE Delete") || chosenMode.equals("SINGLE View") || chosenMode.equals("TRIGGER SINGLE Delete") || chosenMode.equals("TRIGGER SINGLE View"))
		{
			//Format(Separation char = Tilde(~)):		AttendanceStringOperation~chosenMode~DBName~SelectedPresetTableName~numStudents~AttendanceDate(dd/mm/yyyy)~StartTime~EndTime
			attendanceStringORTriggerOperationStringToReturn.append(Preset.timeUserDisplayFormat.format(selectedLectureStartTime)).append(attendanceStringANDTriggerOperationStringEntitiesSeparationChar);
			attendanceStringORTriggerOperationStringToReturn.append(Preset.timeUserDisplayFormat.format(selectedLectureEndTime));
		}else if(chosenMode.equals("BATCH Delete") || chosenMode.equals("BATCH View") || chosenMode.equals("TRIGGER BATCH Delete") || chosenMode.equals("TRIGGER BATCH View"))
		{
			//Format(Separation char = Tilde(~)):		AttendanceStringOperation~chosenMode~DBName~SelectedPresetTableName~numStudents~AttendanceStartDate(dd/mm/yyyy)~AttendanceEndDate(dd/mm/yyyy)~StartTime~EndTime~true/false(=chkBoxDontConsiderLectureTimes)
			attendanceStringORTriggerOperationStringToReturn.append(attendanceEndDatePicker.getDayOfMonth()+"/"+(attendanceEndDatePicker.getMonth()+1)+"/"+attendanceEndDatePicker.getYear()).append(attendanceStringANDTriggerOperationStringEntitiesSeparationChar);
			attendanceStringORTriggerOperationStringToReturn.append(Preset.timeUserDisplayFormat.format(selectedLectureStartTime)).append(attendanceStringANDTriggerOperationStringEntitiesSeparationChar);
			attendanceStringORTriggerOperationStringToReturn.append(Preset.timeUserDisplayFormat.format(selectedLectureEndTime)).append(attendanceStringANDTriggerOperationStringEntitiesSeparationChar);
			attendanceStringORTriggerOperationStringToReturn.append(Boolean.toString(chkBoxDontConsiderLectureTimes.isChecked()));
		}else if(chosenMode.equals("SINGLE Insert/Update") || chosenMode.equals("TRIGGER SINGLE Insert/Update"))
		{
			//Format(Separation char = Tilde(~)):		AttendanceStringOperation~chosenMode~DBName~SelectedPresetTableName~numStudents~AttendanceDate(dd/mm/yyyy)~StartTime~EndTime~Presentees/Absentees~(Absentees/PresenteesRollNums) OR ALL~MedicalLeaveRollNumbers(Reason)~OtherReasonsRollNumbers(Reason)
			//Medical and Other Reasons Roll Numbers are separated by backquotes(`)
			attendanceStringORTriggerOperationStringToReturn.append(Preset.timeUserDisplayFormat.format(selectedLectureStartTime)).append(attendanceStringANDTriggerOperationStringEntitiesSeparationChar);
			attendanceStringORTriggerOperationStringToReturn.append(Preset.timeUserDisplayFormat.format(selectedLectureEndTime)).append(attendanceStringANDTriggerOperationStringEntitiesSeparationChar);
			attendanceStringORTriggerOperationStringToReturn.append(chkBoxIsPresentees.isChecked()?"Presentees":"Absentees").append(attendanceStringANDTriggerOperationStringEntitiesSeparationChar);
			attendanceStringORTriggerOperationStringToReturn.append(chkBoxAllRollNums.isChecked()?"ALL":(editTextRollNums.getText().toString().length()==0?"None":((temp=removeAllSpacesOfParamString(Arrays.deepToString(enteredRollNums))).substring(1, temp.length()-1).trim()))).append(attendanceStringANDTriggerOperationStringEntitiesSeparationChar);		//remove off starting and ending square bracket in toString() representations of Array and in between spaces
			
			if(editTextRollNumsMedicalLeave.getText().toString().trim().length()==0)
				attendanceStringORTriggerOperationStringToReturn.append("None");
			else
			{
				for(Map.Entry<Integer, String> entry:medicalReasons.entrySet())
					attendanceStringORTriggerOperationStringToReturn.append(entry.getKey().toString()).append('(').append(entry.getValue()).append(')').append('`');
				attendanceStringORTriggerOperationStringToReturn = new StringBuffer(attendanceStringORTriggerOperationStringToReturn.substring(0, attendanceStringORTriggerOperationStringToReturn.length()-1));		//remove off last backquote
			}
			attendanceStringORTriggerOperationStringToReturn.append(attendanceStringANDTriggerOperationStringEntitiesSeparationChar);
			
			if(editTextRollNumsOtherReasons.getText().toString().trim().length()==0)
				attendanceStringORTriggerOperationStringToReturn.append("None");
			else
			{
				for(Map.Entry<Integer, String> entry:otherReasons.entrySet())
					attendanceStringORTriggerOperationStringToReturn.append(entry.getKey().toString()).append('(').append(entry.getValue()).append(')').append('`');
				attendanceStringORTriggerOperationStringToReturn = new StringBuffer(attendanceStringORTriggerOperationStringToReturn.substring(0, attendanceStringORTriggerOperationStringToReturn.length()-1));		//remove off last backquote
			}
		}else if(chosenMode.equals("BATCH Insert/Update") || chosenMode.equals("TRIGGER BATCH Insert/Update"))
		{
			//Format(Separation char = Tilde(~)):		AttendanceStringOperation~chosenMode~DBName~SelectedPresetTableName~numStudents~AttendanceStartDate(dd/mm/yyyy)~AttendanceEndDate(dd/mm/yyyy)~StartTime~EndTime~Presentees/Absentees~(Absentees/PresenteesRollNums) OR ALL~MedicalLeaveRollNumbers(Reason)~OtherReasonsRollNumbers(Reason)
			//Medical and Other Reasons Roll Numbers are separated by backquotes(`)
			attendanceStringORTriggerOperationStringToReturn.append(attendanceEndDatePicker.getDayOfMonth()+"/"+(attendanceEndDatePicker.getMonth()+1)+"/"+attendanceEndDatePicker.getYear()).append(attendanceStringANDTriggerOperationStringEntitiesSeparationChar);
			attendanceStringORTriggerOperationStringToReturn.append(Preset.timeUserDisplayFormat.format(selectedLectureStartTime)).append(attendanceStringANDTriggerOperationStringEntitiesSeparationChar);
			attendanceStringORTriggerOperationStringToReturn.append(Preset.timeUserDisplayFormat.format(selectedLectureEndTime)).append(attendanceStringANDTriggerOperationStringEntitiesSeparationChar);
			attendanceStringORTriggerOperationStringToReturn.append(chkBoxIsPresentees.isChecked()?"Presentees":"Absentees").append(attendanceStringANDTriggerOperationStringEntitiesSeparationChar);
			attendanceStringORTriggerOperationStringToReturn.append(chkBoxAllRollNums.isChecked()?"ALL":(editTextRollNums.getText().toString().length()==0?"None":((temp=removeAllSpacesOfParamString(Arrays.deepToString(enteredRollNums))).substring(1, temp.length()-1).trim()))).append(attendanceStringANDTriggerOperationStringEntitiesSeparationChar);		//remove off starting and ending square bracket in toString() representations of Array and in between spaces
			
			if(editTextRollNumsMedicalLeave.getText().toString().trim().length()==0)
				attendanceStringORTriggerOperationStringToReturn.append("None");
			else
			{
				for(Map.Entry<Integer, String> entry:medicalReasons.entrySet())
					attendanceStringORTriggerOperationStringToReturn.append(entry.getKey().toString()).append('(').append(entry.getValue()).append(')').append('`');
				attendanceStringORTriggerOperationStringToReturn = new StringBuffer(attendanceStringORTriggerOperationStringToReturn.substring(0, attendanceStringORTriggerOperationStringToReturn.length()-1));		//remove off last backquote
			}
			attendanceStringORTriggerOperationStringToReturn.append(attendanceStringANDTriggerOperationStringEntitiesSeparationChar);
			
			if(editTextRollNumsOtherReasons.getText().toString().trim().length()==0)
				attendanceStringORTriggerOperationStringToReturn.append("None");
			else
			{
				for(Map.Entry<Integer, String> entry:otherReasons.entrySet())
					attendanceStringORTriggerOperationStringToReturn.append(entry.getKey().toString()).append('(').append(entry.getValue()).append(')').append('`');
				attendanceStringORTriggerOperationStringToReturn = new StringBuffer(attendanceStringORTriggerOperationStringToReturn.substring(0, attendanceStringORTriggerOperationStringToReturn.length()-1));		//remove off last backquote
			}
		}
		Log.d("TRIGGER Op", attendanceStringORTriggerOperationStringToReturn.toString());
		return attendanceStringORTriggerOperationStringToReturn.toString().trim();
	}

	private void saveLectureTime(boolean isStartTime)throws Exception
    {
		BufferedReader br = new BufferedReader(new FileReader(isStartTime?startTimesXMLFileName:endTimesXMLFileName));
		StringBuffer fileContents = new StringBuffer();
		String temp;
		while(isStartTime?(!(temp = br.readLine()).equals("</StartTimes>")):(!(temp = br.readLine()).equals("</EndTimes>")))				//replace root closing tag by new start time and then again write root closing tag
			fileContents.append(temp).append(System.getProperty("line.separator"));
		br.close();
		
		//Truncate File
		FileOutputStream fos = new FileOutputStream(isStartTime?startTimesXMLFileName:endTimesXMLFileName);
		FileChannel fileChannel = fos.getChannel();
		fileChannel.truncate(0);
		fileChannel.close();
		fos.close();
		
		PrintWriter pw = new PrintWriter(isStartTime?startTimesXMLFileName:endTimesXMLFileName);
		br = new BufferedReader(new StringReader(fileContents.toString()));
		while((temp = br.readLine())!=null)
			pw.println(temp);
		if(isStartTime)
		{
			pw.println("<StartTime>"+Preset.timeUserDisplayFormat.format(selectedLectureStartTime)+"</StartTime>");
			pw.println("</StartTimes>");
		}else
		{
			pw.println("<EndTime>"+Preset.timeUserDisplayFormat.format(selectedLectureEndTime)+"</EndTime>");
			pw.println("</EndTimes>");
		}
		pw.close();
	}
	static String getMD5OfParam(File serverDataZippedFile)
	{
		try
		{
			FileInputStream fin = new FileInputStream(serverDataZippedFile);
			MessageDigest mdEncoder = MessageDigest.getInstance("MD5");
			int len;
			byte[] byteArray = new byte[1024];
			while((len=fin.read(byteArray))>0)
				mdEncoder.update(byteArray, 0, len);
			String md5 = new BigInteger(1, mdEncoder.digest()).toString(16);
			fin.close();
			return md5;
			
		}catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	static String getMD5OfParam(String s)
	{
		try
		{
			MessageDigest mdEncoder = MessageDigest.getInstance("MD5");
			mdEncoder.update(s.getBytes());
			String md5 = new BigInteger(1, mdEncoder.digest()).toString(16);
			return md5;
			
		}catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	static String getSHA1OfParam(String s)
	{
		try
		{
			MessageDigest md = MessageDigest.getInstance("SHA-1"); 
			byte[] hash = md.digest(s.getBytes());
			Formatter formatter = new Formatter();
			for (byte b : hash)
				formatter.format("%02x", b);
			String hashString = formatter.toString();
			formatter.close();
			return hashString;
		}catch(Exception e)
		{
			return null;
		}
	}
	static void deleteFolderContents(File dir)
	{
		File[] files = dir.listFiles();
		if(files!=null)
			for(File f:files)
				if(f.isFile())
					f.delete();
				else if(f.isDirectory())
				{
					deleteFolderContents(f);
					f.delete();
				}
	}
	class bgTaskSubmitAttendanceString extends AsyncTask<Void, String, Exception>
	{
		ProgressDialog progressDialog = null;
		String attendanceStringToSend;
		boolean attendanceStringOperationSuccess = false, userCanceledOperation = false;
		ArrayList<edu.vesit.ams.AttendanceRecord> allDesiredTuplesIntermediateResult = null;				//final not intermediate in case of viewing modes(SINGLE and BATCH)
		
		boolean canSendConfirmation = false;
		
		boolean singleDeleteModeConfirmation = true;	//true if user confirms deletion else fase
		Boolean batchDeleteModeConfirmations[];			//array of boolean for arraylist of attendance Records, 1 boolean confirmation(true if user confirms deletion) per attendance Record
		
		Boolean singleInsertUpdateModeConfirmation[] = { false, true };
		AttendanceRecord mergedNewAddedToOldAttendanceRecordToSendToServer;
		/*	Values semantics/interpretation
		 * 	1st boolean(true to keep old value)		2nd boolean(true to keep new value)			meaning/interpretation at server
		 * 	true									false									=> keep old values(no update) OR user canceled operation(i.e no DB modifications = old values kept as it is)
		 *  false									true									=> keep new values by(overwriting old values or new attendance record insertion)
		 *  true									true									=> merge/add, overwrite new values over old values and keep those old value i.e not present in new values i.e add new values
		 *  false									false									=> none
		 *  
		 *  			SAME for batchInsertUpdateModeConfirmation but its a 2D array as rows represent confirmations for each attendance Record
		 *  
		 * */
		Boolean batchInsertUpdateModeConfirmations[][];
		ArrayList<AttendanceRecord> mergedNewAddedToOldAttendanceRecordToSendToServerBatchInsertUpdateMode;
	    	       				
	    @Override
	    protected void onPreExecute()
	    {
	    	progressDialog = new ProgressDialog(ChoosePresetsManipAttendanceActivity.this);
	    	progressDialog.setTitle("Processing ..");
	    	progressDialog.setMessage("Please wait.");
	    	progressDialog.setCancelable(false);
	    	progressDialog.setIndeterminate(true);
	    	progressDialog.show();
	    }
	    	    	    							
	    @SuppressWarnings("unchecked")
		@Override
	    protected Exception doInBackground(Void ...params)
	    {
    		//save start, end times, Send to server and do operations wrt modes
	    	try
	    	{
	    		if(!chkBoxDontConsiderLectureTimes.isChecked() && isChosenStartTimeCustom)
	    		{
	    			//get final start time if startTime is custom					=>						NO NEED OF THIS AS ITS ALREADY DONE WHILE SHOWING REVIEW TO USER
    	    		//selectedLectureStartTime = isChosenStartTimeCustom?(Preset.timeUserDisplayFormat.parse(lectureStartTimePicker.getCurrentHour().toString()+":"+lectureStartTimePicker.getCurrentMinute()+(lectureStartTimePicker.getCurrentHour()<12?" AM":" PM"))):selectedLectureStartTime;
	    			
	    			publishProgress("Saving new start time"+selectedLectureStartTime+" for future reference..");
	    			if(selectedPresetStartTimesAdapter.getPosition(Preset.timeUserDisplayFormat.format(selectedLectureStartTime))>=0)
	    				publishProgress("Already Saved custom Start Time!");
	    			else
	    			{
	    				saveLectureTime(true);
	    				publishProgress("Saved custom Start Time!");
	    			}
	    		}
	    		if(!chkBoxDontConsiderLectureTimes.isChecked() && isChosenEndTimeCustom)
	    		{
	    			//get final end time if endTime is custom					=>						NO NEED OF THIS AS ITS ALREADY DONE WHILE SHOWING REVIEW TO USER
	    			//selectedLectureEndTime = isChosenEndTimeCustom?(Preset.timeUserDisplayFormat.parse((lectureEndTimePicker.getCurrentHour()).toString()+":"+lectureEndTimePicker.getCurrentMinute()+(lectureEndTimePicker.getCurrentHour()<12?" AM":" PM"))):selectedLectureEndTime;
	    			
	    			publishProgress("Saving new end time "+selectedLectureEndTime+"for future reference..");
	    			if(selectedPresetStartTimesAdapter.getPosition(Preset.timeUserDisplayFormat.format(selectedLectureEndTime))>=0)
	    				publishProgress("Already Saved custom End Time!");
	    			else
	    			{
	    				saveLectureTime(false);
	    				publishProgress("Saved custom End Time!");
	    			}
	    		}
	    		publishProgress("Formulating Attendance String ..");
	    		attendanceStringToSend = formulateAttendanceStringOrTriggerOperationString(chosenMode);
	    		publishProgress("Formulated Attendance String!");
	    		
	    		LoginActivity.toServerOOStrm.writeObject(attendanceStringToSend);
	    		LoginActivity.toServerOOStrm.flush();
	    		
	    		Object obj = LoginActivity.fromServerOIS.readObject();							//read the arraylist object
	    		
	    		if(obj==null)
	    			throw new Exception("No attendance data received from server due to error or server being offline.");
	    		else if(obj instanceof Exception)
	    			throw (Exception)obj;
	    		
	    		allDesiredTuplesIntermediateResult = (ArrayList<edu.vesit.ams.AttendanceRecord>) obj;
	    		
	    		//Log.d("IntermediateResult", allDesiredTuplesIntermediateResult.toString());
	    		//Log.d("IntermediateResult Length", Integer.toString(allDesiredTuplesIntermediateResult.size()));
	    		
	    		if(chosenMode.equals("SINGLE View") || chosenMode.equals("BATCH View"))
	    		{
	    			publishProgress("Command=SHOW INTERMEDIATE RESULTS; Mode=View");
	    			attendanceStringOperationSuccess = true;
	    		}else if(chosenMode.equals("SINGLE Delete"))
	    		{
	    			publishProgress("Command=SHOW INTERMEDIATE RESULTS; Mode=SINGLE Delete");
	    			
	    			//loop infinitely until canSendConfirmation is set from AlertDialog because confirmation's default set value(true) is auto-sent without user's confirmation from AlertDialog
	    			while(!canSendConfirmation);
	    			
	    			LoginActivity.toServerOOStrm.writeObject(Boolean.valueOf(singleDeleteModeConfirmation));
	    			LoginActivity.toServerOOStrm.flush();
	    			
	    			if(singleDeleteModeConfirmation)
	    			{
    	    			Object o = LoginActivity.fromServerOIS.readObject();
    	    			if(o instanceof Exception)
    	    				throw (Exception)o;
    	    			else
    	    				attendanceStringOperationSuccess = ((String)o).equals("Deletion Success!");
	    			}else
	    				userCanceledOperation = true;
	    			
	    		}else if(chosenMode.equals("BATCH Delete"))
	    		{
	    			publishProgress("Command=SHOW INTERMEDIATE RESULTS; Mode=BATCH Delete");
	    			
	    			//loop infinitely until canSendConfirmation is set from AlertDialog because confirmation's default set value is auto-sent without user's confirmation from AlertDialog
	    			while(!canSendConfirmation);
	    			
	    			LoginActivity.toServerOOStrm.writeObject(batchDeleteModeConfirmations);
	    			LoginActivity.toServerOOStrm.flush();
	    			
	    			if(batchDeleteModeConfirmations==null)
	    				userCanceledOperation = true;								//will be true if batchDeleteModeConfirmations has all false i.e dont delete for all attendance records or is NULL
	    			else
	    			{
	    				userCanceledOperation = true;								//assigning true because in below for loop if any1 is true in batchDeleteModeConfirmations then at that index Attendance Record must be deleted => userCanceledOperation = false; if all are false then if condition will never be true => userCanceledOperation remains true(its assigned here) 
    	    			for(int i=0;i<batchDeleteModeConfirmations.length;i++)
    	    				if(batchDeleteModeConfirmations[i])
    	    				{
    	    					userCanceledOperation = false;
    	    					break;
    	    				}
	    			}
	    			
	    			if(!userCanceledOperation)
	    			{
	    				Object o = LoginActivity.fromServerOIS.readObject();
    	    			if(o instanceof Exception)
    	    				throw (Exception)o;
    	    			else
    	    				attendanceStringOperationSuccess = ((String)o).equals("Deletion Success!");
	    			}
	    		}else if(chosenMode.equals("SINGLE Insert/Update"))
	    		{
	    			if(allDesiredTuplesIntermediateResult.size()==0)				//No already existing attendance record for that date, lectureStartIme and EndTime so its a insert operation of new attendance Record
	    			{
	    				singleInsertUpdateModeConfirmation[0] = false;			//new record insertion confirmation indicators(similar to keep new, discard old)
	    				singleInsertUpdateModeConfirmation[1] = true;
	    				
	    				LoginActivity.toServerOOStrm.writeObject(singleInsertUpdateModeConfirmation);
	    				LoginActivity.toServerOOStrm.flush();
	    				
	    				Object o = LoginActivity.fromServerOIS.readObject();
    	    			if(o instanceof Exception)
    	    				throw (Exception)o;
    	    			else
    	    				attendanceStringOperationSuccess = ((String)o).equals("Insertion/Updation Success!");
	    			}else
	    			{
	    				publishProgress("Command=SHOW INTERMEDIATE RESULTS; Mode=SINGLE Insert/Update");
	    				
    	    			//loop infinitely until canSendConfirmation is set from AlertDialog because confirmation's default set value is auto-sent without user's confirmation from AlertDialog
    	    			while(!canSendConfirmation);
    	    			
    	    			LoginActivity.toServerOOStrm.writeObject(singleInsertUpdateModeConfirmation);
	    				LoginActivity.toServerOOStrm.flush();
	    				
	    				//Leaving aside above condition we would get some insertion/updation success indicator from server
	    				if(!singleInsertUpdateModeConfirmation[0] && singleInsertUpdateModeConfirmation[1])		//Keep NEW Only
	    				{
	    	    			Object o = LoginActivity.fromServerOIS.readObject();
		    	    		if(o instanceof Exception)
		    	    			throw (Exception)o;
		    	    		else
		    	    			attendanceStringOperationSuccess = ((String)o).equals("Insertion/Updation Success!");
	    				}else if(singleInsertUpdateModeConfirmation[0] && singleInsertUpdateModeConfirmation[1])		//Merging/Adding
	    				{
	    					LoginActivity.toServerOOStrm.writeObject(mergedNewAddedToOldAttendanceRecordToSendToServer);
    	    				LoginActivity.toServerOOStrm.flush();
    	    				
	    	    			Object o = LoginActivity.fromServerOIS.readObject();
		    	    		if(o instanceof Exception)
		    	    			throw (Exception)o;
		    	    		else
		    	    			attendanceStringOperationSuccess = ((String)o).equals("Insertion/Updation Success!");
	    				}else if(singleInsertUpdateModeConfirmation[0] && !singleInsertUpdateModeConfirmation[1])		//Keep OLD Only = User canceled insert/update operation of single attendance record
	    					userCanceledOperation = true;
	    			}
	    		}else if(chosenMode.equals("BATCH Insert/Update"))
	    		{
	    			if(allDesiredTuplesIntermediateResult.size()==0)				//No already existing attendance records for that date range, lectureStartIme and EndTime so its a insert operation of all new attendance Records for that range
	    			{
	    				publishProgress("Please wait ..");
	    				Thread.sleep(5000);									//Make AsyncTask Thread sleep for 5secs(delay) so that AMSServer can do insertions of all new records into DB(AMSServer needs sometime for that  so putting a delay here , if not added we would be waiting for result confirmation and a socket read time out would occur!!)
	    				
	    				Object o = LoginActivity.fromServerOIS.readObject();
    	    			if(o instanceof Exception)
    	    				throw (Exception)o;
    	    			else
    	    				attendanceStringOperationSuccess = ((String)o).equals("Batch Insertion/Updation Success!");
	    			}else
	    			{
	    				publishProgress("Command=SHOW INTERMEDIATE RESULTS; Mode=BATCH Insert/Update");
	    				
    	    			//loop infinitely until canSendConfirmation is set from AlertDialog because confirmation's default set value is auto-sent without user's confirmation from AlertDialog
    	    			while(!canSendConfirmation);
    	    			
    	    			LoginActivity.toServerOOStrm.writeObject(batchInsertUpdateModeConfirmations);
	    				LoginActivity.toServerOOStrm.flush();
	    				
	    				LoginActivity.toServerOOStrm.writeObject(mergedNewAddedToOldAttendanceRecordToSendToServerBatchInsertUpdateMode);
	    				LoginActivity.toServerOOStrm.flush();
	    				
	    				/*boolean userCanceledOperation = true;			//will be true if batchInsertUpdateModeConfirmations has all true, false(Keep OLD Only) pairs for all already existing Attendance Records
	    	    		for(int i=0;i<batchInsertUpdateModeConfirmations.length;i++)
	    	    			if(!(batchInsertUpdateModeConfirmations[i][0] && !batchInsertUpdateModeConfirmations[i][1]))			//if any1 chosen update option is different than Keep OLD Only => means no cancel and if all are Keep OLD Only = userCanceledOperation
	    	    			{
	    	    				userCanceledOperation = false;
	    	    				break;
	    	    			}
	    				
	    				//Leaving aside above condition we would get some insertion/updation success indicator from server if user didnt cancel operation
	    	    		if(userCanceledOperation)
	    	    		{
	    	    			Object o = LoginActivity.fromServerOIS.readObject();
	    	    			if(o instanceof Exception)
	    	    				throw (Exception)o;
	    	    			else
	    	    				attendanceStringOperationSuccess = ((String)o).equals("Batch Insertion/Updation Success!");
	    	    			//user has requested to keep OLD Only Update Option for all already existing Attendance Records BUT REMEMBER there can be non-existing Attendance Records which could have been inserted earlier so need to display successful operation
	    	    		}else
	    	    		{
	    	    			Object o = LoginActivity.fromServerOIS.readObject();
	    	    			if(o instanceof Exception)
	    	    				throw (Exception)o;
	    	    			else
	    	    				attendanceStringOperationSuccess = ((String)o).equals("Batch Insertion/Updation Success!");
	    	    		}*/
	    	    		
	    	    		//Commented shown above indicates => do not distinguish between userCanceledOperation or not while selecting Update Options as for user cancelling the operation, even then we need confirmation for insertion of non-existing attendance records(For Update Option = Keep OLD Only For ALL = User canceled insert/update operation => Dont show user canceled operation toast because there can be insertion of non-existing attendance records)
	    	    		
	    	    		Object o = LoginActivity.fromServerOIS.readObject();
    	    			if(o instanceof Exception)
    	    				throw (Exception)o;
    	    			else
    	    				attendanceStringOperationSuccess = ((String)o).equals("Batch Insertion/Updation Success!");
	    			}
	    		}
	    	}catch (Exception e) 
	    	{
	    		Log.e("Exception", e.getMessage()==null?"null":e.getMessage());
	    		e.printStackTrace();
	    		return e;
	    	}
			return null;
	    }
	    
	    private void saveToHistoryRecords(boolean isOperatingOnBehalf, String attendanceStringToSend, boolean attendanceStringOperationSuccess) throws Exception
	    {
	    	if(!MainActivity.appDataTeachersNonAllotedPresetHistoryAttendanceStringsAppFile.getParentFile().exists())
	    		MainActivity.appDataTeachersNonAllotedPresetHistoryAttendanceStringsAppFile.getParentFile().mkdirs();
	    	if(!MainActivity.appDataTeachersAllotedPresetHistoryAttendanceStringsAppFile.getParentFile().exists())
	    		MainActivity.appDataTeachersAllotedPresetHistoryAttendanceStringsAppFile.getParentFile().mkdirs();
	    	
	    	if(!MainActivity.appDataTeachersNonAllotedPresetHistoryAttendanceStringsAppFile.exists())
	    		MainActivity.appDataTeachersNonAllotedPresetHistoryAttendanceStringsAppFile.createNewFile();
	    	if(!MainActivity.appDataTeachersAllotedPresetHistoryAttendanceStringsAppFile.exists())
	    		MainActivity.appDataTeachersAllotedPresetHistoryAttendanceStringsAppFile.createNewFile();
	    	
			FileWriter fw = new FileWriter(isOperatingOnBehalf?MainActivity.appDataTeachersNonAllotedPresetHistoryAttendanceStringsAppFile:MainActivity.appDataTeachersAllotedPresetHistoryAttendanceStringsAppFile, true);
			fw.write("[ " + MainActivity.logEventTimestampFormat.format(new java.util.Date()) + " ] " + attendanceStringToSend + " .. "+Boolean.toString(attendanceStringOperationSuccess));
			fw.write(System.getProperty("line.separator"));
			fw.close();
		}

		@Override
	    protected void onProgressUpdate(String ...progressParams)
	    {
			if(progressParams[0].equals("Command=SHOW INTERMEDIATE RESULTS; Mode=View"))
			{
				if(progressDialog!=null)
    				progressDialog.dismiss();
				AlertDialog.Builder builder = new AlertDialog.Builder(ChoosePresetsManipAttendanceActivity.this);
    			builder.setTitle("View Results(Total Records: "+allDesiredTuplesIntermediateResult.size()+")");
    			builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
    			{
    				@Override
    				public void onClick(DialogInterface dialog, int which)
    				{
    					dialog.dismiss();
    				}
    			});
    			
    			if(allDesiredTuplesIntermediateResult.size()==0)
    				builder.setMessage("No Attendance Records found matching your request.");
    			else
    			{	
	    			LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    			ScrollView scrollView = (ScrollView)inflater.inflate(R.layout.medical_other_reasons__other_input_dialogs_scrollview, null);
	    			
	    			LinearLayout linearLayout = new LinearLayout(ChoosePresetsManipAttendanceActivity.this);
	    			linearLayout.setOrientation(LinearLayout.VERTICAL);
	    					
	    			for(AttendanceRecord attendanceRecord:allDesiredTuplesIntermediateResult)
	    			{
	    				Log.d("AttendanceRecord", attendanceRecord.toString());
	    				
	    				TextView txtviewShowAttendanceRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
	    				LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
	    				txtviewShowAttendanceRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
	    				txtviewShowAttendanceRecord.setText("Attendance Date: "+Preset.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+System.getProperty("line.separator"));
	    				txtviewShowAttendanceRecord.append("Lecture Start Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+System.getProperty("line.separator"));
	    				txtviewShowAttendanceRecord.append("Lecture End Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+System.getProperty("line.separator"));
	    				txtviewShowAttendanceRecord.append(attendanceRecord.toString());			//toString() only gives back presentees, absentees, medical leaves, other reasons, count of each
	    				linearLayout.addView(txtviewShowAttendanceRecord, layoutParams1);
	    						
	    				View separatorBetweenAttendanceRecords = new View(ChoosePresetsManipAttendanceActivity.this);
	    				LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 4);
	    				layoutParams2.setMargins(0, 10, 0, 10);
	    				separatorBetweenAttendanceRecords.setBackgroundColor(Color.WHITE);
	    				linearLayout.addView(separatorBetweenAttendanceRecords, layoutParams2);
	    			}
	    					
	    			scrollView.addView(linearLayout);
	    			builder.setView(scrollView);
    			}
    			builder.create().show();
			}else if(progressParams[0].equals("Command=SHOW INTERMEDIATE RESULTS; Mode=SINGLE Delete"))
			{
				if(progressDialog!=null)
    				progressDialog.dismiss();
				AlertDialog.Builder builder = new AlertDialog.Builder(ChoosePresetsManipAttendanceActivity.this);
    			builder.setTitle("View Results(Total Records: "+allDesiredTuplesIntermediateResult.size()+")");
    			
    			if(allDesiredTuplesIntermediateResult.size()==0)
    			{
    				builder.setMessage("No Attendance Records found matching your request.");
    				builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
    				{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							singleDeleteModeConfirmation = false;
							dialog.dismiss();
							canSendConfirmation = true;
						}
					});
    			}else
    			{
    				builder.setPositiveButton("Delete", new DialogInterface.OnClickListener()
	    			{
	    				@Override
	    				public void onClick(DialogInterface dialog, int which)
	    				{
	    					singleDeleteModeConfirmation = true;
	    					dialog.dismiss();
	    					if(progressDialog!=null)
	    					{
	    						progressDialog.setMessage("Please wait.");
	    						progressDialog.show();
	    					}
	    					canSendConfirmation = true;
	    				}
	    			});
	    			builder.setNegativeButton("Dont Delete", new DialogInterface.OnClickListener()
	    			{
	    				@Override
	    				public void onClick(DialogInterface dialog, int which)
	    				{
	    					singleDeleteModeConfirmation = false;
	    					dialog.dismiss();
	    					canSendConfirmation = true;
	    				}
	    			});
    				
	    			LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    			ScrollView scrollView = (ScrollView)inflater.inflate(R.layout.medical_other_reasons__other_input_dialogs_scrollview, null);
	    			
	    			LinearLayout linearLayout = new LinearLayout(ChoosePresetsManipAttendanceActivity.this);
	    			linearLayout.setOrientation(LinearLayout.VERTICAL);
	    					
	    			for(AttendanceRecord attendanceRecord:allDesiredTuplesIntermediateResult)
	    			{
	    				Log.d("AttendanceRecord", attendanceRecord.toString());
	    				
	    				TextView txtviewShowAttendanceRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
	    				LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
	    				txtviewShowAttendanceRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
	    				txtviewShowAttendanceRecord.setText("Attendance Date: "+Preset.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+System.getProperty("line.separator"));
	    				txtviewShowAttendanceRecord.append("Lecture Start Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+System.getProperty("line.separator"));
	    				txtviewShowAttendanceRecord.append("Lecture End Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+System.getProperty("line.separator"));
	    				txtviewShowAttendanceRecord.append(attendanceRecord.toString());			//toString() only gives back presentees, absentees, medical leaves, other reasons, count of each
	    				linearLayout.addView(txtviewShowAttendanceRecord, layoutParams1);
	    						
	    				View separatorBetweenAttendanceRecords = new View(ChoosePresetsManipAttendanceActivity.this);
	    				LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 4);
	    				layoutParams2.setMargins(0, 10, 0, 10);
	    				separatorBetweenAttendanceRecords.setBackgroundColor(Color.WHITE);
	    				linearLayout.addView(separatorBetweenAttendanceRecords, layoutParams2);
	    			}
	    					
	    			scrollView.addView(linearLayout);
	    			builder.setView(scrollView);
    			}
    			builder.setCancelable(false);
    			builder.create().show();
			}else if(progressParams[0].equals("Command=SHOW INTERMEDIATE RESULTS; Mode=BATCH Delete"))
			{
				if(progressDialog!=null)
    				progressDialog.dismiss();
				AlertDialog.Builder builder = new AlertDialog.Builder(ChoosePresetsManipAttendanceActivity.this);
    			builder.setTitle("View Results(Total Records: "+allDesiredTuplesIntermediateResult.size()+")");
    			
    			if(allDesiredTuplesIntermediateResult.size()==0)
    			{
    				builder.setMessage("No Attendance Records found matching your request.");
    				builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
    				{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							batchDeleteModeConfirmations = null;					//[null OR all boolean values are false] means user canceled operation
							dialog.dismiss();
							canSendConfirmation = true;
						}
					});
    			}else
    			{
    				batchDeleteModeConfirmations = new Boolean[allDesiredTuplesIntermediateResult.size()];
    				for(int i=0;i<batchDeleteModeConfirmations.length;i++)
    					batchDeleteModeConfirmations[i] = Boolean.valueOf(false);
    				final CheckBox[] chkBoxToDeleteConfirmation = new CheckBox[allDesiredTuplesIntermediateResult.size()];			//to refer later for confirmation array formation
    				
	    			LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    			ScrollView scrollView = (ScrollView)inflater.inflate(R.layout.medical_other_reasons__other_input_dialogs_scrollview, null);
	    			
	    			LinearLayout wrappingLinearLayout = new LinearLayout(ChoosePresetsManipAttendanceActivity.this);
	    			wrappingLinearLayout.setOrientation(LinearLayout.VERTICAL);
	    			
	    			int i = 0;
	    					
	    			for(AttendanceRecord attendanceRecord:allDesiredTuplesIntermediateResult)
	    			{
	    				LinearLayout linearLayout = new LinearLayout(ChoosePresetsManipAttendanceActivity.this);
	    				linearLayout.setOrientation(LinearLayout.HORIZONTAL);
	    				linearLayout.setWeightSum(1.0F);
	    				
	    				TextView txtviewShowAttendanceRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
	    				LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9F);
	    				txtviewShowAttendanceRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
	    				txtviewShowAttendanceRecord.setText("Attendance Date: "+Preset.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+System.getProperty("line.separator"));
	    				txtviewShowAttendanceRecord.append("Lecture Start Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+System.getProperty("line.separator"));
	    				txtviewShowAttendanceRecord.append("Lecture End Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+System.getProperty("line.separator"));
	    				txtviewShowAttendanceRecord.append(attendanceRecord.toString());			//toString() only gives back presentees, absentees, medical leaves, other reasons, count of each
	    				linearLayout.addView(txtviewShowAttendanceRecord, layoutParams1);
	    				
	    				chkBoxToDeleteConfirmation[i] = new CheckBox(ChoosePresetsManipAttendanceActivity.this);
	    				LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.1F);
	    				chkBoxToDeleteConfirmation[i].setChecked(true);
	    				linearLayout.addView(chkBoxToDeleteConfirmation[i], layoutParams2);
	    				
	    				wrappingLinearLayout.addView(linearLayout);
	    						
	    				View separatorBetweenAttendanceRecords = new View(ChoosePresetsManipAttendanceActivity.this);
	    				LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 4);
	    				layoutParams2.setMargins(0, 10, 0, 10);
	    				separatorBetweenAttendanceRecords.setBackgroundColor(Color.WHITE);
	    				wrappingLinearLayout.addView(separatorBetweenAttendanceRecords, layoutParams3);
	    				
	    				i++;
	    			}
	    					
	    			scrollView.addView(wrappingLinearLayout);
	    			builder.setView(scrollView);
	    			
    				builder.setPositiveButton("Delete Selected", new DialogInterface.OnClickListener()
	    			{
	    				@Override
	    				public void onClick(DialogInterface dialog, int which)
	    				{
	    					for(int i=0;i<batchDeleteModeConfirmations.length;i++)
	    						batchDeleteModeConfirmations[i] = Boolean.valueOf(chkBoxToDeleteConfirmation[i].isChecked());
	    						
	    					dialog.dismiss();
	    					if(progressDialog!=null)
	    					{
	    						progressDialog.setMessage("Please wait.");
	    						progressDialog.show();
	    					}
	    					canSendConfirmation = true;
	    				}
	    			});
	    			builder.setNegativeButton("Dont Delete All", new DialogInterface.OnClickListener()
	    			{
	    				@Override
	    				public void onClick(DialogInterface dialog, int which)
	    				{
	    					for(int i=0;i<batchDeleteModeConfirmations.length;i++)
	    						batchDeleteModeConfirmations[i] = Boolean.valueOf(false);						//[null OR all boolean values are false] means user canceled operation
	    					dialog.dismiss();
	    					canSendConfirmation = true;
	    				}
	    			});
	    			builder.setNeutralButton("Delete All", new DialogInterface.OnClickListener()
	    			{	
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
	    					for(int i=0;i<batchDeleteModeConfirmations.length;i++)
	    						batchDeleteModeConfirmations[i] = Boolean.valueOf(true);
	    						
	    					dialog.dismiss();
	    					if(progressDialog!=null)
	    					{
	    						progressDialog.setMessage("Please wait.");
	    						progressDialog.show();
	    					}
	    					canSendConfirmation = true;
						}
					});
    			}
    			builder.setCancelable(false);
    			builder.create().show();
			}else if(progressParams[0].equals("Command=SHOW INTERMEDIATE RESULTS; Mode=SINGLE Insert/Update"))
			{
				if(progressDialog!=null)
					progressDialog.dismiss();
				Calendar cal = Calendar.getInstance();
				//show alert dialog with 3 radio buttons (+ve = keep new, -ve = keep old, cancel = add to old) inside alert dialog that sets confirmation accordingly for that conflicting/already existing Attendance Record
				AlertDialog.Builder builder = new AlertDialog.Builder(ChoosePresetsManipAttendanceActivity.this);
				builder.setTitle("Update already existing..");
				
				LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    			ScrollView scrollView = (ScrollView)inflater.inflate(R.layout.medical_other_reasons__other_input_dialogs_scrollview, null);
    			
    			LinearLayout linearLayout = new LinearLayout(ChoosePresetsManipAttendanceActivity.this);
    			linearLayout.setOrientation(LinearLayout.VERTICAL);
    					
    			for(AttendanceRecord attendanceRecord:allDesiredTuplesIntermediateResult)
    			{
    				Log.d("AttendanceRecord", attendanceRecord.toString());
    				
    				//Old Attendance Record
    				TextView txtviewShowOldAttendanceRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    				txtviewShowOldAttendanceRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				txtviewShowOldAttendanceRecord.setText("\t\tOLD Only");
    				txtviewShowOldAttendanceRecord.append(System.getProperty("line.separator"));
    				txtviewShowOldAttendanceRecord.append("Attendance Date: "+Preset.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+System.getProperty("line.separator"));
    				txtviewShowOldAttendanceRecord.append("Lecture Start Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+System.getProperty("line.separator"));
    				txtviewShowOldAttendanceRecord.append("Lecture End Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+System.getProperty("line.separator"));
    				txtviewShowOldAttendanceRecord.append(attendanceRecord.toString());			//toString() only gives back presentees, absentees, medical leaves, other reasons, count of each
    				linearLayout.addView(txtviewShowOldAttendanceRecord, layoutParams1);
    						
    				View separatorBetweenOldNewMergedAttendanceRecords1 = new View(ChoosePresetsManipAttendanceActivity.this);
    				LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2);
    				layoutParams2.setMargins(0, 10, 0, 10);
    				separatorBetweenOldNewMergedAttendanceRecords1.setBackgroundColor(Color.WHITE);
    				linearLayout.addView(separatorBetweenOldNewMergedAttendanceRecords1, layoutParams2);
    				
    				//New Attendance Record
    				TextView txtviewShowNewAttendanceRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				txtviewShowNewAttendanceRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				txtviewShowNewAttendanceRecord.setText("\t\tNEW Only");
    				txtviewShowNewAttendanceRecord.append(System.getProperty("line.separator"));
    				cal.set(attendanceDatePicker.getYear(), attendanceDatePicker.getMonth(), attendanceDatePicker.getDayOfMonth());
    				txtviewShowNewAttendanceRecord.append("Attendance Date: "+Preset.userDisplayAttendanceDateFormat.format(cal.getTime())+System.getProperty("line.separator"));
    				txtviewShowNewAttendanceRecord.append("Lecture Start Time: "+Preset.timeUserDisplayFormat.format(selectedLectureStartTime)+System.getProperty("line.separator"));
    				txtviewShowNewAttendanceRecord.append("Lecture End Time: "+Preset.timeUserDisplayFormat.format(selectedLectureEndTime)+System.getProperty("line.separator"));
    				txtviewShowNewAttendanceRecord.append(chkBoxIsPresentees.isChecked()?"Presentees: ":"Absentees: ");
    				txtviewShowNewAttendanceRecord.append(chkBoxAllRollNums.isChecked()?"ALL":(editTextRollNums.getText().toString().length()==0?"None":Arrays.deepToString(enteredRollNums)));		//remove off starting and ending square bracket in toString() representations of Array and in between spaces
    				txtviewShowNewAttendanceRecord.append(System.getProperty("line.separator"));
					
    				txtviewShowNewAttendanceRecord.append("Medical Leave: ");
					if(editTextRollNumsMedicalLeave.getText().toString().trim().length()==0)
						txtviewShowNewAttendanceRecord.append("None");
					else
					{
						for(Map.Entry<Integer, String> entry:medicalReasons.entrySet())
						{
							txtviewShowNewAttendanceRecord.append(entry.getKey().toString());
							txtviewShowNewAttendanceRecord.append("(");
							txtviewShowNewAttendanceRecord.append(entry.getValue());
							txtviewShowNewAttendanceRecord.append("), ");
						}
						txtviewShowNewAttendanceRecord.setText(txtviewShowNewAttendanceRecord.getText().toString().substring(0, txtviewShowNewAttendanceRecord.getText().toString().length()-1));		//remove off last comma
						txtviewShowNewAttendanceRecord.append(System.getProperty("line.separator"));
					}
					txtviewShowNewAttendanceRecord.append(System.getProperty("line.separator"));
					
					txtviewShowNewAttendanceRecord.append("Medical Leave: ");
					if(editTextRollNumsOtherReasons.getText().toString().trim().length()==0)
						txtviewShowNewAttendanceRecord.append("None");
					else
					{
						for(Map.Entry<Integer, String> entry:otherReasons.entrySet())
						{
							txtviewShowNewAttendanceRecord.append(entry.getKey().toString());
							txtviewShowNewAttendanceRecord.append("(");
							txtviewShowNewAttendanceRecord.append(entry.getValue());
							txtviewShowNewAttendanceRecord.append("), ");
						}
						txtviewShowNewAttendanceRecord.setText(txtviewShowNewAttendanceRecord.getText().toString().substring(0, txtviewShowNewAttendanceRecord.getText().toString().length()-1));		//remove off last comma
					}
    				linearLayout.addView(txtviewShowNewAttendanceRecord, layoutParams1);
    				
    				View separatorBetweenOldNewMergedAttendanceRecords2 = new View(ChoosePresetsManipAttendanceActivity.this);
    				layoutParams2.setMargins(0, 10, 0, 10);
    				separatorBetweenOldNewMergedAttendanceRecords2.setBackgroundColor(Color.WHITE);
    				linearLayout.addView(separatorBetweenOldNewMergedAttendanceRecords2, layoutParams2);
    				
    				//Merge/ADD to OLD Results
    				TextView txtviewShowMergingAddingAttendanceRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				txtviewShowMergingAddingAttendanceRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				txtviewShowMergingAddingAttendanceRecord.setText("\t\tADD to OLD");
    				txtviewShowMergingAddingAttendanceRecord.append(System.getProperty("line.separator"));
    				txtviewShowMergingAddingAttendanceRecord.append("Attendance Date: "+Preset.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+System.getProperty("line.separator"));
    				txtviewShowMergingAddingAttendanceRecord.append("Lecture Start Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+System.getProperty("line.separator"));
    				txtviewShowMergingAddingAttendanceRecord.append("Lecture End Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+System.getProperty("line.separator"));
    				//actual merging below
    				LinkedHashMap<Integer, String> mergedNewAddedToOldAttendance = attendanceRecord.getAttendance();			//1st take whole old attendance record and then overwrite on it the new entries
    				if(chkBoxAllRollNums.isChecked())
    				{
    					for(int i=1;i<=retrievedNumStudents;i++)
    						mergedNewAddedToOldAttendance.put(i, chkBoxIsPresentees.isChecked()?"P":"A");
    				}else
    				{
    					if(editTextRollNums.getText().toString().trim().length()>0)
    						for(int i=0;i<enteredRollNums.length;i++)
    							mergedNewAddedToOldAttendance.put(Integer.parseInt(enteredRollNums[i]), chkBoxIsPresentees.isChecked()?"P":"A");
    					
    					if(editTextRollNumsMedicalLeave.getText().toString().trim().length()>0)
    					{
    						if(medicalReasons.size()==0)				//for that condition where no reason specified i.e reasons entry button not clicked
    						{
    							for(String rn:editTextRollNumsMedicalLeave.getText().toString().trim().split("[,]"))
    							{
    								medicalReasons.put(Integer.parseInt(rn), "M(None Specified)");
    								mergedNewAddedToOldAttendance.put(Integer.parseInt(rn), "M(None Specified)");
    							}
    						}else
    						{
    							for(Map.Entry<Integer, String> entry:medicalReasons.entrySet())
    								mergedNewAddedToOldAttendance.put(entry.getKey(), "M("+entry.getValue()+")");
    						}
    					}
    					
    					if(editTextRollNumsOtherReasons.getText().toString().trim().length()>0)
    					{
    						if(otherReasons.size()==0)
    						{
    							for(String rn:editTextRollNumsOtherReasons.getText().toString().trim().split("[,]"))
    							{
    								otherReasons.put(Integer.parseInt(rn), "O(None Specified)");
    								mergedNewAddedToOldAttendance.put(Integer.parseInt(rn), "O(None Specified)");
    							}
    						}else
    						{
    							for(Map.Entry<Integer, String> entry:otherReasons.entrySet())
    								mergedNewAddedToOldAttendance.put(entry.getKey(), "O("+entry.getValue()+")");
    						}
    					}
    				}
    				AttendanceRecord mergedNewAddedToOldAttendanceRecord = new AttendanceRecord(attendanceRecord.getAttendanceDate(), attendanceRecord.getLectureStartTime(), attendanceRecord.getLectureEndTime());
    				for(Map.Entry<Integer, String> entry:mergedNewAddedToOldAttendance.entrySet())
    					mergedNewAddedToOldAttendanceRecord.addAttendanceOfNewRollNum(entry.getKey(), entry.getValue());
    				txtviewShowMergingAddingAttendanceRecord.append(mergedNewAddedToOldAttendanceRecord.toString());
    				linearLayout.addView(txtviewShowMergingAddingAttendanceRecord, layoutParams1);
    				
    				mergedNewAddedToOldAttendanceRecordToSendToServer = mergedNewAddedToOldAttendanceRecord;
    						
    				View separatorAttendanceRecords = new View(ChoosePresetsManipAttendanceActivity.this);
    				LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 4);
    				layoutParams2.setMargins(0, 10, 0, 10);
    				separatorAttendanceRecords.setBackgroundColor(Color.CYAN);
    				linearLayout.addView(separatorAttendanceRecords, layoutParams3);
    			}
    					
    			scrollView.addView(linearLayout);
    			builder.setView(scrollView);
				
				builder.setPositiveButton("NEW only", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						singleInsertUpdateModeConfirmation[0] = false;
						singleInsertUpdateModeConfirmation[1] = true;
						dialog.cancel();
						progressDialog.setMessage("Please wait.");
						progressDialog.show();
						canSendConfirmation = true;
					}
				});
				builder.setNegativeButton("OLD only", new DialogInterface.OnClickListener()
				{	
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						singleInsertUpdateModeConfirmation[0] = true;
						singleInsertUpdateModeConfirmation[1] = false;
						dialog.cancel();
						canSendConfirmation = true;
					}
				});
				builder.setNeutralButton("ADD to OLD", new DialogInterface.OnClickListener()
				{	
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						singleInsertUpdateModeConfirmation[0] = true;
						singleInsertUpdateModeConfirmation[1] = true;
						dialog.cancel();
						progressDialog.setMessage("Please wait.");
						progressDialog.show();
						canSendConfirmation = true;
					}
				});
				builder.setCancelable(false);
				builder.create().show();
			}else if(progressParams[0].equals("Command=SHOW INTERMEDIATE RESULTS; Mode=BATCH Insert/Update"))
			{
				if(progressDialog!=null)
					progressDialog.dismiss();
				Calendar cal = Calendar.getInstance();
				//show alert dialog with 3 radio buttons (+ve = keep new, -ve = keep old, cancel = add to old) inside alert dialog that sets confirmation accordingly for each conflicting/already existing Attendance Record
				AlertDialog.Builder builder = new AlertDialog.Builder(ChoosePresetsManipAttendanceActivity.this);
				builder.setTitle("Update already existing..");
				
				LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    			ScrollView scrollView = (ScrollView)inflater.inflate(R.layout.medical_other_reasons__other_input_dialogs_scrollview, null);
    			
    			LinearLayout linearLayout = new LinearLayout(ChoosePresetsManipAttendanceActivity.this);
    			linearLayout.setOrientation(LinearLayout.VERTICAL);
    			
    			final RadioGroup radioGroupForUpdateOptions[] = new RadioGroup[allDesiredTuplesIntermediateResult.size()];
    			final RadioButton radioBtnsUpdateOptions[][] = new RadioButton[allDesiredTuplesIntermediateResult.size()][3];			//for each attendance record hence row size = attendanceRecordsIntermediateResults Size() and 3=column size for Keep NEW, Keep OLD, Add to OLD
    			int attendanceRecordOfIntermediateResultsCnter=0;
    			
    			batchInsertUpdateModeConfirmations = new Boolean[allDesiredTuplesIntermediateResult.size()][2];
    			mergedNewAddedToOldAttendanceRecordToSendToServerBatchInsertUpdateMode = new ArrayList<AttendanceRecord>(allDesiredTuplesIntermediateResult.size());
    					
    			for(AttendanceRecord attendanceRecord:allDesiredTuplesIntermediateResult)
    			{
    				Log.d("AttendanceRecord", attendanceRecord.toString());
    				
    				//Old Attendance Record
    				TextView txtviewShowOldAttendanceRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    				txtviewShowOldAttendanceRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				txtviewShowOldAttendanceRecord.setText("\t\tOLD Only");
    				txtviewShowOldAttendanceRecord.append(System.getProperty("line.separator"));
    				txtviewShowOldAttendanceRecord.append("Attendance Date: "+Preset.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+System.getProperty("line.separator"));
    				txtviewShowOldAttendanceRecord.append("Lecture Start Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+System.getProperty("line.separator"));
    				txtviewShowOldAttendanceRecord.append("Lecture End Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+System.getProperty("line.separator"));
    				txtviewShowOldAttendanceRecord.append(attendanceRecord.toString());			//toString() only gives back presentees, absentees, medical leaves, other reasons, count of each
    				linearLayout.addView(txtviewShowOldAttendanceRecord, layoutParams1);
    						
    				View separatorBetweenOldNewMergedAttendanceRecords1 = new View(ChoosePresetsManipAttendanceActivity.this);
    				LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2);
    				layoutParams2.setMargins(0, 10, 0, 10);
    				separatorBetweenOldNewMergedAttendanceRecords1.setBackgroundColor(Color.WHITE);
    				linearLayout.addView(separatorBetweenOldNewMergedAttendanceRecords1, layoutParams2);
    				
    				//New Attendance Record
    				TextView txtviewShowNewAttendanceRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				txtviewShowNewAttendanceRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				txtviewShowNewAttendanceRecord.setText("\t\tNEW Only");
    				txtviewShowNewAttendanceRecord.append(System.getProperty("line.separator"));
    				cal.set(attendanceDatePicker.getYear(), attendanceDatePicker.getMonth(), attendanceDatePicker.getDayOfMonth());
    				txtviewShowNewAttendanceRecord.append("Attendance Date: "+Preset.userDisplayAttendanceDateFormat.format(cal.getTime())+System.getProperty("line.separator"));
    				txtviewShowNewAttendanceRecord.append("Lecture Start Time: "+Preset.timeUserDisplayFormat.format(selectedLectureStartTime)+System.getProperty("line.separator"));
    				txtviewShowNewAttendanceRecord.append("Lecture End Time: "+Preset.timeUserDisplayFormat.format(selectedLectureEndTime)+System.getProperty("line.separator"));
    				txtviewShowNewAttendanceRecord.append(chkBoxIsPresentees.isChecked()?"Presentees: ":"Absentees: ");
    				txtviewShowNewAttendanceRecord.append(chkBoxAllRollNums.isChecked()?"ALL":(editTextRollNums.getText().toString().length()==0?"None":Arrays.deepToString(enteredRollNums)));		//remove off starting and ending square bracket in toString() representations of Array and in between spaces
    				txtviewShowNewAttendanceRecord.append(System.getProperty("line.separator"));
					
    				txtviewShowNewAttendanceRecord.append("Medical Leave: ");
					if(editTextRollNumsMedicalLeave.getText().toString().trim().length()==0)
						txtviewShowNewAttendanceRecord.append("None");
					else
					{
						for(Map.Entry<Integer, String> entry:medicalReasons.entrySet())
						{
							txtviewShowNewAttendanceRecord.append(entry.getKey().toString());
							txtviewShowNewAttendanceRecord.append("(");
							txtviewShowNewAttendanceRecord.append(entry.getValue());
							txtviewShowNewAttendanceRecord.append("), ");
						}
						txtviewShowNewAttendanceRecord.setText(txtviewShowNewAttendanceRecord.getText().toString().substring(0, txtviewShowNewAttendanceRecord.getText().toString().length()-1));		//remove off last comma
						txtviewShowNewAttendanceRecord.append(System.getProperty("line.separator"));
					}
					txtviewShowNewAttendanceRecord.append(System.getProperty("line.separator"));
					
					txtviewShowNewAttendanceRecord.append("Medical Leave: ");
					if(editTextRollNumsOtherReasons.getText().toString().trim().length()==0)
						txtviewShowNewAttendanceRecord.append("None");
					else
					{
						for(Map.Entry<Integer, String> entry:otherReasons.entrySet())
						{
							txtviewShowNewAttendanceRecord.append(entry.getKey().toString());
							txtviewShowNewAttendanceRecord.append("(");
							txtviewShowNewAttendanceRecord.append(entry.getValue());
							txtviewShowNewAttendanceRecord.append("), ");
						}
						txtviewShowNewAttendanceRecord.setText(txtviewShowNewAttendanceRecord.getText().toString().substring(0, txtviewShowNewAttendanceRecord.getText().toString().length()-1));		//remove off last comma
					}
    				linearLayout.addView(txtviewShowNewAttendanceRecord, layoutParams1);
    				
    				View separatorBetweenOldNewMergedAttendanceRecords2 = new View(ChoosePresetsManipAttendanceActivity.this);
    				layoutParams2.setMargins(0, 10, 0, 10);
    				separatorBetweenOldNewMergedAttendanceRecords2.setBackgroundColor(Color.WHITE);
    				linearLayout.addView(separatorBetweenOldNewMergedAttendanceRecords2, layoutParams2);
    				
    				//Merge/ADD to OLD Results
    				TextView txtviewShowMergingAddingAttendanceRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				txtviewShowMergingAddingAttendanceRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				txtviewShowMergingAddingAttendanceRecord.setText("\t\tADD to OLD");
    				txtviewShowMergingAddingAttendanceRecord.append(System.getProperty("line.separator"));
    				txtviewShowMergingAddingAttendanceRecord.append("Attendance Date: "+Preset.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+System.getProperty("line.separator"));
    				txtviewShowMergingAddingAttendanceRecord.append("Lecture Start Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+System.getProperty("line.separator"));
    				txtviewShowMergingAddingAttendanceRecord.append("Lecture End Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+System.getProperty("line.separator"));
    				//actual merging below
    				LinkedHashMap<Integer, String> mergedNewAddedToOldAttendance = attendanceRecord.getAttendance();			//1st take whole old attendance record and then overwrite on it the new entries
    				if(chkBoxAllRollNums.isChecked())
    				{
    					for(int i=1;i<=retrievedNumStudents;i++)
    						mergedNewAddedToOldAttendance.put(i, chkBoxIsPresentees.isChecked()?"P":"A");
    				}else
    				{
    					if(editTextRollNums.getText().toString().trim().length()>0)
    						for(int i=0;i<enteredRollNums.length;i++)
    							mergedNewAddedToOldAttendance.put(Integer.parseInt(enteredRollNums[i]), chkBoxIsPresentees.isChecked()?"P":"A");
    					
    					if(editTextRollNumsMedicalLeave.getText().toString().trim().length()>0)
    					{
    						if(medicalReasons.size()==0)				//for that condition where no reason specified i.e reasons entry button not clicked
    						{
    							for(String rn:editTextRollNumsMedicalLeave.getText().toString().trim().split("[,]"))
    							{
    								medicalReasons.put(Integer.parseInt(rn), "M(None Specified)");
    								mergedNewAddedToOldAttendance.put(Integer.parseInt(rn), "M(None Specified)");
    							}
    						}else
    						{
    							for(Map.Entry<Integer, String> entry:medicalReasons.entrySet())
    								mergedNewAddedToOldAttendance.put(entry.getKey(), "M("+entry.getValue()+")");
    						}
    					}
    					
    					if(editTextRollNumsOtherReasons.getText().toString().trim().length()>0)
    					{
    						if(otherReasons.size()==0)
    						{
    							for(String rn:editTextRollNumsOtherReasons.getText().toString().trim().split("[,]"))
    							{
    								otherReasons.put(Integer.parseInt(rn), "O(None Specified)");
    								mergedNewAddedToOldAttendance.put(Integer.parseInt(rn), "O(None Specified)");
    							}
    						}else
    						{
    							for(Map.Entry<Integer, String> entry:otherReasons.entrySet())
    								mergedNewAddedToOldAttendance.put(entry.getKey(), "O("+entry.getValue()+")");
    						}
    					}
    				}
    				AttendanceRecord mergedNewAddedToOldAttendanceRecord = new AttendanceRecord(attendanceRecord.getAttendanceDate(), attendanceRecord.getLectureStartTime(), attendanceRecord.getLectureEndTime());
    				for(Map.Entry<Integer, String> entry:mergedNewAddedToOldAttendance.entrySet())
    					mergedNewAddedToOldAttendanceRecord.addAttendanceOfNewRollNum(entry.getKey(), entry.getValue());
    				txtviewShowMergingAddingAttendanceRecord.append(mergedNewAddedToOldAttendanceRecord.toString());
    				linearLayout.addView(txtviewShowMergingAddingAttendanceRecord, layoutParams1);
    				
    				mergedNewAddedToOldAttendanceRecordToSendToServerBatchInsertUpdateMode.add(mergedNewAddedToOldAttendanceRecord);
    						
    				View separatorAttendanceRecords1 = new View(ChoosePresetsManipAttendanceActivity.this);
    				LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 4);
    				layoutParams2.setMargins(0, 10, 0, 10);
    				separatorAttendanceRecords1.setBackgroundColor(Color.CYAN);
    				linearLayout.addView(separatorAttendanceRecords1, layoutParams3);
    				
    				TextView txtviewInstr = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				txtviewInstr.setText("Choose Update Option for ABOVE Attedance Record");
    				linearLayout.addView(txtviewInstr, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    				
    				radioGroupForUpdateOptions[attendanceRecordOfIntermediateResultsCnter] = new RadioGroup(ChoosePresetsManipAttendanceActivity.this);
    				radioGroupForUpdateOptions[attendanceRecordOfIntermediateResultsCnter].setOrientation(RadioGroup.VERTICAL);
    				LinearLayout.LayoutParams layoutParamsForRadioButtons = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    				radioBtnsUpdateOptions[attendanceRecordOfIntermediateResultsCnter][0] = new RadioButton(ChoosePresetsManipAttendanceActivity.this);
    				radioBtnsUpdateOptions[attendanceRecordOfIntermediateResultsCnter][0].setText("NEW Only");
    				radioBtnsUpdateOptions[attendanceRecordOfIntermediateResultsCnter][1] = new RadioButton(ChoosePresetsManipAttendanceActivity.this);
    				radioBtnsUpdateOptions[attendanceRecordOfIntermediateResultsCnter][1].setText("OLD Only");
    				radioBtnsUpdateOptions[attendanceRecordOfIntermediateResultsCnter][2] = new RadioButton(ChoosePresetsManipAttendanceActivity.this);
    				radioBtnsUpdateOptions[attendanceRecordOfIntermediateResultsCnter][2].setText("ADD to OLD");
    				for(int i=0;i<3;i++)
    					radioGroupForUpdateOptions[attendanceRecordOfIntermediateResultsCnter].addView(radioBtnsUpdateOptions[attendanceRecordOfIntermediateResultsCnter][i], layoutParamsForRadioButtons);
    				radioGroupForUpdateOptions[attendanceRecordOfIntermediateResultsCnter].check(radioBtnsUpdateOptions[attendanceRecordOfIntermediateResultsCnter][2].getId());			//default checked is "add to old" for all attendance records			    				
    				linearLayout.addView(radioGroupForUpdateOptions[attendanceRecordOfIntermediateResultsCnter], new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    				
    				attendanceRecordOfIntermediateResultsCnter++;
    				
    				View separatorAttendanceRecords2 = new View(ChoosePresetsManipAttendanceActivity.this);
    				layoutParams2.setMargins(0, 10, 0, 10);
    				separatorAttendanceRecords2.setBackgroundColor(Color.CYAN);
    				linearLayout.addView(separatorAttendanceRecords2, layoutParams3);
    			}
    			
    			LinearLayout.LayoutParams layoutParamsForUpdateOptionsForAllAttendanceRecords = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    			Button btnNEWOnlyUpdateOptionForAllAttendanceRecords = new Button(ChoosePresetsManipAttendanceActivity.this);
    			btnNEWOnlyUpdateOptionForAllAttendanceRecords.setText("NEW Only For ALL");
    			Button btnOLDOnlyUpdateOptionForAllAttendanceRecords = new Button(ChoosePresetsManipAttendanceActivity.this);
    			btnOLDOnlyUpdateOptionForAllAttendanceRecords.setText("OLD Only For ALL");
    			Button btnADDToOLDUpdateOptionForAllAttendanceRecords = new Button(ChoosePresetsManipAttendanceActivity.this);
    			btnADDToOLDUpdateOptionForAllAttendanceRecords.setText("ADD to OLD For ALL");
    			linearLayout.addView(btnNEWOnlyUpdateOptionForAllAttendanceRecords, layoutParamsForUpdateOptionsForAllAttendanceRecords);
    			linearLayout.addView(btnOLDOnlyUpdateOptionForAllAttendanceRecords, layoutParamsForUpdateOptionsForAllAttendanceRecords);
    			linearLayout.addView(btnADDToOLDUpdateOptionForAllAttendanceRecords, layoutParamsForUpdateOptionsForAllAttendanceRecords);
    			
    			scrollView.addView(linearLayout);
    			builder.setView(scrollView);
    			
    			builder.setPositiveButton("Do as Selected", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						for(int i=0;i<batchInsertUpdateModeConfirmations.length;i++)
						{
							if(radioBtnsUpdateOptions[i][0].isChecked())				//NEW Only for that attendance Record
							{
								batchInsertUpdateModeConfirmations[i][0] = false;
								batchInsertUpdateModeConfirmations[i][1] = true;
							}else if(radioBtnsUpdateOptions[i][1].isChecked())				//OLD Only for that attendance Record
							{
								batchInsertUpdateModeConfirmations[i][0] = true;
								batchInsertUpdateModeConfirmations[i][1] = false;
							}else if(radioBtnsUpdateOptions[i][2].isChecked())				//Merging/ADDing NEW to OLD for that attendance Record
							{
								batchInsertUpdateModeConfirmations[i][0] = true;
								batchInsertUpdateModeConfirmations[i][1] = true;
							}
						}
						dialog.cancel();
						progressDialog.setMessage("Please wait.");
						progressDialog.show();
						canSendConfirmation = true;
					}
				});
				builder.setCancelable(false);
				final AlertDialog alertDialogGetUpdateOptions = builder.create();
    			
    			btnNEWOnlyUpdateOptionForAllAttendanceRecords.setOnClickListener(new View.OnClickListener()
    			{
					@Override
					public void onClick(View v)
					{
						for(int i=0;i<batchInsertUpdateModeConfirmations.length;i++)
						{
							batchInsertUpdateModeConfirmations[i][0] = false;					//discard old
							batchInsertUpdateModeConfirmations[i][1] = true;					//keep new
						}
						alertDialogGetUpdateOptions.dismiss();
						progressDialog.setMessage("Please wait..");
						progressDialog.show();
						canSendConfirmation = true;
					}
				});
    			btnOLDOnlyUpdateOptionForAllAttendanceRecords.setOnClickListener(new View.OnClickListener()
    			{
					@Override
					public void onClick(View v)
					{
						for(int i=0;i<batchInsertUpdateModeConfirmations.length;i++)
						{
							batchInsertUpdateModeConfirmations[i][0] = true;					//keep old
							batchInsertUpdateModeConfirmations[i][1] = false;					//discard new
						}
						alertDialogGetUpdateOptions.dismiss();
						progressDialog.setMessage("Please wait..");
						progressDialog.show();
						canSendConfirmation = true;
					}
				});
    			btnADDToOLDUpdateOptionForAllAttendanceRecords.setOnClickListener(new View.OnClickListener()
    			{
					@Override
					public void onClick(View v)
					{
						for(int i=0;i<batchInsertUpdateModeConfirmations.length;i++)
						{
							//Merging/Adding NEW to OLD
							batchInsertUpdateModeConfirmations[i][0] = true;					//keep old
							batchInsertUpdateModeConfirmations[i][1] = true;					//keep new
						}
						alertDialogGetUpdateOptions.dismiss();
						progressDialog.setMessage("Please wait..");
						progressDialog.show();
						canSendConfirmation = true;
					}
				});
				alertDialogGetUpdateOptions.show();
			}else
				progressDialog.setMessage(progressParams[0]);
	    }

		@Override
	    protected void onPostExecute(Exception exceptionOccured)
		{
			if(chosenMode.equals("SINGLE View") || chosenMode.equals("BATCH View") || chosenMode.equals("SINGLE Delete")|| chosenMode.equals("BATCH Delete")|| chosenMode.equals("SINGLE Insert/Update")|| chosenMode.equals("BATCH Insert/Update"))
			{
				if(progressDialog!=null)
					publishProgress("Saving Attendance String as a History Record ..");
	    		try
	    		{
					saveToHistoryRecords(chkBoxOperatingOnBehalf.isChecked(), attendanceStringToSend, attendanceStringOperationSuccess);
					if(progressDialog!=null)
		    			publishProgress("Saved Attendance String as History Record!");
				}catch (Exception e)
				{
					e.printStackTrace();
					if(exceptionOccured!=null)			//if already an exception has occured due to other operations so give that preference over just saving exception of history records
						exceptionOccured = e;
				}
			}
			
			if(progressDialog!=null)
				progressDialog.dismiss();
			
			if(exceptionOccured==null)
			{
				if(attendanceStringOperationSuccess)
					Toast.makeText(getBaseContext(), "Successful attendance operation!", Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(getBaseContext(), userCanceledOperation?"User canceled Operation":"There was an error in attendance modification .. please retry", Toast.LENGTH_SHORT).show();
			}else
				Toast.makeText(getBaseContext(), "Exception occured while attendance operation .. please retry.\nException Details: "+((exceptionOccured.getMessage()==null || exceptionOccured.getMessage().equals(""))?"No error Description Found":exceptionOccured.getMessage()), Toast.LENGTH_LONG).show();
		}
	}
	class bgTaskSubmitTriggerOperationString extends AsyncTask<Void, String, Exception>
	{
		ProgressDialog progressDialog = null;
		String triggerOperationStringToSend;
		boolean triggerOperationSuccess = false, userCanceledOperation = false;
		ArrayList<edu.vesit.ams.FutureTriggersRecord> allDesiredTuplesIntermediateResult = null;				//final not intermediate in case of viewing modes(SINGLE and BATCH)
		
		boolean canSendConfirmation = false;
		
		boolean triggerSingleDeleteModeConfirmation = true;	//true if user confirms deletion else fase
		Boolean triggerBatchDeleteModeConfirmations[];			//array of boolean for arraylist of Trigger Records, 1 boolean confirmation(true if user confirms deletion) per trigger Record
		
		Boolean triggerSingleInsertUpdateModeConfirmation[] = { false, true };
		// 1 Trigger Record = presetTableName + mergedNewAddedToOldAttendanceRecordToSendToServer for SINGLE Insert/Update Mode and Update Option = ADD to OLD
		FutureTriggersRecord mergedNewAddedToOldTriggerRecordToSendToServer;
		/*	Values semantics/interpretation
		 * 	1st boolean(true to keep old value)		2nd boolean(true to keep new value)			meaning/interpretation at server
		 * 	true									false									=> keep old values(no update) OR user canceled operation(i.e no DB modifications = old values kept as it is)
		 *  false									true									=> keep new values by(overwriting old values or new attendance record insertion)
		 *  true									true									=> merge/add, overwrite new values over old values and keep those old value i.e not present in new values i.e add new values
		 *  false									false									=> none
		 *  
		 *  			SAME for batchInsertUpdateModeConfirmation but its a 2D array as rows represent confirmations for each attendance Record
		 *  
		 * */
		Boolean triggerBatchInsertUpdateModeConfirmations[][];
		// 1 Trigger Record = 1 entry in LinkedHashMap = Key(String->presetTableName) and Value = (AttendanceRecord) for BATCH Insert/Update Mode and Update Option = ADD to OLD
		ArrayList<FutureTriggersRecord> mergedNewAddedToOldFutureTriggerRecordsToSendToServerTriggerBatchInsertUpdateMode;
	    	       				
	    @Override
	    protected void onPreExecute()
	    {
	    	progressDialog = new ProgressDialog(ChoosePresetsManipAttendanceActivity.this);
	    	progressDialog.setTitle("Processing ..");
	    	progressDialog.setMessage("Please wait.");
	    	progressDialog.setCancelable(false);
	    	progressDialog.setIndeterminate(true);
	    	progressDialog.show();
	    }
		@SuppressWarnings("unchecked")
		@Override
		protected Exception doInBackground(Void... params)
		{
			//save start, end times, Send to server and do operations wrt modes
	    	try
	    	{
	    		if(isChosenStartTimeCustom)
	    		{
	    			//get final start time if startTime is custom					=>						NO NEED OF THIS AS ITS ALREADY DONE WHILE SHOWING REVIEW TO USER
    	    		//selectedLectureStartTime = isChosenStartTimeCustom?(Preset.timeUserDisplayFormat.parse(lectureStartTimePicker.getCurrentHour().toString()+":"+lectureStartTimePicker.getCurrentMinute()+(lectureStartTimePicker.getCurrentHour()<12?" AM":" PM"))):selectedLectureStartTime;
	    			
	    			publishProgress("Saving new start time"+selectedLectureStartTime+" for future reference..");
	    			if(selectedPresetStartTimesAdapter.getPosition(Preset.timeUserDisplayFormat.format(selectedLectureStartTime))>=0)
	    				publishProgress("Already Saved custom Start Time!");
	    			else
	    			{
	    				saveLectureTime(true);
	    				publishProgress("Saved custom Start Time!");
	    			}
	    		}
	    		if(isChosenEndTimeCustom)
	    		{
	    			//get final end time if endTime is custom					=>						NO NEED OF THIS AS ITS ALREADY DONE WHILE SHOWING REVIEW TO USER
	    			//selectedLectureEndTime = isChosenEndTimeCustom?(Preset.timeUserDisplayFormat.parse((lectureEndTimePicker.getCurrentHour()).toString()+":"+lectureEndTimePicker.getCurrentMinute()+(lectureEndTimePicker.getCurrentHour()<12?" AM":" PM"))):selectedLectureEndTime;
	    			
	    			publishProgress("Saving new end time "+selectedLectureEndTime+"for future reference..");
	    			if(selectedPresetStartTimesAdapter.getPosition(Preset.timeUserDisplayFormat.format(selectedLectureEndTime))>=0)
	    				publishProgress("Already Saved custom End Time!");
	    			else
	    			{
	    				saveLectureTime(false);
	    				publishProgress("Saved custom End Time!");
	    			}
	    		}
	    		publishProgress("Formulating Trigger Operation String ..");
	    		triggerOperationStringToSend = formulateAttendanceStringOrTriggerOperationString(chosenMode);
	    		publishProgress("Formulated Trigger Operation String!");
	    		
	    		LoginActivity.toServerOOStrm.writeObject(triggerOperationStringToSend);
	    		LoginActivity.toServerOOStrm.flush();
	    		
	    		Object obj = LoginActivity.fromServerOIS.readObject();							//read the LinkedHashMap object
	    		
	    		if(obj==null)
	    			throw new Exception("No triggers data received from server due to error or server being offline.");
	    		else if(obj instanceof Exception)
	    			throw (Exception)obj;
	    		
	    		allDesiredTuplesIntermediateResult = (ArrayList<edu.vesit.ams.FutureTriggersRecord>) obj;
	    		
	    		//Log.d("IntermediateResult", allDesiredTuplesIntermediateResult.toString());
	    		//Log.d("IntermediateResult Length", Integer.toString(allDesiredTuplesIntermediateResult.size()));
	    		
	    		if(chosenMode.equals("TRIGGER SINGLE View") || chosenMode.equals("TRIGGER BATCH View"))
	    		{
	    			publishProgress("Command=SHOW FINAL RESULTS; Mode=TRIGGER View");
	    			triggerOperationSuccess = true;
	    		}else if(chosenMode.equals("TRIGGER SINGLE Delete"))
	    		{
	    			publishProgress("Command=SHOW INTERMEDIATE RESULTS; Mode=TRIGGER SINGLE Delete");
	    			
	    			//loop infinitely until canSendConfirmation is set from AlertDialog because confirmation's default set value(true) is auto-sent without user's confirmation from AlertDialog
	    			while(!canSendConfirmation);
	    			
	    			LoginActivity.toServerOOStrm.writeObject(Boolean.valueOf(triggerSingleDeleteModeConfirmation));
	    			LoginActivity.toServerOOStrm.flush();
	    			
	    			if(triggerSingleDeleteModeConfirmation)
	    			{
    	    			Object o = LoginActivity.fromServerOIS.readObject();
    	    			if(o instanceof Exception)
    	    				throw (Exception)o;
    	    			else
    	    				triggerOperationSuccess = ((String)o).equals("Deletion Success!");
	    			}else
	    				userCanceledOperation = true;
	    			
	    		}else if(chosenMode.equals("TRIGGER BATCH Delete"))
	    		{
	    			publishProgress("Command=SHOW INTERMEDIATE RESULTS; Mode=TRIGGER BATCH Delete");
	    			
	    			//loop infinitely until canSendConfirmation is set from AlertDialog because confirmation's default set value is auto-sent without user's confirmation from AlertDialog
	    			while(!canSendConfirmation);
	    			
	    			LoginActivity.toServerOOStrm.writeObject(triggerBatchDeleteModeConfirmations);
	    			LoginActivity.toServerOOStrm.flush();
	    			
	    			if(triggerBatchDeleteModeConfirmations==null)
	    				userCanceledOperation = true;								//will be true if batchDeleteModeConfirmations has all false i.e dont delete for all trigger records or is NULL
	    			else
	    			{
	    				userCanceledOperation = true;								//assigning true because in below for loop if any1 is true in batchDeleteModeConfirmations then at that index Attendance Record must be deleted => userCanceledOperation = false; if all are false then if condition will never be true => userCanceledOperation remains true(its assigned here) 
    	    			for(int i=0;i<triggerBatchDeleteModeConfirmations.length;i++)
    	    				if(triggerBatchDeleteModeConfirmations[i])
    	    				{
    	    					userCanceledOperation = false;
    	    					break;
    	    				}
	    			}
	    			
	    			if(!userCanceledOperation)
	    			{
	    				Object o = LoginActivity.fromServerOIS.readObject();
    	    			if(o instanceof Exception)
    	    				throw (Exception)o;
    	    			else
    	    				triggerOperationSuccess = ((String)o).equals("Deletion Success!");
	    			}
	    		}else if(chosenMode.equals("TRIGGER SINGLE Insert/Update"))
	    		{
	    			if(allDesiredTuplesIntermediateResult.size()==0)				//No already existing attendance record for that date, lectureStartIme and EndTime so its a insert operation of new attendance Record
	    			{
	    				triggerSingleInsertUpdateModeConfirmation[0] = false;			//new record insertion confirmation indicators(similar to keep new, discard old)
	    				triggerSingleInsertUpdateModeConfirmation[1] = true;
	    				
	    				LoginActivity.toServerOOStrm.writeObject(triggerSingleInsertUpdateModeConfirmation);
	    				LoginActivity.toServerOOStrm.flush();
	    				
	    				Object o = LoginActivity.fromServerOIS.readObject();
    	    			if(o instanceof Exception)
    	    				throw (Exception)o;
    	    			else
    	    				triggerOperationSuccess = ((String)o).equals("Insertion/Updation Success!");
	    			}else
	    			{
	    				publishProgress("Command=SHOW INTERMEDIATE RESULTS; Mode=TRIGGER SINGLE Insert/Update");
	    				
    	    			//loop infinitely until canSendConfirmation is set from AlertDialog because confirmation's default set value is auto-sent without user's confirmation from AlertDialog
    	    			while(!canSendConfirmation);
    	    			
    	    			LoginActivity.toServerOOStrm.writeObject(triggerSingleInsertUpdateModeConfirmation);
	    				LoginActivity.toServerOOStrm.flush();
	    				
	    				//Leaving aside user canceled operation(Keep OLD Only) condition we would get some insertion/updation success indicator from server
	    				if(!triggerSingleInsertUpdateModeConfirmation[0] && triggerSingleInsertUpdateModeConfirmation[1])		//Keep NEW Only
	    				{
	    	    			Object o = LoginActivity.fromServerOIS.readObject();
		    	    		if(o instanceof Exception)
		    	    			throw (Exception)o;
		    	    		else
		    	    			triggerOperationSuccess = ((String)o).equals("Insertion/Updation Success!");
	    				}else if(triggerSingleInsertUpdateModeConfirmation[0] && triggerSingleInsertUpdateModeConfirmation[1])		//Merging/Adding
	    				{
	    					LoginActivity.toServerOOStrm.writeObject(mergedNewAddedToOldTriggerRecordToSendToServer);
    	    				LoginActivity.toServerOOStrm.flush();
    	    				
	    	    			Object o = LoginActivity.fromServerOIS.readObject();
		    	    		if(o instanceof Exception)
		    	    			throw (Exception)o;
		    	    		else
		    	    			triggerOperationSuccess = ((String)o).equals("Insertion/Updation Success!");
	    				}else if(triggerSingleInsertUpdateModeConfirmation[0] && !triggerSingleInsertUpdateModeConfirmation[1])		//Keep OLD Only = User canceled insert/update operation of single attendance record
	    					userCanceledOperation = true;
	    			}
	    		}else if(chosenMode.equals("TRIGGER BATCH Insert/Update"))
	    		{
	    			if(allDesiredTuplesIntermediateResult.size()==0)				//No already existing trigger records for that preset, date range, lectureStartIme and EndTime so its a insert operation of all new trigger Records for that range
	    			{
	    				publishProgress("Please wait ..");
	    				Thread.sleep(5000);									//Make AsyncTask Thread sleep for 5secs(delay) so that AMSServer can do insertions of all new records into DB(AMSServer needs sometime for that  so putting a delay here , if not added we would be waiting for result confirmation and a socket read time out would occur!!)
	    				
	    				Object o = LoginActivity.fromServerOIS.readObject();
    	    			if(o instanceof Exception)
    	    				throw (Exception)o;
    	    			else
    	    				triggerOperationSuccess = ((String)o).equals("Batch Insertion/Updation Success!");
	    			}else
	    			{
	    				publishProgress("Command=SHOW INTERMEDIATE RESULTS; Mode=TRIGGER BATCH Insert/Update");
	    				
    	    			//loop infinitely until canSendConfirmation is set from AlertDialog because confirmation's default set value is auto-sent without user's confirmation from AlertDialog
    	    			while(!canSendConfirmation);
    	    			
    	    			LoginActivity.toServerOOStrm.writeObject(triggerBatchInsertUpdateModeConfirmations);
	    				LoginActivity.toServerOOStrm.flush();
	    				
	    				LoginActivity.toServerOOStrm.writeObject(mergedNewAddedToOldFutureTriggerRecordsToSendToServerTriggerBatchInsertUpdateMode);
	    				LoginActivity.toServerOOStrm.flush();
	    				
	    				   	    		
	    	    		//do not distinguish between userCanceledOperation or not while selecting Update Options as for user cancelling the operation, even then we need confirmation for insertion of non-existing attendance records(For Update Option = Keep OLD Only For ALL = User canceled insert/update operation => Dont show user canceled operation toast because there can be insertion of non-existing attendance records)
	    	    		
	    	    		Object o = LoginActivity.fromServerOIS.readObject();
    	    			if(o instanceof Exception)
    	    				throw (Exception)o;
    	    			else
    	    				triggerOperationSuccess = ((String)o).equals("Batch Insertion/Updation Success!");
	    			}
	    		}
	    	}catch (Exception e) 
	    	{
	    		Log.e("Exception", e.getMessage()==null?"null":e.getMessage());
	    		e.printStackTrace();
	    		return e;
	    	}
			return null;
		}
		@Override
	    protected void onProgressUpdate(String ...progressParams)
	    {
			if(progressParams[0].equals("Command=SHOW FINAL RESULTS; Mode=TRIGGER View"))
			{
				if(progressDialog!=null)
    				progressDialog.dismiss();
				AlertDialog.Builder builder = new AlertDialog.Builder(ChoosePresetsManipAttendanceActivity.this);
    			builder.setTitle("View Results(Total Records: "+allDesiredTuplesIntermediateResult.size()+")");
    			builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
    			{
    				@Override
    				public void onClick(DialogInterface dialog, int which)
    				{
    					dialog.dismiss();
    				}
    			});
    			
    			if(allDesiredTuplesIntermediateResult.size()==0)
    				builder.setMessage("No Trigger Records found matching your request.");
    			else
    			{	
	    			LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    			ScrollView scrollView = (ScrollView)inflater.inflate(R.layout.medical_other_reasons__other_input_dialogs_scrollview, null);
	    			
	    			LinearLayout linearLayout = new LinearLayout(ChoosePresetsManipAttendanceActivity.this);
	    			linearLayout.setOrientation(LinearLayout.VERTICAL);
	    					
	    			for(edu.vesit.ams.FutureTriggersRecord triggerRecord:allDesiredTuplesIntermediateResult)
	    			{	
	    				String presetTableName = triggerRecord.getPresetTableName();
	    				AttendanceRecord attendanceRecord = triggerRecord.getAttendanceRecord();
	    				
	    				TextView txtviewShowTriggerRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
	    				LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
	    				txtviewShowTriggerRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
	    				txtviewShowTriggerRecord.setText("Chosen Preset: "+presetTableName+System.getProperty("line.separator"));
	    				txtviewShowTriggerRecord.append("Attendance Date: "+Preset.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+System.getProperty("line.separator"));
	    				txtviewShowTriggerRecord.append("Lecture Start Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+System.getProperty("line.separator"));
	    				txtviewShowTriggerRecord.append("Lecture End Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+System.getProperty("line.separator"));
	    				txtviewShowTriggerRecord.append(attendanceRecord.toString());			//toString() only gives back presentees, absentees, medical leaves, other reasons, count of each
	    				linearLayout.addView(txtviewShowTriggerRecord, layoutParams1);
	    						
	    				View separatorBetweenTriggerRecords = new View(ChoosePresetsManipAttendanceActivity.this);
	    				LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 4);
	    				layoutParams2.setMargins(0, 10, 0, 10);
	    				separatorBetweenTriggerRecords.setBackgroundColor(Color.WHITE);
	    				linearLayout.addView(separatorBetweenTriggerRecords, layoutParams2);
	    			}
	    					
	    			scrollView.addView(linearLayout);
	    			builder.setView(scrollView);
    			}
    			builder.create().show();
			}else if(progressParams[0].equals("Command=SHOW INTERMEDIATE RESULTS; Mode=TRIGGER SINGLE Delete"))
			{
				if(progressDialog!=null)
    				progressDialog.dismiss();
				AlertDialog.Builder builder = new AlertDialog.Builder(ChoosePresetsManipAttendanceActivity.this);
    			builder.setTitle("View Results(Total Records: "+allDesiredTuplesIntermediateResult.size()+")");
    			
    			if(allDesiredTuplesIntermediateResult.size()==0)
    			{
    				builder.setMessage("No Trigger Records found matching your request.");
    				builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
    				{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							triggerSingleDeleteModeConfirmation = false;
							dialog.dismiss();
							canSendConfirmation = true;
						}
					});
    			}else
    			{
    				builder.setPositiveButton("Delete", new DialogInterface.OnClickListener()
	    			{
	    				@Override
	    				public void onClick(DialogInterface dialog, int which)
	    				{
	    					triggerSingleDeleteModeConfirmation = true;
	    					dialog.dismiss();
	    					if(progressDialog!=null)
	    					{
	    						progressDialog.setMessage("Please wait.");
	    						progressDialog.show();
	    					}
	    					canSendConfirmation = true;
	    				}
	    			});
	    			builder.setNegativeButton("Dont Delete", new DialogInterface.OnClickListener()
	    			{
	    				@Override
	    				public void onClick(DialogInterface dialog, int which)
	    				{
	    					triggerSingleDeleteModeConfirmation = false;
	    					dialog.dismiss();
	    					canSendConfirmation = true;
	    				}
	    			});
    				
	    			LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    			ScrollView scrollView = (ScrollView)inflater.inflate(R.layout.medical_other_reasons__other_input_dialogs_scrollview, null);
	    			
	    			LinearLayout linearLayout = new LinearLayout(ChoosePresetsManipAttendanceActivity.this);
	    			linearLayout.setOrientation(LinearLayout.VERTICAL);
	    					
	    			for(FutureTriggersRecord futureTriggerRecord:allDesiredTuplesIntermediateResult)
	    			{
	    				String presetTableName = futureTriggerRecord.getPresetTableName();
	    				AttendanceRecord attendanceRecord = futureTriggerRecord.getAttendanceRecord();
	    				
	    				TextView txtviewShowTriggerRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
	    				LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
	    				txtviewShowTriggerRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
	    				txtviewShowTriggerRecord.setText("Chosen Preset: "+presetTableName+System.getProperty("line.separator"));
	    				txtviewShowTriggerRecord.append("Attendance Date: "+Preset.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+System.getProperty("line.separator"));
	    				txtviewShowTriggerRecord.append("Lecture Start Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+System.getProperty("line.separator"));
	    				txtviewShowTriggerRecord.append("Lecture End Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+System.getProperty("line.separator"));
	    				txtviewShowTriggerRecord.append(attendanceRecord.toString());			//toString() only gives back presentees, absentees, medical leaves, other reasons, count of each
	    				linearLayout.addView(txtviewShowTriggerRecord, layoutParams1);
	    						
	    				View separatorBetweenTriggerRecords = new View(ChoosePresetsManipAttendanceActivity.this);
	    				LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 4);
	    				layoutParams2.setMargins(0, 10, 0, 10);
	    				separatorBetweenTriggerRecords.setBackgroundColor(Color.WHITE);
	    				linearLayout.addView(separatorBetweenTriggerRecords, layoutParams2);
	    			}
	    					
	    			scrollView.addView(linearLayout);
	    			builder.setView(scrollView);
    			}
    			builder.setCancelable(false);
    			builder.create().show();
			}else if(progressParams[0].equals("Command=SHOW INTERMEDIATE RESULTS; Mode=TRIGGER BATCH Delete"))
			{
				if(progressDialog!=null)
    				progressDialog.dismiss();
				AlertDialog.Builder builder = new AlertDialog.Builder(ChoosePresetsManipAttendanceActivity.this);
    			builder.setTitle("View Results(Total Records: "+allDesiredTuplesIntermediateResult.size()+")");
    			
    			if(allDesiredTuplesIntermediateResult.size()==0)
    			{
    				builder.setMessage("No Trigger Records found matching your request.");
    				builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
    				{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							triggerBatchDeleteModeConfirmations = null;					//[null OR all boolean values are false] means user canceled operation
							dialog.dismiss();
							canSendConfirmation = true;
						}
					});
    			}else
    			{
    				triggerBatchDeleteModeConfirmations = new Boolean[allDesiredTuplesIntermediateResult.size()];
    				for(int i=0;i<triggerBatchDeleteModeConfirmations.length;i++)
    					triggerBatchDeleteModeConfirmations[i] = Boolean.valueOf(false);
    				final CheckBox[] chkBoxToDeleteConfirmation = new CheckBox[allDesiredTuplesIntermediateResult.size()];			//to refer later for confirmation array formation
    				
	    			LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    			ScrollView scrollView = (ScrollView)inflater.inflate(R.layout.medical_other_reasons__other_input_dialogs_scrollview, null);
	    			
	    			LinearLayout wrappingLinearLayout = new LinearLayout(ChoosePresetsManipAttendanceActivity.this);
	    			wrappingLinearLayout.setOrientation(LinearLayout.VERTICAL);
	    			
	    			int i = 0;
	    					
	    			for(FutureTriggersRecord futureTriggersRecord:allDesiredTuplesIntermediateResult)
	    			{
	    				String presetTableName = futureTriggersRecord.getPresetTableName();
	    				AttendanceRecord attendanceRecord = futureTriggersRecord.getAttendanceRecord();
	    				
	    				LinearLayout linearLayout = new LinearLayout(ChoosePresetsManipAttendanceActivity.this);
	    				linearLayout.setOrientation(LinearLayout.HORIZONTAL);
	    				linearLayout.setWeightSum(1.0F);
	    				
	    				TextView txtviewShowTriggerRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
	    				LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9F);
	    				txtviewShowTriggerRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
	    				txtviewShowTriggerRecord.setText("Chosen Preset: "+presetTableName+System.getProperty("line.separator"));
	    				txtviewShowTriggerRecord.append("Attendance Date: "+Preset.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+System.getProperty("line.separator"));
	    				txtviewShowTriggerRecord.append("Lecture Start Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+System.getProperty("line.separator"));
	    				txtviewShowTriggerRecord.append("Lecture End Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+System.getProperty("line.separator"));
	    				txtviewShowTriggerRecord.append(attendanceRecord.toString());			//toString() only gives back presentees, absentees, medical leaves, other reasons, count of each
	    				linearLayout.addView(txtviewShowTriggerRecord, layoutParams1);
	    				
	    				chkBoxToDeleteConfirmation[i] = new CheckBox(ChoosePresetsManipAttendanceActivity.this);
	    				LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.1F);
	    				chkBoxToDeleteConfirmation[i].setChecked(true);
	    				linearLayout.addView(chkBoxToDeleteConfirmation[i], layoutParams2);
	    				
	    				wrappingLinearLayout.addView(linearLayout);
	    						
	    				View separatorBetweenTriggerRecords = new View(ChoosePresetsManipAttendanceActivity.this);
	    				LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 4);
	    				layoutParams2.setMargins(0, 10, 0, 10);
	    				separatorBetweenTriggerRecords.setBackgroundColor(Color.WHITE);
	    				wrappingLinearLayout.addView(separatorBetweenTriggerRecords, layoutParams3);
	    				
	    				i++;
	    			}
	    					
	    			scrollView.addView(wrappingLinearLayout);
	    			builder.setView(scrollView);
	    			
    				builder.setPositiveButton("Delete Selected", new DialogInterface.OnClickListener()
	    			{
	    				@Override
	    				public void onClick(DialogInterface dialog, int which)
	    				{
	    					for(int i=0;i<triggerBatchDeleteModeConfirmations.length;i++)
	    						triggerBatchDeleteModeConfirmations[i] = Boolean.valueOf(chkBoxToDeleteConfirmation[i].isChecked());
	    						
	    					dialog.dismiss();
	    					if(progressDialog!=null)
	    					{
	    						progressDialog.setMessage("Please wait.");
	    						progressDialog.show();
	    					}
	    					canSendConfirmation = true;
	    				}
	    			});
	    			builder.setNegativeButton("Dont Delete All", new DialogInterface.OnClickListener()
	    			{
	    				@Override
	    				public void onClick(DialogInterface dialog, int which)
	    				{
	    					for(int i=0;i<triggerBatchDeleteModeConfirmations.length;i++)
	    						triggerBatchDeleteModeConfirmations[i] = Boolean.valueOf(false);						//[null OR all boolean values are false] means user canceled operation
	    					dialog.dismiss();
	    					canSendConfirmation = true;
	    				}
	    			});
	    			builder.setNeutralButton("Delete All", new DialogInterface.OnClickListener()
	    			{	
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
	    					for(int i=0;i<triggerBatchDeleteModeConfirmations.length;i++)
	    						triggerBatchDeleteModeConfirmations[i] = Boolean.valueOf(true);
	    						
	    					dialog.dismiss();
	    					if(progressDialog!=null)
	    					{
	    						progressDialog.setMessage("Please wait.");
	    						progressDialog.show();
	    					}
	    					canSendConfirmation = true;
						}
					});
    			}
    			builder.setCancelable(false);
    			builder.create().show();
			}else if(progressParams[0].equals("Command=SHOW INTERMEDIATE RESULTS; Mode=TRIGGER SINGLE Insert/Update"))
			{
				if(progressDialog!=null)
					progressDialog.dismiss();
				Calendar cal = Calendar.getInstance();
				//show alert dialog with 3 radio buttons (+ve = keep new, -ve = keep old, cancel = add to old) inside alert dialog that sets confirmation accordingly for that conflicting/already existing Attendance Record
				AlertDialog.Builder builder = new AlertDialog.Builder(ChoosePresetsManipAttendanceActivity.this);
				builder.setTitle("Update already existing..");
				
				LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    			ScrollView scrollView = (ScrollView)inflater.inflate(R.layout.medical_other_reasons__other_input_dialogs_scrollview, null);
    			
    			LinearLayout linearLayout = new LinearLayout(ChoosePresetsManipAttendanceActivity.this);
    			linearLayout.setOrientation(LinearLayout.VERTICAL);
    					
    			for(FutureTriggersRecord futureTriggerRecord:allDesiredTuplesIntermediateResult)
    			{
    				String presetTableName = futureTriggerRecord.getPresetTableName();
    				AttendanceRecord attendanceRecord = futureTriggerRecord.getAttendanceRecord();
    				
    				//Old Trigger Record
    				TextView txtviewShowOldTriggerRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    				txtviewShowOldTriggerRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				txtviewShowOldTriggerRecord.setText("\t\tOLD Only");
    				txtviewShowOldTriggerRecord.append(System.getProperty("line.separator"));
    				txtviewShowOldTriggerRecord.append("Chosen Preset: "+presetTableName+System.getProperty("line.separator"));
    				txtviewShowOldTriggerRecord.append("Attendance Date: "+Preset.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+System.getProperty("line.separator"));
    				txtviewShowOldTriggerRecord.append("Lecture Start Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+System.getProperty("line.separator"));
    				txtviewShowOldTriggerRecord.append("Lecture End Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+System.getProperty("line.separator"));
    				txtviewShowOldTriggerRecord.append(attendanceRecord.toString());			//toString() only gives back presentees, absentees, medical leaves, other reasons, count of each
    				linearLayout.addView(txtviewShowOldTriggerRecord, layoutParams1);
    						
    				View separatorBetweenOldNewMergedTriggerRecords1 = new View(ChoosePresetsManipAttendanceActivity.this);
    				LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2);
    				layoutParams2.setMargins(0, 10, 0, 10);
    				separatorBetweenOldNewMergedTriggerRecords1.setBackgroundColor(Color.WHITE);
    				linearLayout.addView(separatorBetweenOldNewMergedTriggerRecords1, layoutParams2);
    				
    				//New Trigger Record
    				TextView txtviewShowNewTriggerRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				txtviewShowNewTriggerRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				txtviewShowNewTriggerRecord.setText("\t\tNEW Only");
    				txtviewShowNewTriggerRecord.append(System.getProperty("line.separator"));
    				txtviewShowNewTriggerRecord.append("Chosen Preset: "+presetTableName+System.getProperty("line.separator"));
    				cal.set(attendanceDatePicker.getYear(), attendanceDatePicker.getMonth(), attendanceDatePicker.getDayOfMonth());
    				txtviewShowNewTriggerRecord.append("Attendance Date: "+Preset.userDisplayAttendanceDateFormat.format(cal.getTime())+System.getProperty("line.separator"));
    				txtviewShowNewTriggerRecord.append("Lecture Start Time: "+Preset.timeUserDisplayFormat.format(selectedLectureStartTime)+System.getProperty("line.separator"));
    				txtviewShowNewTriggerRecord.append("Lecture End Time: "+Preset.timeUserDisplayFormat.format(selectedLectureEndTime)+System.getProperty("line.separator"));
    				txtviewShowNewTriggerRecord.append(chkBoxIsPresentees.isChecked()?"Presentees: ":"Absentees: ");
    				txtviewShowNewTriggerRecord.append(chkBoxAllRollNums.isChecked()?"ALL":(editTextRollNums.getText().toString().length()==0?"None":Arrays.deepToString(enteredRollNums)));		//remove off starting and ending square bracket in toString() representations of Array and in between spaces
    				txtviewShowNewTriggerRecord.append(System.getProperty("line.separator"));
					
    				txtviewShowNewTriggerRecord.append("Medical Leave: ");
					if(editTextRollNumsMedicalLeave.getText().toString().trim().length()==0)
						txtviewShowNewTriggerRecord.append("None");
					else
					{
						for(Map.Entry<Integer, String> entry:medicalReasons.entrySet())
						{
							txtviewShowNewTriggerRecord.append(entry.getKey().toString());
							txtviewShowNewTriggerRecord.append("(");
							txtviewShowNewTriggerRecord.append(entry.getValue());
							txtviewShowNewTriggerRecord.append("), ");
						}
						txtviewShowNewTriggerRecord.setText(txtviewShowNewTriggerRecord.getText().toString().substring(0, txtviewShowNewTriggerRecord.getText().toString().length()-1));		//remove off last comma
						txtviewShowNewTriggerRecord.append(System.getProperty("line.separator"));
					}
					txtviewShowNewTriggerRecord.append(System.getProperty("line.separator"));
					
					txtviewShowNewTriggerRecord.append("Medical Leave: ");
					if(editTextRollNumsOtherReasons.getText().toString().trim().length()==0)
						txtviewShowNewTriggerRecord.append("None");
					else
					{
						for(Map.Entry<Integer, String> entry:otherReasons.entrySet())
						{
							txtviewShowNewTriggerRecord.append(entry.getKey().toString());
							txtviewShowNewTriggerRecord.append("(");
							txtviewShowNewTriggerRecord.append(entry.getValue());
							txtviewShowNewTriggerRecord.append("), ");
						}
						txtviewShowNewTriggerRecord.setText(txtviewShowNewTriggerRecord.getText().toString().substring(0, txtviewShowNewTriggerRecord.getText().toString().length()-1));		//remove off last comma
					}
    				linearLayout.addView(txtviewShowNewTriggerRecord, layoutParams1);
    				
    				View separatorBetweenOldNewMergedTriggerRecords2 = new View(ChoosePresetsManipAttendanceActivity.this);
    				layoutParams2.setMargins(0, 10, 0, 10);
    				separatorBetweenOldNewMergedTriggerRecords2.setBackgroundColor(Color.WHITE);
    				linearLayout.addView(separatorBetweenOldNewMergedTriggerRecords2, layoutParams2);
    				
    				//Merge/ADD to OLD Results
    				TextView txtviewShowMergingAddingTriggerRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				txtviewShowMergingAddingTriggerRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				txtviewShowMergingAddingTriggerRecord.setText("\t\tADD to OLD");
    				txtviewShowMergingAddingTriggerRecord.append(System.getProperty("line.separator"));
    				txtviewShowMergingAddingTriggerRecord.append("Chosen Preset: "+presetTableName+System.getProperty("line.separator"));
    				txtviewShowMergingAddingTriggerRecord.append("Attendance Date: "+Preset.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+System.getProperty("line.separator"));
    				txtviewShowMergingAddingTriggerRecord.append("Lecture Start Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+System.getProperty("line.separator"));
    				txtviewShowMergingAddingTriggerRecord.append("Lecture End Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+System.getProperty("line.separator"));
    				//actual merging below
    				LinkedHashMap<Integer, String> mergedNewAddedToOldAttendance = attendanceRecord.getAttendance();			//1st take whole old attendance record and then overwrite on it the new entries
    				if(chkBoxAllRollNums.isChecked())
    				{
    					for(int i=1;i<=retrievedNumStudents;i++)
    						mergedNewAddedToOldAttendance.put(i, chkBoxIsPresentees.isChecked()?"P":"A");
    				}else
    				{
    					if(editTextRollNums.getText().toString().trim().length()>0)
    						for(int i=0;i<enteredRollNums.length;i++)
    							mergedNewAddedToOldAttendance.put(Integer.parseInt(enteredRollNums[i]), chkBoxIsPresentees.isChecked()?"P":"A");
    					
    					if(editTextRollNumsMedicalLeave.getText().toString().trim().length()>0)
    					{
    						if(medicalReasons.size()==0)				//for that condition where no reason specified i.e reasons entry button not clicked
    						{
    							for(String rn:editTextRollNumsMedicalLeave.getText().toString().trim().split("[,]"))
    							{
    								medicalReasons.put(Integer.parseInt(rn), "M(None Specified)");
    								mergedNewAddedToOldAttendance.put(Integer.parseInt(rn), "M(None Specified)");
    							}
    						}else
    						{
    							for(Map.Entry<Integer, String> entry:medicalReasons.entrySet())
    								mergedNewAddedToOldAttendance.put(entry.getKey(), "M("+entry.getValue()+")");
    						}
    					}
    					
    					if(editTextRollNumsOtherReasons.getText().toString().trim().length()>0)
    					{
    						if(otherReasons.size()==0)
    						{
    							for(String rn:editTextRollNumsOtherReasons.getText().toString().trim().split("[,]"))
    							{
    								otherReasons.put(Integer.parseInt(rn), "O(None Specified)");
    								mergedNewAddedToOldAttendance.put(Integer.parseInt(rn), "O(None Specified)");
    							}
    						}else
    						{
    							for(Map.Entry<Integer, String> entry:otherReasons.entrySet())
    								mergedNewAddedToOldAttendance.put(entry.getKey(), "O("+entry.getValue()+")");
    						}
    					}
    				}
    				AttendanceRecord mergedNewAddedToOldAttendanceRecord = new AttendanceRecord(attendanceRecord.getAttendanceDate(), attendanceRecord.getLectureStartTime(), attendanceRecord.getLectureEndTime());
    				for(Map.Entry<Integer, String> entry:mergedNewAddedToOldAttendance.entrySet())
    					mergedNewAddedToOldAttendanceRecord.addAttendanceOfNewRollNum(entry.getKey(), entry.getValue());
    				txtviewShowMergingAddingTriggerRecord.append(mergedNewAddedToOldAttendanceRecord.toString());
    				linearLayout.addView(txtviewShowMergingAddingTriggerRecord, layoutParams1);
    				
    				mergedNewAddedToOldTriggerRecordToSendToServer = new FutureTriggersRecord(presetTableName, mergedNewAddedToOldAttendanceRecord);
    						
    				View separatorTriggerRecords = new View(ChoosePresetsManipAttendanceActivity.this);
    				LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 4);
    				layoutParams2.setMargins(0, 10, 0, 10);
    				separatorTriggerRecords.setBackgroundColor(Color.CYAN);
    				linearLayout.addView(separatorTriggerRecords, layoutParams3);
    			}
    					
    			scrollView.addView(linearLayout);
    			builder.setView(scrollView);
				
				builder.setPositiveButton("NEW only", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						triggerSingleInsertUpdateModeConfirmation[0] = false;
						triggerSingleInsertUpdateModeConfirmation[1] = true;
						dialog.cancel();
						progressDialog.setMessage("Please wait.");
						progressDialog.show();
						canSendConfirmation = true;
					}
				});
				builder.setNegativeButton("OLD only", new DialogInterface.OnClickListener()
				{	
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						triggerSingleInsertUpdateModeConfirmation[0] = true;
						triggerSingleInsertUpdateModeConfirmation[1] = false;
						dialog.cancel();
						canSendConfirmation = true;
					}
				});
				builder.setNeutralButton("ADD to OLD", new DialogInterface.OnClickListener()
				{	
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						triggerSingleInsertUpdateModeConfirmation[0] = true;
						triggerSingleInsertUpdateModeConfirmation[1] = true;
						dialog.cancel();
						progressDialog.setMessage("Please wait.");
						progressDialog.show();
						canSendConfirmation = true;
					}
				});
				builder.setCancelable(false);
				builder.create().show();
			}else if(progressParams[0].equals("Command=SHOW INTERMEDIATE RESULTS; Mode=TRIGGER BATCH Insert/Update"))
			{
				if(progressDialog!=null)
					progressDialog.dismiss();
				Calendar cal = Calendar.getInstance();
				//show alert dialog with 3 radio buttons (+ve = keep new, -ve = keep old, cancel = add to old) inside alert dialog that sets confirmation accordingly for each conflicting/already existing Trigger Record
				AlertDialog.Builder builder = new AlertDialog.Builder(ChoosePresetsManipAttendanceActivity.this);
				builder.setTitle("Update already existing..");
				
				LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    			ScrollView scrollView = (ScrollView)inflater.inflate(R.layout.medical_other_reasons__other_input_dialogs_scrollview, null);
    			
    			LinearLayout linearLayout = new LinearLayout(ChoosePresetsManipAttendanceActivity.this);
    			linearLayout.setOrientation(LinearLayout.VERTICAL);
    			
    			final RadioGroup radioGroupForUpdateOptions[] = new RadioGroup[allDesiredTuplesIntermediateResult.size()];
    			final RadioButton radioBtnsUpdateOptions[][] = new RadioButton[allDesiredTuplesIntermediateResult.size()][3];			//for each attendance record hence row size = attendanceRecordsIntermediateResults Size() and 3=column size for Keep NEW, Keep OLD, Add to OLD
    			int triggerRecordOfIntermediateResultsCnter=0;
    			
    			triggerBatchInsertUpdateModeConfirmations = new Boolean[allDesiredTuplesIntermediateResult.size()][2];
    			mergedNewAddedToOldFutureTriggerRecordsToSendToServerTriggerBatchInsertUpdateMode = new ArrayList<FutureTriggersRecord>(allDesiredTuplesIntermediateResult.size());
    					
    			for(FutureTriggersRecord ftr:allDesiredTuplesIntermediateResult)
    			{
    				String presetTableName = ftr.getPresetTableName();
    				AttendanceRecord attendanceRecord = ftr.getAttendanceRecord(); 
    				
    				//Old Trigger Record
    				TextView txtviewShowOldTriggerRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    				txtviewShowOldTriggerRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				txtviewShowOldTriggerRecord.setText("\t\tOLD Only");
    				txtviewShowOldTriggerRecord.append(System.getProperty("line.separator"));
    				txtviewShowOldTriggerRecord.append("Chosen Preset: "+presetTableName+System.getProperty("line.separator"));
    				txtviewShowOldTriggerRecord.append("Attendance Date: "+Preset.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+System.getProperty("line.separator"));
    				txtviewShowOldTriggerRecord.append("Lecture Start Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+System.getProperty("line.separator"));
    				txtviewShowOldTriggerRecord.append("Lecture End Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+System.getProperty("line.separator"));
    				txtviewShowOldTriggerRecord.append(attendanceRecord.toString());			//toString() only gives back presentees, absentees, medical leaves, other reasons, count of each
    				linearLayout.addView(txtviewShowOldTriggerRecord, layoutParams1);
    						
    				View separatorBetweenOldNewMergedTriggerRecords1 = new View(ChoosePresetsManipAttendanceActivity.this);
    				LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2);
    				layoutParams2.setMargins(0, 10, 0, 10);
    				separatorBetweenOldNewMergedTriggerRecords1.setBackgroundColor(Color.WHITE);
    				linearLayout.addView(separatorBetweenOldNewMergedTriggerRecords1, layoutParams2);
    				
    				//New Trigger Record
    				TextView txtviewShowNewTriggerRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				txtviewShowNewTriggerRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				txtviewShowNewTriggerRecord.setText("\t\tNEW Only");
    				txtviewShowNewTriggerRecord.append(System.getProperty("line.separator"));
    				txtviewShowNewTriggerRecord.append("Chosen Preset: "+presetTableName+System.getProperty("line.separator"));
    				cal.set(attendanceDatePicker.getYear(), attendanceDatePicker.getMonth(), attendanceDatePicker.getDayOfMonth());
    				txtviewShowNewTriggerRecord.append("Attendance Date: "+Preset.userDisplayAttendanceDateFormat.format(cal.getTime())+System.getProperty("line.separator"));
    				txtviewShowNewTriggerRecord.append("Lecture Start Time: "+Preset.timeUserDisplayFormat.format(selectedLectureStartTime)+System.getProperty("line.separator"));
    				txtviewShowNewTriggerRecord.append("Lecture End Time: "+Preset.timeUserDisplayFormat.format(selectedLectureEndTime)+System.getProperty("line.separator"));
    				txtviewShowNewTriggerRecord.append(chkBoxIsPresentees.isChecked()?"Presentees: ":"Absentees: ");
    				txtviewShowNewTriggerRecord.append(chkBoxAllRollNums.isChecked()?"ALL":(editTextRollNums.getText().toString().length()==0?"None":Arrays.deepToString(enteredRollNums)));		//remove off starting and ending square bracket in toString() representations of Array and in between spaces
    				txtviewShowNewTriggerRecord.append(System.getProperty("line.separator"));
					
    				txtviewShowNewTriggerRecord.append("Medical Leave: ");
					if(editTextRollNumsMedicalLeave.getText().toString().trim().length()==0)
						txtviewShowNewTriggerRecord.append("None");
					else
					{
						for(Map.Entry<Integer, String> entry:medicalReasons.entrySet())
						{
							txtviewShowNewTriggerRecord.append(entry.getKey().toString());
							txtviewShowNewTriggerRecord.append("(");
							txtviewShowNewTriggerRecord.append(entry.getValue());
							txtviewShowNewTriggerRecord.append("), ");
						}
						txtviewShowNewTriggerRecord.setText(txtviewShowNewTriggerRecord.getText().toString().substring(0, txtviewShowNewTriggerRecord.getText().toString().length()-1));		//remove off last comma
						txtviewShowNewTriggerRecord.append(System.getProperty("line.separator"));
					}
					txtviewShowNewTriggerRecord.append(System.getProperty("line.separator"));
					
					txtviewShowNewTriggerRecord.append("Medical Leave: ");
					if(editTextRollNumsOtherReasons.getText().toString().trim().length()==0)
						txtviewShowNewTriggerRecord.append("None");
					else
					{
						for(Map.Entry<Integer, String> entry:otherReasons.entrySet())
						{
							txtviewShowNewTriggerRecord.append(entry.getKey().toString());
							txtviewShowNewTriggerRecord.append("(");
							txtviewShowNewTriggerRecord.append(entry.getValue());
							txtviewShowNewTriggerRecord.append("), ");
						}
						txtviewShowNewTriggerRecord.setText(txtviewShowNewTriggerRecord.getText().toString().substring(0, txtviewShowNewTriggerRecord.getText().toString().length()-1));		//remove off last comma
					}
    				linearLayout.addView(txtviewShowNewTriggerRecord, layoutParams1);
    				
    				View separatorBetweenOldNewMergedTriggerRecords2 = new View(ChoosePresetsManipAttendanceActivity.this);
    				layoutParams2.setMargins(0, 10, 0, 10);
    				separatorBetweenOldNewMergedTriggerRecords2.setBackgroundColor(Color.WHITE);
    				linearLayout.addView(separatorBetweenOldNewMergedTriggerRecords2, layoutParams2);
    				
    				//Merge/ADD to OLD Results
    				TextView txtviewShowMergingAddingTriggerRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				txtviewShowMergingAddingTriggerRecord = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				txtviewShowMergingAddingTriggerRecord.setText("\t\tADD to OLD");
    				txtviewShowMergingAddingTriggerRecord.append(System.getProperty("line.separator"));
    				txtviewShowMergingAddingTriggerRecord.append("Chosen Preset: "+presetTableName+System.getProperty("line.separator"));
    				txtviewShowMergingAddingTriggerRecord.append("Attendance Date: "+Preset.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+System.getProperty("line.separator"));
    				txtviewShowMergingAddingTriggerRecord.append("Lecture Start Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+System.getProperty("line.separator"));
    				txtviewShowMergingAddingTriggerRecord.append("Lecture End Time: "+Preset.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+System.getProperty("line.separator"));
    				//actual merging below
    				LinkedHashMap<Integer, String> mergedNewAddedToOldAttendance = attendanceRecord.getAttendance();			//1st take whole old attendance record and then overwrite on it the new entries
    				if(chkBoxAllRollNums.isChecked())
    				{
    					for(int i=1;i<=retrievedNumStudents;i++)
    						mergedNewAddedToOldAttendance.put(i, chkBoxIsPresentees.isChecked()?"P":"A");
    				}else
    				{
    					if(editTextRollNums.getText().toString().trim().length()>0)
    						for(int i=0;i<enteredRollNums.length;i++)
    							mergedNewAddedToOldAttendance.put(Integer.parseInt(enteredRollNums[i]), chkBoxIsPresentees.isChecked()?"P":"A");
    					
    					if(editTextRollNumsMedicalLeave.getText().toString().trim().length()>0)
    					{
    						if(medicalReasons.size()==0)				//for that condition where no reason specified i.e reasons entry button not clicked
    						{
    							for(String rn:editTextRollNumsMedicalLeave.getText().toString().trim().split("[,]"))
    							{
    								medicalReasons.put(Integer.parseInt(rn), "M(None Specified)");
    								mergedNewAddedToOldAttendance.put(Integer.parseInt(rn), "M(None Specified)");
    							}
    						}else
    						{
    							for(Map.Entry<Integer, String> entry:medicalReasons.entrySet())
    								mergedNewAddedToOldAttendance.put(entry.getKey(), "M("+entry.getValue()+")");
    						}
    					}
    					
    					if(editTextRollNumsOtherReasons.getText().toString().trim().length()>0)
    					{
    						if(otherReasons.size()==0)
    						{
    							for(String rn:editTextRollNumsOtherReasons.getText().toString().trim().split("[,]"))
    							{
    								otherReasons.put(Integer.parseInt(rn), "O(None Specified)");
    								mergedNewAddedToOldAttendance.put(Integer.parseInt(rn), "O(None Specified)");
    							}
    						}else
    						{
    							for(Map.Entry<Integer, String> entry:otherReasons.entrySet())
    								mergedNewAddedToOldAttendance.put(entry.getKey(), "O("+entry.getValue()+")");
    						}
    					}
    				}
    				AttendanceRecord mergedNewAddedToOldAttendanceRecord = new AttendanceRecord(attendanceRecord.getAttendanceDate(), attendanceRecord.getLectureStartTime(), attendanceRecord.getLectureEndTime());
    				for(Map.Entry<Integer, String> entry:mergedNewAddedToOldAttendance.entrySet())
    					mergedNewAddedToOldAttendanceRecord.addAttendanceOfNewRollNum(entry.getKey(), entry.getValue());
    				txtviewShowMergingAddingTriggerRecord.append(mergedNewAddedToOldAttendanceRecord.toString());
    				linearLayout.addView(txtviewShowMergingAddingTriggerRecord, layoutParams1);
    				
    				mergedNewAddedToOldFutureTriggerRecordsToSendToServerTriggerBatchInsertUpdateMode.add(new FutureTriggersRecord(presetTableName, mergedNewAddedToOldAttendanceRecord));
    						
    				View separatorTriggerRecords = new View(ChoosePresetsManipAttendanceActivity.this);
    				LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 4);
    				layoutParams2.setMargins(0, 10, 0, 10);
    				separatorTriggerRecords.setBackgroundColor(Color.CYAN);
    				linearLayout.addView(separatorTriggerRecords, layoutParams3);
    				
    				TextView txtviewInstr = new TextView(ChoosePresetsManipAttendanceActivity.this);
    				txtviewInstr.setText("Choose Update Option for ABOVE Trigger Record");
    				linearLayout.addView(txtviewInstr, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    				
    				radioGroupForUpdateOptions[triggerRecordOfIntermediateResultsCnter] = new RadioGroup(ChoosePresetsManipAttendanceActivity.this);
    				radioGroupForUpdateOptions[triggerRecordOfIntermediateResultsCnter].setOrientation(RadioGroup.VERTICAL);
    				LinearLayout.LayoutParams layoutParamsForRadioButtons = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    				radioBtnsUpdateOptions[triggerRecordOfIntermediateResultsCnter][0] = new RadioButton(ChoosePresetsManipAttendanceActivity.this);
    				radioBtnsUpdateOptions[triggerRecordOfIntermediateResultsCnter][0].setText("NEW Only");
    				radioBtnsUpdateOptions[triggerRecordOfIntermediateResultsCnter][1] = new RadioButton(ChoosePresetsManipAttendanceActivity.this);
    				radioBtnsUpdateOptions[triggerRecordOfIntermediateResultsCnter][1].setText("OLD Only");
    				radioBtnsUpdateOptions[triggerRecordOfIntermediateResultsCnter][2] = new RadioButton(ChoosePresetsManipAttendanceActivity.this);
    				radioBtnsUpdateOptions[triggerRecordOfIntermediateResultsCnter][2].setText("ADD to OLD");
    				for(int i=0;i<3;i++)
    					radioGroupForUpdateOptions[triggerRecordOfIntermediateResultsCnter].addView(radioBtnsUpdateOptions[triggerRecordOfIntermediateResultsCnter][i], layoutParamsForRadioButtons);
    				radioGroupForUpdateOptions[triggerRecordOfIntermediateResultsCnter].check(radioBtnsUpdateOptions[triggerRecordOfIntermediateResultsCnter][2].getId());			//default checked is "add to old" for all attendance records			    				
    				linearLayout.addView(radioGroupForUpdateOptions[triggerRecordOfIntermediateResultsCnter], new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    				
    				triggerRecordOfIntermediateResultsCnter++;
    				
    				View separatorTriggerRecords2 = new View(ChoosePresetsManipAttendanceActivity.this);
    				layoutParams2.setMargins(0, 10, 0, 10);
    				separatorTriggerRecords2.setBackgroundColor(Color.CYAN);
    				linearLayout.addView(separatorTriggerRecords2, layoutParams3);
    			}
    			
    			LinearLayout.LayoutParams layoutParamsForUpdateOptionsForAllTriggerRecords = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    			Button btnNEWOnlyUpdateOptionForAllTriggerRecords = new Button(ChoosePresetsManipAttendanceActivity.this);
    			btnNEWOnlyUpdateOptionForAllTriggerRecords.setText("NEW Only For ALL");
    			Button btnOLDOnlyUpdateOptionForAllTriggerRecords = new Button(ChoosePresetsManipAttendanceActivity.this);
    			btnOLDOnlyUpdateOptionForAllTriggerRecords.setText("OLD Only For ALL");
    			Button btnADDToOLDUpdateOptionForAllTriggerRecords = new Button(ChoosePresetsManipAttendanceActivity.this);
    			btnADDToOLDUpdateOptionForAllTriggerRecords.setText("ADD to OLD For ALL");
    			linearLayout.addView(btnNEWOnlyUpdateOptionForAllTriggerRecords, layoutParamsForUpdateOptionsForAllTriggerRecords);
    			linearLayout.addView(btnOLDOnlyUpdateOptionForAllTriggerRecords, layoutParamsForUpdateOptionsForAllTriggerRecords);
    			linearLayout.addView(btnADDToOLDUpdateOptionForAllTriggerRecords, layoutParamsForUpdateOptionsForAllTriggerRecords);
    			
    			scrollView.addView(linearLayout);
    			builder.setView(scrollView);
    			
    			builder.setPositiveButton("Do as Selected", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						for(int i=0;i<triggerBatchInsertUpdateModeConfirmations.length;i++)
						{
							if(radioBtnsUpdateOptions[i][0].isChecked())				//NEW Only for that attendance Record
							{
								triggerBatchInsertUpdateModeConfirmations[i][0] = false;
								triggerBatchInsertUpdateModeConfirmations[i][1] = true;
							}else if(radioBtnsUpdateOptions[i][1].isChecked())				//OLD Only for that attendance Record
							{
								triggerBatchInsertUpdateModeConfirmations[i][0] = true;
								triggerBatchInsertUpdateModeConfirmations[i][1] = false;
							}else if(radioBtnsUpdateOptions[i][2].isChecked())				//Merging/ADDing NEW to OLD for that attendance Record
							{
								triggerBatchInsertUpdateModeConfirmations[i][0] = true;
								triggerBatchInsertUpdateModeConfirmations[i][1] = true;
							}
						}
						dialog.cancel();
						progressDialog.setMessage("Please wait.");
						progressDialog.show();
						canSendConfirmation = true;
					}
				});
				builder.setCancelable(false);
				final AlertDialog alertDialogGetUpdateOptions = builder.create();
    			
    			btnNEWOnlyUpdateOptionForAllTriggerRecords.setOnClickListener(new View.OnClickListener()
    			{
					@Override
					public void onClick(View v)
					{
						for(int i=0;i<triggerBatchInsertUpdateModeConfirmations.length;i++)
						{
							triggerBatchInsertUpdateModeConfirmations[i][0] = false;					//discard old
							triggerBatchInsertUpdateModeConfirmations[i][1] = true;					//keep new
						}
						alertDialogGetUpdateOptions.dismiss();
						progressDialog.setMessage("Please wait..");
						progressDialog.show();
						canSendConfirmation = true;
					}
				});
    			btnOLDOnlyUpdateOptionForAllTriggerRecords.setOnClickListener(new View.OnClickListener()
    			{
					@Override
					public void onClick(View v)
					{
						for(int i=0;i<triggerBatchInsertUpdateModeConfirmations.length;i++)
						{
							triggerBatchInsertUpdateModeConfirmations[i][0] = true;					//keep old
							triggerBatchInsertUpdateModeConfirmations[i][1] = false;					//discard new
						}
						alertDialogGetUpdateOptions.dismiss();
						progressDialog.setMessage("Please wait..");
						progressDialog.show();
						canSendConfirmation = true;
					}
				});
    			btnADDToOLDUpdateOptionForAllTriggerRecords.setOnClickListener(new View.OnClickListener()
    			{
					@Override
					public void onClick(View v)
					{
						for(int i=0;i<triggerBatchInsertUpdateModeConfirmations.length;i++)
						{
							//Merging/Adding NEW to OLD
							triggerBatchInsertUpdateModeConfirmations[i][0] = true;					//keep old
							triggerBatchInsertUpdateModeConfirmations[i][1] = true;					//keep new
						}
						alertDialogGetUpdateOptions.dismiss();
						progressDialog.setMessage("Please wait..");
						progressDialog.show();
						canSendConfirmation = true;
					}
				});
				alertDialogGetUpdateOptions.show();
			}else
				progressDialog.setMessage(progressParams[0]);
	    }
		@Override
	    protected void onPostExecute(Exception exceptionOccured)
		{
			if(chosenMode.equals("TRIGGER SINGLE View") || chosenMode.equals("TRIGGER BATCH View") || chosenMode.equals("TRIGGER SINGLE Delete")|| chosenMode.equals("TRIGGER BATCH Delete")|| chosenMode.equals("TRIGGER SINGLE Insert/Update")|| chosenMode.equals("TRIGGER BATCH Insert/Update"))
			{
				if(progressDialog!=null)
					publishProgress("Saving Trigger Operation String as a History Record ..");
	    		try
	    		{
					saveToHistoryTriggerOperationString(chkBoxOperatingOnBehalf.isChecked(), triggerOperationStringToSend, triggerOperationSuccess);
					if(progressDialog!=null)
		    			publishProgress("Saved Trigger Operation String as History Record!");
				}catch (Exception e)
				{
					e.printStackTrace();
					if(exceptionOccured!=null)			//if already an exception has occured due to other operations so give that preference over just saving exception of history records
						exceptionOccured = e;
				}
			}
			
			if(progressDialog!=null)
				progressDialog.dismiss();
			
			if(exceptionOccured==null)
			{
				if(triggerOperationSuccess)
					Toast.makeText(getBaseContext(), "Successful trigger operation!", Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(getBaseContext(), userCanceledOperation?"User canceled Operation":"There was an error in requested trigger operation .. please retry", Toast.LENGTH_SHORT).show();
			}else
				Toast.makeText(getBaseContext(), "Exception occured while trigger operation .. please retry.\nException Details: "+((exceptionOccured.getMessage()==null || exceptionOccured.getMessage().equals(""))?"No error Description Found":exceptionOccured.getMessage()), Toast.LENGTH_LONG).show();
		}
		private void saveToHistoryTriggerOperationString(boolean isOperatingOnBehalf, String triggerOperationString, boolean triggerOperationSuccess)
		{
			
		}
	}
	public void onBackPressed()							//Logout confirmation, if user wants to logout then send inform AMSServer that user is logging out so that its ClientServicingThread can be stopped and receive its confirmation 
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(ChoosePresetsManipAttendanceActivity.this);
		builder.setTitle("Logout?");
		builder.setMessage("Are you sure you want to Logout?");
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
		    	AsyncTask<Void, String, Exception> bgTaskInformLoggingOutToAMSServer = new AsyncTask<Void, String, Exception>()
		    	{
		    		ProgressDialog progressDialog = null;
		    		boolean logoutSuccess = false;
		    	    	       				
		    	    @Override
		    	    protected void onPreExecute()
		    	    {
		    	    	progressDialog = new ProgressDialog(ChoosePresetsManipAttendanceActivity.this);
		    	    	progressDialog.setTitle("Processing ..");
		    	    	progressDialog.setMessage("Logging out .. Please wait");
		    	    	progressDialog.setCancelable(false);
		    	    	progressDialog.setIndeterminate(true);
		    	    	progressDialog.show();
		    	    }
		    	    	    	    							
		    	    @Override
		    	    protected Exception doInBackground(Void ...params)
		    	    {
		    	    	try
		    	    	{
		    	    		LoginActivity.toServerOOStrm.writeObject("LOGOUT_DONE_WITH_ATTENDANCE_MODIFICATIONS ");
		    	    		LoginActivity.toServerOOStrm.flush();
		    	    		
		    	    		logoutSuccess = ((String)LoginActivity.fromServerOIS.readObject()).equals("OK! Stopping your servicing thread.");
		    	    	}catch (Exception e) 
		    	    	{
		    	    		Log.e("Exception", e.getMessage()==null?"null":e.getMessage());
		    	    		return e;
		    	    	}
						return null;
		    	    }

					@Override
		    	    protected void onPostExecute(Exception exceptionOccured)
					{
						if(progressDialog!=null)
							progressDialog.dismiss();
						
						if(exceptionOccured==null)
						{
							if(logoutSuccess)
							{
								Toast.makeText(getBaseContext(), "Successfully Logged out.", Toast.LENGTH_SHORT).show();
								ChoosePresetsManipAttendanceActivity.this.finish();
							}else
								Toast.makeText(getBaseContext(), "There was an error in logging out .. please retry", Toast.LENGTH_SHORT).show();
						}else
							Toast.makeText(getBaseContext(), "Exception occured while logging out .. please retry.\nException Details: "+exceptionOccured.getMessage(), Toast.LENGTH_LONG).show();
					}
		    	};
		    	bgTaskInformLoggingOutToAMSServer.execute();
		    	//while(!canFinishActivity);							//loop infinitely until progressDialog is dismissed and canFinishActivity is set in onPostExecute
		    	//finish();
			}
		});
		builder.setNegativeButton("No", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
			}
		});
		builder.setCancelable(false);
		builder.create().show();
	}
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater menuInflater = new MenuInflater(getBaseContext());
		menuInflater.inflate(R.menu.activity_menu_choose_presets_manip_attendance, menu);
		return true;
	}
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.enterRollNumRangeMenuItem:
				if(!(chosenMode.equals("SINGLE Insert/Update") || chosenMode.equals("BATCH Insert/Update") || chosenMode.equals("TRIGGER SINGLE Insert/Update") || chosenMode.equals("TRIGGER BATCH Insert/Update")))
				{
					Toast.makeText(ChoosePresetsManipAttendanceActivity.this.getBaseContext(), "You must be in an apt mode to enter Range of RollNums", Toast.LENGTH_SHORT).show();
				}else if(retrievedNumStudents==0)
					Toast.makeText(ChoosePresetsManipAttendanceActivity.this.getBaseContext(), "A preset must be chosen 1st", Toast.LENGTH_SHORT).show();
				else
				{
					final String enterRangeOfRollNumsInOptions[] = { "Absentees/Presentees",  "Medical Leaves", "Other Leaves" };
					AlertDialog.Builder builder = new AlertDialog.Builder(ChoosePresetsManipAttendanceActivity.this);
					builder.setTitle("Where to enter range of rollNums ..");
					builder.setSingleChoiceItems(enterRangeOfRollNumsInOptions,  0, new DialogInterface.OnClickListener()
					{	
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							String enterRangeOfRollNumsInChosenOption = enterRangeOfRollNumsInOptions[which];
							dialog.dismiss();
							getAndAddRangeOfRollNums(enterRangeOfRollNumsInChosenOption);
						}
					});
					builder.create().show();
				}
			return true;
			
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	protected void getAndAddRangeOfRollNums(final String enterRangeOfRollNumsInChosenOption)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(ChoosePresetsManipAttendanceActivity.this);
		builder.setTitle("Enter range of rollNums ..");
			
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ScrollView scrollView = (ScrollView)inflater.inflate(R.layout.medical_other_reasons__other_input_dialogs_scrollview, null);
		
		LinearLayout wrappingLinearLayout = new LinearLayout(ChoosePresetsManipAttendanceActivity.this);
		wrappingLinearLayout.setOrientation(LinearLayout.VERTICAL);	
		
		LinearLayout linearLayout1 = new LinearLayout(ChoosePresetsManipAttendanceActivity.this);
		linearLayout1.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0F);		
		final EditText editTextGetLowerRangeValue = new EditText(ChoosePresetsManipAttendanceActivity.this);
		editTextGetLowerRangeValue.setHint("Enter lower range value(Inclusive)..");
		linearLayout1.addView(editTextGetLowerRangeValue, layoutParams1);
		
		LinearLayout linearLayout2 = new LinearLayout(ChoosePresetsManipAttendanceActivity.this);
		linearLayout1.setOrientation(LinearLayout.HORIZONTAL);
		final EditText editTextGetUpperRangeValue = new EditText(ChoosePresetsManipAttendanceActivity.this);
		editTextGetUpperRangeValue.setHint("Enter upper range value(Inclusive)..");
		linearLayout2.addView(editTextGetUpperRangeValue, layoutParams1);
			
		wrappingLinearLayout.addView(linearLayout1, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		wrappingLinearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
				
		scrollView.addView(wrappingLinearLayout);
		builder.setView(scrollView);
		
		builder.setPositiveButton("Add", new DialogInterface.OnClickListener()
		{	
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				try
				{
					int upperRangeValue = Integer.parseInt(editTextGetUpperRangeValue.getText().toString());
					int lowerRangeValue = Integer.parseInt(editTextGetLowerRangeValue.getText().toString());
					if(upperRangeValue>retrievedNumStudents || lowerRangeValue<1 || (lowerRangeValue>upperRangeValue))
						throw new Exception();
					else
					{
						if(enterRangeOfRollNumsInChosenOption.equals("Absentees/Presentees") && editTextRollNums.getText().toString()!=null && !editTextRollNums.getText().toString().trim().equals(""))
							editTextRollNums.append(editTextRollNums.getText().toString().endsWith(", ")?"":", ");
						else if(enterRangeOfRollNumsInChosenOption.equals("Medical Leaves") && editTextRollNumsMedicalLeave.getText().toString()!=null && !editTextRollNumsMedicalLeave.getText().toString().trim().equals(""))
							editTextRollNumsMedicalLeave.append(editTextRollNumsMedicalLeave.getText().toString().endsWith(", ")?"":", ");
						else if(enterRangeOfRollNumsInChosenOption.equals("Other Leaves") && editTextRollNumsOtherReasons.getText().toString()!=null && !editTextRollNumsOtherReasons.getText().toString().trim().equals(""))
							editTextRollNumsOtherReasons.append(editTextRollNumsOtherReasons.getText().toString().endsWith(", ")?"":", ");
						
						for(int i=lowerRangeValue;i<upperRangeValue;i++)
							if(enterRangeOfRollNumsInChosenOption.equals("Absentees/Presentees"))
								editTextRollNums.append(i+", ");
							else if(enterRangeOfRollNumsInChosenOption.equals("Medical Leaves"))
								editTextRollNumsMedicalLeave.append(i+", ");
							else if(enterRangeOfRollNumsInChosenOption.equals("Other Leaves"))
								editTextRollNumsOtherReasons.append(i+", ");
						if(enterRangeOfRollNumsInChosenOption.equals("Absentees/Presentees"))
							editTextRollNums.append(Integer.toString(upperRangeValue));
						else if(enterRangeOfRollNumsInChosenOption.equals("Medical Leaves"))
							editTextRollNumsMedicalLeave.append(Integer.toString(upperRangeValue));
						else if(enterRangeOfRollNumsInChosenOption.equals("Other Leaves"))
							editTextRollNumsOtherReasons.append(Integer.toString(upperRangeValue));
						
						//request apt focus so that when focus is lost duplicates and all removed
						if(enterRangeOfRollNumsInChosenOption.equals("Absentees/Presentees"))
							editTextRollNums.requestFocus();
						else if(enterRangeOfRollNumsInChosenOption.equals("Medical Leaves"))
							editTextRollNumsMedicalLeave.requestFocus();
						else if(enterRangeOfRollNumsInChosenOption.equals("Other Leaves"))
							editTextRollNumsOtherReasons.requestFocus();
					}
					dialog.dismiss();
				}catch(Exception e)
				{
					e.printStackTrace();
					dialog.dismiss();
					Toast.makeText(ChoosePresetsManipAttendanceActivity.this, "Invalid/Out of range entries .. re-enter", Toast.LENGTH_SHORT).show();
				}
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
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