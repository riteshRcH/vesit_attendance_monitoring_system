package edu.vesit.DBDataHelper;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.vesit.ams.AndroidLikeToast;
import edu.vesit.ams.AndroidLikeToast.Style;

public class CreatePresetsTablesFrame extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;
	private Connection conn;
	private Statement stmt;
	private ResultSet rs;
	
	private ArrayList<String> alreadyCreatedPresets = new ArrayList<String>();
	
	JPanel jpButtonsToCreatePresets = new JPanel(new GridLayout(0, 1, 0, 15));	//rows, cols, hgap, vgap
	JButton jbtnCreateAllPresets = new JButton();
	
	String DBVendor, DBServerIPPort, dbName;
	
	Logger logger = LoggerFactory.getLogger(CreatePresetsTablesFrame.class);
	
	public CreatePresetsTablesFrame(String DBVendor, String DBServerIPPort, String dbName)
	{
		this.setLayout(new FlowLayout(FlowLayout.CENTER));
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.setVisible(true);
		this.setTitle("Create Attendance Tracking Tables for Presets");
		this.setSize(700, 320);
		this.setResizable(false);
		
		this.DBVendor = DBVendor;
		this.DBServerIPPort = DBServerIPPort;
		this.dbName = dbName;
		
		try
		{
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:"+DBVendor+"://"+DBServerIPPort+"/"+dbName, MainFrame.DBusername, MainFrame.DBpassword);
			stmt = conn.createStatement();
			//System.out.println("[ "+AMSServer.logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSDBDataHelper.CreatePresetsTablesFrame] DB Connection established, ready to create Preset Tables ..");
			logger.info("DB Connection established, ready to create Preset Tables!");
			
			rs = conn.getMetaData().getTables(null, null, "attendance_%", null);
			while(rs.next())
				alreadyCreatedPresets.add(rs.getString("TABLE_NAME").trim());
			rs = null;
			
			rs = stmt.executeQuery("select * from presetsallotment");
			int numOfPresetsToCreate = 0;
			
			while(rs.next())
			{
				StringBuffer tableName = new StringBuffer("attendance_");
				String tempUsernamePWID = rs.getObject("UsernamePWID").toString(), tempStdID = rs.getObject("StdID").toString(), tempBranchID = rs.getObject("BranchID").toString(), tempDivID = rs.getObject("DivID").toString(), tempSubjectID = rs.getObject("SubjectID").toString(), tempLectureType = rs.getObject("LectureType").toString();
				
				ResultSet rsTemp = null;
				Statement stmt2 = conn.createStatement();
				
				rsTemp = stmt2.executeQuery("select TeacherName from registeredteachers where UsernamePWID="+tempUsernamePWID);
				rsTemp.next();
				tableName.append(rsTemp.getObject(1).toString()).append("_");
				rsTemp.close();
				
				//tableName.append(tempBatchID).append("_");			//vesitasms2013to2014 not added
				
				rsTemp = stmt2.executeQuery("select StdYearName from standardyearname where StdID="+tempStdID);
				rsTemp.next();
				tableName.append(rsTemp.getObject(1).toString()).append("_");
				rsTemp.close();
				
				rsTemp = stmt2.executeQuery("select ShortName from branch where BranchID="+tempBranchID);
				rsTemp.next();
				tableName.append(rsTemp.getObject(1).toString()).append("_");
				rsTemp.close();
				
				rsTemp = stmt2.executeQuery("select DivName from divisions where DivID="+tempDivID+" and StdID="+tempStdID+" and BranchID="+tempBranchID);
				rsTemp.next();
				tableName.append(rsTemp.getObject(1).toString()).append("_");
				rsTemp.close();
				
				rsTemp = stmt2.executeQuery("select SubjectCode from lectures where SubjectID="+tempSubjectID);
				rsTemp.next();
				tableName.append(rsTemp.getObject(1).toString()).append("_");
				rsTemp.close();
				
				tableName.append(tempLectureType).append("_");
				
				rsTemp = null;

				tableName = new StringBuffer(tableName.substring(0, tableName.length()-1).trim().toLowerCase());	//remove off last underscore
				if(!(alreadyCreatedPresets.contains(tableName.toString())))				//need toString() as cant test stringbuffer contains in a string arraylist 
				{
					ResultSet rsTempGetNumStudentsInDiv = conn.createStatement().executeQuery("select numStudents from divisions where DivID="+tempDivID+" and StdID="+tempStdID+" and BranchID="+tempBranchID);
					rsTempGetNumStudentsInDiv.next();
					
					JButton jbtn;
					jpButtonsToCreatePresets.add(jbtn = new JButton(tableName.toString() + " => "+rsTempGetNumStudentsInDiv.getObject(1).toString()));		//adding the number of students implies roll num columns to be created later when user clicks the button
					jbtn.addActionListener(this);
					numOfPresetsToCreate++;
					
					rsTempGetNumStudentsInDiv.close();
				}
			}
			if(numOfPresetsToCreate>0)
			{
				this.add(new JLabel("Click on the button to create its preset table; Unique Table Name => Total Roll Numbers"));
				this.add(jbtnCreateAllPresets = new JButton("Create all given Presets"));
				this.add(new JScrollPane(jpButtonsToCreatePresets));
			}else
				this.add(new JLabel("No new Presets to create (add new entry in presetsallotment table to create its attendance table)"));
			jbtnCreateAllPresets.addActionListener(this);
			
			this.addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent we)
				{
					int chosenOption = JOptionPane.showConfirmDialog(CreatePresetsTablesFrame.this, "DB connection pertaining to creation of presets would be closed .. proceed?", "Warning", JOptionPane.YES_NO_OPTION);
					if(chosenOption==JOptionPane.YES_OPTION)
					{
						try
						{
							conn.close();
							CreatePresetsTablesFrame.this.dispose();
						}catch (SQLException e)
						{
							e.printStackTrace();
						}
					}
				}
			});
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void actionPerformed(ActionEvent ae)
	{
		JButton jbtnTemp = ((JButton)ae.getSource()); 
		if(jbtnTemp.equals(jbtnCreateAllPresets))
		{
			if(jpButtonsToCreatePresets.getComponents().length==0)
				JOptionPane.showMessageDialog(CreatePresetsTablesFrame.this, "No preset buttons found!", "Notify", JOptionPane.INFORMATION_MESSAGE);
			else
			{
				int choice = JOptionPane.showConfirmDialog(CreatePresetsTablesFrame.this, new JTextArea("All Attendance tables for the displayed Presets button will be created .. Proceed?"));
				if(choice==JOptionPane.YES_OPTION)
				{
					for(Component c:jpButtonsToCreatePresets.getComponents())
						if(c instanceof JButton)
						{
							JButton jbtn = ((JButton)c);
							jbtn.doClick();
							jpButtonsToCreatePresets.remove(jbtn);
							jpButtonsToCreatePresets.updateUI();
						}
				}
			}
		}else
		{
			String temp[] = jbtnTemp.getActionCommand().split("=>");
			String tableName = temp[0].trim();
			int numStudents = Integer.parseInt(temp[1].trim());
			temp = null;
			
			StringBuffer createAttendanceTableQuery = new StringBuffer("create table "+tableName+"(attendanceDate date, lectureStartTime time, lectureEndTime time, ");
			for(int i=1;i<=numStudents;i++)
				createAttendanceTableQuery.append("`"+i+"` varchar(256) DEFAULT 'P', ");
			createAttendanceTableQuery.append("primary key(attendanceDate, lectureStartTime, lectureEndTime))");
			//varchar to have M (medical reason) and other absent reasons for absentees
			
			try
			{
				stmt.executeUpdate(createAttendanceTableQuery.toString());
				//System.out.println("[ "+AMSServer.logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSDBDataHelper.CreatePresetsTablesFrame ] Created table "+tableName);
				logger.info("Created table "+tableName);
				jpButtonsToCreatePresets.remove(jbtnTemp);
				jpButtonsToCreatePresets.updateUI();
				
				AndroidLikeToast.makeText(CreatePresetsTablesFrame.this, "Successfully created Preset table: "+tableName, AndroidLikeToast.LENGTH_SHORT, Style.SUCCESS).display();
			}catch (SQLException e)
			{
				e.printStackTrace();
				AndroidLikeToast.makeText(CreatePresetsTablesFrame.this, "Error while creating Preset table: "+tableName, AndroidLikeToast.LENGTH_LONG, Style.ERROR).display();
			}
		}
	}
}
