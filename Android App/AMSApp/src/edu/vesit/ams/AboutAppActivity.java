package edu.vesit.ams;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

public class AboutAppActivity extends Activity
{
	TextView txtViewAboutAMS, txtViewAboutDeveloper;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		boolean customTitleSupported = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_about_app);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		if(customTitleSupported)
		{
			getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);
			TextView myTitleText = (TextView)findViewById(R.id.myTitle);
			myTitleText.setText("About AMS");
			//myTitleText.setTextColor(Color.rgb(0,0,139));
		}
		
		getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.bg));
		
		/*******************************************************************************************************************************************************/
		
		initViews();
		
		/*******************************************************************************************************************************************************/
		
		String aboutAppText = "AMS (Attendance Monitoring System)";
		txtViewAboutAMS.setText(aboutAppText);
		
		String aboutDeveloperText = "Developer:	Ritesh Talreja";
		txtViewAboutDeveloper.setText(aboutDeveloperText);
		
		/*******************************************************************************************************************************************************/
	}

	private void initViews()
	{
		txtViewAboutAMS = (TextView)findViewById(R.id.txtViewAboutAMS);
		txtViewAboutDeveloper = (TextView)findViewById(R.id.txtViewAboutDeveloper);
	}
}
