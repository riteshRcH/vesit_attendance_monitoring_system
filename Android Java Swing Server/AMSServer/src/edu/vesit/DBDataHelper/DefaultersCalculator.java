package edu.vesit.DBDataHelper;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.toedter.calendar.JDateChooser;

import edu.vesit.ams.AMSServer;
import edu.vesit.ams.AndroidLikeToast;
import edu.vesit.ams.AndroidLikeToast.Style;
import edu.vesit.ams.AttendanceRecord;
import edu.vesit.ams.BareBonesBrowserLaunch;
import edu.vesit.ams.Preset;

public class DefaultersCalculator extends JFrame
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -3172112421039902358L;
	Connection conn;
	Statement stmt;
	ResultSet rs;
	ArrayList<String> allPresetTableNames = new ArrayList<String>();
	JComboBox<String> jcomboBoxSelectPresetTableName = new JComboBox<String>();
	
	JLabel jlblShowSpecificRollNumsEntry;
	JButton jbtnCalculateDefaultersCmd, jbtnCustomMedicalLeavesConsideration, jbtnCustomOtherLeavesConsideration;
	JTextField jtxtFieldGetSpecificRollNums, jtxfieldGetDefaultersThresHold, jtxfieldGetCriticalDefaultersThresHold;
	JCheckBox jchkboxDontConsiderLectureTimes, jchkboxSelectAllRollNums, jchkboxConsiderAllMedicalLeavesAsAbsentees,jchkboxConsiderAllOtherLeavesAsAbsentees, jchkBoxNeedCustomMedicalConsideration, jchkBoxNeedCustomOtherReasonsConsideration;  
	JSpinner jSpinnerLectureStartTime, jSpinnerLectureEndTime;
	JDateChooser attendanceStartDateChooser, attendanceEndDateChooser; 
	
	LinkedHashMap<String, Boolean> medicalReasonConsiderAsAbsentee = new LinkedHashMap<String, Boolean>(), otherReasonConsiderAsAbsentee = new LinkedHashMap<String, Boolean>();
	LinkedHashMap<Integer, String> attendanceResult = new LinkedHashMap<Integer, String>();		//rollNum => present/outOfLectures(Percentage)
	
	int selectedPresetsNumStudents;
	float selectedPresetsDurationHours, defaultersThreshold, criticalDefaultersThreshold;
	
	Logger logger = LoggerFactory.getLogger(DefaultersCalculator.class);
	String tempLogMsg;
	
	public DefaultersCalculator(String DBVendor, String DBServerIPPort, String dbName)
	{
		setLayout(new FlowLayout(FlowLayout.CENTER));
		this.setSize(505, 500);
		this.setResizable(false);
		this.setTitle("Defaulter Calculator");
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.setLocationRelativeTo(null);
		this.setResizable(false);
		this.setVisible(true);
		
		try
		{
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:"+DBVendor+"://"+DBServerIPPort+"/"+dbName, MainFrame.DBusername, MainFrame.DBpassword);
			//conn = DriverManager.getConnection("jdbc:odbc:MSSQLSampleFinanceInstitution");
			//System.out.println("[ "+edu.vesit.ams.AMSServer.logEventTimestampFormat.format(System.currentTimeMillis())+" - DBHelper ] DB Connection Success for Defaulters Calculation!");
			logger.info("DB Connection Success for Defaulters Calculation!");
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			ResultSet rsAllPresetTables = conn.getMetaData().getTables(null, null, "attendance_%", null);
			while(rsAllPresetTables.next())
			{
				String s = rsAllPresetTables.getString("TABLE_NAME");
				if(!allPresetTableNames.contains(s))
					allPresetTableNames.add(s);
			}
			rsAllPresetTables.close();
		}catch(Exception e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Exception occured while connecting to DB, retry", "Exception occured", JOptionPane.ERROR_MESSAGE);
			this.dispose();
		}
		for(String tableName:allPresetTableNames)
			jcomboBoxSelectPresetTableName.addItem(tableName);
		
		this.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent we)
			{
				if(JOptionPane.showConfirmDialog(DefaultersCalculator.this, "DB connection pertaining to calculation of Defaulters would be closed .. proceed?", "Warning", JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
				{
					try
					{
						conn.close();
						DefaultersCalculator.this.dispose();
					}catch (SQLException e)
					{
						e.printStackTrace();
					}
				}
			}
		});
		jcomboBoxSelectPresetTableName.addItemListener(new ItemListener()
		{	
			@Override
			public void itemStateChanged(ItemEvent arg0)
			{
				Float numStudentsDurationHoursArr[] = getNumStudentsAndDurationForParamPresetTableName(jcomboBoxSelectPresetTableName.getSelectedItem().toString());
				selectedPresetsNumStudents = numStudentsDurationHoursArr[0].intValue();
				selectedPresetsDurationHours = numStudentsDurationHoursArr[1];
				jchkboxSelectAllRollNums.setText("Select ALL Roll Nums(1 to "+selectedPresetsNumStudents+")");
				
				updateSpinnerEndTimePickerWRTDuration();
			}
		});
		
		this.add(new JLabel("Choose Preset: "));
		this.add(jcomboBoxSelectPresetTableName);
		
		this.add(new JLabel("Choose Attendance Start Date: "));
		attendanceStartDateChooser = new JDateChooser();
		attendanceStartDateChooser.setDate(new Date());
		attendanceStartDateChooser.setDateFormatString("dd/MM(MMM)/yyyy");
		/*attendanceStartDateChooser.getDateEditor().addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
		    public void propertyChange(PropertyChangeEvent e)
			{
				if(e.getPropertyName().equals("date"))													//TESTING TO RETRIEVE DATE FROM SPINNER
					System.out.println(e.getPropertyName()+ ": " + (Date) e.getNewValue());
			}
		});*/
		this.add(attendanceStartDateChooser);
		this.add(new JLabel("Choose Attendance End Date: "));
		attendanceEndDateChooser = new JDateChooser();
		attendanceEndDateChooser.setDate(new Date());
		attendanceEndDateChooser.setDateFormatString("dd/MM(MMM)/yyyy");
		this.add(attendanceEndDateChooser);
		
		jSpinnerLectureStartTime = new JSpinner(new SpinnerDateModel());
		jSpinnerLectureStartTime.setEditor(new JSpinner.DateEditor(jSpinnerLectureStartTime, "hh:mm a"));
		jSpinnerLectureStartTime.setValue(new Date());
		jSpinnerLectureStartTime.addChangeListener(new ChangeListener()
		{	
			@Override
			public void stateChanged(ChangeEvent ce)
			{
				updateSpinnerEndTimePickerWRTDuration();
			}
		});
		
		jSpinnerLectureEndTime = new JSpinner(new SpinnerDateModel());
		jSpinnerLectureEndTime.setEditor(new JSpinner.DateEditor(jSpinnerLectureEndTime, "hh:mm a"));
		this.add(jchkboxDontConsiderLectureTimes = new JCheckBox("Dont consider lecture times"));
		jchkboxDontConsiderLectureTimes.addItemListener(new ItemListener()
		{	
			@Override
			public void itemStateChanged(ItemEvent ie)
			{
				jSpinnerLectureStartTime.setEnabled(!jchkboxDontConsiderLectureTimes.isSelected());
				jSpinnerLectureEndTime.setEnabled(!jchkboxDontConsiderLectureTimes.isSelected());
			}
		});
		this.add(jSpinnerLectureStartTime);
		this.add(jSpinnerLectureEndTime);
		
		this.add(jchkboxSelectAllRollNums = new JCheckBox("Select ALL Roll Nums"));
		jchkboxSelectAllRollNums.addItemListener(new ItemListener()
		{	
			@Override
			public void itemStateChanged(ItemEvent ie)
			{
				jlblShowSpecificRollNumsEntry.setEnabled(!jchkboxSelectAllRollNums.isSelected());
				jtxtFieldGetSpecificRollNums.setEnabled(!jchkboxSelectAllRollNums.isSelected());
			}
		});
		Float numStudentsDurationHoursArr[] = getNumStudentsAndDurationForParamPresetTableName(jcomboBoxSelectPresetTableName.getItemAt(0));
		selectedPresetsNumStudents = numStudentsDurationHoursArr[0].intValue();
		selectedPresetsDurationHours = numStudentsDurationHoursArr[1];
		jchkboxSelectAllRollNums.setText("Select ALL Roll Nums(1 to "+selectedPresetsNumStudents+")");
		this.add(new JLabel(" OR "));
		this.add(jlblShowSpecificRollNumsEntry = new JLabel("Enter Specific Roll Nums(Comma Separated, 12-20 = Range)"));
		this.add(jtxtFieldGetSpecificRollNums = new JTextField(11));
		jtxtFieldGetSpecificRollNums.addFocusListener(new FocusListener()
		{	
			@Override
			public void focusLost(FocusEvent fe)
			{
				if(!jchkboxSelectAllRollNums.isSelected() && (jtxtFieldGetSpecificRollNums.getText()!=null || !jtxtFieldGetSpecificRollNums.getText().equals("")))
				{
					try
					{
						//Checking for invalid chars
						jtxtFieldGetSpecificRollNums.setText(removeAllSpacesOfParamString(jtxtFieldGetSpecificRollNums.getText()));
						String enteredText = jtxtFieldGetSpecificRollNums.getText().trim();
						enteredText = enteredText.endsWith(",")?enteredText.substring(0, enteredText.length()-1):enteredText;
						for(int i=0;i<enteredText.length();i++)
						{
							char c = enteredText.charAt(i);
							if(!((c>='0' && c<='9') || c==',' || c=='-'))
								throw new Exception();
						}
						
						//Tryout parsing here only so no error comes later on while query execution
						jtxtFieldGetSpecificRollNums.setText(removeAllSpacesOfParamString(joinArrayElementsByComma(getDistinctValuesOfStringArray(jtxtFieldGetSpecificRollNums.getText().toString().trim().split("[,]"), true))));
						String temp[] = jtxtFieldGetSpecificRollNums.getText().toString().trim().split("[,]");
						for(int i=0;i<temp.length;i++)
						{
							temp[i] = temp[i].trim();
							if(temp[i].contains("-"))
							{
								String temp1[] = temp[i].split("-");
								if(temp1.length==2)
								{
									Integer.parseInt(temp1[0]);
									Integer.parseInt(temp1[1]);
								}else
									throw new Exception();
							}else
								Integer.parseInt(temp[i]);
						}
					}catch(Exception e)
					{
						e.printStackTrace();
						JOptionPane.showMessageDialog(DefaultersCalculator.this, "Invalid entry in rollnums field", "Error", JOptionPane.ERROR_MESSAGE);
						jtxtFieldGetSpecificRollNums.requestFocus();
					}
				}
			}
			@Override
			public void focusGained(FocusEvent fe)
			{
				//Nothing to code
			}
		});
		
		this.add(jchkBoxNeedCustomMedicalConsideration = new JCheckBox("Need Custom Medical", false));
		jchkBoxNeedCustomMedicalConsideration.addItemListener(new ItemListener()
		{	
			@Override
			public void itemStateChanged(ItemEvent ie)
			{
				jbtnCustomMedicalLeavesConsideration.setEnabled(jchkBoxNeedCustomMedicalConsideration.isSelected());
				jchkboxConsiderAllMedicalLeavesAsAbsentees.setEnabled(!jchkBoxNeedCustomMedicalConsideration.isSelected());
			}
		});
		this.add(jchkBoxNeedCustomOtherReasonsConsideration = new JCheckBox("Need Custom Other Reasons", false));
		jchkBoxNeedCustomOtherReasonsConsideration.addItemListener(new ItemListener()
		{	
			@Override
			public void itemStateChanged(ItemEvent ie)
			{
				jbtnCustomOtherLeavesConsideration.setEnabled(jchkBoxNeedCustomOtherReasonsConsideration.isSelected());
				jchkboxConsiderAllOtherLeavesAsAbsentees.setEnabled(!jchkBoxNeedCustomOtherReasonsConsideration.isSelected());
			}
		});
		this.add(jchkboxConsiderAllMedicalLeavesAsAbsentees = new JCheckBox("ALL medical leaves as Absentees", false));
		this.add(new JLabel(" OR "));
		this.add(jbtnCustomMedicalLeavesConsideration = new JButton("Custom Medical Leave Consideration"));
		jbtnCustomMedicalLeavesConsideration.setEnabled(false);
		
		this.add(jchkboxConsiderAllOtherLeavesAsAbsentees = new JCheckBox("ALL other leaves as Absentees"));
		this.add(new JLabel(" OR "));
		this.add(jbtnCustomOtherLeavesConsideration = new JButton("Custom Other Leave Consideration"));
		jbtnCustomOtherLeavesConsideration.setEnabled(false);
		
		this.add(jtxfieldGetDefaultersThresHold = new JTextField("75.0", 9));
		jtxfieldGetDefaultersThresHold.addFocusListener(new FocusListener()
		{	
			@Override
			public void focusLost(FocusEvent fe)
			{
				try
				{
					Float.parseFloat(jtxfieldGetDefaultersThresHold.getText());
				}catch(Exception e)
				{
					JOptionPane.showMessageDialog(DefaultersCalculator.this, "Invalid entry in defaulters threshold", "Error", JOptionPane.ERROR_MESSAGE);
					jtxfieldGetDefaultersThresHold.requestFocus();
				}
			}
			@Override
			public void focusGained(FocusEvent fe)
			{
				//Nothing to code here
			}
		});
		this.add(jtxfieldGetCriticalDefaultersThresHold = new JTextField("50.0", 9));
		jtxfieldGetCriticalDefaultersThresHold.addFocusListener(new FocusListener()
		{	
			@Override
			public void focusLost(FocusEvent fe)
			{
				try
				{
					Float.parseFloat(jtxfieldGetCriticalDefaultersThresHold.getText());
				}catch(Exception e)
				{
					JOptionPane.showMessageDialog(DefaultersCalculator.this, "Invalid entry in critical defaulters threshold", "Error", JOptionPane.ERROR_MESSAGE);
					jtxfieldGetCriticalDefaultersThresHold.requestFocus();
				}
			}
			@Override
			public void focusGained(FocusEvent fe)
			{
				//Nothing to code here
			}
		});
		this.add(jbtnCalculateDefaultersCmd = new JButton("Calculate Defaulters"));
		
		ButtonListener bl = new ButtonListener();
		jbtnCalculateDefaultersCmd.addActionListener(bl);
		jbtnCustomMedicalLeavesConsideration.addActionListener(bl);
		jbtnCustomOtherLeavesConsideration.addActionListener(bl);
		
		updateSpinnerEndTimePickerWRTDuration();
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
	
	private void updateSpinnerEndTimePickerWRTDuration()
	{
		Calendar cal = Calendar.getInstance();
		cal.setTime((Date) jSpinnerLectureStartTime.getValue());
		cal.add(Calendar.HOUR_OF_DAY, (int)selectedPresetsDurationHours);
		cal.add(Calendar.MINUTE, (int)((selectedPresetsDurationHours%1)*60));				//retrievedDurationHours%1 gives fractional part * 60 => minutes to add
		jSpinnerLectureEndTime.setValue(cal.getTime());
	}

	protected Float[] getNumStudentsAndDurationForParamPresetTableName(String selectedPresetTableName)
	{
		String temp[] = selectedPresetTableName.split("[_]");
		String divisionName = temp[4], subjectCode = temp[5], lectureType = temp[6]; 
		String query = "select numStudents from divisions where DivName='"+divisionName+"'";
		
		try
		{
			rs = stmt.executeQuery(query);
			rs.next();
			Float numStudents = (float) rs.getInt("numStudents");
			rs.close();
			
			rs = stmt.executeQuery("select DurationHours from lectures where SubjectCode='"+subjectCode+"' and lectureType='"+lectureType+"'");
			rs.next();
			Float DurationHours = rs.getFloat("DurationHours");
			rs.close();
			
			return new Float[]{numStudents, DurationHours};
		}catch (SQLException e)
		{
			e.printStackTrace();
			return new Float[]{null, null};
		}
	}

	class ButtonListener implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent ae)
		{
			JButton jbtnEventSrc = (JButton) ae.getSource();
					
			if(jbtnEventSrc.equals(jbtnCustomMedicalLeavesConsideration))
				new JDialogCustomMedicalAndOtherLeaveConsiderations(DefaultersCalculator.this, "Custom Medical Reasons Consideration", true);
			else if(jbtnEventSrc.equals(jbtnCustomOtherLeavesConsideration))
				new JDialogCustomMedicalAndOtherLeaveConsiderations(DefaultersCalculator.this, "Custom Other Reasons Consideration", false);
			else if(jbtnEventSrc.equals(jbtnCalculateDefaultersCmd))
			{
				if(!jchkboxSelectAllRollNums.isSelected() && (jtxtFieldGetSpecificRollNums.getText()==null || jtxtFieldGetSpecificRollNums.getText().equals("")))
					JOptionPane.showMessageDialog(DefaultersCalculator.this, "Either select ALL Roll Numbers OR input atleast 1", "Error", JOptionPane.ERROR_MESSAGE);
				else
				{
					StringBuffer reviewStrBuffer = new StringBuffer();
					reviewStrBuffer.append("Preset Name: "+jcomboBoxSelectPresetTableName.getSelectedItem().toString()).append(System.getProperty("line.separator")).append(System.getProperty("line.separator"));
					reviewStrBuffer.append("Attendance Start Date: "+AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.format(attendanceStartDateChooser.getDate())).append(System.getProperty("line.separator"));
					reviewStrBuffer.append("Attendance End Date: "+AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.format(attendanceEndDateChooser.getDate())).append(System.getProperty("line.separator"));
					if(jchkboxDontConsiderLectureTimes.isSelected())
					{
						reviewStrBuffer.append("Lecture Start Time: None").append(System.getProperty("line.separator"));
						reviewStrBuffer.append("Lecture End Time: None").append(System.getProperty("line.separator")).append(System.getProperty("line.separator"));
					}else
					{
						reviewStrBuffer.append("Lecture Start Time: "+Preset.timeUserDisplayFormat.format(jSpinnerLectureStartTime.getValue())).append(System.getProperty("line.separator"));
						reviewStrBuffer.append("Lecture End Time: "+Preset.timeUserDisplayFormat.format(jSpinnerLectureEndTime.getValue())).append(System.getProperty("line.separator")).append(System.getProperty("line.separator"));
					}
					reviewStrBuffer.append("Calculate Defaulters for range of roll numbers: "+(jchkboxSelectAllRollNums.isSelected()?jchkboxSelectAllRollNums.getText().substring(0, jchkboxSelectAllRollNums.getText().indexOf("ALL")):jtxtFieldGetSpecificRollNums.getText())).append(System.getProperty("line.separator")).append(System.getProperty("line.separator"));
					reviewStrBuffer.append("Defaulters Threshold: "+(defaultersThreshold=Float.parseFloat(jtxfieldGetDefaultersThresHold.getText())));
					reviewStrBuffer.append("Critical Defaulters Threshold: "+(criticalDefaultersThreshold=Float.parseFloat(jtxfieldGetCriticalDefaultersThresHold.getText())));
					
					JTextArea jtxtArea = new JTextArea(reviewStrBuffer.toString());
					jtxtArea.setLineWrap(true);
					jtxtArea.setEditable(false);
					if(JOptionPane.showConfirmDialog(DefaultersCalculator.this, new JScrollPane(jtxtArea), "Review and Submit ..", JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION)
					{
						AndroidLikeToast.makeText(DefaultersCalculator.this, "Defaulters Calculation initiated!", AndroidLikeToast.LENGTH_SHORT).display();
						
						StringBuffer[][] attendanceResultDisplayString = new StringBuffer[selectedPresetsNumStudents + 1][7];
						//Per student = 1 row = its rollnum = index to array => columns:	all LectureAttendanceValues sequentially, [Comma separated LectureDate(StartTime-EndTime) for that present day], [Comma separated LectureDate(StartTime-EndTime) for that absent day], [Comma separated LectureDate(StartTime-EndTime)=>MedicalReason for that medical leave day], [Comma separated LectureDate(StartTime-EndTime)=>OtherReason for that other leaves day], [absent + medical/other leaves considered as absent-set earlier], [present + medical/other leaves considered as present-set earlier] 
						int[][] attendanceResultCounts = new int[selectedPresetsNumStudents + 1][7];
						//Per student = 1 row = its rollnum = index to array => columns:	TotalAttendanceRecordHavingThatRollNum, PresenteeCnt, AbsenteeCnt, MedicalLeavesCnt, OtherReasonsCnt, TotalAbsenteeCnt, TotalPresenteeCnt
						for(int i=0;i<attendanceResultDisplayString.length;i++)
							for(int j=0;j<attendanceResultDisplayString[i].length;j++)
								attendanceResultDisplayString[i][j] = new StringBuffer();
						
						String defaultersCalculationQuery, enteredRollNums[] = null;
						if(jchkboxDontConsiderLectureTimes.isSelected())
						{
							if(jchkboxSelectAllRollNums.isSelected())
								defaultersCalculationQuery = "select * from "+jcomboBoxSelectPresetTableName.getSelectedItem().toString()+" where (attendanceDate>='"+AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(attendanceStartDateChooser.getDate())+"' and attendanceDate<='"+AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(attendanceEndDateChooser.getDate())+"')";
							else
							{
								defaultersCalculationQuery = "select attendanceDate, lectureStartTime, lectureEndTime, ";
								String temp[] = enteredRollNums = jtxtFieldGetSpecificRollNums.getText().split("[,]");
								for(int i=0;i<temp.length-1;i++)
									if(temp[i].contains("-"))	//Range of RollNums
										for(int j=Integer.parseInt(temp[i].trim().split("[-]")[0]);j<=Integer.parseInt(temp[i].trim().split("[-]")[1]);j++)
											defaultersCalculationQuery += "`"+j+"`, ";
									else
										defaultersCalculationQuery += "`"+temp[i]+"`, ";
								
								//For last rollnumber
								if(temp[temp.length-1].contains("-"))	//Range of RollNums
									for(int j=Integer.parseInt(temp[temp.length-1].trim().split("[-]")[0]);j<=Integer.parseInt(temp[temp.length-1].trim().split("[-]")[1]);j++)
										defaultersCalculationQuery += "`"+j+"` ";
								else
									defaultersCalculationQuery += "`"+temp[temp.length-1]+"` ";
								defaultersCalculationQuery += " from "+jcomboBoxSelectPresetTableName.getSelectedItem().toString()+" where (attendanceDate>='"+AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(attendanceStartDateChooser.getDate())+"' and attendanceDate<='"+AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(attendanceEndDateChooser.getDate())+"')";
							}
						}else
						{
							if(jchkboxSelectAllRollNums.isSelected())
								defaultersCalculationQuery = "select * from "+jcomboBoxSelectPresetTableName.getSelectedItem().toString()+" where (attendanceDate>='"+AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(attendanceStartDateChooser.getDate())+"' and attendanceDate<='"+AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(attendanceEndDateChooser.getDate())+"') and lectureStartTime='"+AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format((Date)jSpinnerLectureStartTime.getValue())+"' and lectureEndTime='"+AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format((Date)jSpinnerLectureEndTime.getValue())+"'";
							else
							{
								defaultersCalculationQuery = "select attendanceDate, lectureStartTime, lectureEndTime, ";
								String temp[] = enteredRollNums = removeAllSpacesOfParamString(jtxtFieldGetSpecificRollNums.getText()).split("[,]");
								for(int i=0;i<temp.length-1;i++)
									if(temp[i].contains("-"))	//Range of RollNums
										for(int j=Integer.parseInt(temp[i].trim().split("[-]")[0].trim());j<=Integer.parseInt(temp[i].trim().split("[-]")[1].trim());j++)
											defaultersCalculationQuery += "`"+j+"`, ";
									else
										defaultersCalculationQuery += "`"+temp[i].trim()+"`, ";
								
								//For last rollnumber
								if(temp[temp.length-1].contains("-"))	//Range of RollNums
									for(int j=Integer.parseInt(temp[temp.length-1].trim().split("[-]")[0]);j<=Integer.parseInt(temp[temp.length-1].trim().split("[-]")[1]);j++)
										defaultersCalculationQuery += "`"+j+"` ";
								else
									defaultersCalculationQuery += "`"+temp[temp.length-1].trim()+"` ";
								defaultersCalculationQuery += " from "+jcomboBoxSelectPresetTableName.getSelectedItem().toString()+" where (attendanceDate>='"+AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(attendanceStartDateChooser.getDate())+"' and attendanceDate<='"+AttendanceStringOperationExecutor.DBAttendanceDateStorageFormat.format(attendanceEndDateChooser.getDate())+"') and lectureStartTime='"+AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format((Date)jSpinnerLectureStartTime.getValue())+"' and lectureEndTime='"+AttendanceStringOperationExecutor.DBStoragelectureTimeFormat.format((Date)jSpinnerLectureEndTime.getValue())+"'";
							}
						}
						try
						{
							if(!jchkboxSelectAllRollNums.isSelected())
								Collections.sort(Arrays.asList(enteredRollNums));
							//System.out.print("[ "+edu.vesit.ams.AMSServer.logEventTimestampFormat.format(System.currentTimeMillis())+" - DBHelper ] Execution initiated for Defaulters Calculation Query("+defaultersCalculationQuery+") ....");
							tempLogMsg = "Execution initiated for Defaulters Calculation Query("+defaultersCalculationQuery+") ....";
							rs = stmt.executeQuery(defaultersCalculationQuery);
							int totalLectureCount = 0;
							ArrayList<AttendanceRecord> allDesiredTuples = new ArrayList<AttendanceRecord>();
							while(rs.next())
							{
								totalLectureCount++;
								
								AttendanceRecord attendanceRecord = new AttendanceRecord(rs.getDate("attendanceDate"), rs.getTime("lectureStartTime"), rs.getTime("lectureEndTime"));
								for(int i=1;i<=selectedPresetsNumStudents;i++)
									if(jchkboxSelectAllRollNums.isSelected())
										attendanceRecord.addAttendanceOfNewRollNum(i, rs.getString(Integer.toString(i)));
									else
									{
										if(Arrays.binarySearch(enteredRollNums, Integer.toString(i))>=0)
											attendanceRecord.addAttendanceOfNewRollNum(i, rs.getString(Integer.toString(i)));
									}
								allDesiredTuples.add(attendanceRecord);
								for(Map.Entry<Integer, String> entry:attendanceRecord.getAttendance().entrySet())
								{
									attendanceResultDisplayString[entry.getKey()][0].append(AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+"("+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+" - "+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+") => "+entry.getValue()+", ");
									attendanceResultCounts[entry.getKey()][0]++;
									if(entry.getValue().equals("P"))
									{
										attendanceResultCounts[entry.getKey()][1]++;
										attendanceResultDisplayString[entry.getKey()][1].append(AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+"("+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+" - "+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+")"+", ");
										
										attendanceResultCounts[entry.getKey()][6]++;		//TotalPresenteeCnt++
										attendanceResultDisplayString[entry.getKey()][6].append(AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+"("+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+" - "+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+")"+", ");
									}else if(entry.getValue().equals("A"))
									{
										attendanceResultCounts[entry.getKey()][2]++;
										attendanceResultDisplayString[entry.getKey()][2].append(AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+"("+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+" - "+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+")"+", ");
										
										attendanceResultCounts[entry.getKey()][5]++;		//TotalAbsenteeCnt++
										attendanceResultDisplayString[entry.getKey()][5].append(AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+"("+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+" - "+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+")"+", ");
									}else if(entry.getValue().startsWith("M("))
									{
										attendanceResultCounts[entry.getKey()][3]++;
										attendanceResultDisplayString[entry.getKey()][3].append(AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+"("+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+" - "+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+")"+", ");
										
										String medicalReason = entry.getValue().substring(entry.getValue().indexOf('('), entry.getValue().lastIndexOf(')'));
										if(jchkBoxNeedCustomMedicalConsideration.isSelected())
										{
											if(medicalReasonConsiderAsAbsentee.get(medicalReason))
											{
												attendanceResultCounts[entry.getKey()][5]++;		//TotalAbsenteeCnt++
												attendanceResultDisplayString[entry.getKey()][5].append(AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+"("+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+" - "+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+")"+", ");
											}else
											{
												attendanceResultCounts[entry.getKey()][6]++;		//TotalPresenteeCnt++
												attendanceResultDisplayString[entry.getKey()][6].append(AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+"("+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+" - "+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+")"+", ");
											}
										}else
										{
											if(jchkboxConsiderAllMedicalLeavesAsAbsentees.isSelected())
											{
												attendanceResultCounts[entry.getKey()][5]++;		//TotalAbsenteeCnt++
												attendanceResultDisplayString[entry.getKey()][5].append(AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+"("+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+" - "+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+")"+", ");
											}else
											{
												attendanceResultCounts[entry.getKey()][6]++;		//TotalPresenteeCnt++
												attendanceResultDisplayString[entry.getKey()][6].append(AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+"("+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+" - "+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+")"+", ");
											}
										}
									}else if(entry.getValue().startsWith("O("))
									{
										attendanceResultCounts[entry.getKey()][4]++;
										attendanceResultDisplayString[entry.getKey()][4].append(AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+"("+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+" - "+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+")"+", ");
										
										String otherReason = entry.getValue().substring(entry.getValue().indexOf('('), entry.getValue().lastIndexOf(')'));
										if(jchkBoxNeedCustomOtherReasonsConsideration.isSelected())
										{
											if(otherReasonConsiderAsAbsentee.get(otherReason))
											{
												attendanceResultCounts[entry.getKey()][5]++;		//TotalAbsenteeCnt++
												attendanceResultDisplayString[entry.getKey()][5].append(AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+"("+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+" - "+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+")"+", ");
											}else
											{
												attendanceResultCounts[entry.getKey()][6]++;		//TotalPresenteeCnt++
												attendanceResultDisplayString[entry.getKey()][6].append(AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+"("+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+" - "+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+")"+", ");
											}
										}else
										{
											if(jchkboxConsiderAllOtherLeavesAsAbsentees.isSelected())
											{
												attendanceResultCounts[entry.getKey()][5]++;		//TotalAbsenteeCnt++
												attendanceResultDisplayString[entry.getKey()][5].append(AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+"("+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+" - "+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+")"+", ");
											}else
											{
												attendanceResultCounts[entry.getKey()][6]++;		//TotalPresenteeCnt++
												attendanceResultDisplayString[entry.getKey()][6].append(AttendanceStringOperationExecutor.userDisplayAttendanceDateFormat.format(attendanceRecord.getAttendanceDate())+"("+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureStartTime())+" - "+AttendanceStringOperationExecutor.timeUserDisplayFormat.format(attendanceRecord.getLectureEndTime())+")"+", ");
											}
										}
									}
								}
							}
							logger.debug(tempLogMsg += "Execution Success!");
							if(totalLectureCount==0)
								JOptionPane.showMessageDialog(DefaultersCalculator.this, "No lectures present for given time durations", "Notify", JOptionPane.INFORMATION_MESSAGE);
							else
							{
								//Creating XML and XSLT File to display in browser + allowing PHP parsing to send SMS
								SimpleDateFormat sdf = new SimpleDateFormat("ddMMMyyyy");
								if(!AMSServer.attendanceResultsOutputDataDir.exists())
									AMSServer.attendanceResultsOutputDataDir.mkdirs();
								File defaultersOutputXMLFile = new File(AMSServer.attendanceResultsOutputDataDir, jcomboBoxSelectPresetTableName.getSelectedItem().toString()+"("+sdf.format(attendanceStartDateChooser.getDate())+" to "+sdf.format(attendanceEndDateChooser.getDate())+").xml");
								File defaultersOutputXSLFile = new File(AMSServer.attendanceResultsOutputDataDir, jcomboBoxSelectPresetTableName.getSelectedItem().toString()+"("+sdf.format(attendanceStartDateChooser.getDate())+" to "+sdf.format(attendanceEndDateChooser.getDate())+").xsl");
								File defaultersOutputTextFile = new File(AMSServer.attendanceResultsOutputDataDir, jcomboBoxSelectPresetTableName.getSelectedItem().toString()+"("+sdf.format(attendanceStartDateChooser.getDate())+" to "+sdf.format(attendanceEndDateChooser.getDate())+").txt");
								PrintWriter pw;
								try
								{
									if(!defaultersOutputXMLFile.exists())
										defaultersOutputXMLFile.createNewFile();
									if(!defaultersOutputXSLFile.exists())
										defaultersOutputXSLFile.createNewFile();
									if(!defaultersOutputTextFile.exists())
										defaultersOutputTextFile.createNewFile();
									
									//Attendance Results XML File Writing
									pw = new PrintWriter(defaultersOutputXMLFile);
									pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
									pw.println("<?xml-stylesheet type=\"text/xsl\" href=\""+defaultersOutputXSLFile.getName()+"\"?>");
									pw.println("<AttendanceResultsRootTag>");
									pw.println("<Properties>");
									pw.println("<ConsideredLectureTimes>"+!jchkboxDontConsiderLectureTimes.isSelected()+"</ConsideredLectureTimes>");
									if(!jchkboxDontConsiderLectureTimes.isSelected())
									{
										pw.println("<ConsideredLectureStartTime>"+Preset.timeUserDisplayFormat.format(jSpinnerLectureStartTime.getValue())+"</ConsideredLectureStartTime>");
										pw.println("<ConsideredLectureEndTime>"+Preset.timeUserDisplayFormat.format(jSpinnerLectureEndTime.getValue())+"</ConsideredLectureEndTime>");
									}
									pw.println("<RollNumsRange>"+(jchkboxSelectAllRollNums.isSelected()?jchkboxSelectAllRollNums.getText().substring(jchkboxSelectAllRollNums.getText().indexOf("ALL")):jtxtFieldGetSpecificRollNums.getText())+"</RollNumsRange>");
									pw.println("<MedicalLeavesConsiderations>"+(jchkBoxNeedCustomMedicalConsideration.isSelected()?medicalReasonConsiderAsAbsentee.toString():"ALL "+(jchkboxConsiderAllMedicalLeavesAsAbsentees.isSelected()?"Absentees":"Presentees"))+"</MedicalLeavesConsiderations>");
									pw.println("<OtherLeavesConsiderations>"+(jchkBoxNeedCustomOtherReasonsConsideration.isSelected()?otherReasonConsiderAsAbsentee.toString():"ALL "+(jchkboxConsiderAllOtherLeavesAsAbsentees.isSelected()?"Absentees":"Presentees"))+"</OtherLeavesConsiderations>");
									pw.println("<DefaultersThreshold>"+defaultersThreshold+"</DefaultersThreshold>");
									pw.println("<CriticalDefaultersThreshold>"+criticalDefaultersThreshold+"</CriticalDefaultersThreshold>");
									pw.println("</Properties>");
									pw.println("<AttendanceResults>");
									if(jchkboxSelectAllRollNums.isSelected())
										for(int i=1;i<=selectedPresetsNumStudents;i++)
										{
											pw.println("<Student rollNum=\""+i+"\">");
											
											pw.println("<AllAttendanceEntriesCount>"+attendanceResultCounts[i][0]+"</AllAttendanceEntriesCount>");
											pw.println("<AllAttendanceEntries>"+attendanceResultDisplayString[i][0]+"</AllAttendanceEntries>");
											
											pw.println("<PresenteeCount>"+attendanceResultCounts[i][1]+"</PresenteeCount>");
											pw.println("<PresentDays>"+attendanceResultDisplayString[i][1]+"</PresentDays>");
											
											pw.println("<AbsenteeCount>"+attendanceResultCounts[i][2]+"</AbsenteeCount>");
											pw.println("<AbsentDays>"+attendanceResultDisplayString[i][2]+"</AbsentDays>");
											
											pw.println("<MedicalLeavesCount>"+attendanceResultCounts[i][3]+"</MedicalLeavesCount>");
											pw.println("<MedicalLeavesDays>"+attendanceResultDisplayString[i][3]+"</MedicalLeavesDays>");
											
											pw.println("<OtherLeavesCount>"+attendanceResultCounts[i][4]+"</OtherLeavesCount>");
											pw.println("<OtherLeavesDays>"+attendanceResultDisplayString[i][4]+"</OtherLeavesDays>");
											
											pw.println("<TotalPresenteeCount>"+attendanceResultCounts[i][6]+"</TotalPresenteeCount>");
											pw.println("<TotalPresentDays>"+attendanceResultDisplayString[i][6]+"</TotalPresentDays>");
											
											pw.println("<TotalAbsenteeCount>"+attendanceResultCounts[i][5]+"</TotalAbsenteeCount>");
											pw.println("<TotalAbsentDays>"+attendanceResultDisplayString[i][5]+"</TotalAbsentDays>");
											
											pw.println("<AttendanceAbsenteeCount>"+attendanceResultCounts[i][5]+" out of "+totalLectureCount+"</AttendanceAbsenteeCount>");
											pw.println("<AttendancePresenteeCount>"+attendanceResultCounts[i][6]+" out of "+totalLectureCount+"</AttendancePresenteeCount>");
											
											DecimalFormat roundUPTo2Precision = new DecimalFormat("###.##");
											pw.println("<AttendancePercentage>"+roundUPTo2Precision.format(((double)attendanceResultCounts[i][6])/((double)totalLectureCount)*100.00)+"</AttendancePercentage>");
											
											pw.println("</Student>");
										}
									else
									{
										for(int i=0;i<enteredRollNums.length;i++)
										{
											int rollNum = Integer.parseInt(enteredRollNums[i]);
											pw.println("<Student rollNum=\""+rollNum+"\">");
											
											pw.println("<AllAttendanceEntriesCount>"+attendanceResultCounts[rollNum][0]+"</AllAttendanceEntriesCount>");
											pw.println("<AllAttendanceEntries>"+attendanceResultDisplayString[rollNum][0]+"</AllAttendanceEntries>");
											
											pw.println("<PresenteeCount>"+attendanceResultCounts[rollNum][1]+"</PresenteeCount>");
											pw.println("<PresentDays>"+attendanceResultDisplayString[rollNum][1]+"</PresentDays>");
											
											pw.println("<AbsenteeCount>"+attendanceResultCounts[rollNum][2]+"</AbsenteeCount>");
											pw.println("<AbsentDays>"+attendanceResultDisplayString[rollNum][2]+"</AbsentDays>");
											
											pw.println("<MedicalLeavesCount>"+attendanceResultCounts[rollNum][3]+"</MedicalLeavesCount>");
											pw.println("<MedicalLeavesDays>"+attendanceResultDisplayString[rollNum][3]+"</MedicalLeavesDays>");
											
											pw.println("<OtherLeavesCount>"+attendanceResultCounts[rollNum][4]+"</OtherLeavesCount>");
											pw.println("<OtherLeavesDays>"+attendanceResultDisplayString[rollNum][4]+"</OtherLeavesDays>");
											
											pw.println("<TotalPresenteeCount>"+attendanceResultCounts[rollNum][6]+"</TotalPresenteeCount>");
											pw.println("<TotalPresentDays>"+attendanceResultDisplayString[rollNum][6]+"</TotalPresentDays>");
											
											pw.println("<TotalAbsenteeCount>"+attendanceResultCounts[rollNum][5]+"</TotalAbsenteeCount>");
											pw.println("<TotalAbsentDays>"+attendanceResultDisplayString[rollNum][5]+"</TotalAbsentDays>");
											
											pw.println("<AttendanceAbsenteeCount>"+attendanceResultCounts[rollNum][5]+" out of "+totalLectureCount+"</AttendanceAbsenteeCount>");
											pw.println("<AttendancePresenteeCount>"+attendanceResultCounts[rollNum][6]+" out of "+totalLectureCount+"</AttendancePresenteeCount>");
											
											DecimalFormat roundUPTo2Precision = new DecimalFormat("###.##");
											pw.println("<AttendancePercentage>"+roundUPTo2Precision.format(((double)attendanceResultCounts[rollNum][6])/((double)totalLectureCount)*100.00)+"</AttendancePercentage>");
											
											pw.println("</Student>");
										}
									}
									pw.println("</AttendanceResults>");
									pw.print("</AttendanceResultsRootTag>");
									pw.close();
									
									//Attendance Results XSL File Writing
									pw = new PrintWriter(defaultersOutputXSLFile);
									pw.println("<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">");
									pw.println("<xsl:template match=\"/AttendanceResultsRootTag\">");
									pw.println("<html>");
									
									pw.println("<head>");
									pw.println("<title>Attendance Results</title>");
									
									pw.println("<style type=\"text/css\">");
									pw.println(".non-defaulters");
									pw.println("{background: #28A828;}");
									pw.println(".defaulters");
									pw.println("{background: orange}");
									pw.println(".critical-defaulters");
									pw.println("{background: red}");
									pw.println("</style>");
									
									pw.println("<script>");
									pw.println("function formatAndGetExtraDetails(key, value)");
									pw.println("{");
									pw.println("return (value==undefined || value=='')?'None':value.replace(/,/g, \"\\n\")");
									pw.println("}");
									pw.println("</script>");
									pw.println("</head>");
									
									pw.println("<body>");
									
									pw.println("<h4>Properties");
									pw.println("<ul>");
									pw.println("<li>ConsideredLectureTimes => <xsl:value-of select=\"Properties/ConsideredLectureTimes\" /></li><br />");
									if(!jchkboxDontConsiderLectureTimes.isSelected())
									{
										pw.println("<li>ConsideredLectureTimes => <xsl:value-of select=\"Properties/ConsideredLectureStartTime\" /></li><br />");
										pw.println("<li>ConsideredLectureTimes => <xsl:value-of select=\"Properties/ConsideredLectureEndTime\" /></li><br />");
									}
									pw.println("<li>RollNumsRange => <xsl:value-of select=\"Properties/RollNumsRange\" /></li><br />");
									pw.println("<li>MedicalLeavesConsiderations => <xsl:value-of select=\"Properties/MedicalLeavesConsiderations\" /></li><br />");
									pw.println("<li>OtherLeavesConsiderations => <xsl:value-of select=\"Properties/OtherLeavesConsiderations\" /></li><br />");
									pw.println("<li>DefaultersThreshold => <xsl:value-of select=\"Properties/DefaultersThreshold\" /></li><br />");
									pw.println("<li>CriticalDefaultersThreshold => <xsl:value-of select=\"Properties/CriticalDefaultersThreshold\" /></li><br />");
									pw.println("</ul>");
									pw.println("</h4>");
									
									pw.println("<table border=\"4\" bordercolor=\"blue\">");
									pw.println("<tr>");
									pw.println("<th align=\"center\">RollNum</th>");
									pw.println("<th align=\"center\">AllAttendanceEntriesCount</th>");
									pw.println("<th align=\"center\">PresenteeCount</th>");
									pw.println("<th align=\"center\">AbsenteeCount</th>");
									pw.println("<th align=\"center\">MedicalLeavesCount</th>");
									pw.println("<th align=\"center\">OtherLeavesCount</th>");
									pw.println("<th align=\"center\">TotalPresenteeCount(Inclusive of medical/other leaves)</th>");
									pw.println("<th align=\"center\">TotalAbsenteeCount(Inclusive of medical/other leaves)</th>");
									pw.println("<th align=\"center\">AttendanceAbsenteeCount(Inclusive of medical/other leaves)</th>");
									pw.println("<th align=\"center\">AttendancePresenteeCount(Inclusive of medical/other leaves)</th>");
									pw.println("<th align=\"center\">AttendancePercentage(Inclusive of medical/other leaves)</th>");
									pw.println("</tr>");
									pw.println("<xsl:for-each select=\"AttendanceResults/Student\">");
									pw.println("<xsl:variable name=\"PercentageValue\">");
									pw.println("<xsl:value-of select=\"number(AttendancePercentage)\" />");
									pw.println("</xsl:variable>");
									pw.println("<xsl:variable name=\"classValue\">");
									pw.println("<xsl:choose>");
									pw.println("<xsl:when test=\"$PercentageValue &gt;= 75.0\">non-defaulters</xsl:when>");
									pw.println("<xsl:when test=\"$PercentageValue &lt; 75.0 and $PercentageValue &gt;= 50.0\">defaulters</xsl:when>");
									pw.println("<xsl:otherwise>critical-defaulters</xsl:otherwise>");
									pw.println("</xsl:choose>");
									pw.println("</xsl:variable>");
									pw.println("<tr class=\"{$classValue}\">");
									pw.println("<td align=\"center\"><xsl:value-of select=\"@rollNum\" /></td>");
									pw.println("<td align=\"center\"><input type=\"button\" value=\"{AllAttendanceEntriesCount}\" onClick=\"alert(formatAndGetExtraDetails('AllAttendanceEntriesCount', '{AllAttendanceEntries}'))\" /></td>");
									pw.println("<td align=\"center\"><input type=\"button\" value=\"{PresenteeCount}\" onClick=\"alert(formatAndGetExtraDetails('PresenteeCount', '{PresentDays}'))\" /></td>");
									pw.println("<td align=\"center\"><input type=\"button\" value=\"{AbsenteeCount}\" onClick=\"alert(formatAndGetExtraDetails('AbsenteeCount', '{AbsentDays}'))\" /></td>");
									pw.println("<td align=\"center\"><input type=\"button\" value=\"{MedicalLeavesCount}\" onClick=\"alert(formatAndGetExtraDetails('MedicalLeavesCount', '{MedicalLeavesDays}'))\" /></td>");
									pw.println("<td align=\"center\"><input type=\"button\" value=\"{OtherLeavesCount}\" onClick=\"alert(formatAndGetExtraDetails('OtherLeavesCount', '{OtherLeavesDays}'))\" /></td>");
									pw.println("<td align=\"center\"><input type=\"button\" value=\"{TotalPresenteeCount}\" onClick=\"alert(formatAndGetExtraDetails('TotalPresenteeCount', '{TotalPresentDays}'))\" /></td>");
									pw.println("<td align=\"center\"><input type=\"button\" value=\"{TotalAbsenteeCount}\" onClick=\"alert(formatAndGetExtraDetails('TotalAbsenteeCount', '{TotalAbsentDays}'))\" /></td>");
									pw.println("<td align=\"center\"><xsl:value-of select=\"AttendanceAbsenteeCount\" /></td>");
									pw.println("<td align=\"center\"><xsl:value-of select=\"AttendancePresenteeCount\" /></td>");
									pw.println("<td align=\"center\"><xsl:value-of select=\"AttendancePercentage\" /></td>");
									pw.println("</tr>");
									pw.println("</xsl:for-each>");
									pw.println("</table>");
									
									pw.println("</body>");
									pw.println("</html>");
									pw.println("</xsl:template>");
									pw.println("</xsl:stylesheet>");
									pw.close();
									
									//Attendance Results Text File Writing(Only Roll Numbers of defaulters and critical defaulters)
									pw = new PrintWriter(defaultersOutputTextFile);
									pw.println("Defaulters and Critical Defaulters Properties: ");
									pw.println("\tConsidered Lecture Times:\t\t"+!jchkboxDontConsiderLectureTimes.isSelected());
									if(!jchkboxDontConsiderLectureTimes.isSelected())
									{
										pw.println("\t\tConsidered Lecture Start Time:\t"+Preset.timeUserDisplayFormat.format(jSpinnerLectureStartTime.getValue()));
										pw.println("\t\tConsidered Lecture End Time:\t"+Preset.timeUserDisplayFormat.format(jSpinnerLectureEndTime.getValue()));
									}
									pw.println("\tRoll Nums Range:\t\t\t"+(jchkboxSelectAllRollNums.isSelected()?jchkboxSelectAllRollNums.getText().substring(jchkboxSelectAllRollNums.getText().indexOf("ALL")):jtxtFieldGetSpecificRollNums.getText()));
									pw.println("\tMedical Leaves Considerations:\t\t"+(jchkBoxNeedCustomMedicalConsideration.isSelected()?medicalReasonConsiderAsAbsentee.toString():"ALL "+(jchkboxConsiderAllMedicalLeavesAsAbsentees.isSelected()?"Absentees":"Presentees")));
									pw.println("\tOther Leaves Considerations:\t\t"+(jchkBoxNeedCustomOtherReasonsConsideration.isSelected()?otherReasonConsiderAsAbsentee.toString():"ALL "+(jchkboxConsiderAllOtherLeavesAsAbsentees.isSelected()?"Absentees":"Presentees")));
									pw.println("\tDefaulters Threshold:\t\t\t"+defaultersThreshold);
									pw.println("\tCritical Defaulters Threshold:\t\t"+criticalDefaultersThreshold);
									
									StringBuffer defaultersRollNumsAsString = new StringBuffer(), criticalDefaultersRollNumsAsString = new StringBuffer();
									DecimalFormat roundUPTo2Precision = new DecimalFormat("###.##");
									if(jchkboxSelectAllRollNums.isSelected())
										for(int i=1;i<=selectedPresetsNumStudents;i++)
										{
											double attendancePercentage = ((double)attendanceResultCounts[i][6])/((double)totalLectureCount)*100.00;
											if(attendancePercentage<75 && attendancePercentage>=50)
												defaultersRollNumsAsString.append(i+"("+roundUPTo2Precision.format(attendancePercentage)+"), ");
											else if(attendancePercentage<50) 
												criticalDefaultersRollNumsAsString.append(i+"("+roundUPTo2Precision.format(attendancePercentage)+"), ");
										}
									else
										for(int i=0;i<enteredRollNums.length;i++)
										{
											int rollNum = Integer.parseInt(enteredRollNums[i]);
											double attendancePercentage = ((double)attendanceResultCounts[rollNum][6])/((double)totalLectureCount)*100.00;
											if(attendancePercentage<75 && attendancePercentage>=50)
												defaultersRollNumsAsString.append(rollNum+"("+roundUPTo2Precision.format(attendancePercentage)+"), ");
											else if(attendancePercentage<50) 
												criticalDefaultersRollNumsAsString.append(rollNum+"("+roundUPTo2Precision.format(attendancePercentage)+"), ");
										}
									pw.println("Defaulters:\t\t"+(defaultersRollNumsAsString.length()>0?defaultersRollNumsAsString.substring(0,  defaultersRollNumsAsString.length()-2):"None"));
									pw.println("Critical Defaulters:\t"+(criticalDefaultersRollNumsAsString.length()>0?criticalDefaultersRollNumsAsString.substring(0,  criticalDefaultersRollNumsAsString.length()-2):"None"));
									pw.close();
									
									AndroidLikeToast.makeText(DefaultersCalculator.this, "Succesfully calculated defaulters and saved it in XML+XSL (Detailed) and Text (Crisp) Formats", AndroidLikeToast.LENGTH_LONG, Style.SUCCESS).display();
									
									new GotoDefaultersInBrowserDialog(DefaultersCalculator.this, "View Results", defaultersOutputXMLFile);
								}catch(Exception e)
								{
									AndroidLikeToast.makeText(DefaultersCalculator.this, "Exception occured: "+e.getMessage(), AndroidLikeToast.LENGTH_LONG, Style.SUCCESS).display();
									e.printStackTrace();
								}
							}
						}catch (SQLException e)
						{
							logger.error(tempLogMsg += "Execution Failed!");
							e.printStackTrace();
							AndroidLikeToast.makeText(DefaultersCalculator.this, "An exception occured, retry!", AndroidLikeToast.LENGTH_SHORT, Style.ERROR).display();
						}
					}
				}
			}
		}
	}
	class GotoDefaultersInBrowserDialog extends JDialog
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 2377034673948127599L;
		JButton jbtnViewInBrowser;
		JCheckBox jchkBoxNeedSMS;
		
		public GotoDefaultersInBrowserDialog(JFrame parentFrame, String title, final File xmlFileToOpen)
		{
			super(parentFrame, title);
			this.setModal(true);
			this.setLayout(new FlowLayout(FlowLayout.CENTER));
			this.setSize(320, 100);
			this.setResizable(true);
			this.setLocationRelativeTo(null);
			this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			
			this.add(jbtnViewInBrowser = new JButton("View in Browser"));
			this.add(jchkBoxNeedSMS = new JCheckBox("With Free SMS"));
			
			jbtnViewInBrowser.addActionListener(new ActionListener()
			{	
				@Override
				public void actionPerformed(ActionEvent ae)
				{
					try
					{
						if(jchkBoxNeedSMS.isSelected())
						{
							//Open PHP page that parses XML and shows send SMS buttons
						}else
							BareBonesBrowserLaunch.openURL(xmlFileToOpen.toURI().toString());
						GotoDefaultersInBrowserDialog.this.dispose();
					}catch(Exception e)
					{
						AndroidLikeToast.makeText(DefaultersCalculator.this, "Error occured while opening in browser .. please goto DATA directory and open manually", AndroidLikeToast.LENGTH_SHORT, Style.ERROR).display();
						e.printStackTrace();
					}
				}
			});
			this.setVisible(true);
		}
	}
	class JDialogCustomMedicalAndOtherLeaveConsiderations extends JDialog implements ActionListener
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 8395107111903012204L;
		
		JLabel jlblReasons[];
		JPanel jpConsiderationParams;
		GridLayout gridLayout = new GridLayout(0, 2, 5, 5);
		JCheckBox jchkBoxAreAbsentees[];
		JButton jbtnSaveConsideration, jbtnCancel;
		boolean showingForMedicalReasons;
		
		public JDialogCustomMedicalAndOtherLeaveConsiderations(JFrame parentFrame, String title, boolean showingForMedicalReasons)
		{
			super(parentFrame, title);
			this.setModal(true);
			this.setLayout(new BorderLayout());
			this.setSize(500, 400);
			this.setResizable(true);
			this.setLocationRelativeTo(null);
			this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			
			this.showingForMedicalReasons = showingForMedicalReasons;
			
			LinkedHashMap<String, Boolean> oldValues = new LinkedHashMap<String, Boolean>();
			if(showingForMedicalReasons)
				for(Map.Entry<String, Boolean> entry:medicalReasonConsiderAsAbsentee.entrySet())
					oldValues.put(entry.getKey(), entry.getValue());
			else
				for(Map.Entry<String, Boolean> entry:otherReasonConsiderAsAbsentee.entrySet())
					oldValues.put(entry.getKey(), entry.getValue());
			if(showingForMedicalReasons)
				medicalReasonConsiderAsAbsentee.clear();
			else
				otherReasonConsiderAsAbsentee.clear();
			
			jpConsiderationParams = new JPanel(gridLayout);
			
			StringBuffer queryGetAllReasons = new StringBuffer("select * from "+jcomboBoxSelectPresetTableName.getSelectedItem().toString()+" where ");
			for(int i=1;i<=selectedPresetsNumStudents;i++)
				queryGetAllReasons.append("`"+i+"` like "+(showingForMedicalReasons?"'M(%)' OR ":"'O(%)' OR "));
			queryGetAllReasons = new StringBuffer(queryGetAllReasons.substring(0, queryGetAllReasons.length()-4));				//remove off last " OR "
			
			//System.out.print("[ "+edu.vesit.ams.AMSServer.logEventTimestampFormat.format(System.currentTimeMillis())+" - DBHelper ] Executing query("+queryGetAllReasons+") to get all "+(showingForMedicalReasons?"Medical":"Other")+" reasons ....");
			tempLogMsg = "Executing query("+queryGetAllReasons+") to get all "+(showingForMedicalReasons?"Medical":"Other")+" reasons ....";
			
			try
			{
				rs = stmt.executeQuery(queryGetAllReasons.toString());
				while(rs.next())
					for(int i=1;i<=selectedPresetsNumStudents;i++)
					{
						String columnValue = rs.getString(Integer.toString(i));
						if(showingForMedicalReasons)
						{
							if(columnValue.startsWith("M("))
								medicalReasonConsiderAsAbsentee.put(columnValue.substring(columnValue.indexOf('(')+1, columnValue.lastIndexOf(')')), false);
						}else
						{
							if(columnValue.startsWith("O("))
								otherReasonConsiderAsAbsentee.put(columnValue.substring(columnValue.indexOf('(')+1, columnValue.lastIndexOf(')')), false);
						}
					}
				
				//restoring old key's = checked values
				if(showingForMedicalReasons)
				{
					for(Map.Entry<String, Boolean> entry:medicalReasonConsiderAsAbsentee.entrySet())
						if(oldValues.containsKey(entry.getKey()))
							medicalReasonConsiderAsAbsentee.put(entry.getKey(), oldValues.get(entry.getKey()));		//overwrite old checked value over new 1
				}else
				{
					for(Map.Entry<String, Boolean> entry:otherReasonConsiderAsAbsentee.entrySet())
						if(oldValues.containsKey(entry.getKey().toString()))
							otherReasonConsiderAsAbsentee.put(entry.getKey(), oldValues.get(entry.getKey()));		//overwrite old checked value over new 1
				}
				
				if(showingForMedicalReasons)
				{
					if(medicalReasonConsiderAsAbsentee.size()==0)
					{
						jpConsiderationParams.add(new JLabel("Result: "));
						jpConsiderationParams.add(new JLabel("No medical leaves for this preset"));
					}else
					{
						int index = 0;
						jlblReasons = new JLabel[medicalReasonConsiderAsAbsentee.size()];
						jchkBoxAreAbsentees = new JCheckBox[medicalReasonConsiderAsAbsentee.size()];
						jpConsiderationParams.add(new JLabel("Parameter"));
						jpConsiderationParams.add(new JLabel("Value(Check to consider as absentee)"));
						for(Map.Entry<String, Boolean> entry:medicalReasonConsiderAsAbsentee.entrySet())
						{
							jpConsiderationParams.add(jlblReasons[index] = new JLabel(entry.getKey().toString()));
							jpConsiderationParams.add(jchkBoxAreAbsentees[index] = new JCheckBox());
							jchkBoxAreAbsentees[index].setSelected(entry.getValue());								//this also restores old values of checked fields
							index++;
						}
					}
				}else
				{
					if(otherReasonConsiderAsAbsentee.size()==0)
					{
						jpConsiderationParams.add(new JLabel("Result: "));
						jpConsiderationParams.add(new JLabel("No medical leaves for this preset"));
					}else
					{
						int index = 0;
						jlblReasons = new JLabel[otherReasonConsiderAsAbsentee.size()];
						jchkBoxAreAbsentees = new JCheckBox[otherReasonConsiderAsAbsentee.size()];
						jpConsiderationParams.add(new JLabel("Parameter"));
						jpConsiderationParams.add(new JLabel("Value"));
						for(Map.Entry<String, Boolean> entry:otherReasonConsiderAsAbsentee.entrySet())
						{
							jpConsiderationParams.add(jlblReasons[index] = new JLabel(entry.getKey().toString()));
							jpConsiderationParams.add(jchkBoxAreAbsentees[index++] = new JCheckBox());
							jchkBoxAreAbsentees[index].setSelected(entry.getValue());								//this also restores old values of checked fields
							index++;
						}
					}
				}
				logger.debug(tempLogMsg += "Execution Success!");
			}catch (SQLException e)
			{
				e.printStackTrace();
				logger.error(tempLogMsg += "Execution Failed!");
			}
			jpConsiderationParams.add(jbtnSaveConsideration = new JButton("Save"));
			jpConsiderationParams.add(jbtnCancel = new JButton("Cancel"));
			jbtnSaveConsideration.addActionListener(this);
			jbtnCancel.addActionListener(this);
			
			this.add(new JScrollPane(jpConsiderationParams), BorderLayout.CENTER);
			this.setVisible(true);
			this.pack();
		}

		@Override
		public void actionPerformed(ActionEvent ae)
		{
			if(ae.getSource().equals(jbtnSaveConsideration))
			{
				if(showingForMedicalReasons)
				{
					for(int i=0;i<medicalReasonConsiderAsAbsentee.size();i++)
						medicalReasonConsiderAsAbsentee.put(jlblReasons[i].getText(), jchkBoxAreAbsentees[i].isSelected());		//overwrite value true for that key(medical reason)
				}else
				{
					for(int i=0;i<otherReasonConsiderAsAbsentee.size();i++)
						otherReasonConsiderAsAbsentee.put(jlblReasons[i].getText(), jchkBoxAreAbsentees[i].isSelected());		//overwrite value true for that key(other reason)
				}
				AndroidLikeToast.makeText((JFrame) this.getParent(), "Saved Successfully", Style.SUCCESS).display();
			}
			//For both cancel and save button dispose dialog
			this.dispose();
		}
	}
}
