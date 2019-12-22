package edu.vesit.ams;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.vesit.DBDataHelper.AttendanceStringOperationExecutor;
import edu.vesit.DBDataHelper.XMLGenerator;
import edu.vesit.ams.AndroidLikeToast.Style;

@SuppressWarnings("serial")
public class AMSServer extends JFrame
{
	final JButton jbtnStartStopServer, jbtnAboutAMSServer, jbtnShowServerConfig, jbtnInvokeDBModder, jbtnStartStopLoginHandler, jbtnStartStopAttendanceStringOperationExecutor, jbtnCreateXMLZip, jbtnStartStopTriggerStringOperationExecutor, jbtnCalculateDefaulters, jbtnActivateTriggersNow, jbtnScheduleTriggersActivationForToday, jbtnCancelScheduledTriggerActivation;
	JCheckBox jchkboxAppend, jchkBoxActivateBeforeTodayTriggers, jchkBoxDeleteWhenTriggerActivatedSuccessfully;
	
	JSpinner jSpinnerTodaysTriggerActivationTimeTime;
	boolean scheduledTriggerActivation = false;
	Timer timer;
	TimerTask triggerExecutionTimerTask;
	
	ClientConnListeningThread t = new ClientConnListeningThread();
	edu.vesit.DBDataHelper.LoginHandler loginHandler = null;
	edu.vesit.DBDataHelper.AttendanceStringOperationExecutor attendanceStringOperationExecutor = null; 
	edu.vesit.DBDataHelper.TriggerStringOperationExecutor triggerStringOperationExecutor = null;
	LinkedHashMap<String, String> currentlyLoggedInTchrIDs;
	
	JPanel jpRuntimeServerHandlingOperations = new JPanel(new FlowLayout(FlowLayout.CENTER)), jpDBHandlingOperations = new JPanel(new GridLayout(0, 1, 10, 10));
	JTabbedPane jtpOperations = new JTabbedPane(), jtpeventLogs = new JTabbedPane();
	boolean loginHandlerAndExecutorsHaveBeenStarted = false;
	
	JTextField jtxfDBVendor, jtxfDBServerIPPort, jtxfDBNamePrefix, jtxfBatchStartYear, jtxfBatchEndYear;
	
	//public static SimpleDateFormat logEventTimestampFormat = new SimpleDateFormat("dd MMM(MM) yyyy EEE hh:mm:ss:SSS a");
	Logger logger = LoggerFactory.getLogger(AMSServer.class);
	String tempLogMsg = new String();
	final static int SERVER_LISTENING_PORT;
	
	public static File serverDataDir = new File("."+File.separator+"DATA"), classificationDataDir = new File("."+File.separator+"DATA"+File.separator+"Classification"), presetsDataDir = new File("."+File.separator+"DATA"+File.separator+"Presets"), attendanceResultsOutputDataDir = new File("."+File.separator+"DATA"+File.separator+"OUTPUT"+File.separator+"AttendanceResults");
	File serverDataZippedFile = new File(serverDataDir, "serverData.zip");
	
	StringWriter strWriter = new StringWriter();
	
	static
	{
		SERVER_LISTENING_PORT = 12589;
	}
	
	AMSServer()
	{
		super.setLayout(new BorderLayout());
		this.setTitle("Attendance Monitoring System Server! v1.0");
		this.setVisible(true);
		this.setSize(1024, 350);
		this.setLocationRelativeTo(null);														//center locn
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);									//dispose all UI windows
		
		File temp[] = {	serverDataDir, classificationDataDir, presetsDataDir, attendanceResultsOutputDataDir };
		for(File f:temp)
			if(!f.exists())
				f.mkdirs();
		
		jpRuntimeServerHandlingOperations.setSize(200, 500);
		
		jpRuntimeServerHandlingOperations.add(jbtnStartStopServer = new JButton("Start Server!"));
		jpRuntimeServerHandlingOperations.add(jbtnShowServerConfig = new JButton("View Server Config"));
		jpRuntimeServerHandlingOperations.add(jbtnCreateXMLZip = new JButton("Create Presets + Classification XML (and zip it too to export) for all vesitams* DB's"));
		jpRuntimeServerHandlingOperations.add(jbtnAboutAMSServer = new JButton("About AMS"));
		
		JPanel temp1 = new JPanel(new FlowLayout(FlowLayout.CENTER));
		temp1.add(new JLabel("jdbc:"));
		temp1.add(jtxfDBVendor = new JTextField("mysql", 10));
		temp1.add(new JLabel("://"));
		temp1.add(jtxfDBServerIPPort = new JTextField("localhost:3306", 20));
		temp1.add(new JLabel("/"));
		temp1.add(jtxfDBNamePrefix = new JTextField("vesitams", 10));
		temp1.add(jtxfBatchStartYear = new JTextField("2013", 9));
		temp1.add(new JLabel("to"));
		temp1.add(jtxfBatchEndYear = new JTextField("2014", 9));
		temp1.add(jbtnInvokeDBModder = new JButton("DB Operations"));
		temp1.add(new JLabel("Note: the DB Server IP:Port and DB Vendor will be fixed and disabled once login Handler and/or Operation Executors are created"));
		jpDBHandlingOperations.add(temp1);
		JPanel temp2 = new JPanel(new GridLayout(0, 2, 10, 10));
		temp2.add(jbtnStartStopLoginHandler = new JButton("Create and Start Login Handler"));
		temp2.add(jbtnStartStopAttendanceStringOperationExecutor = new JButton("Create and Start AttendanceString Operation Executor"));
		temp2.add(jbtnStartStopTriggerStringOperationExecutor = new JButton("Create and Start TriggerString Operation Executor"));
		temp2.add(jbtnCalculateDefaulters = new JButton("Calculate and Save Defaulters"));
		jpDBHandlingOperations.add(temp2);
		JPanel temp3 = new JPanel(new GridLayout(0, 3, 10, 10));
		temp3.add(jbtnActivateTriggersNow = new JButton("Activate Triggers Now!"));
		temp3.add(jchkBoxActivateBeforeTodayTriggers = new JCheckBox("Activate Before Today("+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.userDisplayAttendanceDateFormat.format(new Date())+") Triggers"));
		temp3.add(jchkBoxDeleteWhenTriggerActivatedSuccessfully = new JCheckBox("Delete Trigger Records once activated successfully", true));
		
