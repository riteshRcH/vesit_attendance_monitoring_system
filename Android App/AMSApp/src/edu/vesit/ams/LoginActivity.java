package edu.vesit.ams;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity 
{
	boolean serverConnEstablished = false;
	static String tchrID = "", password = "";
	static String serverIP = "";
	static ObjectOutputStream toServerOOStrm;
	static ObjectInputStream fromServerOIS;
	static Socket clientSocket;
	
	AutoCompleteTextView autoCompleteTxtViewServerIP, autoCompleteTxtViewDBName, autoCompleteTxtViewTchrUserName;
	EditText editTextPW;
	Button btnLogin, btnStart, btnEstablishConn;
	CheckBox chkBoxShowPassword, chkBoxRememberMe, chkBoxRememberServerIP;
	
	ArrayAdapter<String> adapterSavedServerIPs; 
	ArrayList<String> allSavedServerIPAddresses = new ArrayList<String>();
	String lastUsedServerIPAddress = "";
	
	LoginCredential lastUsedLoginCredential;
	ArrayList<LoginCredential> allSavedLoginCredentials = new ArrayList<LoginCredential>();
	ArrayAdapter<String> adapterSavedTeacherUsenames, adapterSavedDBNames;
	
	private SecretKeySpec secretKey = null;         	// in java.security
	private Cipher cipher = null;        		 		// in javax.crypto
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		boolean customTitleSupported = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_login);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		if(customTitleSupported)
		{
			getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);
			TextView myTitleText = (TextView) findViewById(R.id.myTitle);
			myTitleText.setText("Login");
			//myTitleText.setTextColor(Color.rgb(0,0,139));
		}
		
		getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.bg));
		
		/*************************************************************************************************************************************/
		
		initViews();
		initEncryptorDecryptor();
		
		/*************************************************************************************************************************************/
		
		InputFilter IpAddrRegexfilter = new InputFilter()
		{
			@Override
		    public CharSequence filter(CharSequence source, int start, int end, android.text.Spanned dest, int dstart, int dend) 
		    {
				if (end > start) 
		        {
					String destTxt = dest.toString();
		            String resultingTxt = destTxt.substring(0, dstart) + source.subSequence(start, end) + destTxt.substring(dend);
		            if(!resultingTxt.matches ("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?"))
		            	return "";
		            else
		            {
		            	String[] splits = resultingTxt.split("\\.");
		                for(int i=0; i<splits.length; i++)
		                	if(Integer.valueOf(splits[i]) > 255)
		                		return "";
		            }
		        }
				return null;
			}
		};
		autoCompleteTxtViewServerIP.setFilters(new InputFilter[]{IpAddrRegexfilter});
		    
		/*************************************************************************************************************************************/
		
		chkBoxShowPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{	
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				editTextPW.setTransformationMethod(isChecked?null:new PasswordTransformationMethod());
			}
		});
		
		/*************************************************************************************************************************************/
		
		getSavedServerIPAddresses();
		getSavedLoginCredentials();
		
		/*************************************************************************************************************************************/
		    
	    btnEstablishConn.setOnClickListener(new View.OnClickListener()
	    {
			@Override
			public void onClick(View v) 
			{
				ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
			    NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

			    if (mWifi.isConnected())
			    {
			    	AsyncTask<Void, Void, Exception> bgTaskEstablishConnToServer = new AsyncTask<Void, Void, Exception>()
			    	{
			    		ProgressDialog progressDialog = null;
			    	    	       				
			    	    @Override
			    	    protected void onPreExecute()
			    	    {
			    	    	progressDialog = new ProgressDialog(LoginActivity.this);
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
			    	    		clientSocket = new Socket();
			    	    		clientSocket.connect(new InetSocketAddress(serverIP = autoCompleteTxtViewServerIP.getText().toString(), 12589), 2000);	//2secs connection timeout
			    	    		clientSocket.setSoTimeout(5000);			//5secs read timeout
			    	    		toServerOOStrm = new ObjectOutputStream(clientSocket.getOutputStream());
			    	    		fromServerOIS = new ObjectInputStream(clientSocket.getInputStream());
			    	    		toServerOOStrm.writeObject("Client: Hello!");
			    	    		toServerOOStrm.flush();
			    	    		Object serverReply = (String)fromServerOIS.readObject();
			    	    		if(serverReply!=null)
			    	    			serverConnEstablished = serverReply.equals("Server: Hello!");
			    	    		if(serverConnEstablished && chkBoxRememberServerIP.isChecked())
		    	    				saveServerIPAddress();
			    	    	}catch(Exception e) 
			    	    	{
			    	    		e.printStackTrace();
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
								if(serverConnEstablished)
								{
									Toast.makeText(getBaseContext(), "Server-Client Handshake successful!", Toast.LENGTH_SHORT).show();
									btnEstablishConn.setEnabled(false);
		    	    				autoCompleteTxtViewServerIP.setEnabled(false);
		    	    				btnLogin.setEnabled(true);
		    	    				chkBoxRememberServerIP.setEnabled(false);
								}else
									Toast.makeText(getBaseContext(), "An Error occured establishing connection to server .. please re-try!", Toast.LENGTH_SHORT).show();
							}else if(exceptionOccured instanceof UnknownHostException)
								Toast.makeText(getBaseContext(), "Cant find server .. contact your class CSI co-ords!", Toast.LENGTH_SHORT).show();
							else if(exceptionOccured instanceof IOException)
								Toast.makeText(getBaseContext(), "Exception occured while connecting .. please retry.\nException Details: "+exceptionOccured.getMessage(), Toast.LENGTH_SHORT).show();
							else
								Toast.makeText(getBaseContext(), "Exception occured while connecting .. please retry.\nException Details: "+exceptionOccured.getMessage(), Toast.LENGTH_LONG).show();
						}
			    	};
			    	bgTaskEstablishConnToServer.execute();
			    }else
			    	Toast.makeText(getBaseContext(), "Please connect to a VESIT hosted WiFi network!", Toast.LENGTH_SHORT).show();
			}
		});
		/*************************************************************************************************************************************/
	    btnLogin.setOnClickListener(new View.OnClickListener()
	    {
			@Override
			public void onClick(View v)
			{
				if(autoCompleteTxtViewTchrUserName.getText().toString().trim().equals(""))
					Toast.makeText(getBaseContext(), "Please enter the teacher ID/Username provided to you.", Toast.LENGTH_SHORT).show();
				/*else if(editTextTeacherID.getText().toString().trim().startsWith("TCHR_"))
					tchrID = editTextTeacherID.getText().toString();
				else
					Toast.makeText(getBaseContext(), "Invalid user name", Toast.LENGTH_SHORT).show();*/
				else
					tchrID = autoCompleteTxtViewTchrUserName.getText().toString();
				
				if(!tchrID.trim().equals(""))
				{
					if(editTextPW.getText().toString().trim().equals(""))
						Toast.makeText(getBaseContext(), "Please enter password", Toast.LENGTH_SHORT).show();
					else
					{
						ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
					    NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
					    
					    if(mWifi.isConnected())
					    {
					    	AsyncTask<Void, Void, Exception> bgTaskLoginTchr = new AsyncTask<Void, Void, Exception>()
							{
					    		ProgressDialog progressDialog = null;
							    	    	       				
							    @Override
							    protected void onPreExecute()
							    {
							    	progressDialog = new ProgressDialog(LoginActivity.this);
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
							    		toServerOOStrm.writeObject("LOGIN "+autoCompleteTxtViewTchrUserName.getText().toString()+"~"+editTextPW.getText().toString()+"~"+autoCompleteTxtViewDBName.getText().toString());
							    		toServerOOStrm.flush();
							    		String serverReply = (String)fromServerOIS.readObject();
							    		if(serverReply!=null)
							    		{
							    			Log.d("server reply", serverReply);
							    			tchrID = serverReply.equals(autoCompleteTxtViewTchrUserName.getText().toString()+" LOGIN Success!")?autoCompleteTxtViewTchrUserName.getText().toString():"";
							    		}
							    		if(!tchrID.equals("") && chkBoxRememberMe.isChecked())
					    					saveLoginCredentials();
							    	}catch(Exception e) 
							    	{
							    		e.printStackTrace();
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
										if(tchrID.equals(""))
											Toast.makeText(getBaseContext(), "Unable to login .. make sure its typed correctly!\n\nOR\n\nLogins are currently disabled at server, contact admin!", Toast.LENGTH_SHORT).show();
										else
										{
											Toast.makeText(getBaseContext(), "Teacher ID: "+tchrID+" successfully logged in at Server!", Toast.LENGTH_SHORT).show();
											password = editTextPW.getText().toString();
						    				btnLogin.setEnabled(false);
						    				editTextPW.setEnabled(false);
						    				autoCompleteTxtViewTchrUserName.setEnabled(false);
						    				autoCompleteTxtViewDBName.setEnabled(false);
						    				chkBoxRememberMe.setEnabled(false);
						    				btnStart.setEnabled(true);
										}
									}else
										Toast.makeText(getBaseContext(), "Exception occured while connecting .. please retry.\nException Details: "+exceptionOccured.getMessage(), Toast.LENGTH_SHORT).show();
								}
							};
						bgTaskLoginTchr.execute();
					    }
					}
				}
			}
		});
	    /*************************************************************************************************************************************/
	    btnStart.setOnClickListener(new View.OnClickListener()
	    {
			@Override
			public void onClick(View v)
			{
				Intent i = new Intent("edu.vesit.ams.ChoosePresetsActivity");
				
				Bundle extras = new Bundle();
				extras.putString("DBName", autoCompleteTxtViewDBName.getText().toString());
				extras.putString("teacherUsername", autoCompleteTxtViewTchrUserName.getText().toString());
				extras.putString("teacherPlainPW", editTextPW.getText().toString());
				i.putExtras(extras);
				
				startActivity(i);
			}
		});
	}
	private void initEncryptorDecryptor()
	{
		try
		{
			// Generate a secret key for a symmetric algorithm and
			// create a Cipher instance. 
			//cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			
			//secretKey = new SecretKeySpec("thisIsASecretKey".getBytes(), "AES");
			secretKey = new SecretKeySpec("cooperHawkAESKey".getBytes(), "AES");
		}catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}catch (NoSuchPaddingException e)
		{
			e.printStackTrace();
		}
	}
	public String encryptParam(String plainText)throws Exception
	{
		cipher.init(Cipher.ENCRYPT_MODE, secretKey);
		return Base64.encodeToString(cipher.doFinal(plainText.getBytes()), Base64.DEFAULT);
	}
	public String decryptParam(String cipherText)throws Exception
	{
		cipher.init(Cipher.DECRYPT_MODE, secretKey);
		return new String(cipher.doFinal(Base64.decode(cipherText, Base64.DEFAULT)));
	}
	private void getSavedServerIPAddresses()
	{
		try
		{
			allSavedServerIPAddresses.clear();
			adapterSavedServerIPs.clear();
			
			if(!MainActivity.XMLSavedServerIPAddressesFile.getParentFile().exists())
				MainActivity.XMLSavedServerIPAddressesFile.getParentFile().mkdirs();
			if(!MainActivity.XMLSavedServerIPAddressesFile.exists())
			{
				MainActivity.XMLSavedServerIPAddressesFile.createNewFile();
				FileWriter fw = new FileWriter(MainActivity.XMLSavedServerIPAddressesFile);
				fw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>"+System.getProperty("line.separator")+"<SavedIPs>"+System.getProperty("line.separator")+"</SavedIPs>");
				fw.flush();
				fw.close();
			}
			
			XmlPullParser xpp = Xml.newPullParser();
			xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
		    xpp.setInput(new FileReader(MainActivity.XMLSavedServerIPAddressesFile));
		
		    int eventType = xpp.getEventType();
		    while(eventType != XmlPullParser.END_DOCUMENT)
		    {
		        if (eventType == XmlPullParser.START_TAG)
		            if (xpp.getName().equals("SavedIP"))
		            {
		            	String serverIP = "";
		            	if(xpp.getAttributeValue(null, "lastUsed").equals("true"))
		            		lastUsedServerIPAddress = (serverIP = xpp.nextText());
		            	else
		            		serverIP = xpp.nextText();
		            	allSavedServerIPAddresses.add(serverIP);
		            	adapterSavedServerIPs.add(serverIP);
		            }
		        eventType = xpp.next(); //move to next element
		    }
		    
		    autoCompleteTxtViewServerIP.setAdapter(adapterSavedServerIPs);
		    autoCompleteTxtViewServerIP.setOnItemClickListener(new AdapterView.OnItemClickListener()
		    {
				@Override
				public void onItemClick(AdapterView<?> parent, View v, int position, long id)
				{
					lastUsedServerIPAddress = adapterSavedServerIPs.getItem(position).toString().trim();
				}
			});
		    autoCompleteTxtViewServerIP.setText(lastUsedServerIPAddress);
		}catch(Exception e)
		{
			e.printStackTrace();
			Toast.makeText(getBaseContext(), "Exception occured while retrieving saved IP Addresses .. \nException Details: "+(e==null?"No error description found":e.getMessage()), Toast.LENGTH_SHORT).show();
		}
	}
	private void saveServerIPAddress()throws Exception
	{
		if(!MainActivity.XMLSavedServerIPAddressesFile.getParentFile().exists())
			MainActivity.XMLSavedServerIPAddressesFile.getParentFile().mkdirs();
		if(!MainActivity.XMLSavedServerIPAddressesFile.exists())
			MainActivity.XMLSavedServerIPAddressesFile.createNewFile();
		
		allSavedServerIPAddresses.add(lastUsedServerIPAddress = autoCompleteTxtViewServerIP.getText().toString().trim());
		allSavedServerIPAddresses = new ArrayList<String>(new HashSet<String>(allSavedServerIPAddresses));
		Collections.sort(allSavedServerIPAddresses);
		StringBuffer toWriteIntoFile = new StringBuffer("<?xml version=\"1.0\" encoding=\"utf-8\"?>"+System.getProperty("line.separator")+"<SavedIPs>"+System.getProperty("line.separator"));
		for(String ip:allSavedServerIPAddresses)
			if(!ip.trim().equals("") && ip.trim().length()>0)
				toWriteIntoFile.append("<SavedIP lastUsed=\""+Boolean.toString(lastUsedServerIPAddress.equals(ip))+"\">"+ip+"</SavedIP>"+System.getProperty("line.separator"));
		toWriteIntoFile.append("</SavedIPs>");
		FileWriter fw = new FileWriter(MainActivity.XMLSavedServerIPAddressesFile);
		fw.write(toWriteIntoFile.toString());
		fw.close();
		Log.d("Written", toWriteIntoFile.toString());
	}
	private void getSavedLoginCredentials()
	{
		try
		{
			allSavedLoginCredentials.clear();
			adapterSavedTeacherUsenames.clear();
			adapterSavedDBNames.clear();
			
			if(!MainActivity.XMLsavedLoginCredentialsFile.getParentFile().exists())
				MainActivity.XMLsavedLoginCredentialsFile.getParentFile().mkdirs();
			if(!MainActivity.XMLsavedLoginCredentialsFile.exists())
			{
				MainActivity.XMLsavedLoginCredentialsFile.createNewFile();
				FileWriter fw = new FileWriter(MainActivity.XMLsavedLoginCredentialsFile);
				fw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>"+System.getProperty("line.separator")+"<LoginCredentials>"+System.getProperty("line.separator")+"</LoginCredentials>");
				fw.flush();
				fw.close();
			}
			
			XmlPullParser xpp = Xml.newPullParser();
			xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
		    xpp.setInput(new FileReader(MainActivity.XMLsavedLoginCredentialsFile));
		
		    int eventType = xpp.getEventType();
		    LoginCredential loginCredential = null;
		    boolean insideLoginCredential = false;
		    while(eventType != XmlPullParser.END_DOCUMENT)
		    {
		        if(eventType == XmlPullParser.START_TAG)
		        {
		            if(xpp.getName().equals("LoginCredential"))
		            {
		            	loginCredential = new LoginCredential();
		            	insideLoginCredential = true;
		            }else if(insideLoginCredential && xpp.getName().equals("username"))
		            	loginCredential.setUsername(xpp.nextText());
		            else if(insideLoginCredential && xpp.getName().equals("password"))
		            	loginCredential.setPassword(decryptParam(xpp.nextText()));
		            else if(insideLoginCredential && xpp.getName().equals("dbName"))
		            	loginCredential.setDbName(xpp.nextText());
		            else if(insideLoginCredential && xpp.getName().equals("lastUsed"))
		            	if(xpp.nextText().equals("true"))
		            	{
		            		loginCredential.setLastUsed(true);
		            		lastUsedLoginCredential = loginCredential;
		            	}
		        }else if(eventType == XmlPullParser.END_TAG && xpp.getName().equals("LoginCredential"))
		        {
		        	if(loginCredential!=null)
		        	{
		        		allSavedLoginCredentials.add(loginCredential);
		        		loginCredential = null;
		        	}
		        	insideLoginCredential = false;
		        }
		        eventType = xpp.next(); //move to next element
		    }
		    
		    ArrayList<String> tempUsernames = new ArrayList<String>();
		    ArrayList<String> tempDBNames = new ArrayList<String>();
		    for(LoginCredential lc:allSavedLoginCredentials)
		    {
		    	tempUsernames.add(lc.getUsername());
		    	tempDBNames.add(lc.getDbName());
		    }
		    tempUsernames = new ArrayList<String>(new HashSet<String>(tempUsernames));
		    tempDBNames = new ArrayList<String>(new HashSet<String>(tempDBNames));
		    for(String s:tempUsernames)
		    	adapterSavedTeacherUsenames.add(s);
		    tempUsernames = null;
		    for(String s:tempDBNames)
		    	adapterSavedDBNames.add(s);
		    tempDBNames = null;
		    
		    for(LoginCredential lc:allSavedLoginCredentials)
		    {
		    	if(lc.getLastUsed())
		    	{
		    		autoCompleteTxtViewTchrUserName.setText(lc.getUsername());
		    		editTextPW.setText(lc.getPassword());
		    		autoCompleteTxtViewDBName.setText(lc.getDbName());
		    		lastUsedLoginCredential = lc;
		    		break;
		    	}
		    }
		    
		    autoCompleteTxtViewTchrUserName.setAdapter(adapterSavedTeacherUsenames);
		    autoCompleteTxtViewDBName.setAdapter(adapterSavedDBNames);
		    autoCompleteTxtViewTchrUserName.setOnItemClickListener(new AdapterView.OnItemClickListener()
		    {
				@Override
				public void onItemClick(AdapterView<?> parent, View v, int position, long id)
				{
					String selectedUsername = adapterSavedTeacherUsenames.getItem(position).toString().trim();
					for(LoginCredential lc:allSavedLoginCredentials)
				    {
				    	if(lc.getUsername().equals(selectedUsername))
				    	{
				    		autoCompleteTxtViewTchrUserName.setText(lc.getUsername());
				    		editTextPW.setText(lc.getPassword());
				    		autoCompleteTxtViewDBName.setText(lc.getDbName());
				    		lastUsedLoginCredential = lc;
				    		break;
				    	}
				    }
				}
			});
		}catch(Exception e)
		{
			e.printStackTrace();
			Toast.makeText(getBaseContext(), "Exception occured while retrieving saved Login Credentials .. \nException Details: "+(e==null?"No error description found":e.getMessage()), Toast.LENGTH_SHORT).show();
		}
	}
	private void saveLoginCredentials()throws Exception
	{
		//save login credentials(username, plain password encrypted using AES(Eg present at desktop), DBName, true/false(true for only 1 => last used => auto-loaded in fields next time))	=> use autocompletetextview to choose the list of usernames
		if(!MainActivity.XMLsavedLoginCredentialsFile.getParentFile().exists())
			MainActivity.XMLsavedLoginCredentialsFile.getParentFile().mkdirs();
		if(!MainActivity.XMLsavedLoginCredentialsFile.exists())
			MainActivity.XMLsavedLoginCredentialsFile.createNewFile();
		
		LoginCredential lc = new LoginCredential();
		lc.setUsername(autoCompleteTxtViewTchrUserName.getText().toString().trim());
		lc.setPassword(editTextPW.getText().toString().trim());
		lc.setDbName(autoCompleteTxtViewDBName.getText().toString().trim());
		lc.setLastUsed(true);
		
		//remove all rest login credentials which have ditto same username, password, dbName as that of lc and has false lastUsed => instead put lc that has true lastUsed
		for(int i=0;i<allSavedLoginCredentials.size();i++)
			if(allSavedLoginCredentials.get(i).getUsername().equals(lc.getUsername()) && allSavedLoginCredentials.get(i).getPassword().equals(lc.getPassword()) && allSavedLoginCredentials.get(i).getDbName().equals(lc.getDbName()))
				allSavedLoginCredentials.remove(i);
		
		if(!allSavedLoginCredentials.contains(lc))
		{
			for(LoginCredential loginc:allSavedLoginCredentials)
				loginc.setLastUsed(false);
			allSavedLoginCredentials.add(lastUsedLoginCredential = lc);
		}
		//allSavedLoginCredentials = new ArrayList<LoginCredential>(new HashSet<LoginCredential>(allSavedLoginCredentials));
		StringBuffer toWriteIntoFile = new StringBuffer("<?xml version=\"1.0\" encoding=\"utf-8\"?>"+System.getProperty("line.separator")+"<LoginCredentials>"+System.getProperty("line.separator"));
		for(LoginCredential loginCredential:allSavedLoginCredentials)
		{
			toWriteIntoFile.append("<LoginCredential>"+System.getProperty("line.separator"));
			toWriteIntoFile.append("<username>"+loginCredential.getUsername()+"</username>"+System.getProperty("line.separator"));
			toWriteIntoFile.append("<password>"+encryptParam(loginCredential.getPassword())+"</password>"+System.getProperty("line.separator"));
			toWriteIntoFile.append("<dbName>"+loginCredential.getDbName()+"</dbName>"+System.getProperty("line.separator"));
			toWriteIntoFile.append("<lastUsed>"+loginCredential.getLastUsed()+"</lastUsed>"+System.getProperty("line.separator"));
			toWriteIntoFile.append("</LoginCredential>"+System.getProperty("line.separator"));
		}
		toWriteIntoFile.append("</LoginCredentials>");
		FileWriter fw = new FileWriter(MainActivity.XMLsavedLoginCredentialsFile);
		fw.write(toWriteIntoFile.toString());
		fw.close();
		Log.d("Written", toWriteIntoFile.toString());
	}
	private void initViews()
	{
		autoCompleteTxtViewServerIP = (AutoCompleteTextView)findViewById(R.id.autoCompleteTxtViewServerIP);
		editTextPW = (EditText)findViewById(R.id.editTextPW);
		autoCompleteTxtViewTchrUserName = (AutoCompleteTextView)findViewById(R.id.autoCompleteTxtViewTchrUserName);
	    autoCompleteTxtViewDBName = (AutoCompleteTextView)findViewById(R.id.autoCompleteTxtViewDBName);
	    btnLogin = (Button)findViewById(R.id.btnLogin);
	    btnStart = (Button)findViewById(R.id.btnStart);
	    btnEstablishConn = (Button)findViewById(R.id.btnEstablishConn);
	    chkBoxShowPassword = (CheckBox)findViewById(R.id.chkBoxShowPassword);
	    chkBoxRememberMe = (CheckBox)findViewById(R.id.chkBoxRememberMe);
	    chkBoxRememberServerIP = (CheckBox)findViewById(R.id.chkBoxRememberServerIP);
	    
	    autoCompleteTxtViewServerIP.setThreshold(1);
	    adapterSavedServerIPs = new ArrayAdapter<String>(LoginActivity.this, android.R.layout.simple_dropdown_item_1line);
	    
	    autoCompleteTxtViewTchrUserName.setThreshold(1);
	    autoCompleteTxtViewDBName.setThreshold(1);
	    adapterSavedTeacherUsenames = new ArrayAdapter<String>(LoginActivity.this, android.R.layout.simple_dropdown_item_1line);
	    adapterSavedDBNames = new ArrayAdapter<String>(LoginActivity.this, android.R.layout.simple_dropdown_item_1line);
	}
}