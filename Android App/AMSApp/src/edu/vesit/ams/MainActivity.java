package edu.vesit.ams;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.PasswordTransformationMethod;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity
{
	private TabHost tabHost;
	private TabSpec tab1;
	private TabSpec tab2;
	
	Button btnLogin, btnViewPresets, btnGotoSettings, btnViewPerformedAttendanceOperations, btnDisplayAppUsageInstructions, btnAboutApp;
	
	public static File AMSAppDir = new File(Environment.getExternalStorageDirectory(), ".VESIT AMS");
	
	public static File XMLConfigParamsAppFile = new File(AMSAppDir, "appConfig.xml");
	public static File XMLSavedServerIPAddressesFile = new File(AMSAppDir, "savedServerIPAddresses.xml");
	public static File XMLsavedLoginCredentialsFile = new File(AMSAppDir, "savedLoginCredentials.xml");
	
	public static File DATAAppDir = new File(AMSAppDir, "DATA");
	
	public static File appDataAppDir = new File(DATAAppDir, "appData");	
	public static File appDataPresetsStartTimesAppDir = new File(appDataAppDir, "PresetsStartTimes");
	public static File appDataPresetsEndTimesAppDir = new File(appDataAppDir, "PresetsEndTimes");
	public static File appDataTeachersHistoryAttendanceStringsAppDir = new File(appDataAppDir, "TeachersHistoryAttendanceStrings");
	public static File appDataTeachersAllotedPresetHistoryAttendanceStringsAppFile = new File(appDataTeachersHistoryAttendanceStringsAppDir, "TeachersPresetHistoryAttendanceStrings.txt");
	public static File appDataTeachersNonAllotedPresetHistoryAttendanceStringsAppFile = new File(appDataTeachersHistoryAttendanceStringsAppDir, "TeachersNonPresetHistoryAttendanceStrings.txt");
	
	public static File serverDataAppDir = new File(DATAAppDir, "serverData");		// all 4 not included in creation of dir as this would be created on unzipping serverData.zip or created when needed
	public static File serverDataPresetsAppDir = new File(serverDataAppDir, "Presets");
	public static File serverDataClassificationAppDir = new File(serverDataAppDir, "Classification");
	
	public static SimpleDateFormat logEventTimestampFormat = new SimpleDateFormat("dd MMM(MM) yyyy EEE hh:mm:ss:SSS a", Locale.ENGLISH);
	
	ArrayList<Preset> allPresets = new ArrayList<Preset>(), teacherAllotedPresets = new ArrayList<Preset>(), teacherNonAllotedPresets = new ArrayList<Preset>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		boolean customTitleSupported = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_main);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		if(customTitleSupported)
		{
			getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);
			TextView myTitleText = (TextView)findViewById(R.id.myTitle);
			myTitleText.setText("VESIT AMS");
			//myTitleText.setTextColor(Color.rgb(0,0,139));
		}
		
		getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.bg));
		
		File temp[] = {	AMSAppDir, DATAAppDir, appDataAppDir, appDataPresetsStartTimesAppDir, appDataPresetsEndTimesAppDir, appDataTeachersHistoryAttendanceStringsAppDir};
		for(File f:temp)
			if(!f.exists())
				f.mkdirs();

		try
		{
			if(!appDataTeachersNonAllotedPresetHistoryAttendanceStringsAppFile.exists())
				appDataTeachersNonAllotedPresetHistoryAttendanceStringsAppFile.createNewFile();
			if(!appDataTeachersAllotedPresetHistoryAttendanceStringsAppFile.exists())
				appDataTeachersAllotedPresetHistoryAttendanceStringsAppFile.createNewFile();
		}catch (IOException e)
		{
			e.printStackTrace();
		}
		
		tabHost = (TabHost) findViewById(R.id.tabhost);
		tabHost.setup();
		tab1 = tabHost.newTabSpec("Main");
		tab1.setContent(R.id.tableLayout);			//earlier was tab1 = 1st tableRow of main tab
		tab1.setIndicator("Main");
		tab2 = tabHost.newTabSpec("About");
		tab2.setContent(R.id.tab2);
		tab2.setIndicator("About");
		
		tabHost.addTab(tab1);
        tabHost.addTab(tab2);
        
        for(int j=0;j<tabHost.getTabWidget().getChildCount();j++)
        {
        	/*View v = tabHost.getTabWidget().getChildAt(j);
        	v.setBackgroundResource(R.drawable.tab_selector);*/
        	((TextView)tabHost.getTabWidget().getChildAt(j).findViewById(android.R.id.title)).setTextColor(Color.parseColor("#FFFFFF"));;
        }
		
		Toast.makeText(getBaseContext(), "Welcome to VESIT's Attendance Monitoring System(AMS) ", Toast.LENGTH_LONG).show();
		
		/*******************************************************************************************************************************************************/
		
		initViews();
		
		/*******************************************************************************************************************************************************/
		
		btnLogin.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startActivity(new Intent("edu.vesit.ams.LoginActivity"));
			}
		});
		
		/*******************************************************************************************************************************************************/
		
		btnViewPresets.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setTitle("Enter login credentials to obtain your presets ..");
				
				LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				ScrollView scrollView = (ScrollView) layoutInflater.inflate(R.layout.medical_other_reasons__other_input_dialogs_scrollview, null);
				LinearLayout wrappingLinearLayout = new LinearLayout(MainActivity.this);
				wrappingLinearLayout.setOrientation(LinearLayout.VERTICAL);
				
				final EditText editTextUsernamePWDBName[] = new EditText[3];
				final CheckBox chkBoxShowPW = new CheckBox(MainActivity.this);
				
				for(int i=0;i<editTextUsernamePWDBName.length;i++)
				{
					LinearLayout linearLayout = new LinearLayout(MainActivity.this);
					linearLayout.setOrientation(LinearLayout.HORIZONTAL);
					linearLayout.setWeightSum(1.0F);
					
					editTextUsernamePWDBName[i] = new EditText(MainActivity.this);
					if(i==1)
					{
						//editTextUsernamePWDBName[i].setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
						editTextUsernamePWDBName[i].setTransformationMethod(new PasswordTransformationMethod());
						linearLayout.addView(editTextUsernamePWDBName[i], new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9F));
						linearLayout.addView(chkBoxShowPW, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.1F));
					}else
						linearLayout.addView(editTextUsernamePWDBName[i], new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0F));
					
					wrappingLinearLayout.addView(linearLayout);
				}
				
				editTextUsernamePWDBName[0].setHint("Enter Teacher's Username..");
				editTextUsernamePWDBName[1].setHint("Enter Teacher's Password..");
				editTextUsernamePWDBName[2].setHint("Enter DBName..");
				
				/*editTextUsernamePWDBName[0].setText("Manisha");
				editTextUsernamePWDBName[1].setText("password");
				editTextUsernamePWDBName[2].setText("vesitams2013to2014");*/
				
				chkBoxShowPW.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						editTextUsernamePWDBName[1].setTransformationMethod(chkBoxShowPW.isChecked()?null:new PasswordTransformationMethod());
					}
				});
				
				builder.setPositiveButton("Submit to filter Presets", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						File xmlFilePresetsAllotmentForSelectedDB = new File(MainActivity.serverDataPresetsAppDir, editTextUsernamePWDBName[2].getText().toString()+"_presets_allotment.xml");
						if(xmlFilePresetsAllotmentForSelectedDB.exists())
						{
							//Parse, validateLogin(not necessary for presets viewing as it could be done by lab assistant etc) and populate teacher's presets and start and end times for that preset 
							try
							{
								allPresets.clear();
								teacherAllotedPresets.clear();
								teacherNonAllotedPresets.clear();
								
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
							    
							    String teacherUsername = editTextUsernamePWDBName[0].getText().toString(), teacherPlainPW = editTextUsernamePWDBName[1].getText().toString();
					        	for(Preset p:allPresets)
					        	{
					        		if(p.getTeacherUsername().equals(teacherUsername) && p.getTeacherHashedPW().equals(ChoosePresetsManipAttendanceActivity.getSHA1OfParam(teacherPlainPW + ChoosePresetsManipAttendanceActivity.getMD5OfParam(p.getTeacherSalt() + teacherPlainPW) + p.getTeacherSalt())))
					        			
					        			teacherAllotedPresets.add(p);
					        		else
					        			teacherNonAllotedPresets.add(p);
					        		
					        		/*Log.d("checking username", p.getTeacherUsername()+" and "+teacherUsername);
					        		Log.d("checking pw", p.getTeacherHashedPW()+" and ");
					        		Log.d("Hashed pw generation", teacherPlainPW + " + MD5("+p.getTeacherSalt() +" + "+ teacherPlainPW+") + "+p.getTeacherSalt());
					        		Log.d("checking pw ..", ChoosePresetsManipAttendanceActivity.getSHA1OfParam(teacherPlainPW + ChoosePresetsManipAttendanceActivity.getMD5OfParam(p.getTeacherSalt() + teacherPlainPW) + p.getTeacherSalt()));*/
					        		
					        		//check if start times file is present for that preset
					        		if(!MainActivity.appDataPresetsStartTimesAppDir.exists())
					        			MainActivity.appDataPresetsStartTimesAppDir.mkdirs();
					        		File f1 = new File(MainActivity.appDataPresetsStartTimesAppDir, editTextUsernamePWDBName[2].getText().toString()+"_"+p.getPresetTableName()+".xml");
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
					        		File f2 = new File(MainActivity.appDataPresetsEndTimesAppDir, editTextUsernamePWDBName[2].getText().toString()+"_"+p.getPresetTableName()+".xml");
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
							}catch(Exception e)
							{
								Toast.makeText(getBaseContext(), "Exception occured: "+e.getMessage()+".. try again later!", Toast.LENGTH_SHORT).show();
								e.printStackTrace();
							}
							Intent i = new Intent();
							i.setAction("edu.vesit.ams.ViewPresetsActivity");
							Bundle extras = new Bundle();
							extras.putString("edu.vesit.ams.MainActivity.teacherUsername", editTextUsernamePWDBName[0].getText().toString());
							extras.putString("edu.vesit.ams.MainActivity.teacherPlainPW", editTextUsernamePWDBName[1].getText().toString());
							extras.putString("edu.vesit.ams.MainActivity.teacherSelectedDBName", editTextUsernamePWDBName[2].getText().toString());
							extras.putParcelableArrayList("edu.vesit.ams.MainActivity.allPresets", allPresets);
							extras.putParcelableArrayList("edu.vesit.ams.MainActivity.teacherAllotedPresets", teacherAllotedPresets);
							extras.putParcelableArrayList("edu.vesit.ams.MainActivity.teacherNonAllotedPresets", teacherNonAllotedPresets);
							i.putExtras(extras);
							startActivity(i);
						}else
							Toast.makeText(getBaseContext(), "No Presets Data Found, to resolve login at server 1st and then update/Fetch Latest Presets", Toast.LENGTH_SHORT).show();
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
				scrollView.addView(wrappingLinearLayout);;
				builder.setView(scrollView);
				builder.create().show();
			}
		});
		
		/*******************************************************************************************************************************************************/
		
		btnGotoSettings.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startActivity(new Intent("edu.vesit.ams.SettingsActivity"));
			}
		});
		
		/*******************************************************************************************************************************************************/
		
		btnViewPerformedAttendanceOperations.setOnClickListener(new View.OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				if(appDataTeachersAllotedPresetHistoryAttendanceStringsAppFile.exists() && appDataTeachersNonAllotedPresetHistoryAttendanceStringsAppFile.exists())
					startActivity(new Intent("edu.vesit.ams.ViewPerformedAttendanceOperationsActivity"));
				else
					Toast.makeText(getBaseContext(), "No DATA found! Perform some operations 1st and come back.", Toast.LENGTH_SHORT).show();
			}
		});
		
		/*******************************************************************************************************************************************************/
		
		btnAboutApp.setOnClickListener(new View.OnClickListener()
		{	
			@Override
			public void onClick(View v)
			{
				startActivity(new Intent("edu.vesit.ams.AboutAppActivity"));
			}
		});
		
		btnDisplayAppUsageInstructions.setOnClickListener(new View.OnClickListener()
		{	
			@Override
			public void onClick(View v)
			{
				startActivity(new Intent("edu.vesit.ams.DispAppUsageInstructionsActivity"));
			}
		});
		
		/*******************************************************************************************************************************************************/
	}

	private void initViews()
	{
		btnLogin = (Button)findViewById(R.id.btnLogin);
		btnViewPresets = (Button)findViewById(R.id.btnViewPresets);
		btnViewPerformedAttendanceOperations = (Button)findViewById(R.id.btnViewPerformedAttendanceOperations);
		btnGotoSettings = (Button)findViewById(R.id.btnGotoSettings);
		btnAboutApp = (Button)findViewById(R.id.btnAboutApp);
		btnDisplayAppUsageInstructions = (Button)findViewById(R.id.btnAboutDeveloper);
	}
}