		jSpinnerTodaysTriggerActivationTimeTime = new JSpinner(new SpinnerDateModel());
		jSpinnerTodaysTriggerActivationTimeTime.setEditor(new JSpinner.DateEditor(jSpinnerTodaysTriggerActivationTimeTime, "hh:mm a"));
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.HOUR, 1);
		jSpinnerTodaysTriggerActivationTimeTime.setValue(cal.getTime());
		temp3.add(jSpinnerTodaysTriggerActivationTimeTime);
		temp3.add(jbtnScheduleTriggersActivationForToday = new JButton("Schedule Trigger Activation with current params"));
		temp3.add(jbtnCancelScheduledTriggerActivation = new JButton("Cancel Scheduled Trigger Activation"));
		jpDBHandlingOperations.add(temp3);
		
		jtxfDBNamePrefix.setToolTipText("DB Name's Prefix");
		jtxfBatchStartYear.setToolTipText("Batch Year Start");
		jtxfBatchEndYear.setToolTipText("Batch Year End");
		jtxfDBVendor.setToolTipText("Enter DB Vendor Name specific to jdbc connection URL");
		jtxfDBServerIPPort.setToolTipText("Enter DBServerIP:Port at which DBMS is listening for remote connections, note: format in complaince with jdbc connection URL");
		
		jtpOperations.add("Server", jpRuntimeServerHandlingOperations);
		jtpOperations.add("DB", jpDBHandlingOperations);
		jtpOperations.setSelectedIndex(1);
		jtpOperations.addChangeListener(new ChangeListener()
		{	
			@Override
			public void stateChanged(ChangeEvent ce)
			{
				JTabbedPane srcTabbedPane = (JTabbedPane) ce.getSource();
				loginHandlerAndExecutorsHaveBeenStarted = !(attendanceStringOperationExecutor==null || loginHandler==null || triggerStringOperationExecutor==null);
				if(srcTabbedPane.getSelectedIndex()==0 && !loginHandlerAndExecutorsHaveBeenStarted)
				{
					AndroidLikeToast.makeText(AMSServer.this, "Login Handlers and all the executors must be started 1st before starting the main connections server", AndroidLikeToast.LENGTH_SHORT, Style.NORMAL).display();
					srcTabbedPane.setSelectedIndex(1);
				}
			}
		});
		
		this.add(jtpOperations, BorderLayout.CENTER);
		
		ButtonListener bl = new ButtonListener();
		jbtnStartStopServer.addActionListener(bl);
		jbtnAboutAMSServer.addActionListener(bl);
		jbtnShowServerConfig.addActionListener(bl);
		jbtnCreateXMLZip.addActionListener(bl);
		
		jbtnInvokeDBModder.addActionListener(bl);
		jbtnStartStopLoginHandler.addActionListener(bl);
		jbtnStartStopAttendanceStringOperationExecutor.addActionListener(bl);
		jbtnStartStopTriggerStringOperationExecutor.addActionListener(bl);
		jbtnCalculateDefaulters.addActionListener(bl);
		jbtnActivateTriggersNow.addActionListener(bl);
		jbtnScheduleTriggersActivationForToday.addActionListener(bl);
		jbtnCancelScheduledTriggerActivation.addActionListener(bl);
		
		this.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent we)
			{
				if(JOptionPane.YES_OPTION==JOptionPane.showConfirmDialog(AMSServer.this, "Are you sure you want to exit?", "Exit Confirmation", JOptionPane.YES_NO_OPTION))
				{
					//System.out.println("[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer ] Client Connected Sockets were: "+(((t.clientConnectedSockets.size())==0)?"none":t.clientConnectedSockets));
					logger.info("Client Connected Sockets were: "+(((t.clientConnectedSockets.size())==0)?"none":t.clientConnectedSockets));
					t.serverRunning = false;
					try
					{
						if(t.serverSocket!=null)
							t.serverSocket.close();
					}catch(Exception e)
					{
						e.printStackTrace();
					}
					//System.out.println("[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer ] Cya soon! :)");
					if(attendanceStringOperationExecutor!=null)
					{
						attendanceStringOperationExecutor.getDataSource().shutdown();
						logger.info("Successfully shutdown the Attendance StringOperation Executor's DB Connection Pool!");
					}
					if(triggerStringOperationExecutor!=null)
					{
						triggerStringOperationExecutor.getDataSource().shutdown();
						logger.info("Successfully shutdown the Trigger StringOperation Executor's DB Connection Pool!");
					}
					if(loginHandler!=null)
					{
						loginHandler.getDataSource().shutdown();
						logger.info("Successfully shutdown the LoginHandler's DB Connection Pool!");
					}
					dispose();										//dispose off UI window
					System.gc();									//perform garbage collection
					logger.info("Performed Garbage Collection!");
					logger.info("Cya soon! :)");
				}
			}
		});
		
		currentlyLoggedInTchrIDs = new LinkedHashMap<String, String>();
	}
	public static void main(String args[])
	{
		try
		{
			SwingUtilities.invokeAndWait(new Runnable()							//make GUI on event dispatching thread
			{
					public void run()
					{
						try
						{
							UIManager.setLookAndFeel(new NimbusLookAndFeel());
						}catch (UnsupportedLookAndFeelException e)
						{
							e.printStackTrace();
						}
						new AMSServer();
					}
			});
		}catch(InterruptedException intre)
		{
			intre.printStackTrace();
		}catch(InvocationTargetException invocationTargetExcep)
		{
			invocationTargetExcep.printStackTrace();
		}
	}
	void deleteFolderContents(File dir)
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
	void logStackTraceOfException(Exception e)
	{
		strWriter.getBuffer().setLength(0);
		e.printStackTrace(new PrintWriter(strWriter));
		logger.error(strWriter.toString());
	}
	class ButtonListener implements ActionListener
	{	
		public void actionPerformed(ActionEvent ae)
		{
			if(ae.getSource().equals(jbtnStartStopServer) && !t.serverRunning)
			{
				if(serverDataZippedFile.exists())
				{
					t.serverRunning = true;
					t.start();
					jbtnStartStopServer.setEnabled(false);
				}else
					JOptionPane.showMessageDialog(AMSServer.this, "serverData.zip must be prepared 1st in order to start the server!", "Error", JOptionPane.ERROR_MESSAGE);
			}else if(ae.getSource().equals(jbtnAboutAMSServer))
				JOptionPane.showMessageDialog(AMSServer.this, "AMS Server v1.0\nBy Ritesh Talreja", "About AMS", JOptionPane.INFORMATION_MESSAGE);
			else if(ae.getSource().equals(jbtnShowServerConfig))
					JOptionPane.showMessageDialog(null, getCurrentServerConfiguration(), "Server Config", JOptionPane.INFORMATION_MESSAGE);
			else if(ae.getSource().equals(jbtnCreateXMLZip))
			{
				if(JOptionPane.showConfirmDialog(AMSServer.this, "Re-create/update Data from DB i.e to be exported to Clients .. Proceed?", "Warning", JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
				{
					jbtnCreateXMLZip.setEnabled(false);
					
					//deleting all contents of server DATA directory but not the DATA dir, only its contents
					deleteFolderContents(serverDataDir);
					serverDataZippedFile = null;
					
					XMLGenerator xmlMakerInstance = new XMLGenerator(jtxfDBVendor.getText().toString().trim(), jtxfDBServerIPPort.getText().toString().trim());
					if(xmlMakerInstance.createPresetsXMLForAllDB())
					{
						if(xmlMakerInstance.createClassficationXMLs())
						{
							ZipUnzipDir obj = new ZipUnzipDir();
							serverDataZippedFile = obj.createZipOfDir(new File(serverDataDir, "serverData.zip").getAbsolutePath(), serverDataDir);
							if(serverDataZippedFile==null)
								JOptionPane.showMessageDialog(AMSServer.this, "error occured while zipping server DATA directory", "Error", JOptionPane.ERROR_MESSAGE);
							else
							{
								//System.out.println("[ "+AMSServer.logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer] Successfully zipped server DATA directory!");
								logger.info("Successfully zipped server DATA directory!");
								JOptionPane.showMessageDialog(AMSServer.this, "Successfully zipped server DATA directory", "Notify", JOptionPane.INFORMATION_MESSAGE);
							}
							obj = null;
						}else
							JOptionPane.showMessageDialog(AMSServer.this, "error occured while creating classification xmls", "Error", JOptionPane.ERROR_MESSAGE);
					}else
						JOptionPane.showMessageDialog(AMSServer.this, "error occured while creating presets xml", "Error", JOptionPane.ERROR_MESSAGE);
				
					xmlMakerInstance = null;
					
					//unzip testing
					/*if(ZipUnzipDir.unzipToDir(serverDataZippedFile, new File("C:/Users/Ritesh/Desktop/VESIT AMS unzipping testing")))
						System.out.println("unzipping success");*/
				}
				
			}else if(ae.getSource().equals(jbtnInvokeDBModder))
				new edu.vesit.DBDataHelper.MainFrame(jtxfDBVendor.getText().toString().trim(), jtxfDBServerIPPort.getText().toString().trim(), jtxfDBNamePrefix.getText().toString().trim()+Integer.parseInt(jtxfBatchStartYear.getText().toString().trim())+"to"+Integer.parseInt(jtxfBatchEndYear.getText().toString().trim()));
			else if(ae.getSource().equals(jbtnStartStopLoginHandler))
			{
				jtxfDBServerIPPort.setEnabled(false);
				jtxfDBVendor.setEnabled(false);
				
				if(loginHandler==null)
					loginHandler = new edu.vesit.DBDataHelper.LoginHandler(jtxfDBVendor.getText().toString().trim(), jtxfDBServerIPPort.getText().toString().trim(), jtxfDBNamePrefix.getText().toString().trim()+Integer.parseInt(jtxfBatchStartYear.getText().toString().trim())+"to"+Integer.parseInt(jtxfBatchEndYear.getText().toString().trim()));
				else
					loginHandler.toggleAcceptLogins();
				
				jbtnStartStopLoginHandler.setText((jbtnStartStopLoginHandler.getText().startsWith("Start") || jbtnStartStopLoginHandler.getText().startsWith("Create"))?"Stop Login Handler":"Start Login Handler");
				
			}else if(ae.getSource().equals(jbtnStartStopAttendanceStringOperationExecutor))
			{
				jtxfDBServerIPPort.setEnabled(false);
				jtxfDBVendor.setEnabled(false);
				
				if(attendanceStringOperationExecutor==null)
					attendanceStringOperationExecutor = new edu.vesit.DBDataHelper.AttendanceStringOperationExecutor(jtxfDBVendor.getText().toString().trim(), jtxfDBServerIPPort.getText().toString().trim());
				else
					attendanceStringOperationExecutor.toggleAcceptAttendanceStringOperations();
				
				jbtnStartStopAttendanceStringOperationExecutor.setText((jbtnStartStopAttendanceStringOperationExecutor.getText().startsWith("Start") || jbtnStartStopAttendanceStringOperationExecutor.getText().startsWith("Create"))?"Stop AttendanceString Operation Executor":"Start AttendanceString Operation Executor");
			}else if(ae.getSource().equals(jbtnStartStopTriggerStringOperationExecutor))
			{
				jtxfDBServerIPPort.setEnabled(false);
				jtxfDBVendor.setEnabled(false);
				
				if(triggerStringOperationExecutor==null)
					triggerStringOperationExecutor = new edu.vesit.DBDataHelper.TriggerStringOperationExecutor(jtxfDBVendor.getText().toString().trim(), jtxfDBServerIPPort.getText().toString().trim());
				else
					triggerStringOperationExecutor.toggleAcceptTriggerStringOperations();
				
				jbtnStartStopTriggerStringOperationExecutor.setText((jbtnStartStopTriggerStringOperationExecutor.getText().startsWith("Start") || jbtnStartStopTriggerStringOperationExecutor.getText().startsWith("Create"))?"Stop TriggerString Operation Executor":"Start TriggerString Operation Executor");
			}else if(ae.getSource().equals(jbtnCalculateDefaulters))
				new edu.vesit.DBDataHelper.DefaultersCalculator(jtxfDBVendor.getText().toString().trim(), jtxfDBServerIPPort.getText().toString().trim(), jtxfDBNamePrefix.getText().toString().trim()+Integer.parseInt(jtxfBatchStartYear.getText().toString().trim())+"to"+Integer.parseInt(jtxfBatchEndYear.getText().toString().trim()));
			else if(ae.getSource().equals(jbtnActivateTriggersNow))
			{
				jbtnActivateTriggersNow.setEnabled(false);
				if(edu.vesit.DBDataHelper.AttendanceTriggersExecutor.execute(jtxfDBVendor.getText().toString().trim(), jtxfDBServerIPPort.getText().toString().trim(), jtxfDBNamePrefix.getText().toString().trim()+Integer.parseInt(jtxfBatchStartYear.getText().toString().trim())+"to"+Integer.parseInt(jtxfBatchEndYear.getText().toString().trim()), jchkBoxActivateBeforeTodayTriggers.isSelected(), jchkBoxDeleteWhenTriggerActivatedSuccessfully.isSelected()))
					AndroidLikeToast.makeText(AMSServer.this, "Successfully executed today and/or past triggers", AndroidLikeToast.LENGTH_SHORT, Style.SUCCESS).display();
				else
					AndroidLikeToast.makeText(AMSServer.this, "There was an error executing triggers for today, refer log and retry", AndroidLikeToast.LENGTH_SHORT, Style.ERROR).display();
				jbtnActivateTriggersNow.setEnabled(true);
			}else if(ae.getSource().equals(jbtnScheduleTriggersActivationForToday))
			{
				/*if(new Date().after((Date) jSpinnerTodaysTriggerActivationTimeTime.getValue()))
					AndroidLikeToast.makeText(AMSServer.this, "Trigger Activation scheduled time must be a time in future!", AndroidLikeToast.LENGTH_SHORT).display();
				else
				{*/
					long l;
					timer = new Timer();
					Calendar cal1 = Calendar.getInstance(), cal2 = Calendar.getInstance();
					cal1.setTimeInMillis(System.currentTimeMillis());
					
					Date d = (Date) jSpinnerTodaysTriggerActivationTimeTime.getValue();
					cal2.setTime(d);
					cal2.set(Calendar.DAY_OF_MONTH, cal1.get(Calendar.DAY_OF_MONTH));
					cal2.set(Calendar.MONTH, cal1.get(Calendar.MONTH));
					cal2.set(Calendar.YEAR, cal1.get(Calendar.YEAR));
					l=cal2.getTimeInMillis()-cal1.getTimeInMillis();
					timer.schedule(triggerExecutionTimerTask = new TimerTaskExecuteTriggers(jtxfDBVendor.getText().toString().trim(), jtxfDBServerIPPort.getText().toString().trim(), jtxfDBNamePrefix.getText().toString().trim()+Integer.parseInt(jtxfBatchStartYear.getText().toString().trim())+"to"+Integer.parseInt(jtxfBatchEndYear.getText().toString().trim()), jchkBoxActivateBeforeTodayTriggers.isSelected(), jchkBoxDeleteWhenTriggerActivatedSuccessfully.isSelected()), l);
					scheduledTriggerActivation = true;
					AndroidLikeToast.makeText(AMSServer.this, "Trigger Activation scheduled  successfully for future after "+((l/1000)/60)+" minutes", AndroidLikeToast.LENGTH_LONG).display();
				//}
			}else if(ae.getSource().equals(jbtnCancelScheduledTriggerActivation))
			{
				if(scheduledTriggerActivation)
				{
					timer.cancel();
					triggerExecutionTimerTask.cancel();
					AndroidLikeToast.makeText(AMSServer.this, "Scheduled Trigger Activation successfully canceled!", AndroidLikeToast.LENGTH_SHORT).display();
				}else
					AndroidLikeToast.makeText(AMSServer.this, "Trigger Activation not scheduled yet!", AndroidLikeToast.LENGTH_SHORT).display();
			}
		}
	}
	class TimerTaskExecuteTriggers extends TimerTask
	{
		String DBVendor, DBServerIPPort, DBName;
		boolean activateBeforeTodayTriggers,  deleteWhenTriggerActivatedSuccessfully;
		
		TimerTaskExecuteTriggers(String DBVendor, String DBServerIPPort, String DBName, boolean activateBeforeTodayTriggers, boolean deleteWhenTriggerActivatedSuccessfully)
		{
			this.DBVendor = DBVendor;
			this.DBServerIPPort = DBServerIPPort;
			this.DBName = DBName;
			this.activateBeforeTodayTriggers = activateBeforeTodayTriggers;
			this.deleteWhenTriggerActivatedSuccessfully = deleteWhenTriggerActivatedSuccessfully;
		}
		
		@Override
		public void run()
		{
			jbtnActivateTriggersNow.setEnabled(false);
			if(edu.vesit.DBDataHelper.AttendanceTriggersExecutor.execute(DBVendor, DBServerIPPort, DBName, activateBeforeTodayTriggers, deleteWhenTriggerActivatedSuccessfully))
				AndroidLikeToast.makeText(AMSServer.this, "Successfully executed today and/or past triggers", AndroidLikeToast.LENGTH_SHORT, Style.SUCCESS).display();
			else
				AndroidLikeToast.makeText(AMSServer.this, "There was an error executing triggers for today, refer log and retry", AndroidLikeToast.LENGTH_SHORT, Style.ERROR).display();
			jbtnActivateTriggersNow.setEnabled(true);
			timer.cancel();
			triggerExecutionTimerTask.cancel();
		}
		
	}
	String getCurrentServerConfiguration()
	{
		String configStr = "";
		try
		{
			configStr += "This Machine's Pvt IP: \t\t"+InetAddress.getLocalHost().toString()+"\t"+System.getProperty("line.separator");
			configStr += "Is Server Running: \t\t"+t.serverRunning+"\t"+System.getProperty("line.separator");
			if(t.serverRunning)
			{
				configStr += "Server Running on: \t\t"+InetAddress.getLocalHost().toString()+"\t"+System.getProperty("line.separator");
				configStr += "Server listening (local) port: \t\t"+t.serverSocket.getLocalPort()+"\t"+System.getProperty("line.separator");
			}
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		return configStr;
	}
	class ClientServicingThread extends Thread
	{
		Socket clientConnectedSocket;
		ObjectInputStream fromClientOIStrm;
		ObjectOutputStream toClientOOStrm;
		String forTchrID = "", myUniqueThreadName, attendanceStringComponents[], triggerStringComponents[], queryToExecute;
		
		ClientServicingThread(Socket clientConnectedSocket, String ClientServicingThreadName)
		{
			super(ClientServicingThreadName);
			myUniqueThreadName = ClientServicingThreadName;
			this.clientConnectedSocket = clientConnectedSocket;
			try
			{
				fromClientOIStrm = new ObjectInputStream(clientConnectedSocket.getInputStream());
				toClientOOStrm = new ObjectOutputStream(clientConnectedSocket.getOutputStream());									//serialize and send out EXIFileContents as byte[] OR Text Replies to Client
			}catch(IOException ioe)
			{
				ioe.printStackTrace();
			}
			//System.out.println("[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread ] "+ClientServicingThreadName+" has been started for remote Client: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort());
			logger.info(ClientServicingThreadName+" has been started for remote Client: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort());
		}
		public void run()
		{
			try
			{
				while(true)
				{
					Object o = fromClientOIStrm.readObject();
					String clientRequest = (o instanceof String)?(String)o:null;
					if(clientRequest!=null)
					{
						if(clientRequest.equals("Client: Hello!"))
						{
							//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread ] Message Received: \""+clientRequest+"\" => Handshake request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
							tempLogMsg = "Message Received: \""+clientRequest+"\" => Handshake request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
							toClientOOStrm.writeObject("Server: Hello!");
							toClientOOStrm.flush();
							logger.info(tempLogMsg += "Handshake success!");
						}else if(clientRequest.startsWith("LOGIN "))
						{
							//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread ] Message Received: \""+clientRequest+"\" => New Login from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
							tempLogMsg = "Message Received: \""+clientRequest+"\" => New Login from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
							clientRequest = clientRequest.replace("LOGIN ", "");
							String tchrIDpwDBName[] = clientRequest.split("[~]");			//TCHR ID=arr[0], PW = arr[1], DBName = arr[2]
							forTchrID = tchrIDpwDBName[0];
							
							try
							{
								if(loginHandler.loginValidate(tchrIDpwDBName[0], tchrIDpwDBName[1], tchrIDpwDBName[2]))
								{
									currentlyLoggedInTchrIDs.put(tchrIDpwDBName[0], new String());
									toClientOOStrm.writeObject(tchrIDpwDBName[0]+" LOGIN Success!");
									logger.info(tempLogMsg += "LOGIN Success!");
								}else
									throw new Exception();
								
							}catch(Exception e)
							{
								toClientOOStrm.writeObject(tchrIDpwDBName[0]+" LOGIN Failed! ["+((e.getMessage()==null||e.getMessage()=="")?"No error description found!":e.getMessage())+"]");
								logger.info(tempLogMsg += "LOGIN Failed!");
							}
							toClientOOStrm.flush();
						}else if(clientRequest.equals("GET_SERVER_DATA_ZIP"))
						{
							//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] Message Received: \""+clientRequest+"\" => Request for server DATA from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
							logger.info("Message Received: \""+clientRequest+"\" => Request for server DATA from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
							
							String md5;
							toClientOOStrm.writeObject("md5:"+(md5 = AMSServer.getMD5OfParam(serverDataZippedFile)));			//send md5 to client to check if it needs updation of server data(presets, classficiation etc) or not
							toClientOOStrm.flush();
							logger.info("sent md5: "+md5);//////////////////////////////////////////////
							
							Object obj = fromClientOIStrm.readObject();
							boolean clientsNeedUpdation = obj instanceof Boolean && ((Boolean)obj).booleanValue();
							logger.info("updation needed: "+clientsNeedUpdation);//////////////////////////////////////////////
							if(clientsNeedUpdation)		//client will send back true if client needs updation(didnt match md5) else false
							{	
								byte[] byteArray = new byte[1024];
								FileInputStream fin = new FileInputStream(serverDataZippedFile);
								int len;
								while((len=fin.read(byteArray))>0)
								{
									toClientOOStrm.writeObject(Arrays.copyOf(byteArray, len));
									toClientOOStrm.flush();
								}
								
								fin.close();
								
								md5 = AMSServer.getMD5OfParam(serverDataZippedFile);
								
								toClientOOStrm.writeObject("FINISHED_SENDING_ZIP md5:"+md5);
								toClientOOStrm.flush();
							}
		
							logger.info("Request Accepted, File Sent(MD5: "+md5+")! [ Client needed updation: "+clientsNeedUpdation+" ]");
						}else if(clientRequest.startsWith("AttendanceStringOperation~SINGLE View~"))
						{
							//System.out.println(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread ] Message Received: \""+clientRequest+"\" => Attendance String Operation(SINGLE View) request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+".");
							logger.info("Message Received: \""+clientRequest+"\" => Attendance String Operation(SINGLE View) request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+".");
							
							//Format(Separation char = Tilde(~)):		AttendanceStringOperation~chosenMode~DBName~SelectedPresetTableName~numStudents~AttendanceDate(dd/mm/yyyy)~StartTime~EndTime
							attendanceStringComponents = clientRequest.trim().split("[~]");
							
							try
							{
								queryToExecute = "select * from "+attendanceStringComponents[2].trim()+"."+attendanceStringComponents[3].trim()+" where attendanceDate='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.parse(attendanceStringComponents[5]))+"' and lectureStartTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[6]))+"' and lectureEndTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[7]))+"'";
								//System.out.println(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] 1st Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
								tempLogMsg = "1st Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
								
								ArrayList<edu.vesit.ams.AttendanceRecord> allDesiredTuples = attendanceStringOperationExecutor.executeAttendanceStringOperation(this.myUniqueThreadName, attendanceStringComponents[2]+"."+attendanceStringComponents[3], Integer.parseInt(attendanceStringComponents[4]), queryToExecute, attendanceStringComponents[1]);
								toClientOOStrm.writeObject(allDesiredTuples);
								toClientOOStrm.flush();
								
								logger.info(tempLogMsg += "Execution Success .. Sent Results!");
							}catch(Exception e)
							{
								toClientOOStrm.writeObject(e);
								toClientOOStrm.flush();
								e.printStackTrace();
								logger.info(tempLogMsg += "Execution Failed!");
							}
							
						}else if(clientRequest.startsWith("AttendanceStringOperation~BATCH View~"))
						{
							//System.out.println(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread ] Message Received: \""+clientRequest+"\" => Attendance String Operation(BATCH View) request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
							logger.info("Message Received: \""+clientRequest+"\" => Attendance String Operation(BATCH View) request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
							
							//Format(Separation char = Tilde(~)):		AttendanceStringOperation~chosenMode~DBName~SelectedPresetTableName~numStudents~AttendanceStartDate(dd/mm/yyyy)~AttendanceEndDate(dd/mm/yyyy)~StartTime~EndTime~true/false(=chkBoxDontConsiderLectureTimes)
							attendanceStringComponents = clientRequest.trim().split("[~]");
							
							try
							{
								if(attendanceStringComponents[9].equals(Boolean.toString(true)))		//query without start and end times in lecture times as chkBoxDontConsiderLectureTimeswasChosen
									queryToExecute = "select * from "+attendanceStringComponents[2].trim()+"."+attendanceStringComponents[3]+" where (attendanceDate>='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.parse(attendanceStringComponents[5]))+"' and attendanceDate<='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.parse(attendanceStringComponents[6]))+"' )";
								else
									queryToExecute = "select * from "+attendanceStringComponents[2].trim()+"."+attendanceStringComponents[3]+" where (attendanceDate>='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.parse(attendanceStringComponents[5]))+"' and attendanceDate<='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.parse(attendanceStringComponents[6]))+"' ) and lectureStartTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[7]))+"' and lectureEndTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[8]))+"'";
								//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] 1st Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
								tempLogMsg = "1st Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
								
								ArrayList<edu.vesit.ams.AttendanceRecord> allDesiredTuples = attendanceStringOperationExecutor.executeAttendanceStringOperation(this.myUniqueThreadName, attendanceStringComponents[2]+"."+attendanceStringComponents[3], Integer.parseInt(attendanceStringComponents[4]), queryToExecute, attendanceStringComponents[1]);
								toClientOOStrm.writeObject(allDesiredTuples);
								toClientOOStrm.flush();
								
								logger.info(tempLogMsg += "Execution Success .. Sent Results!");
							}catch(Exception e)
							{
								toClientOOStrm.writeObject(e);
								toClientOOStrm.flush();
								e.printStackTrace();
								logger.info(tempLogMsg += "Execution Failed!");
							}
						}else if(clientRequest.startsWith("AttendanceStringOperation~SINGLE Delete~"))
						{
							//System.out.println(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread ] Message Received: \""+clientRequest+"\" => Attendance String Operation(SINGLE Delete) request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
							logger.info("Message Received: \""+clientRequest+"\" => Attendance String Operation(SINGLE Delete) request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
							
							//Format(Separation char = Tilde(~)):		AttendanceStringOperation~chosenMode~DBName~SelectedPresetTableName~numStudents~AttendanceDate(dd/mm/yyyy)~StartTime~EndTime
							attendanceStringComponents = clientRequest.trim().split("[~]");
							
							try
							{
								queryToExecute = "select * from "+attendanceStringComponents[2].trim()+"."+attendanceStringComponents[3].trim()+" where attendanceDate='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.parse(attendanceStringComponents[5]))+"' and lectureStartTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[6]))+"' and lectureEndTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[7]))+"'";
								//System.out.println(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] 1st Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
								tempLogMsg = "1st Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
								
								ArrayList<edu.vesit.ams.AttendanceRecord> allDesiredTuples = attendanceStringOperationExecutor.executeAttendanceStringOperation(this.myUniqueThreadName, attendanceStringComponents[2]+"."+attendanceStringComponents[3], Integer.parseInt(attendanceStringComponents[4]), queryToExecute, attendanceStringComponents[1]);
								toClientOOStrm.writeObject(allDesiredTuples);
								toClientOOStrm.flush();
								
								logger.info(tempLogMsg += "Execution Success .. Sent Intermediate Results .. Awaiting Confirmation!");
								
								/*********************************/
								
								Boolean singleDeleteModeConfirmation = (Boolean)fromClientOIStrm.readObject();	//if true delete and remove from saved state of executor, if false only remove from saved state of executor
								
								if(singleDeleteModeConfirmation)
								{
									queryToExecute = "delete from "+attendanceStringComponents[2]+"."+attendanceStringComponents[3]+" where attendanceDate='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.parse(attendanceStringComponents[5]))+"' and lectureStartTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[6]))+"' and lectureEndTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[7]))+"'";
									//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] 2nd Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
									tempLogMsg = "2nd Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
									
									if(attendanceStringOperationExecutor.executeAttendanceStringOperation(this.myUniqueThreadName, attendanceStringComponents[2]+"."+attendanceStringComponents[3], Integer.parseInt(attendanceStringComponents[4]), queryToExecute, attendanceStringComponents[1])==null)
										if(attendanceStringOperationExecutor.currentSessionAwaitingUserConfirmationOperations.get(this.myUniqueThreadName).endsWith("true"))		//check deletion query success indicator
										{
											toClientOOStrm.writeObject("Deletion Success!");
											toClientOOStrm.flush();
											logger.info(tempLogMsg += "Deletion Success!");
										}else
										{
											toClientOOStrm.writeObject("Deletion Failed!");
											toClientOOStrm.flush();
											logger.info(tempLogMsg += "Deletion Failed!");
										}
								}else
									logger.info("Deletion Operation canceled by user!");
										
								//removing from saved state for both confirmation as true and false
								attendanceStringOperationExecutor.currentSessionAwaitingUserConfirmationOperations.remove(this.myUniqueThreadName);
							}catch(Exception e)
							{
								toClientOOStrm.writeObject(e);
								toClientOOStrm.flush();
								e.printStackTrace();
								logger.info(tempLogMsg += "Execution Failed!");
							}
						}else if(clientRequest.startsWith("AttendanceStringOperation~BATCH Delete~"))
						{
							//System.out.println(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread ] Message Received: \""+clientRequest+"\" => Attendance String Operation(BATCH Delete) request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
							logger.info("Message Received: \""+clientRequest+"\" => Attendance String Operation(BATCH Delete) request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
							
							//Format(Separation char = Tilde(~)):		AttendanceStringOperation~chosenMode~DBName~SelectedPresetTableName~numStudents~AttendanceStartDate(dd/mm/yyyy)~AttendanceEndDate(dd/mm/yyyy)~StartTime~EndTime~true/false(=chkBoxDontConsiderLectureTimes)
							attendanceStringComponents = clientRequest.trim().split("[~]");
							
							try
							{
								if(attendanceStringComponents[9].equals(Boolean.toString(true)))		//query without start and end times in lecture times as chkBoxDontConsiderLectureTimeswasChosen
									queryToExecute = "select * from "+attendanceStringComponents[2].trim()+"."+attendanceStringComponents[3]+" where (attendanceDate>='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.parse(attendanceStringComponents[5]))+"' and attendanceDate<='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.parse(attendanceStringComponents[6]))+"' )";
								else
									queryToExecute = "select * from "+attendanceStringComponents[2].trim()+"."+attendanceStringComponents[3]+" where (attendanceDate>='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.parse(attendanceStringComponents[5]))+"' and attendanceDate<='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.parse(attendanceStringComponents[6]))+"' ) and lectureStartTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[7]))+"' and lectureEndTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[8]))+"'";
								//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] 1st Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
								tempLogMsg = "1st Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
								
								ArrayList<edu.vesit.ams.AttendanceRecord> allDesiredTuples = attendanceStringOperationExecutor.executeAttendanceStringOperation(this.myUniqueThreadName, attendanceStringComponents[2]+"."+attendanceStringComponents[3], Integer.parseInt(attendanceStringComponents[4]), queryToExecute, attendanceStringComponents[1]);
								toClientOOStrm.writeObject(allDesiredTuples);
								toClientOOStrm.flush();
								
								logger.info(tempLogMsg += "Execution Success .. Sent Intermediate Results .. Awaiting Confirmation!");
								
								/*********************************/
								
								Object obj = fromClientOIStrm.readObject();
								if(obj==null)									//User didnt received any attendance records for his result and sent a null to cancel operation
									logger.info(tempLogMsg += "Deletion Operation canceled by user!");
								else
								{
									Boolean[] batchDeleteModeConfirmations = (Boolean[])obj;
									boolean userCanceledOperation = true;					//true if user did receive >=1 attendance records but did not want to delete any of them(so all boolean values would be false)
									for(int i=0;i<batchDeleteModeConfirmations.length;i++)
										if(batchDeleteModeConfirmations[i])					//if any1 is true, means user wants to delete that attendanceRecord hence its true at that index
										{
											userCanceledOperation = false;
											break;
										}
									
									if(userCanceledOperation)
										logger.info(tempLogMsg += "Deletion Operation canceled by user!");
									else
									{
										StringBuffer queryToExecuteStrBuffer = new StringBuffer("delete from "+attendanceStringComponents[2].trim()+"."+attendanceStringComponents[3]+" where ");
										for(int i=0;i<batchDeleteModeConfirmations.length;i++)
											if(batchDeleteModeConfirmations[i])
											{
												queryToExecuteStrBuffer.append("(");
												AttendanceRecord ar = allDesiredTuples.get(i);
												queryToExecuteStrBuffer.append("attendanceDate='"+AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(ar.getAttendanceDate())+"' and ");
												queryToExecuteStrBuffer.append("lectureStartTime='"+AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(ar.getLectureStartTime())+"' and ");
												queryToExecuteStrBuffer.append("lectureEndTime='"+AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(ar.getLectureEndTime())+"'");
												queryToExecuteStrBuffer.append(") OR ");			//OR all deletion conditions wrt composite primary key
											}
										
										queryToExecute = queryToExecuteStrBuffer.toString();
										if(queryToExecute.endsWith(" OR "))								//remove off last OR
											queryToExecute = queryToExecute.substring(0, queryToExecute.length()-4);
										
										//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] 2nd Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
										tempLogMsg = "2nd Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
										if(attendanceStringOperationExecutor.executeAttendanceStringOperation(this.myUniqueThreadName, attendanceStringComponents[2]+"."+attendanceStringComponents[3], Integer.parseInt(attendanceStringComponents[4]), queryToExecute, attendanceStringComponents[1])==null)
											if(attendanceStringOperationExecutor.currentSessionAwaitingUserConfirmationOperations.get(this.myUniqueThreadName).endsWith("true"))		//check deletion query success indicator
											{
												toClientOOStrm.writeObject("Deletion Success!");
												toClientOOStrm.flush();
												logger.info(tempLogMsg += "Deletion Success!");
											}else
											{
												toClientOOStrm.writeObject("Deletion Failed!");
												toClientOOStrm.flush();
												logger.info(tempLogMsg += "Deletion Failed!");
											}
									}
								}
								
							}catch(Exception e)
							{
								toClientOOStrm.writeObject(e);
								toClientOOStrm.flush();
								e.printStackTrace();
								logger.error(tempLogMsg += "Execution Failed!");
								logStackTraceOfException(e);
							}
							//removing from saved state for both confirmation as true and false
							attendanceStringOperationExecutor.currentSessionAwaitingUserConfirmationOperations.remove(this.myUniqueThreadName);
						}else if(clientRequest.startsWith("AttendanceStringOperation~SINGLE Insert/Update~"))
						{
							//System.out.println(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread ] Message Received: \""+clientRequest+"\" => Attendance String Operation(SINGLE Insert/Update) request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
							logger.info("Message Received: \""+clientRequest+"\" => Attendance String Operation(SINGLE Insert/Update) request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
							
							//Format(Separation char = Tilde(~)):		AttendanceStringOperation~chosenMode~DBName~SelectedPresetTableName~numStudents~AttendanceDate(dd/mm/yyyy)~StartTime~EndTime~Presentees/Absentees~(Absentees/PresenteesRollNums) OR ALL~MedicalLeaveRollNumbers(Reason)~OtherReasonsRollNumbers(Reason)
							//Medical and Other Reasons Roll Numbers are separated by backquotes(`)
							attendanceStringComponents = clientRequest.trim().split("[~]");
							
							try
							{
								queryToExecute = "select * from "+attendanceStringComponents[2].trim()+"."+attendanceStringComponents[3].trim()+" where attendanceDate='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.parse(attendanceStringComponents[5]))+"' and lectureStartTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[6]))+"' and lectureEndTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[7]))+"'";
								//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] 1st Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
								tempLogMsg = "1st Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
								
								ArrayList<edu.vesit.ams.AttendanceRecord> allDesiredTuples = attendanceStringOperationExecutor.executeAttendanceStringOperation(this.myUniqueThreadName, attendanceStringComponents[2]+"."+attendanceStringComponents[3], Integer.parseInt(attendanceStringComponents[4]), queryToExecute, attendanceStringComponents[1]);
								toClientOOStrm.writeObject(allDesiredTuples);
								toClientOOStrm.flush();
								
								logger.info(tempLogMsg += "Execution Success .. Sent Intermediate Results .. Awaiting Confirmation!");
								
								/****************************************/
								
								boolean givenNumbersAreAbsentees = attendanceStringComponents[8].equals("Absentees");
								String enteredRollNums[] = null;
								if(!attendanceStringComponents[9].equals("None"))
									if(attendanceStringComponents[9].equals("ALL"))
									{
										enteredRollNums = new String[Integer.parseInt(attendanceStringComponents[4])];
										for(int i=0;i<enteredRollNums.length;i++)
											enteredRollNums[i] = Integer.toString(i+1);
									}else
										enteredRollNums = attendanceStringComponents[9].split("[,]");
								
								LinkedHashMap<Integer, String> medicalReasons = new LinkedHashMap<Integer, String>(), otherReasons = new LinkedHashMap<Integer, String>();
								if(!attendanceStringComponents[10].equals("None"))
								{
									String temp[] = attendanceStringComponents[10].split("[`]");
									for(int i=0;i<temp.length;i++)
										medicalReasons.put(Integer.parseInt(temp[i].substring(0, temp[i].indexOf('(')).trim()), temp[i].substring(temp[i].indexOf('(')+1, temp[i].lastIndexOf(')')));
								}
								if(!attendanceStringComponents[11].equals("None"))
								{
									String temp[] = attendanceStringComponents[11].split("[`]");
									for(int i=0;i<temp.length;i++)
										otherReasons.put(Integer.parseInt(temp[i].substring(0, temp[i].indexOf('(')).trim()), temp[i].substring(temp[i].indexOf('(')+1, temp[i].lastIndexOf(')')));
								}
								
								/****************************************/
								
								Boolean singleInsertUpdateModeConfirmation[] = (Boolean[]) fromClientOIStrm.readObject();
								StringBuffer queryToExecuteStrBuffer = null;
								if(!singleInsertUpdateModeConfirmation[0] && singleInsertUpdateModeConfirmation[1])			//for false, true(keep new or insert new)
								{
									if(allDesiredTuples.size()>0)		//for updation but user choosed option Keep NEW Only, so 1st deleting old(Command=deletingOldBeforeKeepNewOnly~quertToExecute => here command also informing executor not to update insertionupdationSuccessIndicator because its just deleting old attendance record to make place for new(as pk=attendanceDate+start+endtime)) and then proceeding as if its a new insertion
									{
										queryToExecute = "delete from "+attendanceStringComponents[2].trim()+"."+attendanceStringComponents[3].trim()+" where attendanceDate='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.parse(attendanceStringComponents[5]))+"' and lectureStartTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[6]))+"' and lectureEndTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[7]))+"'";
										queryToExecute = "Command=deletingOldBeforeKeepNewOnly~" + queryToExecute;
										
										//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] 2nd Query (Keep NEW Only .. deleting old record)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
										tempLogMsg = "2nd Query (Keep NEW Only .. deleting old record)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
										
										ArrayList<AttendanceRecord> temp;
										temp=attendanceStringOperationExecutor.executeAttendanceStringOperation(this.myUniqueThreadName, attendanceStringComponents[2]+"."+attendanceStringComponents[3], Integer.parseInt(attendanceStringComponents[4]), queryToExecute, attendanceStringComponents[1]);
										if(!(temp!=null && temp.size()==0))			//successful deletion operation(before keep new Only option of updation) indicator
											throw new Exception("An exception occured .. retry");
										else
											logger.info(tempLogMsg += "Old Record SuccessFully deleted to make space for new 1!");
									}
									
									queryToExecuteStrBuffer = new StringBuffer("insert into "+attendanceStringComponents[2].trim()+"."+attendanceStringComponents[3].trim()+"(attendanceDate, lectureStartTime, lectureEndTime");
									StringBuffer valuesOFInsertQuery = new StringBuffer("values(");
									valuesOFInsertQuery.append("'"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.parse(attendanceStringComponents[5]))+"', ");
									valuesOFInsertQuery.append("'"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[6]))+"', ");
									valuesOFInsertQuery.append("'"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[7]))+"'");
									
									if(givenNumbersAreAbsentees)
									{
										/*										OLD CODE
										queryToExecuteStrBuffer.append(", ");
										valuesOFInsertQuery.append(", ");
										if(!attendanceStringComponents[9].equals("None"))
											for(int i=0;i<enteredRollNums.length-1;i++)
											{
												queryToExecuteStrBuffer.append("`"+enteredRollNums[i]+"`, ");
												valuesOFInsertQuery.append("'A', ");
											}
										if(attendanceStringComponents[9].equals("ALL") || (medicalReasons.size()==0 && otherReasons.size()==0))
										{
											queryToExecuteStrBuffer.append("`"+enteredRollNums[enteredRollNums.length-1]+"`)");
											valuesOFInsertQuery.append("'A')");
										}else
										{
											if(enteredRollNums!=null && enteredRollNums.length==1)											//Condition = Keep NEW Only Update Option + atleast 1 i.e >=1 in medical OR Other Reasons Roll Num + absentees/presentees only 1 specified => above for loop and its next if both get false so that single absentees/presentees is skipped and not put in insertQuery so default 'P' is inserted even if 'A' was specified for it 
											{
												queryToExecuteStrBuffer.append("`"+enteredRollNums[0]+"`, ");
												valuesOFInsertQuery.append("'A', ");
											}
											for(Map.Entry<Integer, String> entry:medicalReasons.entrySet())
											{
												queryToExecuteStrBuffer.append("`"+entry.getKey().toString()+"`, ");
												valuesOFInsertQuery.append("'M("+entry.getValue()+")', ");
											}
											for(Map.Entry<Integer, String> entry:otherReasons.entrySet())
											{
												queryToExecuteStrBuffer.append("`"+entry.getKey().toString()+"`, ");
												valuesOFInsertQuery.append("'O("+entry.getValue()+")', ");
											}
											valuesOFInsertQuery = new StringBuffer(valuesOFInsertQuery.substring(0, valuesOFInsertQuery.length()-2) + ")");
											queryToExecuteStrBuffer = new StringBuffer(queryToExecuteStrBuffer.substring(0, queryToExecuteStrBuffer.length()-2).concat(") "));		//remove off last comma and space and put a closing bracket over there
										}*/
										queryToExecuteStrBuffer.append(", ");
										valuesOFInsertQuery.append(", ");
										if(!attendanceStringComponents[9].equals("None"))
											for(int i=0;i<enteredRollNums.length;i++)
											{
												queryToExecuteStrBuffer.append("`"+enteredRollNums[i]+"`, ");
												valuesOFInsertQuery.append("'A', ");
											}
										for(Map.Entry<Integer, String> entry:medicalReasons.entrySet())
										{
											queryToExecuteStrBuffer.append("`"+entry.getKey().toString()+"`, ");
											valuesOFInsertQuery.append("'M("+entry.getValue()+")', ");
										}
										for(Map.Entry<Integer, String> entry:otherReasons.entrySet())
										{
											queryToExecuteStrBuffer.append("`"+entry.getKey().toString()+"`, ");
											valuesOFInsertQuery.append("'O("+entry.getValue()+")', ");
										}
										valuesOFInsertQuery = new StringBuffer(valuesOFInsertQuery.substring(0, valuesOFInsertQuery.length()-2) + ")");
										queryToExecuteStrBuffer = new StringBuffer(queryToExecuteStrBuffer.substring(0, queryToExecuteStrBuffer.length()-2).concat(") "));		//remove off last comma and space and put a closing bracket over there
									}else
									{
										if(attendanceStringComponents[9].equals("ALL"))
										{
											//using default 'P' values for all roll numbers
											queryToExecuteStrBuffer.append(")");
											valuesOFInsertQuery.append(")");
										}else
										{
											queryToExecuteStrBuffer.append(", ");
											valuesOFInsertQuery.append(", ");
											
											ArrayList<Integer> calculatedAbsenteesRollNumbers = new ArrayList<Integer>();
											if(!attendanceStringComponents[9].equals("None"))
											{
												for(Integer rollNum=1;rollNum<=Integer.parseInt(attendanceStringComponents[4]);rollNum++)			// 1 to numStudents
													if(!medicalReasons.containsKey(rollNum) && !otherReasons.containsKey(rollNum) && Arrays.binarySearch(enteredRollNums, rollNum.toString())<0)
														calculatedAbsenteesRollNumbers.add(rollNum);
											}
											
											for(int i=0;i<calculatedAbsenteesRollNumbers.size();i++)			//cant be of 0 length because ALL condition is checked above, earlier upper bound of loop was .size()-1
											{
												queryToExecuteStrBuffer.append("`"+calculatedAbsenteesRollNumbers.get(i).toString()+"`, ");
												valuesOFInsertQuery.append("'A', ");
											}
											
											if(medicalReasons.size()==0 && otherReasons.size()==0)			//nothing more to add so put brackets
											{
												queryToExecuteStrBuffer.append("`"+calculatedAbsenteesRollNumbers.get(calculatedAbsenteesRollNumbers.size()-1).toString()+"`)");
												valuesOFInsertQuery.append("'A')");
											}else
											{
												for(Map.Entry<Integer, String> entry:medicalReasons.entrySet())
												{
													queryToExecuteStrBuffer.append("`"+entry.getKey().toString()+"`, ");
													valuesOFInsertQuery.append("'M("+entry.getValue()+")', ");
												}
												for(Map.Entry<Integer, String> entry:otherReasons.entrySet())
												{
													queryToExecuteStrBuffer.append("`"+entry.getKey().toString()+"`, ");
													valuesOFInsertQuery.append("'O("+entry.getValue()+")', ");
												}
												valuesOFInsertQuery = new StringBuffer(valuesOFInsertQuery.substring(0, valuesOFInsertQuery.length()-2) + ")");
												queryToExecuteStrBuffer = new StringBuffer(queryToExecuteStrBuffer.substring(0, queryToExecuteStrBuffer.length()-2).concat(") "));		//remove off last comma and space and put a closing bracket over there
											}
										}
									}
									queryToExecuteStrBuffer.append(valuesOFInsertQuery);
								}else if(singleInsertUpdateModeConfirmation[0] && singleInsertUpdateModeConfirmation[1])			//for true, true(merging/adding new to old)
								{
									AttendanceRecord mergedNewAddedToOldAttendanceRecord = (AttendanceRecord)fromClientOIStrm.readObject();
									queryToExecuteStrBuffer = new StringBuffer("update "+attendanceStringComponents[2]+"."+attendanceStringComponents[3]+" set ");
									for(Map.Entry<Integer, String> entry:mergedNewAddedToOldAttendanceRecord.getAttendance().entrySet())
										queryToExecuteStrBuffer.append("`"+entry.getKey()+"` = '"+entry.getValue()+"', ");
									queryToExecuteStrBuffer = new StringBuffer(queryToExecuteStrBuffer.substring(0, queryToExecuteStrBuffer.length()-2));		//remove off last comma and space
									queryToExecuteStrBuffer.append(" where attendanceDate='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.parse(attendanceStringComponents[5]))+"' and lectureStartTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[6]))+"' and lectureEndTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[7]))+"'");									
								}else if(singleInsertUpdateModeConfirmation[0] && !singleInsertUpdateModeConfirmation[1])			//for true, false(Keep OLD Only)
									logger.info(tempLogMsg += "Insertion/Updation Operation canceled by user!");
								
								if(queryToExecuteStrBuffer!=null)
								{
									queryToExecute = queryToExecuteStrBuffer.toString();
									
									//2nd query if needed updation as Keep NEW only, so 2nd query = delete old 1 so below 1 is 3rd, if updating directly(Add NEW to OLD) then its 2nd query(chcek by 2nd or condition of mode checking)
									//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] "+((allDesiredTuples.size()>0 || (!(!singleInsertUpdateModeConfirmation[0] && singleInsertUpdateModeConfirmation[1])))?"3rd":"2nd")+" Query (Insert/Update Query)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
									tempLogMsg = ((allDesiredTuples.size()>0 || (!(!singleInsertUpdateModeConfirmation[0] && singleInsertUpdateModeConfirmation[1])))?"3rd":"2nd")+" Query (Insert/Update Query)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
									
									if(attendanceStringOperationExecutor.executeAttendanceStringOperation(this.myUniqueThreadName, attendanceStringComponents[2]+"."+attendanceStringComponents[3], Integer.parseInt(attendanceStringComponents[4]), queryToExecute, attendanceStringComponents[1])==null)
										if(attendanceStringOperationExecutor.currentSessionAwaitingUserConfirmationOperations.get(this.myUniqueThreadName).endsWith(Boolean.toString(true)))
										{
											toClientOOStrm.writeObject("Insertion/Updation Success!");
											toClientOOStrm.flush();
											logger.info(tempLogMsg += "Insertion/Updation Success!");
										}else
										{
											toClientOOStrm.writeObject("Insertion/Updation Failed!");
											toClientOOStrm.flush();
											logger.info(tempLogMsg += "Insertion/Updation Failed!");
										}
								}
							}catch(Exception e)
							{
								toClientOOStrm.writeObject(e);
								toClientOOStrm.flush();
								e.printStackTrace();
								logger.error(tempLogMsg += "Execution Failed!");
								logStackTraceOfException(e);
							}
							//removing from saved state for both confirmation as true and false
							attendanceStringOperationExecutor.currentSessionAwaitingUserConfirmationOperations.remove(this.myUniqueThreadName);
						}else if(clientRequest.startsWith("AttendanceStringOperation~BATCH Insert/Update~"))
						{
							//System.out.println(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread ] Message Received: \""+clientRequest+"\" => Attendance String Operation(BATCH Insert/Update) request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
							logger.info("Message Received: \""+clientRequest+"\" => Attendance String Operation(BATCH Insert/Update) request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
							
							//Format(Separation char = Tilde(~)):		AttendanceStringOperation~chosenMode~DBName~SelectedPresetTableName~numStudents~AttendanceStartDate(dd/mm/yyyy)~AttendanceEndDate(dd/mm/yyyy)~StartTime~EndTime~Presentees/Absentees~(Absentees/PresenteesRollNums) OR ALL~MedicalLeaveRollNumbers(Reason)~OtherReasonsRollNumbers(Reason)
							//Medical and Other Reasons Roll Numbers are separated by backquotes(`)
							attendanceStringComponents = clientRequest.trim().split("[~]");
							
							try
							{
								queryToExecute = "select * from "+attendanceStringComponents[2].trim()+"."+attendanceStringComponents[3]+" where (attendanceDate>='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.parse(attendanceStringComponents[5]))+"' and attendanceDate<='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.parse(attendanceStringComponents[6]))+"' ) and lectureStartTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[7]))+"' and lectureEndTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[8]))+"'";
								//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] 1st Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
								tempLogMsg = "1st Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
								
								ArrayList<edu.vesit.ams.AttendanceRecord> allDesiredTuples = attendanceStringOperationExecutor.executeAttendanceStringOperation(this.myUniqueThreadName, attendanceStringComponents[2]+"."+attendanceStringComponents[3], Integer.parseInt(attendanceStringComponents[4]), queryToExecute, attendanceStringComponents[1]);
								toClientOOStrm.writeObject(allDesiredTuples);
								toClientOOStrm.flush();
								
								logger.info(tempLogMsg += "Execution Success .. Sent Intermediate Results .. Awaiting Confirmation!");
								
								/****************************************/
								
								boolean givenNumbersAreAbsentees = attendanceStringComponents[9].equals("Absentees");
								String enteredRollNums[] = null;
								if(!attendanceStringComponents[10].equals("None"))
									if(attendanceStringComponents[10].equals("ALL"))
									{
										enteredRollNums = new String[Integer.parseInt(attendanceStringComponents[4])];
										for(int i=0;i<enteredRollNums.length;i++)
											enteredRollNums[i] = Integer.toString(i+1);
									}else
										enteredRollNums = attendanceStringComponents[10].split("[,]");
								
								LinkedHashMap<Integer, String> medicalReasons = new LinkedHashMap<Integer, String>(), otherReasons = new LinkedHashMap<Integer, String>();
								if(!attendanceStringComponents[11].equals("None"))
								{
									String temp[] = attendanceStringComponents[11].split("[`]");
									for(int i=0;i<temp.length;i++)
										medicalReasons.put(Integer.parseInt(temp[i].substring(0, temp[i].indexOf('(')).trim()), temp[i].substring(temp[i].indexOf('(')+1, temp[i].lastIndexOf(')')));
								}
								if(!attendanceStringComponents[12].equals("None"))
								{
									String temp[] = attendanceStringComponents[12].split("[`]");
									for(int i=0;i<temp.length;i++)
										otherReasons.put(Integer.parseInt(temp[i].substring(0, temp[i].indexOf('(')).trim()), temp[i].substring(temp[i].indexOf('(')+1, temp[i].lastIndexOf(')')));
								}
								
								/****************************************/
								
								ArrayList<Date> allDatesInRange = new ArrayList<Date>();
								
								//Insertion for Non-existing Attendance Records
								Calendar cal = Calendar.getInstance();
								cal.setTime(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.parse(attendanceStringComponents[6]));
								cal.add(Calendar.DATE, 1);
								Date attendanceEndDatePlus1Day = cal.getTime();
								for(cal.setTime(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.parse(attendanceStringComponents[5]));cal.getTime().before(attendanceEndDatePlus1Day);cal.add(Calendar.DATE, 1))
								{
									allDatesInRange.add(cal.getTime());									
									
									boolean foundAttendanceDateLectureStartEndTimeFlag = false;
									for(AttendanceRecord ar:allDesiredTuples)
										if(ar.getAttendanceDate().equals(cal.getTime()) && ar.getLectureStartTime().equals(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[7])) && ar.getLectureEndTime().equals(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[8])))
										{
											foundAttendanceDateLectureStartEndTimeFlag = true;
											break;
										}
									if(foundAttendanceDateLectureStartEndTimeFlag)
									{
										//Nothing to code here as its handled by below iteration wrt option selected(Keep NEW, Keep OLD, Add to OLD) for that iteration's AttendanceRecord
									}else
									{
										//							DITTO SAME QUERY MAKING AS IN SINGLE INSERT/UPDATE BUT HERE BELOW THING IS REPEATED FOR EACH NON-EXISTING PRIMAY KEY=ATTENDATE DATE + LECTURE START TIME + LECTURE END TIME)
										
										//Non-existing attendance record for that date but insertion requested by user during BATCH Insert/Update so performing a insert query => without changing state of executor for this thread, so putting "Command: isLastQuery=false"+queryToExecute else "Command: isLastQuery=true"+queryToExecute for last insertion/updation query so that successIndicator can be set to give user confirmation
										StringBuffer queryToExecuteStrBuffer = new StringBuffer("insert into "+attendanceStringComponents[2].trim()+"."+attendanceStringComponents[3].trim()+"(attendanceDate, lectureStartTime, lectureEndTime");
										StringBuffer valuesOFInsertQuery = new StringBuffer("values(");
										valuesOFInsertQuery.append("'"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(cal.getTime())+"', ");
										valuesOFInsertQuery.append("'"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[7]))+"', ");
										valuesOFInsertQuery.append("'"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[8]))+"'");
										
										if(givenNumbersAreAbsentees)
										{
											/*														OLD CODE
											queryToExecuteStrBuffer.append(", ");
											valuesOFInsertQuery.append(", ");
											if(!attendanceStringComponents[10].equals("None"))
												for(int j=0;j<enteredRollNums.length-1;j++)
												{
													queryToExecuteStrBuffer.append("`"+enteredRollNums[j]+"`, ");
													valuesOFInsertQuery.append("'A', ");
												}
											if(attendanceStringComponents[10].equals("ALL") || (medicalReasons.size()==0 && otherReasons.size()==0))
											{
												queryToExecuteStrBuffer.append("`"+enteredRollNums[enteredRollNums.length-1]+"`)");
												valuesOFInsertQuery.append("'A')");
											}else
											{
												if(enteredRollNums!=null && enteredRollNums.length==1)											//Condition = Keep NEW Only Update Option + atleast 1 i.e >=1 in medical OR Other Reasons Roll Num + absentees/presentees only 1 specified => above for loop and its next if both get false so that single absentees/presentees is skipped and not put in insertQuery so default 'P' is inserted even if 'A' was specified for it 
												{
													queryToExecuteStrBuffer.append("`"+enteredRollNums[0]+"`, ");
													valuesOFInsertQuery.append("'A', ");
												}
												for(Map.Entry<Integer, String> entry:medicalReasons.entrySet())
												{
													queryToExecuteStrBuffer.append("`"+entry.getKey().toString()+"`, ");
													valuesOFInsertQuery.append("'M("+entry.getValue()+")', ");
												}
												for(Map.Entry<Integer, String> entry:otherReasons.entrySet())
												{
													queryToExecuteStrBuffer.append("`"+entry.getKey().toString()+"`, ");
													valuesOFInsertQuery.append("'O("+entry.getValue()+")', ");
												}
												valuesOFInsertQuery = new StringBuffer(valuesOFInsertQuery.substring(0, valuesOFInsertQuery.length()-2) + ")");
												queryToExecuteStrBuffer = new StringBuffer(queryToExecuteStrBuffer.substring(0, queryToExecuteStrBuffer.length()-2).concat(") "));		//remove off last comma and space and put a closing bracket over there
											}*/
											queryToExecuteStrBuffer.append(", ");
											valuesOFInsertQuery.append(", ");
											if(!attendanceStringComponents[10].equals("None"))
												for(int j=0;j<enteredRollNums.length;j++)
												{
													queryToExecuteStrBuffer.append("`"+enteredRollNums[j]+"`, ");
													valuesOFInsertQuery.append("'A', ");
												}
											for(Map.Entry<Integer, String> entry:medicalReasons.entrySet())
											{
												queryToExecuteStrBuffer.append("`"+entry.getKey().toString()+"`, ");
												valuesOFInsertQuery.append("'M("+entry.getValue()+")', ");
											}
											for(Map.Entry<Integer, String> entry:otherReasons.entrySet())
											{
												queryToExecuteStrBuffer.append("`"+entry.getKey().toString()+"`, ");
												valuesOFInsertQuery.append("'O("+entry.getValue()+")', ");
											}
											valuesOFInsertQuery = new StringBuffer(valuesOFInsertQuery.substring(0, valuesOFInsertQuery.length()-2) + ")");
											queryToExecuteStrBuffer = new StringBuffer(queryToExecuteStrBuffer.substring(0, queryToExecuteStrBuffer.length()-2).concat(") "));		//remove off last comma and space and put a closing bracket over there
										}else
										{
											if(attendanceStringComponents[10].equals("ALL"))
											{
												//using default 'P' values for all roll numbers
												queryToExecuteStrBuffer.append(")");
												valuesOFInsertQuery.append(")");
											}else
											{
												queryToExecuteStrBuffer.append(", ");
												valuesOFInsertQuery.append(", ");
												
												ArrayList<Integer> calculatedAbsenteesRollNumbers = new ArrayList<Integer>();
												if(!attendanceStringComponents[10].equals("None"))
												{
													for(Integer rollNum=1;rollNum<=Integer.parseInt(attendanceStringComponents[4]);rollNum++)			// 1 to numStudents
														if(!medicalReasons.containsKey(rollNum) && !otherReasons.containsKey(rollNum) && Arrays.binarySearch(enteredRollNums, rollNum.toString())<0)
															calculatedAbsenteesRollNumbers.add(rollNum);
												}
												
												for(int j=0;j<calculatedAbsenteesRollNumbers.size();j++)			//cant be of 0 length because ALL condition is checked above, earlier upper bound of loop was .size()-1
												{
													queryToExecuteStrBuffer.append("`"+calculatedAbsenteesRollNumbers.get(j).toString()+"`, ");
													valuesOFInsertQuery.append("'A', ");
												}
												
												if(medicalReasons.size()==0 && otherReasons.size()==0)			//nothing more to add so put brackets
												{
													queryToExecuteStrBuffer.append("`"+calculatedAbsenteesRollNumbers.get(calculatedAbsenteesRollNumbers.size()-1).toString()+"`)");
													valuesOFInsertQuery.append("'A')");
												}else
												{
													for(Map.Entry<Integer, String> entry:medicalReasons.entrySet())
													{
														queryToExecuteStrBuffer.append("`"+entry.getKey().toString()+"`, ");
														valuesOFInsertQuery.append("'M("+entry.getValue()+")', ");
													}
													for(Map.Entry<Integer, String> entry:otherReasons.entrySet())
													{
														queryToExecuteStrBuffer.append("`"+entry.getKey().toString()+"`, ");
														valuesOFInsertQuery.append("'O("+entry.getValue()+")', ");
													}
													valuesOFInsertQuery = new StringBuffer(valuesOFInsertQuery.substring(0, valuesOFInsertQuery.length()-2) + ")");
													queryToExecuteStrBuffer = new StringBuffer(queryToExecuteStrBuffer.substring(0, queryToExecuteStrBuffer.length()-2).concat(") "));		//remove off last comma and space and put a closing bracket over there
												}
											}
										}
										queryToExecuteStrBuffer.append(valuesOFInsertQuery);
										
										//If only insertion for all batch insert/update operation => no conflicting as no existing existing records with same date, start and end time(i.e allDesiredTuples.size==0) and we are at insertion of last attendance date
										if(cal.getTime().equals(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.parse(attendanceStringComponents[6])) && allDesiredTuples.size()==0)
										{
											queryToExecute = "Command: isLastQuery=true"+"~"+queryToExecuteStrBuffer.toString();
											//no need of where condition here as we would confirm out insertion using currentSessionAwaitingUserConfirmationOperations.get(this.myUniqueThreadTime).endsWith(true)
											
											//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] LAST Query (Inserting for Non-existing primary key attendance records which are requested by user in batch insert/update)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
											tempLogMsg = "LAST Query (Inserting for Non-existing primary key attendance records which are requested by user in batch insert/update)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
											
											if(attendanceStringOperationExecutor.executeAttendanceStringOperation(this.myUniqueThreadName, attendanceStringComponents[2]+"."+attendanceStringComponents[3], Integer.parseInt(attendanceStringComponents[4]), queryToExecute, attendanceStringComponents[1])==null)
												if(attendanceStringOperationExecutor.currentSessionAwaitingUserConfirmationOperations.get(this.myUniqueThreadName).endsWith(Boolean.toString(true)))
												{
													toClientOOStrm.writeObject("Batch Insertion/Updation Success!");
													toClientOOStrm.flush();
													logger.info(tempLogMsg += "Batch Insertion/Updation Success!");
												}else
												{
													toClientOOStrm.writeObject("Batch Insertion/Updation Failed!");
													toClientOOStrm.flush();
													logger.error(tempLogMsg += "Batch Insertion/Updation Failed!");
												}
										}else
										{
											queryToExecute = "Command: isLastQuery=false"+"~"+queryToExecuteStrBuffer.toString();
											String whereConditionForConfirmationOfInsertion = " where attendanceDate='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(cal.getTime())+"' and lectureStartTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[7]))+"' and lectureEndTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(attendanceStringComponents[8]))+"'";
											queryToExecute = queryToExecute + "~" + whereConditionForConfirmationOfInsertion;
											
											//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] Query (Inserting for Non-existing primary key attendance records which are requested by user in batch insert/update)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
											tempLogMsg = "Query (Inserting for Non-existing primary key attendance records which are requested by user in batch insert/update)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
											
											ArrayList<AttendanceRecord> temp;
											temp=attendanceStringOperationExecutor.executeAttendanceStringOperation(this.myUniqueThreadName, attendanceStringComponents[2]+"."+attendanceStringComponents[3], Integer.parseInt(attendanceStringComponents[4]), queryToExecute, attendanceStringComponents[1]);
											if(!(temp!=null && temp.size()==1))			//successful insertion operation of 1 tuple that was non-existing (found by primary key) and requested by user too , indicator = size of returning arraylist = 1(that was just inserted)
												throw new Exception("An exception occured .. retry");
											else
												logger.info(tempLogMsg += "New Record SuccessFully inserted that didnt conflict with any of pre-existing!");
										}
									}		
								}
								
								/****************************************/
								
								if(allDesiredTuples.size()>0)
								{
									Boolean batchInsertUpdateModeConfirmations[][] = (Boolean[][]) fromClientOIStrm.readObject();
									@SuppressWarnings("unchecked")
									ArrayList<AttendanceRecord> mergedNewAddedToOldAttendanceRecordBatchInsertUpdateMode = (ArrayList<AttendanceRecord>) fromClientOIStrm.readObject();
									for(int i=0;i<batchInsertUpdateModeConfirmations.length;i++)
									{
										StringBuffer queryToExecuteStrBuffer = null;				//fresh new querToExecute Per Attendance Record
										if(!batchInsertUpdateModeConfirmations[i][0] && batchInsertUpdateModeConfirmations[i][1])			//for false, true(keep new or insert new)
										{
											/** DELETING OLD ATTENDANCE RECORD CODE STARTTED HERE */
											
											queryToExecute = "delete from "+attendanceStringComponents[2].trim()+"."+attendanceStringComponents[3].trim()+" where attendanceDate='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(allDesiredTuples.get(i).getAttendanceDate())+"' and lectureStartTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(allDesiredTuples.get(i).getLectureStartTime())+"' and lectureEndTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(allDesiredTuples.get(i).getLectureEndTime())+"'";
											queryToExecute = "Command=deletingOldBeforeKeepNewOnly~" + queryToExecute;
											
											//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] Query (Keep NEW Only .. deleting old record)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
											tempLogMsg = "Query (Keep NEW Only .. deleting old record)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
											
											ArrayList<AttendanceRecord> temp;
											temp=attendanceStringOperationExecutor.executeAttendanceStringOperation(this.myUniqueThreadName, attendanceStringComponents[2]+"."+attendanceStringComponents[3], Integer.parseInt(attendanceStringComponents[4]), queryToExecute, attendanceStringComponents[1]);
											if(!(temp!=null && temp.size()==0))			//successful deletion operation(before keep new Only option of updation) indicator
												throw new Exception("An exception occured .. retry");
											else
												logger.info(tempLogMsg += "Old Record SuccessFully deleted to make space for new 1!");
											
											/** DELETING OLD ATTEDANCE RECORD CODE FINISHED HERE */
											
											queryToExecuteStrBuffer = new StringBuffer("insert into "+attendanceStringComponents[2].trim()+"."+attendanceStringComponents[3].trim()+"(attendanceDate, lectureStartTime, lectureEndTime");
											StringBuffer valuesOFInsertQuery = new StringBuffer("values(");
											valuesOFInsertQuery.append("'"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(allDesiredTuples.get(i).getAttendanceDate())+"', ");
											valuesOFInsertQuery.append("'"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(allDesiredTuples.get(i).getLectureStartTime())+"', ");
											valuesOFInsertQuery.append("'"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(allDesiredTuples.get(i).getLectureEndTime())+"'");
											
											if(givenNumbersAreAbsentees)
											{
												queryToExecuteStrBuffer.append(", ");
												valuesOFInsertQuery.append(", ");
												if(!attendanceStringComponents[10].equals("None"))
													for(int j=0;j<enteredRollNums.length-1;j++)
													{
														queryToExecuteStrBuffer.append("`"+enteredRollNums[j]+"`, ");
														valuesOFInsertQuery.append("'A', ");
													}
												if(attendanceStringComponents[10].equals("ALL") || (medicalReasons.size()==0 && otherReasons.size()==0))
												{
													queryToExecuteStrBuffer.append("`"+enteredRollNums[enteredRollNums.length-1]+"`)");
													valuesOFInsertQuery.append("'A')");
												}else
												{
													if(enteredRollNums!=null && enteredRollNums.length==1)											//Condition = Keep NEW Only Update Option + atleast 1 i.e >=1 in medical OR Other Reasons Roll Num + absentees/presentees only 1 specified => above for loop and its next if both get false so that single absentees/presentees is skipped and not put in insertQuery so default 'P' is inserted even if 'A' was specified for it 
													{
														queryToExecuteStrBuffer.append("`"+enteredRollNums[0]+"`, ");
														valuesOFInsertQuery.append("'A', ");
													}
													for(Map.Entry<Integer, String> entry:medicalReasons.entrySet())
													{
														queryToExecuteStrBuffer.append("`"+entry.getKey().toString()+"`, ");
														valuesOFInsertQuery.append("'M("+entry.getValue()+")', ");
													}
													for(Map.Entry<Integer, String> entry:otherReasons.entrySet())
													{
														queryToExecuteStrBuffer.append("`"+entry.getKey().toString()+"`, ");
														valuesOFInsertQuery.append("'O("+entry.getValue()+")', ");
													}
													valuesOFInsertQuery = new StringBuffer(valuesOFInsertQuery.substring(0, valuesOFInsertQuery.length()-2) + ")");
													queryToExecuteStrBuffer = new StringBuffer(queryToExecuteStrBuffer.substring(0, queryToExecuteStrBuffer.length()-2).concat(") "));		//remove off last comma and space and put a closing bracket over there
												}
											}else
											{
												if(attendanceStringComponents[10].equals("ALL"))
												{
													//using default 'P' values for all roll numbers
													queryToExecuteStrBuffer.append(")");
													valuesOFInsertQuery.append(")");
												}else
												{
													queryToExecuteStrBuffer.append(", ");
													valuesOFInsertQuery.append(", ");
													
													ArrayList<Integer> calculatedAbsenteesRollNumbers = new ArrayList<Integer>();
													if(!attendanceStringComponents[10].equals("None"))
													{
														for(Integer rollNum=1;rollNum<=Integer.parseInt(attendanceStringComponents[4]);rollNum++)			// 1 to numStudents
															if(!medicalReasons.containsKey(rollNum) && !otherReasons.containsKey(rollNum) && Arrays.binarySearch(enteredRollNums, rollNum.toString())<0)
																calculatedAbsenteesRollNumbers.add(rollNum);
													}
													
													for(int j=0;j<calculatedAbsenteesRollNumbers.size()-1;j++)			//cant be of 0 length because ALL condition is checked above
													{
														queryToExecuteStrBuffer.append("`"+calculatedAbsenteesRollNumbers.get(j).toString()+"`, ");
														valuesOFInsertQuery.append("'A', ");
													}
													
													if(medicalReasons.size()==0 && otherReasons.size()==0)			//nothing more to add so put brackets
													{
														queryToExecuteStrBuffer.append("`"+calculatedAbsenteesRollNumbers.get(calculatedAbsenteesRollNumbers.size()-1).toString()+"`)");
														valuesOFInsertQuery.append("'A')");
													}else
													{
														for(Map.Entry<Integer, String> entry:medicalReasons.entrySet())
														{
															queryToExecuteStrBuffer.append("`"+entry.getKey().toString()+"`, ");
															valuesOFInsertQuery.append("'M("+entry.getValue()+")', ");
														}
														for(Map.Entry<Integer, String> entry:otherReasons.entrySet())
														{
															queryToExecuteStrBuffer.append("`"+entry.getKey().toString()+"`, ");
															valuesOFInsertQuery.append("'O("+entry.getValue()+")', ");
														}
														valuesOFInsertQuery = new StringBuffer(valuesOFInsertQuery.substring(0, valuesOFInsertQuery.length()-2) + ")");
														queryToExecuteStrBuffer = new StringBuffer(queryToExecuteStrBuffer.substring(0, queryToExecuteStrBuffer.length()-2).concat(") "));		//remove off last comma and space and put a closing bracket over there
													}
												}
											}
											queryToExecuteStrBuffer.append(valuesOFInsertQuery);
										}else if(batchInsertUpdateModeConfirmations[i][0] && batchInsertUpdateModeConfirmations[i][1])			//for true, true(merging/adding new to old)
										{
											AttendanceRecord mergedNewAddedToOldAttendanceRecord = mergedNewAddedToOldAttendanceRecordBatchInsertUpdateMode.get(i);
											queryToExecuteStrBuffer = new StringBuffer("update "+attendanceStringComponents[2]+"."+attendanceStringComponents[3]+" set ");
											for(Map.Entry<Integer, String> entry:mergedNewAddedToOldAttendanceRecord.getAttendance().entrySet())
												queryToExecuteStrBuffer.append("`"+entry.getKey()+"` = '"+entry.getValue()+"', ");
											queryToExecuteStrBuffer = new StringBuffer(queryToExecuteStrBuffer.substring(0, queryToExecuteStrBuffer.length()-2));		//remove off last comma and space
											queryToExecuteStrBuffer.append(" where attendanceDate='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(allDesiredTuples.get(i).getAttendanceDate())+"' and lectureStartTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(allDesiredTuples.get(i).getLectureStartTime())+"' and lectureEndTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(allDesiredTuples.get(i).getLectureEndTime())+"'");									
										}else if(batchInsertUpdateModeConfirmations[i][0] && !batchInsertUpdateModeConfirmations[i][1])			//for true, false(Keep OLD Only)
										{
											AttendanceRecord ar = allDesiredTuples.get(i);
											logger.info("Insertion/Updation Operation canceled by user for attendanceRecord("+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.format(ar.getAttendanceDate())+", "+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.format(ar.getLectureStartTime())+", "+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.format(ar.getLectureEndTime())+")!");
										}
										
										if(queryToExecuteStrBuffer==null)				//if user has chosen old only for current iteration's attendance record
										{
											if(i==batchInsertUpdateModeConfirmations.length-1)								//if current iteration's attendance record = last record then send confirmation of success to client
											{
												toClientOOStrm.writeObject("Batch Insertion/Updation Success!");
												toClientOOStrm.flush();
												logger.info("Batch Insertion/Updation Success!");
											}else
											{
												//nothing to code here as current iteration's attendance is not the last one and for that iteration's attendance record user has requested Keep OLD Only hence queryToExecute==null => true
											}
										}else
										{
											queryToExecute = queryToExecuteStrBuffer.toString();
											
											if(i==batchInsertUpdateModeConfirmations.length-1)				//check if its the last query
											{
												queryToExecute = "Command: isLastQuery=true"+"~"+queryToExecuteStrBuffer.toString();
												//no need of where condition here as we would confirm out insertion using currentSessionAwaitingUserConfirmationOperations.get(this.myUniqueThreadTime).endsWith(true)
												
												//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] LAST Query (Inserting for Non-existing primary key attendance records which are requested by user in batch insert/update)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
												tempLogMsg = "LAST Query (Inserting for Non-existing primary key attendance records which are requested by user in batch insert/update)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
												
												if(attendanceStringOperationExecutor.executeAttendanceStringOperation(this.myUniqueThreadName, attendanceStringComponents[2]+"."+attendanceStringComponents[3], Integer.parseInt(attendanceStringComponents[4]), queryToExecute, attendanceStringComponents[1])==null)
													if(attendanceStringOperationExecutor.currentSessionAwaitingUserConfirmationOperations.get(this.myUniqueThreadName).endsWith(Boolean.toString(true)))
													{
														toClientOOStrm.writeObject("Batch Insertion/Updation Success!");
														toClientOOStrm.flush();
														logger.info(tempLogMsg += "Batch Insertion/Updation Success!");
													}else
													{
														toClientOOStrm.writeObject("Batch Insertion/Updation Failed!");
														toClientOOStrm.flush();
														logger.error(tempLogMsg += "Batch Insertion/Updation Failed!");
													}
											}else
											{
												queryToExecute = "Command: isLastQuery=false"+"~"+queryToExecuteStrBuffer.toString();
												String whereConditionForConfirmationOfInsertion = " where attendanceDate='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(allDesiredTuples.get(i).getAttendanceDate())+"' and lectureStartTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(allDesiredTuples.get(i).getLectureStartTime())+"' and lectureEndTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(allDesiredTuples.get(i).getLectureEndTime())+"'";
												queryToExecute = queryToExecute + "~" + whereConditionForConfirmationOfInsertion;
												
												//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] Query (Inserting for Non-existing primary key attendance records which are requested by user in batch insert/update)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
												tempLogMsg = "Query (Inserting for Non-existing primary key attendance records which are requested by user in batch insert/update)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
												
												ArrayList<AttendanceRecord> temp;
												temp=attendanceStringOperationExecutor.executeAttendanceStringOperation(this.myUniqueThreadName, attendanceStringComponents[2]+"."+attendanceStringComponents[3], Integer.parseInt(attendanceStringComponents[4]), queryToExecute, attendanceStringComponents[1]);
												if(!(temp!=null && temp.size()==1))			//successful insertion operation of 1 tuple that was non-existing (found by primary key) and requested by user too , indicator = size of returning arraylist = 1(that was just inserted)
													throw new Exception("An exception occured .. retry");
												else
													logger.info(tempLogMsg += "New Record SuccessFully updated that conflicted with pre-existing Attendance Record!");
											}
										}
									}
								}
							}catch(Exception e)
							{
								toClientOOStrm.writeObject(e);
								toClientOOStrm.flush();
								e.printStackTrace();
								logger.error(tempLogMsg += "Execution Failed!");
								logStackTraceOfException(e);
							}
							//removing from saved state for both confirmation as true and false
							attendanceStringOperationExecutor.currentSessionAwaitingUserConfirmationOperations.remove(this.myUniqueThreadName);
						}else if(clientRequest.startsWith("TriggerStringOperation~TRIGGER SINGLE View~"))
						{
							//System.out.println(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread ] Message Received: \""+clientRequest+"\" => Trigger String Operation(TRIGGER SINGLE View) request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+".");
							logger.info("Message Received: \""+clientRequest+"\" => Trigger String Operation(TRIGGER SINGLE View) request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+".");
							
							//Format(Separation char = Tilde(~)):		TriggerStringOperation~chosenMode~DBName~SelectedPresetTableName~numStudents~AttendanceDate(dd/mm/yyyy)~StartTime~EndTime
							triggerStringComponents = clientRequest.trim().split("[~]");
							//SelectedPresetTableName and future_triggers are in SAME DBName
							try
							{
								queryToExecute = "select * from "+triggerStringComponents[2].trim()+"."+"future_triggers where preset_table_name='"+triggerStringComponents[3]+"' and to_insert_in_preset_table_attendanceDate='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.userDisplayAttendanceDateFormat.parse(triggerStringComponents[5]))+"' and to_insert_in_preset_table_lectureStartTime='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[6]))+"' and to_insert_in_preset_table_lectureEndTime='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[7]))+"'";
								//System.out.println(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] 1st Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
								tempLogMsg = "1st Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
								
								ArrayList<edu.vesit.ams.FutureTriggersRecord> allDesiredTuples = triggerStringOperationExecutor.executeTriggerStringOperation(this.myUniqueThreadName, triggerStringComponents[2], Integer.parseInt(triggerStringComponents[4]), queryToExecute, triggerStringComponents[1]);
								toClientOOStrm.writeObject(allDesiredTuples);
								toClientOOStrm.flush();
								
								logger.info(tempLogMsg += "Execution Success .. Sent Results!");
							}catch(Exception e)
							{
								toClientOOStrm.writeObject(e);
								toClientOOStrm.flush();
								e.printStackTrace();
								logger.error(tempLogMsg += "Execution Failed!");
								logStackTraceOfException(e);
							}	
						}else if(clientRequest.startsWith("TriggerStringOperation~TRIGGER BATCH View~"))
						{
							//System.out.println(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread ] Message Received: \""+clientRequest+"\" => Trigger String Operation(BATCH View) request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
							logger.info("Message Received: \""+clientRequest+"\" => Trigger String Operation(BATCH View) request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
							
							//Format(Separation char = Tilde(~)):		TriggerStringOperation~chosenMode~DBName~SelectedPresetTableName~numStudents~AttendanceStartDate(dd/mm/yyyy)~AttendanceEndDate(dd/mm/yyyy)~StartTime~EndTime~true/false(=chkBoxDontConsiderLectureTimes)
							triggerStringComponents = clientRequest.trim().split("[~]");
							
							try
							{
								if(triggerStringComponents[9].equals(Boolean.toString(true)))
									queryToExecute = "select * from "+triggerStringComponents[2].trim()+"."+"future_triggers where preset_table_name='"+triggerStringComponents[3]+"' and (to_insert_in_preset_table_attendanceDate>='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.userDisplayAttendanceDateFormat.parse(triggerStringComponents[5]))+"' and to_insert_in_preset_table_attendanceDate<='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.userDisplayAttendanceDateFormat.parse(triggerStringComponents[6]))+"')";
								else
									queryToExecute = "select * from "+triggerStringComponents[2].trim()+"."+"future_triggers where preset_table_name='"+triggerStringComponents[3]+"' and (to_insert_in_preset_table_attendanceDate>='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.userDisplayAttendanceDateFormat.parse(triggerStringComponents[5]))+"' and to_insert_in_preset_table_attendanceDate<='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.userDisplayAttendanceDateFormat.parse(triggerStringComponents[6]))+"') and to_insert_in_preset_table_lectureStartTime='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[7]))+"' and to_insert_in_preset_table_lectureEndTime='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[8]))+"'";
								//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] 1st Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
								tempLogMsg = "1st Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
								
								ArrayList<edu.vesit.ams.FutureTriggersRecord> allDesiredTuples = triggerStringOperationExecutor.executeTriggerStringOperation(this.myUniqueThreadName, triggerStringComponents[2], Integer.parseInt(triggerStringComponents[4]), queryToExecute, triggerStringComponents[1]);
								toClientOOStrm.writeObject(allDesiredTuples);
								toClientOOStrm.flush();
								
								logger.info(tempLogMsg += "Execution Success .. Sent Results!");
							}catch(Exception e)
							{
								toClientOOStrm.writeObject(e);
								toClientOOStrm.flush();
								e.printStackTrace();
								logger.error(tempLogMsg += "Execution Failed!");
								logStackTraceOfException(e);
							}
						}else if(clientRequest.startsWith("TriggerStringOperation~TRIGGER SINGLE Delete~"))
						{
							//System.out.println(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread ] Message Received: \""+clientRequest+"\" => Trigger String Operation(TRIGGER SINGLE Delete) request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
							logger.info("Message Received: \""+clientRequest+"\" => Trigger String Operation(TRIGGER SINGLE Delete) request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
							
							//Format(Separation char = Tilde(~)):		TriggerStringOperation~chosenMode~DBName~SelectedPresetTableName~numStudents~AttendanceDate(dd/mm/yyyy)~StartTime~EndTime
							triggerStringComponents = clientRequest.trim().split("[~]");
							
							try
							{
								queryToExecute = "select * from "+triggerStringComponents[2].trim()+"."+"future_triggers where preset_table_name='"+triggerStringComponents[3]+"' and to_insert_in_preset_table_attendanceDate='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.userDisplayAttendanceDateFormat.parse(triggerStringComponents[5]))+"' and to_insert_in_preset_table_lectureStartTime='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[6]))+"' and to_insert_in_preset_table_lectureEndTime='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[7]))+"'";
								//System.out.println(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] 1st Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
								tempLogMsg = "1st Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
								
								ArrayList<edu.vesit.ams.FutureTriggersRecord> allDesiredTuples = triggerStringOperationExecutor.executeTriggerStringOperation(this.myUniqueThreadName, triggerStringComponents[2], Integer.parseInt(triggerStringComponents[4]), queryToExecute, triggerStringComponents[1]);
								toClientOOStrm.writeObject(allDesiredTuples);
								toClientOOStrm.flush();
								
								logger.info(tempLogMsg += "Execution Success .. Sent Intermediate Results .. Awaiting Confirmation!");
								
								/*********************************/
								
								Boolean triggerSingleDeleteModeConfirmation = (Boolean)fromClientOIStrm.readObject();	//if true delete and remove from saved state of executor, if false only remove from saved state of executor
								
								if(triggerSingleDeleteModeConfirmation)
								{
									queryToExecute = "delete from "+triggerStringComponents[2].trim()+"."+"future_triggers where preset_table_name='"+triggerStringComponents[3]+"' and to_insert_in_preset_table_attendanceDate='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.userDisplayAttendanceDateFormat.parse(triggerStringComponents[5]))+"' and to_insert_in_preset_table_lectureStartTime='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[6]))+"' and to_insert_in_preset_table_lectureEndTime='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[7]))+"'";
									//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] 2nd Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
									tempLogMsg = "2nd Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
									
									if(triggerStringOperationExecutor.executeTriggerStringOperation(this.myUniqueThreadName, triggerStringComponents[2], Integer.parseInt(triggerStringComponents[4]), queryToExecute, triggerStringComponents[1])==null)
										if(triggerStringOperationExecutor.currentSessionAwaitingUserConfirmationOperations.get(this.myUniqueThreadName).endsWith("true"))		//check deletion query success indicator
										{
											toClientOOStrm.writeObject("Deletion Success!");
											toClientOOStrm.flush();
											logger.info(tempLogMsg += "Deletion Success!");
										}else
										{
											toClientOOStrm.writeObject("Deletion Failed!");
											toClientOOStrm.flush();
											logger.error(tempLogMsg += "Deletion Failed!");
										}
								}else
									logger.info(tempLogMsg += "Deletion Operation canceled by user!");
										
								//removing from saved state for both confirmation as true and false
								triggerStringOperationExecutor.currentSessionAwaitingUserConfirmationOperations.remove(this.myUniqueThreadName);
							}catch(Exception e)
							{
								toClientOOStrm.writeObject(e);
								toClientOOStrm.flush();
								e.printStackTrace();
								logger.error(tempLogMsg += "Execution Failed!");
								logStackTraceOfException(e);
							}
						}else if(clientRequest.startsWith("TriggerStringOperation~TRIGGER BATCH Delete~"))
						{
							//System.out.println(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread ] Message Received: \""+clientRequest+"\" => Trigger String Operation(BATCH Delete) request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
							logger.info("Message Received: \""+clientRequest+"\" => Trigger String Operation(BATCH Delete) request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
							
							//Format(Separation char = Tilde(~)):		TriggerStringOperation~chosenMode~DBName~SelectedPresetTableName~numStudents~AttendanceStartDate(dd/mm/yyyy)~AttendanceEndDate(dd/mm/yyyy)~StartTime~EndTime~true/false(=chkBoxDontConsiderLectureTimes)
							triggerStringComponents = clientRequest.trim().split("[~]");
							
							try
							{
								if(triggerStringComponents[9].equals(Boolean.toString(true)))
									queryToExecute = "select * from "+triggerStringComponents[2].trim()+"."+"future_triggers where preset_table_name='"+triggerStringComponents[3]+"' and (to_insert_in_preset_table_attendanceDate>='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.userDisplayAttendanceDateFormat.parse(triggerStringComponents[5]))+"' and to_insert_in_preset_table_attendanceDate<='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.userDisplayAttendanceDateFormat.parse(triggerStringComponents[6]))+"')";
								else
									queryToExecute = "select * from "+triggerStringComponents[2].trim()+"."+"future_triggers where preset_table_name='"+triggerStringComponents[3]+"' and (to_insert_in_preset_table_attendanceDate>='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.userDisplayAttendanceDateFormat.parse(triggerStringComponents[5]))+"' and to_insert_in_preset_table_attendanceDate<='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.userDisplayAttendanceDateFormat.parse(triggerStringComponents[6]))+"') and to_insert_in_preset_table_lectureStartTime='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[7]))+"' and to_insert_in_preset_table_lectureEndTime='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[8]))+"'";
								//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] 1st Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
								tempLogMsg = "1st Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
								
								ArrayList<edu.vesit.ams.FutureTriggersRecord> allDesiredTuples = triggerStringOperationExecutor.executeTriggerStringOperation(this.myUniqueThreadName, triggerStringComponents[2], Integer.parseInt(triggerStringComponents[4]), queryToExecute, triggerStringComponents[1]);
								toClientOOStrm.writeObject(allDesiredTuples);
								toClientOOStrm.flush();
								
								logger.info(tempLogMsg += "Execution Success .. Sent Intermediate Results .. Awaiting Confirmation!");
								
								/*********************************/
								
								Object obj = fromClientOIStrm.readObject();
								if(obj==null)									//User didnt received any attendance records for his result and sent a null to cancel operation
									logger.info("Deletion Operation canceled by user!");
								else
								{
									Boolean[] triggerBatchDeleteModeConfirmations = (Boolean[])obj;
									boolean userCanceledOperation = true;					//true if user did receive >=1 attendance records but did not want to delete any of them(so all boolean values would be false)
									for(int i=0;i<triggerBatchDeleteModeConfirmations.length;i++)
										if(triggerBatchDeleteModeConfirmations[i])					//if any1 is true, means user wants to delete that attendanceRecord hence its true at that index
										{
											userCanceledOperation = false;
											break;
										}
									
									if(userCanceledOperation)
										logger.info("Deletion Operation canceled by user!");
									else
									{
										StringBuffer queryToExecuteStrBuffer = new StringBuffer("delete from "+triggerStringComponents[2].trim()+".future_triggers where ");
										for(int i=0;i<triggerBatchDeleteModeConfirmations.length;i++)
											if(triggerBatchDeleteModeConfirmations[i])
											{
												queryToExecuteStrBuffer.append("(");
												AttendanceRecord ar = allDesiredTuples.get(i).getAttendanceRecord();
												queryToExecuteStrBuffer.append("preset_table_name='"+allDesiredTuples.get(i).getPresetTableName()+"' and ");
												queryToExecuteStrBuffer.append("to_insert_in_preset_table_attendanceDate='"+AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(ar.getAttendanceDate())+"' and ");
												queryToExecuteStrBuffer.append("to_insert_in_preset_table_lectureStartTime='"+AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(ar.getLectureStartTime())+"' and ");
												queryToExecuteStrBuffer.append("to_insert_in_preset_table_lectureEndTime='"+AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(ar.getLectureEndTime())+"'");
												queryToExecuteStrBuffer.append(") OR ");			//OR all deletion conditions wrt composite primary key
											}
										
										queryToExecute = queryToExecuteStrBuffer.toString();
										if(queryToExecute.endsWith(" OR "))								//remove off last OR
											queryToExecute = queryToExecute.substring(0, queryToExecute.length()-4);
										
										//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] 2nd Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
										tempLogMsg = "2nd Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
										if(triggerStringOperationExecutor.executeTriggerStringOperation(this.myUniqueThreadName, triggerStringComponents[2], Integer.parseInt(triggerStringComponents[4]), queryToExecute, triggerStringComponents[1])==null)
											if(triggerStringOperationExecutor.currentSessionAwaitingUserConfirmationOperations.get(this.myUniqueThreadName).endsWith("true"))		//check deletion query success indicator
											{
												toClientOOStrm.writeObject("Deletion Success!");
												toClientOOStrm.flush();
												logger.info(tempLogMsg += "Deletion Success!");
											}else
											{
												toClientOOStrm.writeObject("Deletion Failed!");
												toClientOOStrm.flush();
												logger.error(tempLogMsg += "Deletion Failed!");
											}
									}
								}
								
								//removing from saved state for both confirmation as true and false
								triggerStringOperationExecutor.currentSessionAwaitingUserConfirmationOperations.remove(this.myUniqueThreadName);
							}catch(Exception e)
							{
								toClientOOStrm.writeObject(e);
								toClientOOStrm.flush();
								e.printStackTrace();
								logger.error(tempLogMsg += "Execution Failed!");
								logStackTraceOfException(e);
							}
						}else if(clientRequest.startsWith("TriggerStringOperation~TRIGGER SINGLE Insert/Update~"))
						{
							//System.out.println(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread ] Message Received: \""+clientRequest+"\" => Trigger String Operation(TRIGGER SINGLE Insert/Update) request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
							logger.info("Message Received: \""+clientRequest+"\" => Trigger String Operation(TRIGGER SINGLE Insert/Update) request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
							
							//Format(Separation char = Tilde(~)):		TriggerStringOperation~chosenMode~DBName~SelectedPresetTableName~numStudents~AttendanceDate(dd/mm/yyyy)~StartTime~EndTime~Presentees/Absentees~(Absentees/PresenteesRollNums) OR ALL~MedicalLeaveRollNumbers(Reason)~OtherReasonsRollNumbers(Reason)
							//Medical and Other Reasons Roll Numbers are separated by backquotes(`)
							triggerStringComponents = clientRequest.trim().split("[~]");
							
							try
							{
								queryToExecute = "select * from "+triggerStringComponents[2].trim()+"."+"future_triggers where preset_table_name='"+triggerStringComponents[3]+"' and to_insert_in_preset_table_attendanceDate='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.userDisplayAttendanceDateFormat.parse(triggerStringComponents[5]))+"' and to_insert_in_preset_table_lectureStartTime='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[6]))+"' and to_insert_in_preset_table_lectureEndTime='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[7]))+"'";
								//System.out.println(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] 1st Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
								tempLogMsg = "1st Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
								
								ArrayList<edu.vesit.ams.FutureTriggersRecord> allDesiredTuples = triggerStringOperationExecutor.executeTriggerStringOperation(this.myUniqueThreadName, triggerStringComponents[2], Integer.parseInt(triggerStringComponents[4]), queryToExecute, triggerStringComponents[1]);
								toClientOOStrm.writeObject(allDesiredTuples);
								toClientOOStrm.flush();
								
								logger.info(tempLogMsg += "Execution Success .. Sent Intermediate Results .. Awaiting Confirmation!");
																
								/****************************************/
								
								Boolean triggerSingleInsertUpdateModeConfirmation[] = (Boolean[]) fromClientOIStrm.readObject();
								StringBuffer queryToExecuteStrBuffer = null;
								if(!triggerSingleInsertUpdateModeConfirmation[0] && triggerSingleInsertUpdateModeConfirmation[1])			//for false, true(keep new or insert new)
								{
									if(allDesiredTuples.size()>0)		//for updation but user choosed option Keep NEW Only, so 1st deleting old(Command=deletingOldBeforeKeepNewOnly~quertToExecute => here command also informing executor not to update insertionupdationSuccessIndicator because its just deleting old attendance record to make place for new(as pk=attendanceDate+start+endtime)) and then proceeding as if its a new insertion
									{
										queryToExecute = "delete from "+triggerStringComponents[2].trim()+"."+"future_triggers where preset_table_name='"+triggerStringComponents[3]+"' and to_insert_in_preset_table_attendanceDate='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.userDisplayAttendanceDateFormat.parse(triggerStringComponents[5]))+"' and to_insert_in_preset_table_lectureStartTime='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[6]))+"' and to_insert_in_preset_table_lectureEndTime='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[7]))+"'";
										queryToExecute = "Command=deletingOldBeforeKeepNewOnly~" + queryToExecute;
										
										//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] 2nd Query (Keep NEW Only .. deleting old record)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
										tempLogMsg = "2nd Query (Keep NEW Only .. deleting old record)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
										
										ArrayList<FutureTriggersRecord> temp;
										temp=triggerStringOperationExecutor.executeTriggerStringOperation(this.myUniqueThreadName, triggerStringComponents[2], Integer.parseInt(triggerStringComponents[4]), queryToExecute, triggerStringComponents[1]);
										if(!(temp!=null && temp.size()==0))			//successful deletion operation(before keep new Only option of updation) indicator
											throw new Exception("An exception occured .. retry");
										else
											logger.info(tempLogMsg += "Old Record SuccessFully deleted to make space for new 1!");
									}
									
									queryToExecuteStrBuffer = new StringBuffer("insert into "+triggerStringComponents[2].trim()+".future_triggers(preset_table_name, to_insert_in_preset_table_attendanceDate, to_insert_in_preset_table_lectureStartTime, to_insert_in_preset_table_lectureEndTime, num_students, ");
									StringBuffer valuesOFInsertQuery = new StringBuffer("values(");
									valuesOFInsertQuery.append("'"+triggerStringComponents[3]+"', ");
									valuesOFInsertQuery.append("'"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.userDisplayAttendanceDateFormat.parse(triggerStringComponents[5]))+"', ");
									valuesOFInsertQuery.append("'"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[6]))+"', ");
									valuesOFInsertQuery.append("'"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[7]))+"', ");
									valuesOFInsertQuery.append(triggerStringComponents[4]+", ");
									
									if(triggerStringComponents[8].equals("Absentees"))
									{
										queryToExecuteStrBuffer.append("absentees, ");
										valuesOFInsertQuery.append("'"+triggerStringComponents[9]+"', ");			//Puts Both None and RollNumbers
										
										if(triggerStringComponents[9].equals("ALL"))			// => Absentees are ALL => Presentees are None
										{
											queryToExecuteStrBuffer.append("presentees, ");
											valuesOFInsertQuery.append("'None', ");
										}
									}else if(triggerStringComponents[8].equals("Presentees"))
									{
										queryToExecuteStrBuffer.append("presentees, ");
										valuesOFInsertQuery.append("'"+triggerStringComponents[9]+"', ");			//Puts Both None and RollNumbers
										
										if(triggerStringComponents[9].equals("ALL"))			// => Presentees are ALL => Absentees are None
										{
											queryToExecuteStrBuffer.append("absentees, ");
											valuesOFInsertQuery.append("'None', ");
										}
									}
									
									queryToExecuteStrBuffer.append("medical_leaves, ");
									valuesOFInsertQuery.append("'"+triggerStringComponents[10].replace('`', '~')+"', ");				//Puts Both None and RollNums(Reason)
									
									queryToExecuteStrBuffer.append("other_leaves)");
									valuesOFInsertQuery.append("'"+triggerStringComponents[11].replace('`', '~')+"')");				//Puts Both None and RollNums(Reason)
									
									queryToExecuteStrBuffer.append(valuesOFInsertQuery);
								}else if(triggerSingleInsertUpdateModeConfirmation[0] && triggerSingleInsertUpdateModeConfirmation[1])			//for true, true(merging/adding new to old)
								{
									int presenteesCnt = 0, absenteesCnt = 0;
									StringBuffer absentees = new StringBuffer(), presentees = new StringBuffer(), medical_leaves = new StringBuffer(), other_leaves = new StringBuffer();
									FutureTriggersRecord mergedNewAddedToOldTriggerRecord = (FutureTriggersRecord)fromClientOIStrm.readObject();
									queryToExecuteStrBuffer = new StringBuffer("update "+triggerStringComponents[2]+".future_triggers set ");
									for(Map.Entry<Integer, String> entry:mergedNewAddedToOldTriggerRecord.getAttendanceRecord().getAttendance().entrySet())
									{
										if(entry.getValue()!=null)
										{
											if(entry.getValue().equals("P"))
											{
												presentees.append(Integer.toString(entry.getKey())+",");
												presenteesCnt++;
											}else if(entry.getValue().equals("A"))
											{
												absentees.append(Integer.toString(entry.getKey())+",");
												absenteesCnt++;
											}else if(entry.getValue().startsWith("M"))
												medical_leaves.append(Integer.toString(entry.getKey())+entry.getValue().substring(entry.getValue().indexOf('('))+",");
											else if(entry.getValue().startsWith("O"))
												other_leaves.append(Integer.toString(entry.getKey())+entry.getValue().substring(entry.getValue().indexOf('('))+",");
										}
									}
									if(presentees.length()==0)
										presentees = new StringBuffer("None");
									else if(presenteesCnt==Integer.parseInt(triggerStringComponents[4]))		//presenteesCnt = numSTudent(ALL)
										presentees = new StringBuffer("ALL");
									else
										presentees = new StringBuffer(presentees.substring(0,  presentees.length()-1));		//remove off last comma
									
									if(absentees.length()==0)
										absentees = new StringBuffer("None");
									else if(absenteesCnt==Integer.parseInt(triggerStringComponents[4]))		//absenteesCnt = numSTudent(ALL)
										absentees = new StringBuffer("ALL");
									else
										absentees = new StringBuffer(absentees.substring(0,  absentees.length()-1));		//remove off last comma
									
									if(medical_leaves.length()==0)
										medical_leaves = new StringBuffer("None");
									else
										medical_leaves = new StringBuffer(medical_leaves.substring(0,  medical_leaves.length()-1));		//remove off last comma
									
									if(other_leaves.length()==0)
										other_leaves = new StringBuffer("None");
									else
										other_leaves = new StringBuffer(other_leaves.substring(0,  other_leaves.length()-1));		//remove off last comma
									
									queryToExecuteStrBuffer.append("presentees='"+presentees+"', absentees='"+absentees+"', medical_leaves='"+medical_leaves+"', other_leaves = '"+other_leaves+"'");
									queryToExecuteStrBuffer.append(" where preset_table_name='"+mergedNewAddedToOldTriggerRecord.getPresetTableName()+"' and to_insert_in_preset_table_attendanceDate='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.parse(triggerStringComponents[5]))+"' and to_insert_in_preset_table_lectureStartTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[6]))+"' and to_insert_in_preset_table_lectureEndTime='"+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[7]))+"'");									
								}else if(triggerSingleInsertUpdateModeConfirmation[0] && !triggerSingleInsertUpdateModeConfirmation[1])			//for true, false(Keep OLD Only)
									logger.info("Insertion/Updation Operation canceled by user!");
								
								if(queryToExecuteStrBuffer!=null)
								{
									queryToExecute = queryToExecuteStrBuffer.toString();
									
									//2nd query if needed updation as Keep NEW only, so 2nd query = delete old 1 so below 1 is 3rd, if updating directly(Add NEW to OLD) then its 2nd query(chcek by 2nd or condition of mode checking)
									//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] "+((allDesiredTuples.size()>0 || (!(!triggerSingleInsertUpdateModeConfirmation[0] && triggerSingleInsertUpdateModeConfirmation[1])))?"3rd":"2nd")+" Query (Insert/Update Query)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
									tempLogMsg = ((allDesiredTuples.size()>0 || (!(!triggerSingleInsertUpdateModeConfirmation[0] && triggerSingleInsertUpdateModeConfirmation[1])))?"3rd":"2nd")+" Query (Insert/Update Query)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
									
									if(triggerStringOperationExecutor.executeTriggerStringOperation(this.myUniqueThreadName, triggerStringComponents[2], Integer.parseInt(triggerStringComponents[4]), queryToExecute, triggerStringComponents[1])==null)
										if(triggerStringOperationExecutor.currentSessionAwaitingUserConfirmationOperations.get(this.myUniqueThreadName).endsWith(Boolean.toString(true)))
										{
											toClientOOStrm.writeObject("Insertion/Updation Success!");
											toClientOOStrm.flush();
											logger.info(tempLogMsg += "Insertion/Updation Success!");
										}else
										{
											toClientOOStrm.writeObject("Insertion/Updation Failed!");
											toClientOOStrm.flush();
											logger.error(tempLogMsg += "Insertion/Updation Failed!");
										}
								}
							}catch(Exception e)
							{
								toClientOOStrm.writeObject(e);
								toClientOOStrm.flush();
								e.printStackTrace();
								logger.error(tempLogMsg += "Execution Failed!");
								logStackTraceOfException(e);
							}
							//removing from saved state for both confirmation as true and false
							triggerStringOperationExecutor.currentSessionAwaitingUserConfirmationOperations.remove(this.myUniqueThreadName);
						}else if(clientRequest.startsWith("TriggerStringOperation~TRIGGER BATCH Insert/Update~"))
						{
							//System.out.println(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread ] Message Received: \""+clientRequest+"\" => Trigger String Operation(TRIGGER BATCH Insert/Update) request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
							logger.info("Message Received: \""+clientRequest+"\" => Trigger String Operation(TRIGGER BATCH Insert/Update) request from: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
							
							//Format(Separation char = Tilde(~)):		TriggerStringOperation~chosenMode~DBName~SelectedPresetTableName~numStudents~AttendanceStartDate(dd/mm/yyyy)~AttendanceEndDate(dd/mm/yyyy)~StartTime~EndTime~Presentees/Absentees~(Absentees/PresenteesRollNums) OR ALL~MedicalLeaveRollNumbers(Reason)~OtherReasonsRollNumbers(Reason)
							triggerStringComponents = clientRequest.trim().split("[~]");
							
							try
							{
								queryToExecute = "select * from "+triggerStringComponents[2].trim()+"."+"future_triggers where preset_table_name='"+triggerStringComponents[3]+"' and (to_insert_in_preset_table_attendanceDate>='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.userDisplayAttendanceDateFormat.parse(triggerStringComponents[5]))+"' and to_insert_in_preset_table_attendanceDate<='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.userDisplayAttendanceDateFormat.parse(triggerStringComponents[6]))+"') and to_insert_in_preset_table_lectureStartTime='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[7]))+"' and to_insert_in_preset_table_lectureEndTime='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[8]))+"'";
								//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] 1st Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
								tempLogMsg = "1st Query("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
								
								ArrayList<edu.vesit.ams.FutureTriggersRecord> allDesiredTuples = triggerStringOperationExecutor.executeTriggerStringOperation(this.myUniqueThreadName, triggerStringComponents[2], Integer.parseInt(triggerStringComponents[4]), queryToExecute, triggerStringComponents[1]);
								toClientOOStrm.writeObject(allDesiredTuples);
								toClientOOStrm.flush();
								
								logger.info(tempLogMsg += "Execution Success .. Sent Intermediate Results .. Awaiting Confirmation!");
								
								/****************************************/
								
								ArrayList<Date> allDatesInRange = new ArrayList<Date>();
								
								//Insertion for Non-existing Trigger Records
								Calendar cal = Calendar.getInstance();
								cal.setTime(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.parse(triggerStringComponents[6]));
								cal.add(Calendar.DATE, 1);
								Date triggerEndDatePlus1Day = cal.getTime();
								for(cal.setTime(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.parse(triggerStringComponents[5]));cal.getTime().before(triggerEndDatePlus1Day);cal.add(Calendar.DATE, 1))
								{
									allDatesInRange.add(cal.getTime());									
									
									boolean foundTriggerPresetTableDateLectureStartEndTimeFlag = false;
									for(FutureTriggersRecord ftr:allDesiredTuples)
										if(ftr.getAttendanceRecord().getAttendanceDate().equals(cal.getTime()) && ftr.getAttendanceRecord().getLectureStartTime().equals(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[7])) && ftr.getAttendanceRecord().getLectureEndTime().equals(edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[8])))
										{
											foundTriggerPresetTableDateLectureStartEndTimeFlag = true;
											break;
										}
									if(foundTriggerPresetTableDateLectureStartEndTimeFlag)
									{
										//Nothing to code here as its handled by below iteration wrt option selected(Keep NEW, Keep OLD, Add to OLD) for that iteration's FutureTriggerRecord
									}else
									{
										//							DITTO SAME QUERY MAKING AS IN SINGLE INSERT/UPDATE BUT HERE BELOW THING IS REPEATED FOR EACH NON-EXISTING PRIMAY KEY=ATTENDATE DATE + LECTURE START TIME + LECTURE END TIME)
										
										//Non-existing trigger record for that date but insertion requested by user during BATCH Insert/Update so performing a insert query => without changing state of executor for this thread, so putting "Command: isLastQuery=false"+queryToExecute else "Command: isLastQuery=true"+queryToExecute for last insertion/updation query so that successIndicator can be set to give user confirmation
										StringBuffer queryToExecuteStrBuffer = new StringBuffer("insert into "+triggerStringComponents[2].trim()+".future_triggers(preset_table_name, to_insert_in_preset_table_attendanceDate, to_insert_in_preset_table_lectureStartTime, to_insert_in_preset_table_lectureEndTime, num_students, ");
										StringBuffer valuesOFInsertQuery = new StringBuffer("values(");
										valuesOFInsertQuery.append("'"+triggerStringComponents[3]+"', ");
										valuesOFInsertQuery.append("'"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(cal.getTime())+"', ");
										valuesOFInsertQuery.append("'"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[7]))+"', ");
										valuesOFInsertQuery.append("'"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[8]))+"', ");
										valuesOFInsertQuery.append(triggerStringComponents[4]+", ");
										
										if(triggerStringComponents[9].equals("Absentees"))
										{
											queryToExecuteStrBuffer.append("absentees, ");
											valuesOFInsertQuery.append("'"+triggerStringComponents[10]+"', ");			//Puts Both None and RollNumbers
											
											if(triggerStringComponents[10].equals("ALL"))			// => Absentees are ALL => Presentees are None
											{
												queryToExecuteStrBuffer.append("presentees, ");
												valuesOFInsertQuery.append("'None', ");
											}
										}else if(triggerStringComponents[9].equals("Presentees"))
										{
											queryToExecuteStrBuffer.append("presentees, ");
											valuesOFInsertQuery.append("'"+triggerStringComponents[10]+"', ");			//Puts Both None and RollNumbers
											
											if(triggerStringComponents[10].equals("ALL"))			// => Presentees are ALL => Absentees are None
											{
												queryToExecuteStrBuffer.append("absentees, ");
												valuesOFInsertQuery.append("'None', ");
											}
										}
										
										queryToExecuteStrBuffer.append("medical_leaves, ");
										valuesOFInsertQuery.append("'"+triggerStringComponents[11].replace('`', '~')+"', ");				//Puts Both None and RollNums(Reason)
										
										queryToExecuteStrBuffer.append("other_leaves)");
										valuesOFInsertQuery.append("'"+triggerStringComponents[12].replace('`', '~')+"')");				//Puts Both None and RollNums(Reason)
										
										queryToExecuteStrBuffer.append(valuesOFInsertQuery);
										
										//If only insertion for all batch insert/update operation => no conflicting as no existing existing records with same date, start and end time(i.e allDesiredTuples.size==0) and we are at insertion of last attendance date
										if(cal.getTime().equals(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.userDisplayAttendanceDateFormat.parse(triggerStringComponents[6])) && allDesiredTuples.size()==0)
										{
											queryToExecute = "Command: isLastQuery=true"+"~"+queryToExecuteStrBuffer.toString();
											//no need of where condition here as we would confirm out insertion using currentSessionAwaitingUserConfirmationOperations.get(this.myUniqueThreadTime).endsWith(true)
											
											//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] LAST Query (Inserting for Non-existing primary key trigger records which are requested by user in batch insert/update)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
											tempLogMsg = "LAST Query (Inserting for Non-existing primary key trigger records which are requested by user in batch insert/update)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
											
											if(triggerStringOperationExecutor.executeTriggerStringOperation(this.myUniqueThreadName, triggerStringComponents[2], Integer.parseInt(triggerStringComponents[4]), queryToExecute, triggerStringComponents[1])==null)
												if(triggerStringOperationExecutor.currentSessionAwaitingUserConfirmationOperations.get(this.myUniqueThreadName).endsWith(Boolean.toString(true)))
												{
													toClientOOStrm.writeObject("Batch Insertion/Updation Success!");
													toClientOOStrm.flush();
													logger.info(tempLogMsg += "Batch Insertion/Updation Success!");
												}else
												{
													toClientOOStrm.writeObject("Batch Insertion/Updation Failed!");
													toClientOOStrm.flush();
													logger.error(tempLogMsg += "Batch Insertion/Updation Failed!");
												}
										}else
										{
											queryToExecute = "Command: isLastQuery=false"+"~"+queryToExecuteStrBuffer.toString();
											String whereConditionForConfirmationOfInsertion = " where preset_table_name='"+triggerStringComponents[3]+"' and to_insert_in_preset_table_attendanceDate='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(cal.getTime())+"' and to_insert_in_preset_table_lectureStartTime='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[7]))+"' and to_insert_in_preset_table_lectureEndTime='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(edu.vesit.DBDataHelper.TriggerStringOperationExecutor.timeUserDisplayFormat.parse(triggerStringComponents[8]))+"'";
											queryToExecute = queryToExecute + "~" + whereConditionForConfirmationOfInsertion;
											
											//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] Query (Inserting for Non-existing primary key trigger records which are requested by user in batch insert/update)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
											tempLogMsg = "Query (Inserting for Non-existing primary key trigger records which are requested by user in batch insert/update)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
											
											ArrayList<FutureTriggersRecord> temp;
											temp=triggerStringOperationExecutor.executeTriggerStringOperation(this.myUniqueThreadName, triggerStringComponents[2], Integer.parseInt(triggerStringComponents[4]), queryToExecute, triggerStringComponents[1]);
											if(temp!=null && temp.size()==1)			//successful insertion operation of 1 tuple that was non-existing (found by primary key) and requested by user too , indicator = size of returning arraylist = 1(that was just inserted)
												logger.info(tempLogMsg += "New Record SuccessFully inserted that didnt conflict with any of pre-existing!");
											else
												throw new Exception("An exception occured .. retry");
										}
									}		
								}
								
								/****************************************/
								
								if(allDesiredTuples.size()>0)
								{
									Boolean batchInsertUpdateModeConfirmations[][] = (Boolean[][]) fromClientOIStrm.readObject();
									@SuppressWarnings("unchecked")
									ArrayList<FutureTriggersRecord> mergedNewAddedToOldFutureTriggerRecordsToSendToServerTriggerBatchInsertUpdateMode = (ArrayList<FutureTriggersRecord>) fromClientOIStrm.readObject();
									for(int i=0;i<batchInsertUpdateModeConfirmations.length;i++)
									{
										StringBuffer queryToExecuteStrBuffer = null;				//fresh new querToExecute Per Trigger Record
										if(!batchInsertUpdateModeConfirmations[i][0] && batchInsertUpdateModeConfirmations[i][1])			//for false, true(keep new or insert new)
										{
											/** DELETING OLD TRIGGER RECORD CODE STARTTED HERE */
											
											queryToExecute = "delete from "+triggerStringComponents[2].trim()+"."+"future_triggers where preset_table_name='"+triggerStringComponents[3]+"' and to_insert_in_preset_table_attendanceDate='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(allDesiredTuples.get(i).getAttendanceRecord().getAttendanceDate())+"' and to_insert_in_preset_table_lectureStartTime='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(allDesiredTuples.get(i).getAttendanceRecord().getLectureStartTime())+"' and to_insert_in_preset_table_lectureEndTime='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(allDesiredTuples.get(i).getAttendanceRecord().getLectureEndTime())+"'";
											queryToExecute = "Command=deletingOldBeforeKeepNewOnly~" + queryToExecute;
											
											//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] Query (Keep NEW Only .. deleting old record)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
											tempLogMsg = "Query (Keep NEW Only .. deleting old record)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
											
											ArrayList<FutureTriggersRecord> temp;
											temp=triggerStringOperationExecutor.executeTriggerStringOperation(this.myUniqueThreadName, triggerStringComponents[2], Integer.parseInt(triggerStringComponents[4]), queryToExecute, triggerStringComponents[1]);
											if(temp!=null && temp.size()==0)			//successful deletion operation(before keep new Only option of updation) indicator
												logger.info(tempLogMsg += "Old Record SuccessFully deleted to make space for new 1!");
											else
												throw new Exception("An exception occured .. retry");
											
											/** DELETING OLD TRIGGER RECORD CODE FINISHED HERE */
											
											queryToExecuteStrBuffer = new StringBuffer("insert into "+triggerStringComponents[2].trim()+".future_triggers(preset_table_name, to_insert_in_preset_table_attendanceDate, to_insert_in_preset_table_lectureStartTime, to_insert_in_preset_table_lectureEndTime, num_students, ");
											StringBuffer valuesOFInsertQuery = new StringBuffer("values(");
											valuesOFInsertQuery.append("'"+triggerStringComponents[3]+"', ");
											valuesOFInsertQuery.append("'"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(allDesiredTuples.get(i).getAttendanceRecord().getAttendanceDate())+"', ");
											valuesOFInsertQuery.append("'"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(allDesiredTuples.get(i).getAttendanceRecord().getLectureStartTime())+"', ");
											valuesOFInsertQuery.append("'"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(allDesiredTuples.get(i).getAttendanceRecord().getLectureEndTime())+"', ");
											valuesOFInsertQuery.append(triggerStringComponents[4]+", ");
											
											if(triggerStringComponents[9].equals("Absentees"))
											{
												queryToExecuteStrBuffer.append("absentees, ");
												valuesOFInsertQuery.append("'"+triggerStringComponents[10]+"', ");			//Puts Both None and RollNumbers
												
												if(triggerStringComponents[10].equals("ALL"))			// => Absentees are ALL => Presentees are None
												{
													queryToExecuteStrBuffer.append("presentees, ");
													valuesOFInsertQuery.append("'None', ");
												}
											}else if(triggerStringComponents[9].equals("Presentees"))
											{
												queryToExecuteStrBuffer.append("presentees, ");
												valuesOFInsertQuery.append("'"+triggerStringComponents[10]+"', ");			//Puts Both None and RollNumbers
												
												if(triggerStringComponents[10].equals("ALL"))			// => Presentees are ALL => Absentees are None
												{
													queryToExecuteStrBuffer.append("absentees, ");
													valuesOFInsertQuery.append("'None', ");
												}
											}
											
											queryToExecuteStrBuffer.append("medical_leaves, ");
											valuesOFInsertQuery.append("'"+triggerStringComponents[11].replace('`', '~')+"', ");				//Puts Both None and RollNums(Reason)
											
											queryToExecuteStrBuffer.append("other_leaves)");
											valuesOFInsertQuery.append("'"+triggerStringComponents[12].replace('`', '~')+"')");				//Puts Both None and RollNums(Reason)
											
											queryToExecuteStrBuffer.append(valuesOFInsertQuery);
										}else if(batchInsertUpdateModeConfirmations[i][0] && batchInsertUpdateModeConfirmations[i][1])			//for true, true(merging/adding new to old)
										{
											FutureTriggersRecord mergedNewAddedToOldTriggerRecord = mergedNewAddedToOldFutureTriggerRecordsToSendToServerTriggerBatchInsertUpdateMode.get(i);
											int presenteesCnt = 0, absenteesCnt = 0;
											StringBuffer absentees = new StringBuffer(), presentees = new StringBuffer(), medical_leaves = new StringBuffer(), other_leaves = new StringBuffer();
											queryToExecuteStrBuffer = new StringBuffer("update "+triggerStringComponents[2]+".future_triggers set ");
											for(Map.Entry<Integer, String> entry:mergedNewAddedToOldTriggerRecord.getAttendanceRecord().getAttendance().entrySet())
											{
												if(entry.getValue()!=null)
												{
													if(entry.getValue().equals("P"))
													{
														presentees.append(Integer.toString(entry.getKey())+",");
														presenteesCnt++;
													}else if(entry.getValue().equals("A"))
													{
														absentees.append(Integer.toString(entry.getKey())+",");
														absenteesCnt++;
													}else if(entry.getValue().startsWith("M"))
														medical_leaves.append(Integer.toString(entry.getKey())+entry.getValue().substring(entry.getValue().indexOf('('))+",");
													else if(entry.getValue().startsWith("O"))
														other_leaves.append(Integer.toString(entry.getKey())+entry.getValue().substring(entry.getValue().indexOf('('))+",");
												}
											}
											if(presentees.length()==0)
												presentees = new StringBuffer("None");
											else if(presenteesCnt==Integer.parseInt(triggerStringComponents[4]))		//presenteesCnt = numSTudent(ALL)
												presentees = new StringBuffer("ALL");
											else
												presentees = new StringBuffer(presentees.substring(0,  presentees.length()-1));		//remove off last comma
											
											if(absentees.length()==0)
												absentees = new StringBuffer("None");
											else if(absenteesCnt==Integer.parseInt(triggerStringComponents[4]))		//absenteesCnt = numSTudent(ALL)
												absentees = new StringBuffer("ALL");
											else
												absentees = new StringBuffer(absentees.substring(0,  absentees.length()-1));		//remove off last comma
											
											if(medical_leaves.length()==0)
												medical_leaves = new StringBuffer("None");
											else
												medical_leaves = new StringBuffer(medical_leaves.substring(0,  medical_leaves.length()-1));		//remove off last comma
											
											if(other_leaves.length()==0)
												other_leaves = new StringBuffer("None");
											else
												other_leaves = new StringBuffer(other_leaves.substring(0,  other_leaves.length()-1));		//remove off last comma
											
											queryToExecuteStrBuffer.append("presentees='"+presentees+"', absentees='"+absentees+"', medical_leaves='"+medical_leaves+"', other_leaves = '"+other_leaves+"'");
											queryToExecuteStrBuffer.append(" where preset_table_name='"+triggerStringComponents[3]+"' and to_insert_in_preset_table_attendanceDate='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(allDesiredTuples.get(i).getAttendanceRecord().getAttendanceDate())+"' and to_insert_in_preset_table_lectureStartTime='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(allDesiredTuples.get(i).getAttendanceRecord().getLectureStartTime())+"' and to_insert_in_preset_table_lectureEndTime='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(allDesiredTuples.get(i).getAttendanceRecord().getLectureEndTime())+"'");									
										}else if(batchInsertUpdateModeConfirmations[i][0] && !batchInsertUpdateModeConfirmations[i][1])			//for true, false(Keep OLD Only)
										{
											AttendanceRecord ar = allDesiredTuples.get(i).getAttendanceRecord();
											logger.info("Insertion/Updation Operation canceled by user for attendanceRecord("+allDesiredTuples.get(i).getPresetTableName()+", "+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.format(ar.getAttendanceDate())+", "+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.format(ar.getLectureStartTime())+", "+edu.vesit.DBDataHelper.AttendanceStringOperationExecutor.timeUserDisplayFormat.format(ar.getLectureEndTime())+")!");
										}
										
										if(queryToExecuteStrBuffer==null)				//if user has chosen old only for current iteration's trigger record
										{
											if(i==batchInsertUpdateModeConfirmations.length-1)								//if current iteration's trigger record = last record then send confirmation of success to client
											{
												toClientOOStrm.writeObject("Batch Insertion/Updation Success!");
												toClientOOStrm.flush();
												logger.info("Batch Insertion/Updation Success!");
											}else
											{
												//nothing to code here as current iteration's attendance is not the last one and for that iteration's trigger record user has requested Keep OLD Only hence queryToExecute==null => true
											}
										}else
										{
											queryToExecute = queryToExecuteStrBuffer.toString();
											
											if(i==batchInsertUpdateModeConfirmations.length-1)				//check if its the last query
											{
												queryToExecute = "Command: isLastQuery=true"+"~"+queryToExecuteStrBuffer.toString();
												//no need of where condition here as we would confirm out insertion using currentSessionAwaitingUserConfirmationOperations.get(this.myUniqueThreadTime).endsWith(true)
												
												//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] LAST Query (Inserting for Non-existing primary key trigger records which are requested by user in batch insert/update)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
												tempLogMsg = "LAST Query (Inserting for Non-existing primary key trigger records which are requested by user in batch insert/update)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
												
												if(triggerStringOperationExecutor.executeTriggerStringOperation(this.myUniqueThreadName, triggerStringComponents[2], Integer.parseInt(triggerStringComponents[4]), queryToExecute, triggerStringComponents[1])==null)
													if(triggerStringOperationExecutor.currentSessionAwaitingUserConfirmationOperations.get(this.myUniqueThreadName).endsWith(Boolean.toString(true)))
													{
														toClientOOStrm.writeObject("Batch Insertion/Updation Success!");
														toClientOOStrm.flush();
														logger.info(tempLogMsg += "Batch Insertion/Updation Success!");
													}else
													{
														toClientOOStrm.writeObject("Batch Insertion/Updation Failed!");
														toClientOOStrm.flush();
														logger.error(tempLogMsg += "Batch Insertion/Updation Failed!");
													}
											}else
											{
												queryToExecute = "Command: isLastQuery=false"+"~"+queryToExecuteStrBuffer.toString();
												String whereConditionForConfirmationOfInsertion = " where preset_table_name='"+triggerStringComponents[3]+"' and to_insert_in_preset_table_attendanceDate='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBAttendanceDateStorageFormat.format(allDesiredTuples.get(i).getAttendanceRecord().getAttendanceDate())+"' and to_insert_in_preset_table_lectureStartTime='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(allDesiredTuples.get(i).getAttendanceRecord().getLectureStartTime())+"' and to_insert_in_preset_table_lectureEndTime='"+edu.vesit.DBDataHelper.TriggerStringOperationExecutor.DBStoragelectureTimeFormat.format(allDesiredTuples.get(i).getAttendanceRecord().getLectureEndTime())+"'";
												queryToExecute = queryToExecute + "~" + whereConditionForConfirmationOfInsertion;
												
												//System.out.print(tempEventDescription = "[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread] Query (Inserting for Non-existing primary key trigger records which are requested by user in batch insert/update)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....");
												tempLogMsg = "Query (Inserting for Non-existing primary key trigger records which are requested by user in batch insert/update)("+queryToExecute+") Execution initiated for: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" ....";
												
												ArrayList<FutureTriggersRecord> temp;
												temp=triggerStringOperationExecutor.executeTriggerStringOperation(this.myUniqueThreadName, triggerStringComponents[2], Integer.parseInt(triggerStringComponents[4]), queryToExecute, triggerStringComponents[1]);
												if(temp!=null && temp.size()==1)			//successful insertion operation of 1 tuple that was non-existing (found by primary key) and requested by user too , indicator = size of returning arraylist = 1(that was just inserted)
													logger.info(tempLogMsg += "New Record SuccessFully updated that conflicted with pre-existing Attendance Record!");
												else
													throw new Exception("An exception occured .. retry");
											}
										}
									}
								}
							}catch(Exception e)
							{
								toClientOOStrm.writeObject(e);
								toClientOOStrm.flush();
								e.printStackTrace();
								logger.error(tempLogMsg += "Execution Failed!");
								logStackTraceOfException(e);
							}
							//removing from saved state for both confirmation as true and false
							triggerStringOperationExecutor.currentSessionAwaitingUserConfirmationOperations.remove(this.myUniqueThreadName);
						}else if(clientRequest.startsWith("LOGOUT_DONE_WITH_ATTENDANCE_MODIFICATIONS "))
						{
							toClientOOStrm.writeObject("OK! Stopping your servicing thread.");
							toClientOOStrm.flush();
							break;
						}
					}
				}
				clientConnectedSocket.close();
				toClientOOStrm.close();
				fromClientOIStrm.close();
				//System.out.println("[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientServicingThread ] Remote Client's: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" {for TeacherUserName: "+forTchrID+"} socket closed (its "+this.getName()+" has been stopped) .. has finished!");
				logger.info("Remote Client's: "+clientConnectedSocket.getInetAddress()+":"+clientConnectedSocket.getPort()+" {for TeacherUserName: "+forTchrID+"} socket closed (its "+this.getName()+" has been stopped) .. has finished!");
				//serverSocket.close();
			}catch(IOException ioe)
			{
				ioe.printStackTrace();
			} catch (ClassNotFoundException cnfe)
			{
				cnfe.printStackTrace();
			}
		}
	}
	class ClientConnListeningThread extends Thread
	{
		volatile boolean serverRunning;					//avoid caching of true value and indicates other threads can change it too
		ServerSocket serverSocket;
		Socket clientConnectedSocket;
		ArrayList<Socket> clientConnectedSockets = new ArrayList<Socket>();
		int clientsConnectedSoFarCnter;
		
		public void run()
		{
			while(serverRunning)
			{
				try
				{
					if(serverSocket==null)
					{
						serverSocket = new ServerSocket(SERVER_LISTENING_PORT);
						serverSocket.setReuseAddress(true);
						//System.out.println("[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientConnListeningThread ] Server started listening on port number: "+SERVER_LISTENING_PORT+" ....");
						logger.info("Server started listening on port number: "+SERVER_LISTENING_PORT+" ....");
						//System.out.println("[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientConnListeningThread ] Waiting for client requests .... ");
						logger.info("Waiting for client requests .... ");
					}
					clientConnectedSocket = serverSocket.accept();
					clientConnectedSockets.add(clientConnectedSocket);
					new ClientServicingThread(clientConnectedSocket, "ClientServicingThread"+(++clientsConnectedSoFarCnter)).start();
				}catch(SocketException se)
				{
					if(se.getMessage().equals("socket closed"))
						//System.out.println("[ "+logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSServer.ClientConnListeningThread ] Connection Listening Server stopped!");
						logger.info("Connection Listening Server stopped!");
				}catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	static public String getMD5OfParam(File serverDataZippedFile)
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
	static public String getMD5OfParam(String s)
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
	static public String getSHA1OfParam(String s)
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
}