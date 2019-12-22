package com.example.tryout;

import java.io.File;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Button btn = (Button)findViewById(R.id.button1);
		btn.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// TODO Auto-generated method stub
				try {
					CodecXMLEXI.decodeEXIToXMLSchemaLess(Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"presets_allotment.exi", Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"presets_allotment.xml");
					//String xml = DecodeEXIToXML.decodeEXIToXML(Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"presets_allotment.exi");
					//Toast.makeText(getBaseContext(), xml, Toast.LENGTH_LONG).show();
					} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(new File(Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"presets_allotment.xml").exists())
					Toast.makeText(getBaseContext(), "Success", Toast.LENGTH_SHORT).show();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
