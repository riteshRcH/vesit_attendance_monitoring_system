package edu.vesit.ams;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity
{
	Button btnStartNewFreshAcademicYearByDeletingAppsSDCARDFolder;//, btnChooseRememberMeEncryptionAlgo;
	//String rememberMeEncryptionAlgo = "AES", rememberMeEncryptionAlgos[] = {"DES", "DESede", "AES"};
	//Taken AES only, Plain password -> AES Encrypt -> Base64 Encode -> XML -> Base64 Decode -> AES Decrypt -> Plain Password 
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		boolean customTitleSupported = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_settings);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		if(customTitleSupported)
		{
			getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);
			TextView myTitleText = (TextView)findViewById(R.id.myTitle);
			myTitleText.setText("Settings");
			//myTitleText.setTextColor(Color.rgb(0,0,139));
		}
		
		initViews();
		//getCurrentRememberMeEncryptionAlgo();
		
		btnStartNewFreshAcademicYearByDeletingAppsSDCARDFolder.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
				builder.setTitle("Start Fresh Academic Year");
				builder.setMessage("Are you REALLY sure you want to start a fresh new academic year?"+System.getProperty("line.separator")+" All server received Data(Presets etc) and start, end times would be deleted..");
				builder.setPositiveButton("Yes", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						ChoosePresetsManipAttendanceActivity.deleteFolderContents(MainActivity.AMSAppDir);
						Toast.makeText(getBaseContext(), "Successful in starting a new academic year", Toast.LENGTH_SHORT).show();
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
				builder.create().show();
			}
		});
		/*btnChooseRememberMeEncryptionAlgo.setOnClickListener(new View.OnClickListener()
		{	
			@Override
			public void onClick(View v)
			{
				try
				{
					if(MainActivity.XMLConfigParamsAppFile.exists())
					{
						Arrays.sort(rememberMeEncryptionAlgos);
					    
					    AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
					    builder.setTitle("Choose Password Protection level ..");
					    builder.setSingleChoiceItems(rememberMeEncryptionAlgos, Arrays.binarySearch(rememberMeEncryptionAlgos, rememberMeEncryptionAlgo), new DialogInterface.OnClickListener()
					    {
							@Override
							public void onClick(DialogInterface dialog, int which)
							{
								btnChooseRememberMeEncryptionAlgo.setText("Choose \"Remember Me\" password encryption(Current="+(rememberMeEncryptionAlgo = rememberMeEncryptionAlgos[which])+")");
								writeParamsToFile();
								dialog.dismiss();
							}
						});
					    builder.create().show();
					}					
				}catch(Exception e)
				{
					e.printStackTrace();
					Toast.makeText(getBaseContext(), "An exception ocurred, details: "+e.getMessage()+" .. retry!", Toast.LENGTH_SHORT).show();
					
				}
			}
		});*/
	}
	/*private void getCurrentRememberMeEncryptionAlgo()
	{
		try
		{
			if(MainActivity.XMLConfigParamsAppFile.exists())
			{
				XmlPullParser xpp = Xml.newPullParser();
				xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			    xpp.setInput(new FileReader(MainActivity.XMLConfigParamsAppFile));
			
			    int eventType = xpp.getEventType();
			    while(eventType != XmlPullParser.END_DOCUMENT)
			    {
			        if (eventType == XmlPullParser.START_TAG)
			        {
			            if (xpp.getName().equals("RememberMeEncryptionAlgo"))
			            	rememberMeEncryptionAlgo = xpp.nextText();
			        }
			        eventType = xpp.next(); //move to next element
			    }
			    btnChooseRememberMeEncryptionAlgo.setText("Choose \"Remember Me\" password encryption(Current="+rememberMeEncryptionAlgo+")");
			}else
			{
				MainActivity.XMLConfigParamsAppFile.createNewFile();
				writeParamsToFile();									//Default Values written out
				getCurrentRememberMeEncryptionAlgo();
			}
		}catch(Exception e)
		{
			Toast.makeText(SettingsActivity.this, "An error occured while retrieving current protection", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
	}
	private void writeParamsToFile()
	{
		try
		{
			PrintWriter pw = new PrintWriter(MainActivity.XMLConfigParamsAppFile);
			pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
			pw.println("<appConfigParams>");
			pw.println("<RememberMeEncryptionAlgo>"+rememberMeEncryptionAlgo+"</RememberMeEncryptionAlgo>");
			pw.println("</appConfigParams>");
			pw.close();
		}catch(Exception e)
		{
			Toast.makeText(getBaseContext(), "An exception ocurred while saving your settings .. retry!", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
	}*/
	private void initViews()
	{
		btnStartNewFreshAcademicYearByDeletingAppsSDCARDFolder = (Button)findViewById(R.id.btnStartNewFreshAcademicYearByDeletingAppsSDCARDFolder);
		//btnChooseRememberMeEncryptionAlgo = (Button)findViewById(R.id.btnStartChooseRememberMeEncryptionAlgo);
	}
}
