package edu.vesit.ams;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

public class DispAppUsageInstructionsActivity extends Activity
{
	TextView txtViewAppUsageInstructions;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		boolean customTitleSupported = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_disp_app_usage_instructions);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		if(customTitleSupported)
		{
			getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);
			TextView myTitleText = (TextView)findViewById(R.id.myTitle);
			myTitleText.setText("Usage Instructions");
			//myTitleText.setTextColor(Color.rgb(0,0,139));
		}
		
		getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.bg));
		
		/*******************************************************************************************************************************************************/
		
		initViews();
		
		/*******************************************************************************************************************************************************/
		
		String appUsageInstructionsText = "AMS (Attendance Monitoring System) Instructions are as follows: ";
		txtViewAppUsageInstructions.setText(appUsageInstructionsText);
		
		/*******************************************************************************************************************************************************/
	}

	private void initViews()
	{
		txtViewAppUsageInstructions = (TextView)findViewById(R.id.txtViewAppUsageInstructions);
	}
}
